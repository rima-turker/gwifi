package org.fiz.ise.gwifi.dataset.test.read;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.fiz.ise.gwifi.dataset.shorttext.test.LabelsOfTheTexts;
import org.fiz.ise.gwifi.util.Config;

import edu.kit.aifb.gwifi.model.Category;

public class ReadTestDataset {
	
	
	public static List<String> read_AG_BasedOnCategory(Category c){
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
					dataset.add(title+" "+description);
				}
//				if (c.getTitle().equals("Technology")) {
//					if (label.equals("4")) {
//						String title = split[1].replace("\"", "");
//						String description = split[2].replace("\"", "");
//						dataset.add(title+" "+description);
//					}
//				}
//				else if (label.equals(valueOfCat)) {
//					String title = split[1].replace("\"", "");
//					String description = split[2].replace("\"", "");
//					dataset.add(title+" "+description);
//				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dataset;
	}
}