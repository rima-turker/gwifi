package org.fiz.ise.gwifi.dataset;

import java.awt.List;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.lf5.viewer.categoryexplorer.CategoryPath;
import org.fiz.ise.gwifi.util.FileUtil;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.util.PageIterator;
import weka.gui.SysErrLog;

public class DatasetGenerationLINE {


	private Wikipedia wikipedia=null;
	private static ArrayList<String> listEntityCategory;
	public static void main(String[] args) {
		listEntityCategory = new ArrayList<String>();
		DatasetGenerationLINE data =new DatasetGenerationLINE();
		data.generateDatasetForEntityCategory();
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
			Map<Category, Set<Article>> mapCatsAndItsArticles = new HashMap<>();

			while (pageIterator.hasNext()) {
				Page page = pageIterator.next();
				if (page.getType().equals(PageType.article)) {
					Article cArticle = getArticle(page.getTitle()); //example: Anarchism
					//System.err.println(cArticle);
					Category[] categoriesCArticle = cArticle.getParentCategories(); //Category of Anarchism Get all the categories at the bottom of the article
					for (int j = 0; j < categoriesCArticle.length; j++) {
						if (!mapCatsAndItsArticles.containsKey(categoriesCArticle[j])) {
							Set<Article> childArticlesSet = new HashSet<Article>(Arrays.asList(categoriesCArticle[j].getChildArticles()));
							mapCatsAndItsArticles.put(categoriesCArticle[j], childArticlesSet);
						}
					}
					Set<Article> setCArticleLinkOutLinkIn = new HashSet<Article>();
					Article[] linkOutMainArticle = cArticle.getLinksOut(); //All the entities inside the Anarchism article such as Agriculture
					for (int j = 0; j < linkOutMainArticle.length; j++) {
						Article[] linksOutLinkInAnArticle = linkOutMainArticle[j].getLinksIn(); //All the entities contains AgreeCulture in their article
						//System.out.println(Arrays.toString(linksOutLinkInAnArticle)); 
						Collections.addAll(setCArticleLinkOutLinkIn, linksOutLinkInAnArticle);
						//setCArticleLinkInLinkIn.forEach(System.out::println);
					}
					findCommonCategories(cArticle,categoriesCArticle,mapCatsAndItsArticles,setCArticleLinkOutLinkIn);
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
	private void findCommonCategories(final Article article,final Category[] categoriesCArticle,final Map<Category,  Set<Article>>  mapCatsAndItsArticles,
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
			listEntityCategory.add(article+"\t"+entry.getValue()+"\t"+entry.getKey());
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
