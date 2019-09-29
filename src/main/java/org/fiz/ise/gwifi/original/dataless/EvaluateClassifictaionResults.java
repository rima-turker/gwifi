package org.fiz.ise.gwifi.original.dataless;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LabelsOfTheTexts;

import edu.kit.aifb.gwifi.model.Category;

public class EvaluateClassifictaionResults 
{
	public static void main(String[] args) {
//		String fileName = "AG_News.txt";
		String fileName = "/home/rtue/eclipse-workspace/Resources/DatalessClassificationResults/WebSnippets.txt";
		String fileClassification= "/home/rtue/eclipse-workspace/Resources/DatalessClassificationResults/WEB_W2V_TopDown/log";
		evaluateWebNews(fileName,fileClassification);
		//evaluateAGNews(fileName,fileClassification);
	}
	public static void evaluateWebNews(String fileName, String fileClassification) {
		try {
			List<String> lines = new ArrayList<>(FileUtils.readLines(new File(fileName), "utf-8"));
			List<String> linesClassification = new ArrayList<>(FileUtils.readLines(new File(fileClassification), "utf-8"));
			Map<String, String> mapDataset = new HashMap<>();
			Map<String, String> mapClassificaton = new HashMap<>();
			/*
			 * Business
    Computers
    Culture-Arts-Entertainment
    Education-Science
    Engineering
    Health
    Politics-Society
    Sports
			 */
			
			


			for(String line: lines) {
				String[] split = line.split("\t");
				mapDataset.put(split[0], split[1]);
			}
			for(String line: linesClassification) {
				String[] split = line.split("\t");
				mapClassificaton.put(split[0], split[1]);
			}

			int countRoot=0;
			int countTruePositive=0;
			int countFalsePositive=0;
			for(Entry<String, String> e : mapDataset.entrySet()) {
				String textId = e.getKey();
				String text = e.getValue();
				String[] split = text.split(" ");
				String gtLabel = split[split.length-1].toLowerCase();
				String classificationLabel = mapClassificaton.get(textId);
				if (classificationLabel.equalsIgnoreCase("root")) {
					countRoot++;
					continue;
				}
				if (gtLabel.equalsIgnoreCase(classificationLabel)) {
					countTruePositive++;
				}
				else {
					countFalsePositive++;
				}

			}
			System.out.println("countRoot: "+countRoot+" countFalsePos: "+countFalsePositive+" countTruePos: "+countTruePositive);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}
	public static void evaluateAGNews(String dataset, String classificationResults) {
		try {
			List<String> lines = new ArrayList<>(FileUtils.readLines(new File(dataset), "utf-8"));
//			List<String> linesClassification = new ArrayList<>(FileUtils.readLines(new File("/home/rtue/eclipse-workspace/DatalessClassificationResults/AG_BottomUp/AG_BottomUp.txt"), "utf-8"));
			List<String> linesClassification = new ArrayList<>(FileUtils.readLines(new File(classificationResults), "utf-8"));
			Map<String, String> mapDataset = new HashMap<>();
			Map<String, String> mapClassificaton = new HashMap<>();
			Map<Integer, Category> catValue_AG = LabelsOfTheTexts.getLables_AG_category();
			catValue_AG.put(1, WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Society"));
			catValue_AG.put(4, WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science and technology"));

			for(String line: lines) {
				String[] split = line.split("\t");
				mapDataset.put(split[0], split[1]);
			}
			for(String line: linesClassification) {
				String[] split = line.split("\t");
				mapClassificaton.put(split[0], split[1]);
			}

			int countRoot=0;
			int countTruePositive=0;
			int countFalsePositive=0;
			for(Entry<String, String> e : mapDataset.entrySet()) {
				String textId = e.getKey();
				String text = e.getValue();
				String[] split = text.split("\",\"");
				String gtLabelID = split[0].replace("\"", "");
				String classificationLabel = mapClassificaton.get(textId);
				if (classificationLabel.equalsIgnoreCase("root")) {
					countRoot++;
					continue;
				}
				String gtLabelName = catValue_AG.get(Integer.valueOf(gtLabelID)).getTitle().toLowerCase();
				if (gtLabelName.equalsIgnoreCase(classificationLabel)) {
					countTruePositive++;
				}
				else {
					countFalsePositive++;
				}

			}
			System.out.println("countRoot: "+countRoot+" countFalsePos: "+countFalsePositive+" countTruePos: "+countTruePositive);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}
}
