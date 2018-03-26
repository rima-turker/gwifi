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
import org.apache.lucene.search.Sort;
import org.fiz.ise.gwifi.Singleton.FilteredWikipediaPagesSingleton;
import org.fiz.ise.gwifi.dataset.train.DatasetGenerationLINE_EntityEntity2;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;
import org.fiz.ise.gwifi.util.TimeUtil;

import edu.kit.aifb.gwifi.model.Article;
import net.didion.jwnl.util.cache.Cache;
@Deprecated
public class EntityEntityWeight_myApproach {
	private static final Logger LOG = Logger.getLogger(DatasetGenerationLINE_EntityEntity2.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final long now = System.currentTimeMillis();
	private static final String OUTPUT_FOLDER = "NewData";
	private ExecutorService executor;
	private static SynchronizedCounter countArticle;
	private final Integer NUMBER_OF_THREADS=  Config.getInt("NUMBER_OF_THREADS",-1);
	private Set<Article> filteredArticles ;
	private static Map<String, Integer> mapGlobal;
	private static Set<String> setGlobal;
	private Article[] arrMainFilteredArticles; 
	
	public static void main(String[] args) {
		
		EntityEntityWeight_myApproach data = new EntityEntityWeight_myApproach();
		data.initializeVariables();
		System.out.println("Thread started...");
		final Thread t = new Thread (new Runnable() {
			@Override
			public void run() {
				while(true) {
					System.out.println("number of article processed "+ countArticle.value()+" minutes "+ TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}			
				}
			}
		});
		t.setDaemon(true);
		t.start();
		data.generateDatasetEntityEntiy();
	}
	private static final Map<Integer,Set<Article>> cache = new ConcurrentHashMap<>();

	private void initializeVariables() {
		mapGlobal = new ConcurrentHashMap<>();
		setGlobal  = Collections.synchronizedSet(new HashSet<>()); 
		FilteredWikipediaPagesSingleton singleton = FilteredWikipediaPagesSingleton.getInstance();
		filteredArticles=Collections.unmodifiableSet(new HashSet<Article>(singleton.articles));
		arrMainFilteredArticles = filteredArticles.toArray(new Article[filteredArticles.size()]);
		Arrays.sort(arrMainFilteredArticles);
		countArticle = new SynchronizedCounter();
	}
	private void generateDatasetEntityEntiy() {
		FileUtil.deleteFolder(OUTPUT_FOLDER);
		FileUtil.createFolder(OUTPUT_FOLDER);
		try {
			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			for (int j = 0; j < arrMainFilteredArticles.length; j++) {
				executor.execute(handle(arrMainFilteredArticles[j],j));
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
			System.out.println("writing to a file");
			FileUtil.writeDataToFile(mapGlobal, OUTPUT_FOLDER + File.separator + "EntityEntity.txt");		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private Runnable handle(final Article articleToProcess, int index) {
		return () -> {
			handleParallel(articleToProcess, index);
			countArticle.incrementbyValue(1);
		};
	}
	private void handleParallel(Article articleToProcess,int index) {
		Set<Article> setTemp = getFromCacheInLinks(articleToProcess);
		Article[] outLinks = articleToProcess.getLinksOut();
		Arrays.sort(outLinks);
		for (int j = 0; j < outLinks.length; j++) {
			//Set<Article> setMain = new HashSet<>(Arrays.asList(arrMainFilteredArticles[j].getLinksIn()));
			Set<Article> setMain = getFromCacheInLinks(outLinks[j]);
			setMain.retainAll(setTemp);
			if (setMain.size()>0) {
				String key = articleToProcess.getId()+"\t"+outLinks[j].getId();
				//secondLOG.info(key+"\t\t"+setTemp.size());
			}
		}
	}
//	private void handleSequential(Article articleToProcess,int index) {
//		//System.out.println("Inside handleSequential");
//		//Set<Article> setTemp = new HashSet<>(Arrays.asList(articleToProcess.getLinksIn()));
//		Set<Article> setTemp = getFromCache(articleToProcess);
//		//System.out.println("index is "+ index+ " loop will be "+( arrMainFilteredArticles.length-index));
//		for (int j = index+1; j < arrMainFilteredArticles.length; j++) {
//			//Set<Article> setMain = new HashSet<>(Arrays.asList(arrMainFilteredArticles[j].getLinksIn()));
//			Set<Article> setMain = getFromCache(arrMainFilteredArticles[j]);
//			setMain.retainAll(setTemp);
//			if (setMain.size()>0) {
//				String key = articleToProcess.getId()+"\t"+arrMainFilteredArticles[j].getId();
//				//mapGlobal.put(key, setTemp.size());
//				//setGlobal.add(key);
//				secondLOG.info(key+"\t\t"+setTemp.size());
//				
//			}
//		}
//	}

	private Set<Article> getFromCacheInLinks(Article articleToProcess) {
		Set<Article> result = cache.get(articleToProcess.getId());
		if(result==null) {
			Set<Article> hashSet = new HashSet<>(Arrays.asList(articleToProcess.getLinksIn()));
			cache.put(articleToProcess.getId(),hashSet);
			return new HashSet<>(hashSet);
		}else {
			return new HashSet<>(result);
		}
	}
}
