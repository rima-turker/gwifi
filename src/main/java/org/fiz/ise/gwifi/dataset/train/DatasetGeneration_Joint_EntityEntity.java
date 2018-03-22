package org.fiz.ise.gwifi.dataset.train;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.FilteredWikipediaPagesSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LINE.Category.EntityEntityWeight_myApproach;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;
import org.w3c.dom.ls.LSException;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.util.PageIterator;

public class DatasetGeneration_Joint_EntityEntity {
	private static final Logger LOG = Logger.getLogger(DatasetGenerationLINE_EntityEntity2.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final long now = System.currentTimeMillis();
	private static final String OUTPUT_FOLDER = "NewData";
	private ExecutorService executor;
	private static SynchronizedCounter countArticle;
	private final Integer NUMBER_OF_THREADS=  Config.getInt("NUMBER_OF_THREADS",-1);
	private static final Map<Integer,Set<Article>> cache = new ConcurrentHashMap<>();
	private Wikipedia wikipedia;
	private List<String> globalList;
	public static void main(String[] args) {
		DatasetGeneration_Joint_EntityEntity data = new DatasetGeneration_Joint_EntityEntity();
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
	private void initializeVariables() {
		WikipediaSingleton wikipediaSingleton = WikipediaSingleton.getInstance();
		globalList = Collections.synchronizedList(new ArrayList<String>());
		wikipedia = wikipediaSingleton.wikipedia;
		countArticle = new SynchronizedCounter();
	}
	private void generateDatasetEntityEntiy() {
		PageIterator pageIterator = wikipedia.getPageIterator();
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		try {
			int i = 1;
			while (pageIterator.hasNext()) {
				Page page = pageIterator.next();
				if (page.getType().equals(PageType.article)) {
					Article article = wikipedia.getArticleByTitle(page.getTitle());
					if(article==null) {
						continue;
					}
					executor.execute(handle(article, i));
					i++;
				}
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
			System.out.println("writing to a file");
			FileUtil.writeDataToFile(globalList, "EntityEntity_LINE_dataset.txt", false);	
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
			Set<Article> setMain = getFromCacheInLinks(outLinks[j]);
			setMain.retainAll(setTemp);
			if (setMain.size()>0) {
				String key = articleToProcess.getId()+"\t"+outLinks[j].getId();
				//secondLOG.info(key+"\t\t"+setTemp.size());
				globalList.add(key);
			}
		}
	}
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
