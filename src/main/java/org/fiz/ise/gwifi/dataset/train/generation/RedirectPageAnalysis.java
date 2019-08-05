package org.fiz.ise.gwifi.dataset.train.generation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.test.ReadDataset;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.SynchronizedCounter;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Page;


public class RedirectPageAnalysis {
	private static Map<String, String> mapRedirectPages;  //new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);//
	private static final String REDIRECT_PAGE_ADDRESS = Config.getString("REDIRECT_PAGE_ADDRESS", "");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");

	private final static Integer NUMBER_OF_THREADS= Config.getInt("NUMBER_OF_THREADS",-1);	
	private ExecutorService executor;

	private static SynchronizedCounter totalRedirect;
	private static SynchronizedCounter resolvedRedirect;
	private static SynchronizedCounter mapDoesnotContain;
	private static SynchronizedCounter couldNotResolved;

	public static void main(String[] args) throws Exception {
		mapRedirectPages= new HashMap<>(loadRedirectPages());
		totalRedirect=new SynchronizedCounter();
		resolvedRedirect=new SynchronizedCounter();
		mapDoesnotContain=new SynchronizedCounter();
		couldNotResolved=new SynchronizedCounter();
		RedirectPageAnalysis test = new RedirectPageAnalysis();
		test.analyseDatasetFromTrainSetParalel();

		System.out.println("totalRedirect:"+totalRedirect.value());
		System.out.println("resolvedRedirect:"+resolvedRedirect.value());
		System.out.println("couldNotResolved:"+couldNotResolved.value());
		System.out.println("mapDoesnotContain:"+mapDoesnotContain.value());
	}
	public static Map<String, String> loadRedirectPages() {
		Map<String, String> mapRedirectPages = new HashMap<String, String>();
		try { final BufferedReader br = new BufferedReader( new FileReader(REDIRECT_PAGE_ADDRESS)); String line; 
	while ((line = br.readLine()) != null) { if (line == null || line.isEmpty()) { continue; } final String[] data = line.split("\t"); mapRedirectPages.put(data[0], data[1]); } br.close(); }catch(Exception e) { e.printStackTrace(); } 
	return mapRedirectPages;
	}

	public void analyseDatasetFromTrainSetParalel() {
		try {
			Map<String, List<Article>> dataset = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION);
			int count =0;
			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			for(Entry<String, List<Article>> e: dataset.entrySet()) {
				executor.execute(analyseRedirectPage(e.getKey(),++count));
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	private Runnable analyseRedirectPage(String description, int i ) {
		return () -> {
			List<Annotation> lstAnnotations = new ArrayList<>();
			try {
				AnnotationSingleton.getInstance().service.annotate(description, lstAnnotations);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			for(Annotation a : lstAnnotations) {
				if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())){
					if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())==null) {
						Page p = new Page(WikipediaSingleton.getInstance().wikipedia.getEnvironment(), a.getId());
						if (p.getType().equals(Page.PageType.redirect)) {
							totalRedirect.increment();
							String key = a.getURL().replace("http://en.wikipedia.org/wiki/", "");
							if(mapRedirectPages.containsKey(key)) {
								String tName = mapRedirectPages.get(key).replace("_", " ");
								Article article = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(tName);
								if (article!=null) {
									resolvedRedirect.increment();
								}
								else {
									couldNotResolved.increment();
									secondLOG.info(key);
								}
							}
							else {
								mapDoesnotContain.increment();
								resultLog.info(key);
							}
						}
					}
				}
			}
			System.out.println(i+" files are processed. totalRedirect: "+totalRedirect.value()+" resolvedRedirect: "+resolvedRedirect.value()+" mapDoesnotContain: "+mapDoesnotContain.value());
		};
	}
}


