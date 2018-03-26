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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.FilteredWikipediaPagesSingleton;
import org.fiz.ise.gwifi.dataset.train.DatasetGenerationLINE_EntityEntity2;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;
import org.fiz.ise.gwifi.util.TimeUtil;

import TEST.SparseMatrix;
import TEST.SparseMatrix.SparseMatrixNode;
import edu.kit.aifb.gwifi.model.Article;
@Deprecated
public class EntityEntityWeight_SparceMatrix {
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
	private static final int SIZE = 100000000;
	private static final int NUMBER_OF_PAGES = Config.getInt("NUMBER_OF_PAGES",-1);
	private static final SparseMatrix m = new SparseMatrix(SIZE, SIZE);
	
	/*
	private static Map<String,Integer> map = new ConcurrentHashMap<>();
	private static AtomicLong ID = new AtomicLong(1);
	
	public static void main(String[] args) throws InterruptedException {
		for(int i=0;i<SIZE;i++) {
			for(int j=i+1;j<SIZE;j++) {
				System.err.println(i+" "+j);
				e.execute(handle(i));
			}
		}	
		e.shutdown();
		e.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		for(int i=0;i<2000;i++) {
			SparseMatrixNode search = m.search(i, i);
			if(search==null) {

			}else {
				System.err.println(i+" with "+ i + " == "+search.key);
			}
		}
		for(int i=0;i<SIZE;i++) {
			for(int j=0;j<SIZE;j++) {
				m.search(i, j);
			}	
		}
	}

	private static Runnable handle(int i) {
		return ()->{
			addOrIncreamet(i, i);
			};
	}
	 * 
	 */
	
	private Set<Article> filteredArticles ;
	
	public static void main(String[] args) {
		EntityEntityWeight_SparceMatrix data = new EntityEntityWeight_SparceMatrix();
		data.initializeVariables();
		
		data.generateDatasetEntityEntiy_parallel();
	}
	private void initializeVariables() {
		FilteredWikipediaPagesSingleton singleton = FilteredWikipediaPagesSingleton.getInstance();
		filteredArticles=Collections.unmodifiableSet(new HashSet<Article>(singleton.articles));
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
	private void handleSequential(List<Article> articleList, int i2) {
		System.out.println("Inside handleSequential");
		Long now = TimeUtil.getStart();
		final Map<String,Long> localMap = new HashMap<>();
		int countArticle =0;
		for(final Article article: articleList) {
			final Article[] linksOut = article.getLinksOut();
			for(int i=0;i<linksOut.length;i++) {
				for(int j=i+1;j<linksOut.length;j++) {					
					String key = linksOut[i].getId()+"\t"+linksOut[j].getId();
					if (filteredArticles.contains(linksOut[i])&&filteredArticles.contains(linksOut[j])) {
						//localMap.put(key, localMap.getOrDefault(key, 0L) + 1);
						addOrIncreamet(linksOut[i].getId(),linksOut[j].getId());
					}
				}
			}
			System.out.println("size of articleList "+articleList.size()+" articleList size "+(++countArticle ));
		}
		System.out.println("Total time for iterations "+(++countArticle)+" "+TimeUtil.getEnd(TimeUnit.SECONDS, now) + " article List size: "+ articleList.size() );
//		System.out.println("writing to a file");
//		FileUtil.writeDataToFile(localMap, OUTPUT_FOLDER + File.separator + fileName.incrementAndGet() + ".txt", false);		
	}
	
	private static void addOrIncreamet(int x,int y) {		
		final SparseMatrixNode search = m.search(x, y);
		if(search==null) {
			m.add(1, x, y);
		}else {
			m.add(search.key+1, x, y);
		}
	}
}