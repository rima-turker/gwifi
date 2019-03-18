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
import org.fiz.ise.gwifi.dataset.test.ReadTestDataset;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Print;

import com.mongodb.util.Hash;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Category;

public class DatasetGenerationBasedOnVector {

	private final static TestDatasetType_Enum TEST_DATASET_TYPE = Config.getEnum("TEST_DATASET_TYPE");
	private static boolean LOAD_MODEL = Config.getBoolean("LOAD_MODEL", false);
	private static final Logger LOG = Logger.getLogger(HeuristicAproachEntEnt.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	static int numberOfSamples=50;
	static int countCorrect=0;

	public static void main(String[] args) {
		//		datasetGenerateFromTestSet();

	}
	private static void datasetGenerateFromTrainSet() {

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
		TestBasedonSortTextDatasets test = new TestBasedonSortTextDatasets();
		Map<String, List<Category>> read_dataset_AG = test.read_dataset_AG(AG_DataType.TITLEANDDESCRIPTION);
		Map<String, Integer> mapResult = new HashMap<String, Integer>();;
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
				}
				else if(gtlist.get(0).getTitle().equals("Sports")&&strObtainedCat.getTitle().equals("Sport")) {
					countCorrect++;
				}
				else
				{
					int count = mapResult.containsKey(strObtainedCat.getTitle()) ? mapResult.get(strObtainedCat.getTitle()) : 0;
					mapResult.put(strObtainedCat.getTitle(), count + 1);

				}
				FileUtil.writeDataToFile(Arrays.asList(s), directory+File.separator+ ++i,false);
			}
		}
		System.out.println("countCorrect "+countCorrect);
		Print.printMap(mapResult);
	}
}
