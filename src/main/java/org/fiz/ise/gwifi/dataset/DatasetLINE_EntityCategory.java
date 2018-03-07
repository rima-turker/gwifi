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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.util.AtomicCounter;
import org.fiz.ise.gwifi.util.SynchronizedCounter;
import org.fiz.ise.gwifi.util.FileUtil;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.util.PageIterator;

public class DatasetLINE_EntityCategory {
	private static final Logger LOG = Logger.getLogger(DatasetLINE_EntityCategory.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger thirdLOG = Logger.getLogger("reportsLogger");

	private Wikipedia wikipedia=null;
	private static ExecutorService executor;
	private static AtomicCounter countArticle;
	private static SynchronizedCounter countCategoryPerArticle;
	private static AtomicCounter countNullArticle;
/*
 * This class is responsible of creating a dataset for LINE algorithm
 * Dataset contains entity category weight
 */
	public static void main(String[] args) {
		countNullArticle= new AtomicCounter();
		countArticle = new AtomicCounter();
		countCategoryPerArticle = new SynchronizedCounter();
		DatasetLINE_EntityCategory data =new DatasetLINE_EntityCategory();
		data.generateDatasetEntityCategory_parallel();
		//data.generateDatasetForEntityCategory();
	}
	/*
	 * iterate over all the wikipedia pages (articles only)
	 * Then send it to Entity category Function
	 */
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
					Article article  =  getArticle(page.getTitle());
					if (article!=null) {
						executor.execute(EntityCategory(article));						
					}
					else{
						countNullArticle.increment();
					}
				}
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("We have "+countCategoryPerArticle.value()+" categories per article");
			System.out.println("Total time minutes "+TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()-now));
			System.out.println("Total number of null articles "+countNullArticle.value());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	/*
	 * Calls the general function for each article 
	 */
	private Runnable EntityCategory(Article article) {
		return () -> {
			findWeightOfEachCategory(article);
			countArticle.increment();
			System.out.println("number of article processed "+ countArticle.value());
		};
	}
	
	/*
	 * for a given article first entities that in the article 
	 * then find the articles that contains those entities 
	 * send those articles to calculateWeightForEntityCategory
	 */
	private void findWeightOfEachCategory(Article article) {
		try {

			Set<Article> setCArticleLinkOutLinkIn = new HashSet<Article>();
			Article[] linkOutMainArticle = article.getLinksOut(); //All the entities inside the main (Anarchism) article such as Agriculture
			for (int j = 0; j < linkOutMainArticle.length; j++) {
				Article[] linksOutLinkInAnArticle = linkOutMainArticle[j].getLinksIn(); //All the entities contains Agriculture in their article
				Collections.addAll(setCArticleLinkOutLinkIn, linksOutLinkInAnArticle);
			}
			calculateWeightForEntityCategory(article,setCArticleLinkOutLinkIn);
			//System.out.println("numberOfEntitiesProcessed " +numberOfEntitiesProcessed);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
//	public void generateDatasetForEntityCategory() {
//		try {
//			int numberOfEntitiesProcessed =0;
//			final long now = System.currentTimeMillis();
//			final File databaseDirectory= new File("configs/wikipedia-template-en.xml");
//			wikipedia = new Wikipedia(databaseDirectory, false);
//			System.out.println("The Wikipedia environment has been initialized.");
//			PageIterator pageIterator = wikipedia.getPageIterator();
//			int i = 0;
//			while (pageIterator.hasNext()) {
//				Page page = pageIterator.next();
//				if (page.getType().equals(PageType.article)) {
//					//					Article cArticle = getArticle(page.getTitle()); //example: Anarchism
//					//					Category[] categoriesCArticle = cArticle.getParentCategories(); //Category of Anarchism Get all the categories at the bottom of the article
//					//					Set<Article> setCArticleLinkOutLinkIn = new HashSet<Article>();
//					//					Article[] linkOutMainArticle = cArticle.getLinksOut(); //All the entities inside the Anarchism article such as Agriculture
//					//					for (int j = 0; j < linkOutMainArticle.length; j++) {
//					//						Article[] linksOutLinkInAnArticle = linkOutMainArticle[j].getLinksIn(); //All the entities contains AgreeCulture in their article
//					//						Collections.addAll(setCArticleLinkOutLinkIn, linksOutLinkInAnArticle);
//					//					}
//					//					findCommonCategories(cArticle,categoriesCArticle,setCArticleLinkOutLinkIn);
//					//					numberOfEntitiesProcessed+=1;
//					//					System.out.println("numberOfEntitiesProcessed " +numberOfEntitiesProcessed);
//					findWeightOfEachCategory(getArticle(page.getTitle()));
//					System.out.println("numberOfEntitiesProcessed " +numberOfEntitiesProcessed);
//					numberOfEntitiesProcessed+=1;
//				}	
//			}	
//			System.out.println("We have "+i+" article processed");
//			System.out.println("The size of the list is "+listEntityCategory.size());
//			FileUtil.writeDataToFile(listEntityCategory, new File("listEntityCategory"));
//			System.out.println("Total time minutes "+TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()-now));
//		} catch (Exception e) {
//			System.out.println(e.getMessage());
//		}
//	}
	/*
	 * In this function the intersection of category articles and article is calculated as a weight values
	 */
	private void calculateWeightForEntityCategory(final Article article,
			final Set<Article> setCArticleLinkOutLinkIn) {
		
		Category[] categoriesCArticle = article.getParentCategories(); //(Category of Anarchism) Get all the categories at the bottom of the article
		countCategoryPerArticle.incrementbyValue(categoriesCArticle.length);
		Map<Category, Integer> mapCatVal = new HashMap<>();
		for (int i = 0; i < categoriesCArticle.length; i++) {
			Set<Article> childArticlesSet = new HashSet<Article>(Arrays.asList(categoriesCArticle[i].getChildArticles()));
			childArticlesSet.retainAll(setCArticleLinkOutLinkIn);
			mapCatVal.put(categoriesCArticle[i],childArticlesSet.size());
//			for(Article art : setCArticleLinkOutLinkIn)
//			{
//				if (childArticlesSet.contains(art)) {
//					if (mapCatVal.containsKey(categoriesCArticle[i])) {
//						Integer temp = mapCatVal.get(categoriesCArticle[i]);
//						temp +=1;
//						mapCatVal.put(categoriesCArticle[i], temp);
//					}
//					else{
//						mapCatVal.put(categoriesCArticle[i], 1);
//					}
//				}
//			}
		}
		for(Entry<Category, Integer>entry:mapCatVal.entrySet()){
			secondLOG.info(article.getTitle()+"\t"+entry.getKey().getTitle()+"\t"+entry.getValue());
			thirdLOG.info(article.getId()+"\t"+entry.getKey().getId()+"\t"+entry.getValue());
			//listEntityCategory.add(article+"\t"+entry.getValue()+"\t"+entry.getKey());
		}
	}
	public Article getArticle(String title) {
		Article article = wikipedia.getArticleByTitle(title);
		if (article == null) {
			System.out.println("Could not find exact match of an article for a given title " +title);
			return null;
		}
		return article;
	}

}
