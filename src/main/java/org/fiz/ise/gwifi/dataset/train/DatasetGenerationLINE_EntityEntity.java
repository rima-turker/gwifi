package org.fiz.ise.gwifi.dataset.train;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.util.AtomicCounter;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.util.PageIterator;
@Deprecated
public class DatasetGenerationLINE_EntityEntity {
	private static final Logger LOG = Logger.getLogger(DatasetGenerationLINE_EntityEntity.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger thirdLOG = Logger.getLogger("reportsLogger");

	private Wikipedia wikipedia = null;
	private ExecutorService executor;
	private static AtomicCounter countArticle;
	private static SynchronizedCounter countCategoryPerArticle;
	private static AtomicCounter countNullArticle;
	private Map<Article, Map<Article, Integer>> mapEntityEntity;
	private Map<Article, List<Article>> mapListEntityEntity;


	public static void main(String[] args) {
		DatasetGenerationLINE_EntityEntity data = new DatasetGenerationLINE_EntityEntity();
		data.initializeVariables();
		data.generateDatasetEntityEntiy_parallel();
		//data.writeDatasetToFile_list();
	}
	
	private  void writeDatasetToFile_list(List<Article> id) {
			Map<Article, Long> map =
					id.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
			for(Entry<Article, Long> count : map.entrySet())
			{
				//secondLOG.info(count.getKey().getTitle()+"\t"+count.getKey().getTitle()+"\t"+count.getValue());
				thirdLOG.info(count.getKey().getId()+"\t"+count.getKey().getId()+"\t"+count.getValue());
			}
			//			for(Article article: list){	
			//				secondLOG.info(mainArticle.getTitle()+"\t"+article.getTitle()+"\t"+1);
			//				thirdLOG.info(mainArticle.getId()+"\t"+article.getId()+"\t"+1);
			//			}
	}

//	private  void writeDatasetToFile_list() {
//		System.out.println("Writing to a file..");
//		for(Entry<Article, List<Article>> entry:mapListEntityEntity.entrySet()){
//			Article mainArticle = entry.getKey();
//
//			List<Article> list = new ArrayList<>(entry.getValue());
//			Map<Article, Long> map =
//					list.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
//			for(Entry<Article, Long> count : map.entrySet())
//			{
//				secondLOG.info(mainArticle.getTitle()+"\t"+count.getKey().getTitle()+"\t"+count.getValue());
//				thirdLOG.info(mainArticle.getId()+"\t"+count.getKey().getId()+"\t"+count.getValue());
//			}
//			//			for(Article article: list){	
//			//				secondLOG.info(mainArticle.getTitle()+"\t"+article.getTitle()+"\t"+1);
//			//				thirdLOG.info(mainArticle.getId()+"\t"+article.getId()+"\t"+1);
//			//			}
//		}
//	}
	private  void writeDatasetToFile() {
		for(Entry<Article, Map<Article, Integer>> entry:mapEntityEntity.entrySet()){
			Article article = entry.getKey();
			Map<Article, Integer> mapValue = new HashMap<>(entry.getValue());
			for(Entry<Article, Integer> insideEntry: mapValue.entrySet()){
				secondLOG.info(article.getTitle()+"\t"+insideEntry.getKey().getTitle()+"\t"+insideEntry.getValue());
				thirdLOG.info(article.getId()+"\t"+insideEntry.getKey().getId()+"\t"+insideEntry.getValue());
			}
		}
	}
	private void initializeVariables() {
		countNullArticle = new AtomicCounter();
		countArticle = new AtomicCounter();
		countCategoryPerArticle = new SynchronizedCounter();
		mapEntityEntity = new HashMap<>();
		mapListEntityEntity = new HashMap<>();
		File dir = new File("all");
		dir.mkdir();
	}

	void generateDatasetEntityEntiy_parallel() {
		int NUMBER_OF_THREADS = 1;
		try {
			final long now = System.currentTimeMillis();
			final File databaseDirectory = new File("configs/wikipedia-template-en.xml");
			wikipedia = new Wikipedia(databaseDirectory, false);
			System.out.println("The Wikipedia environment has been initialized.");
			PageIterator pageIterator = wikipedia.getPageIterator();
			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			while (pageIterator.hasNext()) {
				Page page = pageIterator.next();
				//1001917 1001919
				System.out.println(wikipedia.getArticleById(1001919)+" "+wikipedia.getArticleById(1001917));
				
				if (page.getType().equals(PageType.article)) {
					Article article = getArticle(page.getTitle());
					if (article != null) {
						// executor.execute(EntityEntity(article));
						//findWeightOfEachEntity(article.getLinksOut());
						executor.execute(EntityEntity(article));
					} else {
						countNullArticle.increment();
					}
				}
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("We have " + countCategoryPerArticle.value() + " categories per article");
			System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private Runnable EntityEntity(Article article) {
		return () -> {
			writeEachArticleSeparetely(article,article.getLinksOut());
			countArticle.increment();
			System.out.println("number of article processed "+ countArticle.value());
		};
	}
	private void writeEachWeight(Article[] articles) { 
		try {
			for (int i = 0; i < articles.length; i++) {
				List<Article> oldList = new ArrayList<>(Arrays.asList(articles));
				if (mapListEntityEntity.containsKey(articles[i])) {
					oldList.addAll(mapListEntityEntity.get(articles[i]));
				}
				mapListEntityEntity.put(articles[i], oldList);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void writeEachArticleSeparetely(Article mainArticle,Article[] articles) { 
		try {
			List<String> id = new ArrayList<>();
			for (int i = 0; i < articles.length; i++) {
				for (int j = i+1; j < articles.length; j++) {
					//name.add((mainArticle.getTitle()+"\t"+articles[j].getTitle()+"\t"+1));
					id.add((articles[i].getId()+"\t"+articles[j].getId()+"\t"+1));
				}
			}
//			File file=new File();
			//FileUtil.writeDataToFile(id,new File("all/"+String.valueOf(mainArticle.getId())) );
//			writeDatasetToFile_list(id);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//	private void writeEachWeight(Article[] articles) { // I am aweare that we also add entity itself the cell of e1 and e1
	//		try {
	//			for (int i = 0; i < articles.length; i++) {
	//				Article mainArticle = articles[i];
	//				for (int j = i+1; j < articles.length; j++) {
	//					//secondLOG.info(mainArticle.getTitle()+"\t"+articles[j].getTitle()+"\t"+1);
	//					thirdLOG.info(mainArticle.getId()+"\t"+articles[j].getId()+"\t"+1);
	//				}
	//			}
	//			
	//		} catch (Exception e) {
	//			e.printStackTrace();
	//		}
	//	}

	private void findWeightOfEachEntity(Article[] articles) { // I am aweare that we also add entitiy itself the cell of
		// e1 and e1
		try {
			for (int i = 0; i < articles.length; i++) {
				Article mainArticle = articles[i];
				if (mapEntityEntity.get(mainArticle) == null) {
					Map<Article, Integer> single = new HashMap<>();
					for (int j = 0; j < articles.length; j++) {
						single.put(articles[j], 1);
					}
					mapEntityEntity.put(mainArticle, single);
					countArticle.increment();
					System.out.println("Count of the pages processed "+countArticle.value());
				} else {
					Map<Article, Integer> mapMain = new HashMap<>(mapEntityEntity.get(mainArticle));
					for (int j = 0; j < articles.length; j++) {
						if (mapMain.containsKey(articles[j])) {
							Integer key = mapMain.get(articles[j]);
							key += 1;
							mapMain.put(articles[j], key);
						} else {
							mapMain.put(articles[j], 1);
						}
					}
					mapEntityEntity.put(mainArticle, mapMain);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public  Article getArticle(String title) {
		if (wikipedia.getArticleByTitle(title) == null) {
			System.out.println("Could not find exact match of an article for a given title " + title);
			return null;
		}
		return wikipedia.getArticleByTitle(title);
	}
}
