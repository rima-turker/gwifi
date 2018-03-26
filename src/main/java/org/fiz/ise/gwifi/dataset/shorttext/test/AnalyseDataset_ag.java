package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hdfs.HsftpFileSystem;
import org.apache.log4j.Logger;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.SynchronizedCounter;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class AnalyseDataset_ag {
	private static final String DATASET_TEST_AG = Config.getString("DATASET_TEST_AG","");
	private final static Integer NUMBER_OF_SAMPLES_FROM_EACH_CAT=Config.getInt("NUMBER_OF_SAMPLES_FROM_EACH_CAT",-1);
	private static Wikipedia wikipedia = WikipediaSingleton.getInstance().wikipedia;
	private static SynchronizedCounter counterTruePositive;
	private static SynchronizedCounter counterFalsePositive;
	private static boolean LOAD_MODEL = Config.getBoolean("LOAD_MODEL", false);
	private final static TestDatasetType_Enum TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE"); 
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	private final static Integer NUMBER_OF_THREADS=  Config.getInt("NUMBER_OF_THREADS",-1);
	private static Map<Category, Integer> truePositveResult = new ConcurrentHashMap<>();
	private static Map<String, Category> falsePositiveResult = new ConcurrentHashMap<>();
	private static Map<String, Integer> mapMissClassified = new ConcurrentHashMap<>();
	long now = System.currentTimeMillis();
	private SynchronizedCounter counterProcessed;
	private ExecutorService executor;
	private static CategorySingleton singCategory= CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE));
	private static Map<Category, Set<Category>> mapCategories;
	private final String DATASET_TEST_WEB = Config.getString("DATASET_TEST_WEB","");

	public static void main(String[] args) {
		initializeVariables();

	}
	private static void initializeVariables() {
		AnalyseDataset_ag ag = new AnalyseDataset_ag();
		Map<Category, Set<Category>> mapTemp = new HashMap<>(singCategory.map);
		System.out.println("NUMBER_OF_THREADS "+NUMBER_OF_THREADS);
		for(Entry<Category, Set<Category>> e: mapTemp.entrySet())
		{
			Category main = e.getKey();
			//			System.out.println(main+" number of child categories "+e.getValue().size());
			Set<Category> temp = new HashSet<>();
			int numberOfArticles=0;
			for(Category c: e.getValue() ) {
				if (c.getChildArticles().length>0) {
					temp.add(c);
					numberOfArticles+=c.getChildArticles().length;
				}
			}
			mapTemp.put(main, temp);
			//			System.out.println(main+" number of child categories after filtering "+temp.size());
			//			System.out.println(main+" number of child aricles after filtering "+numberOfArticles);
		}
		mapCategories= new HashMap<>(mapTemp);
		
//		Set<Category> setMainCategories= new HashSet<>(singCategory.setMainCategories);
//		for (Category c: setMainCategories) {
//			for (Category d: setMainCategories) {
//				System.out.println(c.getTitle()+" "+d.getTitle()+" "+EmbeddingsService.getSimilarity(String.valueOf(c.getId()), String.valueOf(d.getId())));
//			}
//		}
		
		///////////////////////////////////////////////////////////////////////////////
		

/*		Map<String, Set<Category>> mapDepth = new HashMap<>(singCategory.mapCategoryDept);
		Map<Category, Set<Category>> mapBusiness=new HashMap<>();
		for (Entry <String, Set<Category>> e: mapDepth.entrySet()) {
			Category mainCat = wikipedia.getCategoryByTitle(e.getKey().split("\t")[0]);
			Integer dept = Integer.valueOf(e.getKey().split("\t")[1]);
			Set<Category> set = new HashSet<>(e.getValue());
			if (dept==0) {
				if (mapBusiness.containsKey(mainCat)) {
					Set<Category> temp = new HashSet<>(mapBusiness.get(mainCat));
					temp.addAll(set);
					mapBusiness.put(mainCat, temp);
				}
				else {
					mapBusiness.put(mainCat, set);
				}
			}
			else if (dept==1 && !mainCat.equals(wikipedia.getCategoryByTitle("Business"))) {
				if (mapBusiness.containsKey(mainCat)) {
					Set<Category> temp = new HashSet<>(mapBusiness.get(mainCat));
					temp.addAll(set);
					mapBusiness.put(mainCat, temp);
				}
				else {
					mapBusiness.put(mainCat, set);
				}
			}
//			else if (dept==2 && mainCat.equals(wikipedia.getCategoryByTitle("Business"))) {
//				if (mapBusiness.containsKey(mainCat)) {
//					Set<Category> temp = new HashSet<>(mapBusiness.get(mainCat));
//					temp.addAll(set);
//					mapBusiness.put(mainCat, temp);
//				}
//				else {
//					mapBusiness.put(mainCat, set);
//				}
//			}
		}
		
		Map<Category, Set<Category>> mapTemp = new HashMap<>(mapBusiness);
		System.out.println("NUMBER_OF_THREADS "+NUMBER_OF_THREADS);
		for(Entry<Category, Set<Category>> e: mapTemp.entrySet())
		{
			Category main = e.getKey();
			//			System.out.println(main+" number of child categories "+e.getValue().size());
			Set<Category> temp = new HashSet<>();
			int numberOfArticles=0;
			for(Category c: e.getValue() ) {
				if (c.getChildArticles().length>0) {
					temp.add(c);
					numberOfArticles+=c.getChildArticles().length;
				}
			}
			mapTemp.put(main, temp);
			//			System.out.println(main+" number of child categories after filtering "+temp.size());
			//			System.out.println(main+" number of child aricles after filtering "+numberOfArticles);
		}
		mapCategories= new HashMap<>(mapTemp);
		////////////////////////////////////////////////////////////////////////////
		
	*/	
		Print.printMapSize(mapCategories);
		
		if (LOAD_MODEL) {
			LINE_modelSingleton.getInstance();
		}
		if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.AG)) {
			ag.dataset_AG();
		}
		else if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.WEB_SNIPPETS)) {
			System.out.println("started processing");
			ag.dataset_WEB();
		}
	}
	public void dataset_WEB() {
		try {
			Map<String,List<Category>> dataset = new HashMap<>();
			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_WEB), "utf-8");
			System.out.println("size of the file "+lines.size());
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
						gtList.add(wikipedia.getCategoryByTitle(StringUtils.capitalize(splitLabel[j])));
					}
				}
				else{
					gtList.add(wikipedia.getCategoryByTitle(StringUtils.capitalize(label)));
				}
				dataset.put(snippet, gtList);
			}
			System.out.println("started processing");
			startProcessingData(dataset);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	public void dataset_AG() {
		Map<String,List<Category>> dataset = new HashMap<>();
		Map<Integer, Category> mapLabel = new HashMap<>();
		mapLabel.put(1, wikipedia.getCategoryByTitle("World"));
		mapLabel.put(2, wikipedia.getCategoryByTitle("Sports"));
		mapLabel.put(3, wikipedia.getCategoryByTitle("Business"));
		mapLabel.put(4, wikipedia.getCategoryByTitle("Science"));
		mapLabel.put(5, wikipedia.getCategoryByTitle("Technology"));
		List<String> lines = new ArrayList<>(generateRandomDataset_AG());
		String[] arrLines = new String[lines.size()];
		arrLines = lines.toArray(arrLines);
		int i=0;
		for (i = 0; i < arrLines.length; i++) {
			List<Category> gtList = new ArrayList<>(); 
			String[] split = arrLines[i].split("\",\"");
			String label = split[0].replace("\"", "");
			if (label.equals("4")||label.equals("5")) {
				gtList.add(mapLabel.get(4));
				gtList.add(mapLabel.get(5));
			}
			else {
				gtList.add(mapLabel.get(Integer.valueOf(label)));
			}
			String title = split[1].replace("\"", "");
			String description = split[2].replace("\"", "");
			dataset.put(title, gtList);
		}
		startProcessingData(dataset);
	}
	private void startProcessingData(Map<String,List<Category>> dataset) {
		counterProcessed= new SynchronizedCounter();
		counterFalsePositive= new SynchronizedCounter();
		counterTruePositive= new SynchronizedCounter();
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		int count=0;
		try {
			for(Entry<String, List<Category>> e : dataset.entrySet()) {
				executor.execute(handle(e.getKey(),e.getValue(),++count));
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
			System.out.println("Number of true positive: "+counterTruePositive.value()+" number of processed: "+counterProcessed.value());
			Print.printMap(truePositveResult);
			Print.printMap(mapMissClassified);
			resultLog.info("MISS CLASSIFIED RESULTS " );
			for(Entry<Category, Integer> e: truePositveResult.entrySet()){
				resultLog.info(e.getKey()+"\t"+e.getValue());
			}
			resultLog.info("\n"+"TRUE POSITIVE RESULTS " );
			resultLog.info("Total number processed "+ count+", true positive "+counterTruePositive.value());
			for(Entry<Category, Integer> e: truePositveResult.entrySet()){
				resultLog.info(e.getKey()+"\t"+e.getValue());
			}
			resultLog.info("FALSE POSITIVE RESULTS " );
			resultLog.info("Total number processed "+ count+", false positive "+counterFalsePositive.value());
			for(Entry<String,Category> e: falsePositiveResult.entrySet()){
				resultLog.info(e.getKey()+"\t"+e.getValue());
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	private Runnable handle(String description, List<Category> gtList,int i) {
		return () -> {
			Category bestMatchingCategory = HeuristicApproach_loadModel.getBestMatchingCategory(description,gtList,mapCategories);
			counterProcessed.increment();
			if (gtList.contains(bestMatchingCategory)) {
				counterTruePositive.increment();
				truePositveResult.put(gtList.get(0), truePositveResult.getOrDefault(gtList.get(0), 0) + 1);
				System.out.println(" total processed: "+i+" True positive "+counterTruePositive.value());
			}
			else{
				falsePositiveResult.put(description+"\n"+gtList.get(0).getTitle(), bestMatchingCategory);
				counterFalsePositive.increment();
				String key=bestMatchingCategory+"\t"+gtList.get(0);
				mapMissClassified.put(key, mapMissClassified.getOrDefault(key, 0) + 1);
				System.out.println(" total processed: "+i+" True positive "+counterTruePositive.value());
			}
		};
	}
	static List<String> generateRandomDataset_AG(){
		List<String> result = new ArrayList<>();
		try {
			Map<String, Integer> map = new HashMap<>();
			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_AG), "utf-8");
			for(String l : lines) {
				String[] split = l.split("\",\"");
				String label = split[0].replace("\"", "");
				int count = map.containsKey(label) ? map.get(label) : 0;
				if (count<=NUMBER_OF_SAMPLES_FROM_EACH_CAT) {
					map.put(label, map.getOrDefault(label, 1) + 1);
					result.add(l);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Number of lines "+result.size());
		return result;
	}
}
