package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.model.Model_LINE;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;

import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;

import edu.kit.aifb.gwifi.service.NLPAnnotationService;
import edu.kit.aifb.gwifi.util.PageIterator;


public class AnalyseDataset_websnippets {
	private static final Logger LOG = Logger.getLogger(AnalyseDataset_websnippets.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	private final Integer NUMBER_OF_THREADS=  Config.getInt("NUMBER_OF_THREADS",-1);
	private ExecutorService executor;
	private SynchronizedCounter counterTruePositive;
	private SynchronizedCounter counterProcessed;
	private final String DATASET_TEST_WEB = Config.getString("DATASET_TEST_WEB","");
	private static boolean LOAD_MODEL = Config.getBoolean("LOAD_MODEL", false);
	AnnotationSingleton singleton;
	
	public static void main(String[] args) {
		AnalyseDataset_websnippets data = new AnalyseDataset_websnippets();
		data.readRandomlyGeneratedDataset();
	}
 
	private void readRandomlyGeneratedDataset() {
		counterTruePositive = new SynchronizedCounter();
		counterProcessed= new SynchronizedCounter();
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		long now = System.currentTimeMillis();
		try {
			WikipediaSingleton wikiSing = WikipediaSingleton.getInstance();
			Wikipedia wiki = wikiSing.wikipedia;
			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_WEB), "utf-8");
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			for (int i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split(" ");
				String label = split[split.length-1];
				String snippet = arrLines[i].substring(0, arrLines[i].length()-(label).length()).trim();
				List<Category> gtList = new ArrayList<>(); 
				if (label.contains("-")) {
					String[] splitLabel = label.split("-");
					for (int j = 0; j < splitLabel.length; j++) {
						gtList.add(wiki.getCategoryByTitle(StringUtils.capitalize(splitLabel[j])));
					}
				}
				else{
					gtList.add(wiki.getCategoryByTitle(StringUtils.capitalize(label)));
				}
				executor.execute(handle(snippet,gtList));
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
			System.out.println("Number of true positive: "+counterTruePositive.value()+" number of processed: "+counterProcessed.value());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private Runnable handle(String snippet, List<Category> gtList) {
		return () -> {
			Category bestMatchingCategory = HeuristicApproach_loadModel.getBestMatchingCategory(snippet,gtList);
			counterProcessed.increment();
			if (gtList.contains(bestMatchingCategory)) {
				counterTruePositive.increment();
				System.out.println("True positive "+counterTruePositive.value()+" total processed: "+counterProcessed.value());
			}
			else{
				System.out.println("GT :"+gtList+", responce: "+  bestMatchingCategory+" total processed: "+counterProcessed.value()+"True positive "+counterTruePositive.value());
			}
			
		};
	}
//	private void readRandomlyGeneratedDataset() {
//		try {
//			WikipediaSingleton wikiSing = WikipediaSingleton.getInstance();
//			Wikipedia wiki = wikiSing.wikipedia;
//			AnnotationSingleton singleton = AnnotationSingleton.getInstance();
//			NLPAnnotationService service = singleton.service;
//			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_WEB), "utf-8");
//			String[] arrLines = new String[lines.size()];
//			arrLines = lines.toArray(arrLines);
//			for (int i = 0; i < arrLines.length; i++) {
//				String[] split = arrLines[i].split(" ");
//				String label = split[split.length-1];
//				String snippet = arrLines[i].substring(0, arrLines[i].length()-(label).length()).trim();
//				if (label.contains("-")) {
//					String[] splitLabel = label.split("-");
//					for (int j = 0; j < splitLabel.length; j++) {
//						System.err.println(wiki.getCategoryByTitle(StringUtils.capitalize(splitLabel[j])));
//					}
//				}
//				else
//				{
//					Category category = wiki.getCategoryByTitle(StringUtils.capitalize(label));
//					System.err.println(category);
//				}
//				HeuristicApproach.getBestMatchingCategory(snippet);
//			}
//
//		} catch (Exception e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//	}
}
