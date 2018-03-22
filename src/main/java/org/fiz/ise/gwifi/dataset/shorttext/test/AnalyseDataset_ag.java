package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.SynchronizedCounter;

import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class AnalyseDataset_ag {
	private static final String DATASET_TEST_AG = Config.getString("DATASET_TEST_AG","");
	private static Wikipedia wikipedia = WikipediaSingleton.getInstance().wikipedia;
	private static SynchronizedCounter counterTruePositive;
	private static Integer count=250;
	private static boolean LOAD_MODEL = Config.getBoolean("LOAD_MODEL", false);
	private final static TestDatasetType_Enum TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE"); 

	public static void main(String[] args) {
		initializeVariables();

	}
	private static void initializeVariables() {
		if (LOAD_MODEL) {
			LINE_modelSingleton.getInstance();
		}
		if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.AG)) {
			dataset_AG();
		}
	}
	public static void dataset_AG() {

		try {
			counterTruePositive= new SynchronizedCounter();
			Map<Integer, Category> mapLabel = new HashMap<>();
			mapLabel.put(1, wikipedia.getCategoryByTitle("World"));
			//mapLabel.put(2, wikipedia.getCategoryByTitle("Sports"));
			mapLabel.put(2, wikipedia.getCategoryByTitle("Politics and sports"));
			mapLabel.put(3, wikipedia.getCategoryByTitle("Business"));
			mapLabel.put(4, wikipedia.getCategoryByTitle("Science"));
			mapLabel.put(5, wikipedia.getCategoryByTitle("Technology"));
			List<String> lines = new ArrayList<>(generateRandomDataset_AG());
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			for (int i = 0; i < arrLines.length; i++) {
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
				//				String title = split[1].substring(1,split[1].length()-1);
				String description = split[2].replace("\"", "");
				//				String description = split[2].substring(1,split[2].length()-1);
				Category bestMatchingCategory = HeuristicApproach_loadModel.getBestMatchingCategory(description,gtList);
				if (gtList.contains(bestMatchingCategory)) {
					counterTruePositive.increment();
					System.out.println("True positive "+counterTruePositive.value()+" total processed: "+i);
				}
				else{
					System.out.println("GT :"+gtList+", responce: "+  bestMatchingCategory+" total processed: "+i+"True positive "+counterTruePositive.value());
				}
			}

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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
				if (count<200) {
					map.put(label, map.getOrDefault(label, 1) + 1);
					if (label.equals("2")) {
						result.add(l);
					}
				}
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
}
