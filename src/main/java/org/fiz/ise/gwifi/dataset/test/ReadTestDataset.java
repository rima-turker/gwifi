package org.fiz.ise.gwifi.dataset.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.Print;

import edu.kit.aifb.gwifi.model.Category;

public class ReadTestDataset {

	public static List<String> read_AG_BasedOnType(String agDataset, AG_DataType type){
		List<String> dataset = new ArrayList<>();
		try {
			List<String>  lines = new ArrayList<>(FileUtils.readLines(new File(agDataset), "utf-8"));
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
	@Deprecated
	public static List<String> read_AG_Discription(){
		List<String> dataset = new ArrayList<>();
		try {
			List<String>  lines = new ArrayList<>(FileUtils.readLines(new File(Config.getString("DATASET_TEST_AG","")), "utf-8"));
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			int i=0;
			for (i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split("\",\"");
				String description = split[2].replace("\"", "");
				dataset.add(description);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dataset;
	}
	@Deprecated
	public static List<String> read_AG_TitleAndDiscription(){
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
				dataset.add(title+" "+description);
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