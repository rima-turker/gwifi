package org.fiz.ise.gwifi.dataset.LINE.Category;

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

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.FilteredWikipediaPagesSingleton;
import org.fiz.ise.gwifi.dataset.train.DatasetGeneration_Joint_EntityCategory;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.util.AtomicCounter;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.SynchronizedCounter;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;

public class DatasetGeneration_EntityCategory_CatFiltered {

	private static final Logger LOG = Logger.getLogger(DatasetGeneration_Joint_EntityCategory.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger thirdLOG = Logger.getLogger("reportsLogger");
	private final Integer NUMBER_OF_THREADS= Config.getInt("NUMBER_OF_THREADS",-1);
	private final TestDatasetType_Enum TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private static ExecutorService executor;
	private static AtomicCounter countArticle;
	private static SynchronizedCounter countCategoryPerArticle;
	private Set<Category> setCategory;
	/*
	 * This class is responsible of creating a dataset for LINE algorithm
	 * each line of the Data set is "entity category weight"
	 * weight value is estimated:
	 * iterate over all the wikipedia articles 
	 * 1) for each page get all the categories
	 * 2) get its context entities
	 * 3) and find articles that contains those entities
	 * 4) find the categories of the number 3 and 
	 * 5) and intersection of the  1 and 4
	 *However we consider only categories we are interested such as websinippets categories and
	 *their sub categories in this class it will be 7 depth of the category tree 
	 */
	public static void main(String[] args) {
		countArticle = new AtomicCounter();
		countCategoryPerArticle = new SynchronizedCounter();
		DatasetGeneration_EntityCategory_CatFiltered data =new DatasetGeneration_EntityCategory_CatFiltered();
		data.generateDatasetEntityCategory_parallel();
	}
	/*
	 * iterate over all the wikipedia pages (articles only)
	 * Then send it to Entity category Function
	 */
	void generateDatasetEntityCategory_parallel() {

		try {
			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			final long now = System.currentTimeMillis();
			FilteredWikipediaPagesSingleton singleton = FilteredWikipediaPagesSingleton.getInstance();
			final Set<Article> filteredArticles=Collections.unmodifiableSet(new HashSet<Article>(singleton.categoryFilteredArticles));
			CategorySingleton singletonCategories = CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE));
			setCategory=Collections.unmodifiableSet(new HashSet<Category>(singletonCategories.setAllCategories));
			System.out.println("size of the articles "+filteredArticles.size());
			int i = 1;
			System.out.println("we have "+filteredArticles.size()+" to process");
			for (Article article:filteredArticles) {
				executor.execute(EntityCategory(article));
				i++;
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("We have "+countCategoryPerArticle.value()+" categories per article");
			System.out.println("Total time minutes "+TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()-now));
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
	/*
	 * In this function the intersection of category articles and article is calculated as a weight values
	 */
	private void calculateWeightForEntityCategory(final Article article,
			final Set<Article> setCArticleLinkOutLinkIn) {
		Category[] categoriesCArticle = article.getParentCategories(); //(Category of Anarchism) Get all the categories at the bottom of the article
		Map<Category, Integer> mapCatVal = new HashMap<>();
		for (int i = 0; i < categoriesCArticle.length; i++) {
			if (setCategory.contains(categoriesCArticle[i])) {
				Set<Article> childArticlesSet = new HashSet<Article>(Arrays.asList(categoriesCArticle[i].getChildArticles()));
				childArticlesSet.retainAll(setCArticleLinkOutLinkIn);
				mapCatVal.put(categoriesCArticle[i],childArticlesSet.size());
				countCategoryPerArticle.increment();
			}
		}
		for(Entry<Category, Integer>entry:mapCatVal.entrySet()){
			secondLOG.info(article.getTitle()+"\t"+entry.getKey().getTitle()+"\t"+entry.getValue());
			thirdLOG.info(article.getId()+"\t"+entry.getKey().getId()+"\t"+entry.getValue());
			//listEntityCategory.add(article+"\t"+entry.getValue()+"\t"+entry.getKey());
		}
	}

}
