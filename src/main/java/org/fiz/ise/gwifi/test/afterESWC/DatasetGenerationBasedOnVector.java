package org.fiz.ise.gwifi.test.afterESWC;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections15.map.HashedMap;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.dataset.test.ReadDataset;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Print;

import com.mongodb.util.Hash;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;

public class DatasetGenerationBasedOnVector {

	private final static TestDatasetType_Enum TEST_DATASET_TYPE = Config.getEnum("TEST_DATASET_TYPE");
	private final static String TRAIN_SET_AG = Config.getString("DATASET_TRAIN_AG","");
	private final static String TRAIN_SET_WEB = Config.getString("DATASET_TRAIN_WEB","");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	static int numberOfSamples=50;
	static int countCorrect=0;
	static int countWrong=0;

	public static void main(String[] args) {
		datasetGenerateFromTrainSet();
	}
	private static void datasetGenerateFromTrainSet() {
		Map<String, Integer> mapResult = new HashMap<String, Integer>();
		try {
			Set<Category> setMainCategories = new HashSet<>(
					CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);	
//			for(Category c : setMainCategories) {
//				File directory = new File(c.getTitle());
//				if (! directory.exists()){
//					directory.mkdir();
//				}
//			}
			TestBasedonSortTextDatasets datasetRead =  new TestBasedonSortTextDatasets();
			Map<String, List<Category>> dataset = null;
			Map<String, List<Category>> map_result_To_Compare = new HashedMap<String, List<Category>>();
			Category bestMatchingCategory=null;
			if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.AG)) {
				dataset = datasetRead.read_dataset_AG(AG_DataType.TITLEANDDESCRIPTION, TRAIN_SET_AG);
				int i =0;
				for(Entry<String, List<Category>> e: dataset.entrySet()) {
					bestMatchingCategory = HeuristicBasedOnEntitiyVectorSimilarity.getBestMatchingCategory(e.getKey(),e.getValue());
					i++;
					//FileUtil.writeDataToFile(Arrays.asList(e.getKey()), bestMatchingCategory.getTitle()+File.separator+ i,false);
					
					if (e.getValue().contains(bestMatchingCategory)) {
						countCorrect++;
					}
					else if(e.getValue().get(0).getTitle().equals("Sports")&&bestMatchingCategory.getTitle().equals("Sport")) {
						countCorrect++;
					}
					else
					{
//						String key = bestMatchingCategory.getTitle();
						String key = bestMatchingCategory.getTitle()+"-"+e.getValue().get(0).getTitle();
						int count = mapResult.containsKey(key) ? mapResult.get(key) : 0;
						mapResult.put(key, count + 1);
						resultLog.info("wrong classified: "+ bestMatchingCategory.getTitle()+"\t"+i+"\t"+e.getKey()+"\n");
//						System.out.println("wrong classified: "+ bestMatchingCategory.getTitle()+"\t"+i+"\t"+e.getKey());
						countWrong++;
					}
					//System.out.println(i+" files are processed. Correctly: "+countCorrect+" Wrongly: "+countWrong);
					if (bestMatchingCategory.getTitle().equals("Sport")) {
						map_result_To_Compare.put(e.getKey(), Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Sports")));
					}
					else {
						map_result_To_Compare.put(e.getKey(), Arrays.asList(bestMatchingCategory));
					}
				}
			}
			else if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.WEB_SNIPPETS)) {
				dataset = datasetRead.read_dataset_WEB_for_DatasetGeneration(TRAIN_SET_WEB);
				System.out.println("Dataset size: "+dataset.size());
				int i =0;
				for(Entry<String, List<Category>> e: dataset.entrySet()) {
					Article bestMatchingArticle = HeuristicBasedOnEntitiyVectorSimilarity.getBestMatchingArticle(e.getKey(),e.getValue());
					bestMatchingCategory= WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle(bestMatchingArticle.getTitle());
					i++;
					//FileUtil.writeDataToFile(Arrays.asList(e.getKey()), bestMatchingCategory.getTitle()+File.separator+ i,false);
					
					if (e.getValue().get(0).equals(bestMatchingCategory)) {
						secondLOG.info("classified: "+ bestMatchingArticle.getTitle()+"\t"+i+"\t"+e.getKey()+"\n");
						countCorrect++;
					}
					else if(e.getValue().get(0).getTitle().equals("Sports")&&bestMatchingCategory.getTitle().equals("Sport")) {
						countCorrect++;
					}
					else//wrong classified
					{
						String key = bestMatchingCategory.getTitle()+"-"+e.getValue().get(0).getTitle();
						int count = mapResult.containsKey(key) ? mapResult.get(key) : 0;
						mapResult.put(key, count + 1);
						resultLog.info("wrong classified: "+ bestMatchingArticle.getTitle()+"\t"+i+"\t"+e.getKey()+"\t"+e.getValue().get(0)+"\n");
						countWrong++;
					}
					
					//For comparison
					if (bestMatchingCategory.getTitle().equals("Sport")) {
						map_result_To_Compare.put(e.getKey(), Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Sports")));
					}
					else {
						map_result_To_Compare.put(e.getKey(), Arrays.asList(bestMatchingCategory));
					}
				}
			}
			System.out.println("Test calculation is started...");
			int matchingSentences =0;
			for(Entry<String, List<Category>> e : dataset.entrySet()) {
				if (map_result_To_Compare.get(e.getKey()).contains(e.getValue().get(0))) {
					matchingSentences++;
				}
			}
			System.out.println("matching sentences between artificial ds and the original"+matchingSentences);
			System.out.println("countCorrect "+countCorrect+"\nWrongly assigned labels: "+countWrong);
			System.out.println("Total classified "+(countCorrect+countWrong));
			System.out.println("Accuracy "+(countCorrect/(dataset.size()*1.0)));
			Print.printMap(mapResult);
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public static void writeGeneratedDataToFile(String folderName, String data, int fileName ){
		String mainFolderName="TrainTFID_AG_"+numberOfSamples;
		File directory = new File(mainFolderName);
		if (! directory.exists()){
			directory.mkdir();
		}
		directory = new File(mainFolderName+File.separator+folderName);
		if (! directory.exists()){
			directory.mkdir();
		}
		FileUtil.writeDataToFile(Arrays.asList(data), directory+File.separator+ fileName,false);
	}
	private static void datasetGenerateFromTestSet() {
		String fileName="TrainTFID_AG_"+numberOfSamples;
		File directory = new File(fileName);
		if (! directory.exists()){
			directory.mkdir();
		}

		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
		Map<String, List<String>> result = new HashedMap<String, List<String>>();
		//		List<String> dataset = new ArrayList<>(ReadTestDataset.read_AG_BasedOnType(AG_DataType.TITLEANDDESCRIPTION));
		Map<String, Double> mapCatValues = new HashedMap<String, Double>();
		//		for(String text:dataset) {
		//			String bestMatchingCategory = HeuristicBasedOnEntityVector.getBestMatchingCategory(text);
		//			String[] split = bestMatchingCategory.split("\t\t");
		//			mapCatValues.put(text+"\t\t"+split[0], Double.valueOf(split[1]));
		//			resultLog.info(text+"\t\t"+split[0]+"\t\t"+split[1]);
		//		}
		try {
			List<String>  lines = new ArrayList<>(FileUtils.readLines(new File("/home/rtue/eclipse-workspace/gwifi/log/classificationResults_AG",""), "utf-8"));
			for(String str:lines) {
				String[] split = str.split("\t\t");
				mapCatValues.put(split[0]+"\t\t"+split[1], Double.valueOf(split[2]));
			}

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		for (Category mainCat : setMainCategories) {
			String entityCat = WikipediaSingleton.getInstance().getArticle(mainCat.getTitle()).getTitle();
			List<String> lst = new ArrayList<String>();
			System.out.println(entityCat+" "+lst);
			result.put(entityCat, lst);
		}
		Map<String, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapCatValues));

		for(Entry <String, Double> e: sortedMap.entrySet() ) {
			String[] split = e.getKey().split("\t\t");
			String text = split[0];
			Double val = e.getValue();
			String entityCat = WikipediaSingleton.getInstance().getArticle(split[1]).getTitle();
			System.out.println(entityCat);
			List<String> lst = new ArrayList<String>(result.get(entityCat));
			if (lst.size()<numberOfSamples) {
				lst.add(text);
				result.put(entityCat, lst);
				secondLOG.info(entityCat+"\t"+text+"\t"+val);
				System.out.println((entityCat+"\t"+text+"\t"+val));
				//if(entityCat.equals(anObject))
			}

		}
		Print.printMap(result);
		calculateAccuracyBasedOnVectorSimilarity(fileName, result);
	}
	private static void calculateAccuracyBasedOnVectorSimilarity(String fileName, Map<String, List<String>> result) {
		File directory;
		TestBasedonSortTextDatasets test = new TestBasedonSortTextDatasets();
		Map<String, List<Category>> read_dataset_AG = test.read_dataset_AG(AG_DataType.TITLEANDDESCRIPTION,TRAIN_SET_AG);
		Map<String, Integer> mapResultWrongAssignedLabel = new HashMap<String, Integer>();;
		int i =0;
		for(Entry<String, List<String>> e: result.entrySet() ) {
			Category strObtainedCat=WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle(e.getKey());
			System.out.println(strObtainedCat);
			String folderName = e.getKey();
			directory = new File(fileName+File.separator+folderName);
			if (! directory.exists()){
				directory.mkdir();
			}
			List<String> lstData = new ArrayList<>(e.getValue());
			for(String s : lstData) {
				List<Category> gtlist = read_dataset_AG.get(s);

				if (gtlist.contains(strObtainedCat)) {
					countCorrect++;
					FileUtil.writeDataToFile(Arrays.asList(s), directory+File.separator+ ++i,false);
				}
				else if(gtlist.get(0).getTitle().equals("Sports")&&strObtainedCat.getTitle().equals("Sport")) {
					countCorrect++;
					FileUtil.writeDataToFile(Arrays.asList(s), directory+File.separator+ ++i,false);
				}
				else
				{
					int count = mapResultWrongAssignedLabel.containsKey(strObtainedCat.getTitle()) ? mapResultWrongAssignedLabel.get(strObtainedCat.getTitle()) : 0;
					mapResultWrongAssignedLabel.put(strObtainedCat.getTitle(), count + 1);
				}
			}
		}
		System.out.println("countCorrect "+countCorrect+"\nWrongly assigned labels");
		Print.printMap(mapResultWrongAssignedLabel);
	}
}
