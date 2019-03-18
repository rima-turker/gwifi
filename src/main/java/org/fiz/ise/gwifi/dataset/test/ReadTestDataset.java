package org.fiz.ise.gwifi.dataset.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.Print;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class ReadTestDataset {
	private static Map<Category, Integer> numberOfSamplesPerCategory = new ConcurrentHashMap<>();
	
	public static Map<String,List<Category>> read_dataset_AG(AG_DataType type) {
		Map<String,List<Category>> dataset = new HashMap<>();
		Map<Integer, Category> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_AG());
		try {
			NLPAnnotationService service = AnnotationSingleton.getInstance().service;
			List<String> lines = FileUtils.readLines(new File(Config.getString("DATASET_TEST_AG","")), "utf-8");
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			int i=0;
			for (i = 0; i < arrLines.length; i++) {
				List<Category> gtList = new ArrayList<>(); 
				String[] split = arrLines[i].split("\",\"");
				String label = split[0].replace("\"", "");
				if (mapLabel.containsKey(Integer.valueOf(label))) {
					if (label.equals("4")) {
						numberOfSamplesPerCategory.put(mapLabel.get(4), numberOfSamplesPerCategory.getOrDefault(mapLabel.get(4), 0) + 1);
						gtList.add(mapLabel.get(4));
					}
					else {
						numberOfSamplesPerCategory.put(mapLabel.get(Integer.valueOf(label)), numberOfSamplesPerCategory.getOrDefault(mapLabel.get(Integer.valueOf(label)), 0) + 1);
						gtList.add(mapLabel.get(Integer.valueOf(label)));
					}
					if (type==AG_DataType.TITLE) {
						String title = split[1].replace("\"", "");
						List<Annotation> lstAnnotations = new ArrayList<>();
						service.annotate(title, lstAnnotations);//annotate the given text
						if (lstAnnotations.size()<1) {
							//System.out.println(title);
						}
						if (dataset.containsKey(title)) {
							if (!gtList.contains(dataset.get(title).get(0))) {
								gtList.addAll(dataset.get(title));
							}
							title=title+" ";
						}
						dataset.put(title, gtList);
					}
					else if (type==AG_DataType.DESCRIPTION) {
						String description = split[2].replace("\"", "");
						dataset.put(description, gtList);

					}
					if (type==AG_DataType.TITLEANDDESCRIPTION) {
						String title = split[1].replace("\"", "");
						String description = split[2].replace("\"", "");
						dataset.put(title+" "+description, gtList);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataset;
	}
	public static List<String> read_AG_BasedOnType(AG_DataType type){
		List<String> dataset = new ArrayList<>();
		try {
			List<String>  lines = new ArrayList<>(FileUtils.readLines(new File(Config.getString("DATASET_TEST_AG","")), "utf-8"));
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			int i=0;
			for (i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split("\",\"");
				String title = split[1].replace("\"", "");
				String description = split[2].replace("\"", "");
				if (type.equals(AG_DataType.TITLE)) {
					dataset.add(title);
				}
				else if (type.equals(AG_DataType.DESCRIPTION)) {
					dataset.add(description);
				}
				else if (type.equals(AG_DataType.TITLEANDDESCRIPTION)) {
					dataset.add(title+" "+description);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dataset;
	}
	public static List<String> read_AG_BasedOnCategory(Category c, AG_DataType type){
		List<String> dataset = new ArrayList<>();
		List<String> lines;
		Map<Category, Integer> map = new HashMap<>(LabelsOfTheTexts.getCatValue_AG());
		try {
			lines = new ArrayList<>(FileUtils.readLines(new File(Config.getString("DATASET_TEST_AG","")), "utf-8"));
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			int i=0;
			String valueOfCat = String.valueOf(map.get(c));
			for (i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split("\",\"");
				String label = split[0].replace("\"", "");
				if (label.equals(valueOfCat)) {
					String title = split[1].replace("\"", "");
					String description = split[2].replace("\"", "");
					if (type.equals(AG_DataType.TITLE)) {
						dataset.add(title);
					}
					else if (type.equals(AG_DataType.DESCRIPTION)) {
						dataset.add(description);
					}
					else if (type.equals(AG_DataType.TITLEANDDESCRIPTION)) {
						dataset.add(title+" "+description);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dataset;
	}
	public static List<String> read_WEB(){
		try {
			List<String> dataset = new ArrayList<>();
			List<String> lines = FileUtils.readLines(new File(Config.getString("DATASET_TEST_WEB","")), "utf-8");
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			for (int i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split(" ");
				String label = split[split.length-1];
				String snippet = arrLines[i].substring(0, arrLines[i].length()-(label).length()).trim();
				dataset.add(snippet);
			}
			return dataset;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
	public static List<String> read_WEB_BasedOnCategory(Category c){
		try {
			List<String> dataset = new ArrayList<>();
			List<String> lines = FileUtils.readLines(new File(Config.getString("DATASET_TEST_WEB","")), "utf-8");
			System.out.println("size of the file "+lines.size());
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			for (int i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split(" ");
				String label = split[split.length-1];
				String snippet = arrLines[i].substring(0, arrLines[i].length()-(label).length()).trim();
				if (label.contains(c.getTitle().toLowerCase())) {
					dataset.add(snippet);
				}
			}
			return dataset;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
}