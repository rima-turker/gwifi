package org.fiz.ise.gwifi.dataset;

import java.awt.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.util.AtomicCounter;
import org.fiz.ise.gwifi.util.FileUtil;
import org.xml.sax.SAXException;

import com.hankcs.hanlp.dependency.nnparser.util.Log;
import com.sleepycat.je.EnvironmentLockedException;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.util.PageIterator;

import weka.gui.SysErrLog;

public class DatasetGenerationLINE {

	private static final Logger LOG = Logger.getLogger(DatasetGenerationLINE.class);
	static final Logger secondLOD = Logger.getLogger("debugLogger");
	private Wikipedia wikipedia=null;
	private static ArrayList<String> listEntityCategory;
	private static ExecutorService executor;
	private static AtomicCounter countArticle;
	
	public static void main(String[] args) {
		LOG.info("rima");
		secondLOD.info("rima");
//		listEntityCategory = new ArrayList<String>();
//		countArticle = new AtomicCounter();
//		DatasetGenerationLINE data =new DatasetGenerationLINE();
//		data.generateDatasetEntityCategory_parallel();
	}
	
	void generateDatasetEntityCategory_parallel() {
		int NUMBER_OF_THREADS = 55;
		try {
			final long now = System.currentTimeMillis();
			final File databaseDirectory= new File("configs/wikipedia-template-en.xml");
			wikipedia = new Wikipedia(databaseDirectory, false);
			System.out.println("The Wikipedia environment has been initialized.");
			PageIterator pageIterator = wikipedia.getPageIterator();
			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			while (pageIterator.hasNext()) {
				Page page = pageIterator.next();
				if (page.getType().equals(PageType.article)) {
					executor.submit(handle(getArticle(page.getTitle())));
				}
				countArticle.increment();
				System.out.println("number of article processed "+ countArticle.value());
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			//System.out.println("We have "+i+" article processed");
			System.out.println("Total time minutes "+TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()-now));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	private Runnable handle(Article article) {
		return () -> {
			try {
				Category[] categoriesCArticle = article.getParentCategories(); //Category of Anarchism Get all the categories at the bottom of the article
				Set<Article> setCArticleLinkOutLinkIn = new HashSet<Article>();
				Article[] linkOutMainArticle = article.getLinksOut(); //All the entities inside the Anarchism article such as Agriculture
				for (int j = 0; j < linkOutMainArticle.length; j++) {
					Article[] linksOutLinkInAnArticle = linkOutMainArticle[j].getLinksIn(); //All the entities contains AgreeCulture in their article
					Collections.addAll(setCArticleLinkOutLinkIn, linksOutLinkInAnArticle);
				}
				findCommonCategories(article,categoriesCArticle,setCArticleLinkOutLinkIn);
				//System.out.println("numberOfEntitiesProcessed " +numberOfEntitiesProcessed);
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
	}
	public void generateDatasetForEntityCategory() {

		try {
			int numberOfEntitiesProcessed =0;
			final long now = System.currentTimeMillis();
			final File databaseDirectory= new File("configs/wikipedia-template-en.xml");
			wikipedia = new Wikipedia(databaseDirectory, false);
			System.out.println("The Wikipedia environment has been initialized.");
			PageIterator pageIterator = wikipedia.getPageIterator();
			int i = 0;
			//Map<Category, Set<Article>> mapCatsAndItsArticles = new HashMap<>();

			while (pageIterator.hasNext()) {
				Page page = pageIterator.next();
				if (page.getType().equals(PageType.article)) {
					Article cArticle = getArticle(page.getTitle()); //example: Anarchism
					//System.err.println(cArticle);
					Category[] categoriesCArticle = cArticle.getParentCategories(); //Category of Anarchism Get all the categories at the bottom of the article
					for (int j = 0; j < categoriesCArticle.length; j++) {
//						if (!mapCatsAndItsArticles.containsKey(categoriesCArticle[j])) {
//							Set<Article> childArticlesSet = new HashSet<Article>(Arrays.asList(categoriesCArticle[j].getChildArticles()));
//							mapCatsAndItsArticles.put(categoriesCArticle[j], childArticlesSet);
//						}
					}
					Set<Article> setCArticleLinkOutLinkIn = new HashSet<Article>();
					Article[] linkOutMainArticle = cArticle.getLinksOut(); //All the entities inside the Anarchism article such as Agriculture
					for (int j = 0; j < linkOutMainArticle.length; j++) {
						Article[] linksOutLinkInAnArticle = linkOutMainArticle[j].getLinksIn(); //All the entities contains AgreeCulture in their article
						//System.out.println(Arrays.toString(linksOutLinkInAnArticle)); 
						Collections.addAll(setCArticleLinkOutLinkIn, linksOutLinkInAnArticle);
						//setCArticleLinkInLinkIn.forEach(System.out::println);
					}
					findCommonCategories(cArticle,categoriesCArticle,setCArticleLinkOutLinkIn);
					numberOfEntitiesProcessed+=1;
					System.out.println("numberOfEntitiesProcessed " +numberOfEntitiesProcessed);
				}	
			}	
			System.out.println("We have "+i+" article processed");
			System.out.println("The size of the list is "+listEntityCategory.size());
			FileUtil.writeDataToFile(listEntityCategory, new File("listEntityCategory"));
			System.out.println("Total time minutes "+TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()-now));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}


	}
	private void findCommonCategories(final Article article,final Category[] categoriesCArticle,
			final Set<Article> setCArticleLinkInLinkIn) {
		Map<Category, Integer> mapCatVal = new HashMap<>();
		for (int i = 0; i < categoriesCArticle.length; i++) {
			Set<Article> childArticlesSet = new HashSet<Article>(Arrays.asList(categoriesCArticle[i].getChildArticles()));
			for(Article art : setCArticleLinkInLinkIn)
			{
				if (childArticlesSet.contains(art)) {
					if (mapCatVal.containsKey(categoriesCArticle[i])) {
						Integer temp = mapCatVal.get(categoriesCArticle[i]);
						temp +=1;
						mapCatVal.put(categoriesCArticle[i], temp);
					}
					else
					{
						mapCatVal.put(categoriesCArticle[i], 1);
					}
				}
			}
		}
		for(Entry<Category, Integer>entry:mapCatVal.entrySet())
		{
			LOG.info(article+"\t"+entry.getValue()+"\t"+entry.getKey());
			//listEntityCategory.add(article+"\t"+entry.getValue()+"\t"+entry.getKey());
		}
		//System.out.println("Size of the categories "+categoriesCArticle.length+" Size of the list "+listEntityCategory.size());
	}



	public Article getArticle(String title) {

		Article article = wikipedia.getArticleByTitle(title);
		if (article == null) {
			System.out.println("Could not find exact match of an article for a given title " +title);
			System.exit(1);;
		}
		return article;
	}
	private double calculateWeightValueEntityEntity() {
		double weight=0.0;

		return weight;

	}
}
