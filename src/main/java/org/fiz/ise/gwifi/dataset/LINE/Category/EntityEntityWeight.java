package org.fiz.ise.gwifi.dataset.LINE.Category;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.FilteredWikipediaPagesSingleton;
import org.fiz.ise.gwifi.dataset.train.DatasetGenerationLINE_EntityEntity2;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;
import org.fiz.ise.gwifi.util.TimeUtil;

import edu.kit.aifb.gwifi.model.Article;
@Deprecated
public class EntityEntityWeight {
	private static final Logger LOG = Logger.getLogger(DatasetGenerationLINE_EntityEntity2.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger thirdLOG = Logger.getLogger("reportsLogger");
	private static final String OUTPUT_FOLDER = "NewData";
	private ExecutorService executor;
	private static SynchronizedCounter countCategoryPerArticle;
	private static SynchronizedCounter countArticle;
	private static AtomicInteger fileName; 
	private final Integer NUMBER_OF_THREADS= Config.getInt("NUMBER_OF_THREADS",-1);
	final long now = System.currentTimeMillis();
	//private static List<String> safeList;
	private static final int NUMBER_OF_PAGES = Config.getInt("NUMBER_OF_PAGES",-1);
	
	
	
	private Set<Article> filteredArticles ;
	public static void main(String[] args) {
		EntityEntityWeight data = new EntityEntityWeight();
		data.initializeVariables();
		data.generateDatasetEntityEntiy_parallel();
	}
	private void initializeVariables() {
		FilteredWikipediaPagesSingleton singleton = FilteredWikipediaPagesSingleton.getInstance();
		filteredArticles=Collections.unmodifiableSet(new HashSet<Article>(singleton.categoryFilteredArticles));
		countArticle = new SynchronizedCounter();
		countCategoryPerArticle = new SynchronizedCounter();
		fileName= new AtomicInteger(0);
	}
	private void generateDatasetEntityEntiy_parallel() {
		FileUtil.deleteFolder(OUTPUT_FOLDER);
		FileUtil.createFolder(OUTPUT_FOLDER);
		try {
			System.out.println("size of the articles "+filteredArticles.size());
			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			List<Article> localList = new ArrayList<>();
			int i = 1;
			for (Article article:filteredArticles) {
				if(localList.size()<NUMBER_OF_PAGES) {
					localList.add(article);
				}else {
					executor.execute(handle(new ArrayList<Article>(localList),i++));
					localList.clear();
					localList.add(article);
				}
			}
			if (localList.size()>0) {
				executor.execute(handle(new ArrayList<Article>(localList),i));
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private Runnable handle(final List<Article> articleList, int i) {
		return () -> {
			handleSequential(articleList,i);
			countArticle.incrementbyValue(articleList.size());
			System.out.println("number of article processed "+ countArticle.value()+" minutes "+ TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
		};
	}
//	
//	private void handleParallel(List<Article> articleList, int i2) {
//		System.out.println("Inside handleParallel");
//		final Map<Integer,List<Article>> mapLocal= new HashMap<>();
//		Long now = TimeUtil.getStart();
//		int countArticle =0;
//		for(final Article article: articleList) {
//			ArrayList<Article> linksOut = new ArrayList<Article>(Arrays.asList(article.getLinksOut()));
//			int i=0;
//			for(Article link : linksOut)
//			{
//				List<Article> sub = new ArrayList<Article>(linksOut.subList(++i, linksOut.size()));
//				List<Article> existList = new ArrayList<>(mapLocal.get(link));
//				if (existList!=null) {
//					existList.addAll(sub);
//					mapLocal.put(link.getId(), existList);
//					
//					List<Article> newList = new ArrayList<Article>(sub);
//					newList.addAll(existList);
//					
//					mapLocal.put(link.getId(), mapLocal.getOrDefault(link.getId(), sub),newList);
//				}
//				
//			}
//			
//		}
//		System.out.println("Total time for iterations "+(++countArticle)+" "+TimeUtil.getEnd(TimeUnit.SECONDS, now) + " article List size: "+ articleList.size() );
//		System.out.println("Size of the mySet "+mySet.size());
////		
//////		for(String str:localList){
//////			localMap.put(str, localMap.getOrDefault(str, 0L) + 1);
//////		}
////		System.out.println("writing to a file");
//		FileUtil.writeDataToFile(localMap, OUTPUT_FOLDER + File.separator + fileName.incrementAndGet() + ".txt", false);		
//	}
	private void handleSequential(List<Article> articleList, int i2) {
		System.out.println("Inside handleSequential");
		final Map<String,Long> localMap = new HashMap<>();
		Long now = TimeUtil.getStart();
		int countArticle =0;
		for(final Article article: articleList) {
			final Article[] linksOut = article.getLinksOut();
			for(int i=0;i<linksOut.length;i++) {
				for(int j=i+1;j<linksOut.length;j++) {					
					String key = linksOut[i].getId()+"\t"+linksOut[j].getId();
					if (filteredArticles.contains(linksOut[i])&&filteredArticles.contains(linksOut[j])) {
						localMap.put(key, localMap.getOrDefault(key, 0L) + 1);
					}
				}
			}
			System.out.println("size of localMap "+localMap.size()+" articleList size "+(++countArticle ));
		}
		System.out.println("Total time for iterations "+(++countArticle)+" "+TimeUtil.getEnd(TimeUnit.SECONDS, now) + " article List size: "+ articleList.size() );
//		System.out.println("writing to a file");
//		FileUtil.writeDataToFile(localMap, OUTPUT_FOLDER + File.separator + fileName.incrementAndGet() + ".txt", false);		
	}
}