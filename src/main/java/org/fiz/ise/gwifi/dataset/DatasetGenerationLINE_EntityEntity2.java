package org.fiz.ise.gwifi.dataset;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.util.AtomicCounter;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;
import org.fiz.ise.gwifi.util.TimeUtil;

import TEST.SparseMatrix;
import TEST.SparseMatrix.SparseMatrixNode;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.util.PageIterator;

public class DatasetGenerationLINE_EntityEntity2 {
	private static final Logger LOG = Logger.getLogger(DatasetGenerationLINE_EntityEntity2.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger thirdLOG = Logger.getLogger("reportsLogger");

	private static final String OUTPUT_FOLDER = "NewData";

	private static final int NUMBER_OF_PAGES = 1000;
	
	private Wikipedia wikipedia = null;
	private ExecutorService executor;
	//private static AtomicCounter countArticle;
	private static SynchronizedCounter countCategoryPerArticle;
	private static SynchronizedCounter countArticle;
	private static AtomicCounter countNullArticle;
	private Map<Article, Map<Article, Integer>> mapEntityEntity;
	private Map<Article, List<Article>> mapListEntityEntity;

	private static final List<Map<String,Integer>> listOfMaps = new CopyOnWriteArrayList<Map<String,Integer>>();	

	private static Map<String,Integer> map = new ConcurrentHashMap<>();

	//	public static void main(String[] args) {
	//		for(int i=0;i<SIZE;i++) {
	//			for(int j=0;j<SIZE;j++) {
	//				map.put(i+"\t"+j, i+j);
	//			}
	//		}
	//		
	//		List<String> collect = map.entrySet().parallelStream().map(entry -> new String(entry.getKey()+"\t"+entry.getValue())).collect(Collectors.toList());
	//		
	//		//collect.forEach(rima->System.err.println(rima));
	//
	//	}

	public static void main(String[] args) {
		DatasetGenerationLINE_EntityEntity2 data = new DatasetGenerationLINE_EntityEntity2();
		data.initializeVariables();
		data.generateDatasetEntityEntiy_parallel();
	}
	private void initializeVariables() {
		countNullArticle = new AtomicCounter();
		countArticle = new SynchronizedCounter();
		countCategoryPerArticle = new SynchronizedCounter();
		mapEntityEntity = new HashMap<>();
		mapListEntityEntity = new HashMap<>();
	}

	void generateDatasetEntityEntiy_parallel() {
		final int NUMBER_OF_THREADS = 5;
		FileUtil.createFolder(OUTPUT_FOLDER);
		
		try {
			final long now = System.currentTimeMillis();
			final File databaseDirectory = new File("configs/wikipedia-template-en.xml");
			wikipedia = new Wikipedia(databaseDirectory, false);
			System.out.println("The Wikipedia environment has been initialized.");
			PageIterator pageIterator = wikipedia.getPageIterator();
			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			List<Article> localList = new ArrayList<>();
			int i = 1;
			while (pageIterator.hasNext()) {
				Page page = pageIterator.next();
				if (page.getType().equals(PageType.article)) {
					Article article = getArticle(page.getTitle());
					if(article==null) {
						continue;
					}
					if(localList.size()<NUMBER_OF_PAGES) {
						localList.add(article);
					}else {
						executor.execute(handle(new ArrayList<Article>(localList),i++));
						localList.clear();
						localList.add(article);
					}
				}
			}
			executor.execute(handle(new ArrayList<Article>(localList),i));

			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("We have " + countCategoryPerArticle.value() + " categories per article");
			System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Runnable handle(final List<Article> articleList, int i) {
		return () -> {
			handleSequential(articleList,i);
			countArticle.incrementbyValue(articleList.size());
			System.out.println("number of article processed "+ countArticle.value());
		};
	}

	private void handleSequential(List<Article> articleList, int i2) {
		final Map<String,Long> localMap = new HashMap<>();

		final List<String> pairs = new ArrayList<>();
//		long start = TimeUtil.getStart();
		for(final Article article: articleList) {
			final Article[] linksOut = article.getLinksOut();

			for(int i=0;i<linksOut.length;i++) {
				for(int j=i+1;j<linksOut.length;j++) {					
					pairs.add(linksOut[i].getId()<linksOut[j].getId()?linksOut[i].getId()+"\t"+linksOut[j].getId():linksOut[j].getId()+"\t"+linksOut[i].getId());
				}
			}
		}
		
		pairs.stream().collect(Collectors.groupingBy(p -> p, ()-> localMap,Collectors.counting()));
//		System.err.println(i2 +"== "+TimeUtil.getEnd(TimeUnit.SECONDS, start));
		FileUtil.writeDataToFile(localMap, OUTPUT_FOLDER + File.separator + (((i2 - 1) * NUMBER_OF_PAGES) + "_" + i2* NUMBER_OF_PAGES) + ".txt", false);		
	}

	
	public Article getArticle(String title) {
		if (wikipedia.getArticleByTitle(title) == null) {
			System.out.println("Could not find exact match of an article for a given title " + title);
			return null;
		}
		return wikipedia.getArticleByTitle(title);
	}
}
