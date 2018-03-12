package org.fiz.ise.gwifi.dataset.test.ag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.fiz.ise.gwifi.util.Config;

public class AnalyseDataset {
	private final String DATASET_TEST_AG = Config.getString("DATASET_TEST_AG","");

	public static void main(String[] args) {
		AnalyseDataset data = new AnalyseDataset();
		data.processDataset();
	}
	private void processDataset() {
		try {
			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_AG), "utf-8");
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			for (int i = 0; i < arrLines.length; i++) {
				System.out.println(arrLines[i]);
				String[] split = arrLines[i].split(",");
				String label = split[0].substring(1,split[0].length()-1);
				String title = split[1].substring(1,split[1].length()-1);
				String description = split[2].substring(1,split[2].length()-1);
				System.out.println(label+", "+title+", "+description);
			}
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
			
			

	}
}
