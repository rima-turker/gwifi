package org.fiz.ise.gwifi.test.longDocument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.longText.TestBasedonLongTextDatasets;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;

import edu.kit.aifb.gwifi.model.Category;

public class BaselineUsingWords {
	/*
	 * This class is a baseline of our approach which considers words to present a document vector by using
	 *  a pre trained word embeddings. 
	 *  More specifically it can be considered as a pure dataless classification since we do only a similarity based classification
	 *  cosine similarity between a document vector and all the other category/word vectors  
	 */
	private final static Integer NUMBER_OF_THREADS=  Config.getInt("NUMBER_OF_THREADS",-1);
	private static SynchronizedCounter counterTruePositive;
	private static SynchronizedCounter counterFalsePositive;
	private static SynchronizedCounter counterProcessed;
	private static Map<Category, Integer> truePositive = new ConcurrentHashMap<>();
	private static Map<Category, Integer> falsePositive = new ConcurrentHashMap<>();
	private static Map<String, Category> falsePositiveResult = new ConcurrentHashMap<>();
	private static Map<String, Integer> mapMissClassified = new ConcurrentHashMap<>();
	private ExecutorService executor;
	private static Map<Category, Set<Category>> mapCategories;
	long now = System.currentTimeMillis();

	public static void main(String[] args) {
		counterProcessed= new SynchronizedCounter();
		counterFalsePositive= new SynchronizedCounter();
		counterTruePositive= new SynchronizedCounter();
		LINE_modelSingleton.getInstance();

		TestBasedonLongTextDatasets test = new TestBasedonLongTextDatasets();
		Map<String,List<Category>> dataset = new HashMap<>(test.initializeDataset()); //here you initialize the dataset also based on a number of sentences
		test.setCategoryList(dataset);
		
		//			Set<Category> setMainCategories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
		//			Word2Vec model = LINE_modelSingleton.getInstance().lineModel;
		//			for(Category c : setMainCategories) {
		//				if (!model.hasWord(c.getTitle())) {
		//					System.out.println(c.getTitle());
		//				}
		//			}
		
		BaselineUsingWords base = new BaselineUsingWords();
		base.startProcessingData(dataset);
	}
	public void startProcessingData(Map<String,List<Category>> dataset) {
		int count=0;
		try {
			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			for(Entry<String, List<Category>> e : dataset.entrySet()) {
				executor.execute(handle(e.getKey(),e.getValue(),++count));
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
			System.out.println("Number of true positive: "+counterTruePositive.value()+" number of processed: "+counterProcessed.value());
			Double d = (counterTruePositive.value()*0.1)/(counterProcessed.value()*0.1);
			System.out.println("Accuracy: "+d);
			System.out.println("Calculating F measures");
			FileUtil.writeDataToFile(truePositive,"TRUE_POSITIVE_RESULTS");
			FileUtil.writeDataToFile(falsePositiveResult,"FALSE_POSITIVE_RESULTS");
			FileUtil.writeDataToFile(mapMissClassified,"MISS_CLASSIFIED_RESULTS");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	private Runnable handle(String description, List<Category> gtList,int i) {
		return () -> {
			Category bestMatchingCategory= BasedOnWordsCategorize.getBestMatchingCategory(description,gtList,mapCategories);
			counterProcessed.increment();
			if (gtList.contains(bestMatchingCategory)) {
				counterTruePositive.increment();
				truePositive.put(gtList.get(0), truePositive.getOrDefault(gtList.get(0), 0) + 1);
				System.out.println(" total processed: "+i+" True positive "+counterTruePositive.value());
			}
			else{
				try {
					falsePositiveResult.put(description+"\n gt:"+gtList.get(0).getTitle(), bestMatchingCategory);
					falsePositive.put(gtList.get(0), falsePositive.getOrDefault(gtList.get(0), 0) + 1);
				} catch (Exception e) {
					System.out.println("Exception msg "+e.getMessage());
					System.out.println("description "+description+" "+gtList+" "+bestMatchingCategory );
					System.exit(1);
				}
				counterFalsePositive.increment();
				String key=gtList.get(0)+"\t"+"predicted: "+bestMatchingCategory;
				mapMissClassified.put(key, mapMissClassified.getOrDefault(key, 0) + 1);
				System.out.println(" total processed: "+i+" True positive "+counterTruePositive.value());
			}
		};
	}

}
