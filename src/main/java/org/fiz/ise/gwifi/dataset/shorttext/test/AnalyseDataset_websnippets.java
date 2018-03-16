package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.model.Model_LINE;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;

import org.fiz.ise.gwifi.util.Config;

import edu.kit.aifb.gwifi.service.NLPAnnotationService;


public class AnalyseDataset_websnippets {
	private static final Logger LOG = Logger.getLogger(AnalyseDataset_websnippets.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");

	private final String DATASET_TEST_WEB = Config.getString("DATASET_TEST_WEB","");
	AnnotationSingleton singleton;
	public static void main(String[] args) {
		AnalyseDataset_websnippets data = new AnalyseDataset_websnippets();
		data.readRandomlyGeneratedDataset();
	}
	private void readRandomlyGeneratedDataset() {
		try {
			WikipediaSingleton wikiSing = WikipediaSingleton.getInstance();
			Wikipedia wiki = wikiSing.wikipedia;
			AnnotationSingleton singleton = AnnotationSingleton.getInstance();
			NLPAnnotationService service = singleton.service;
			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_WEB), "utf-8");
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			for (int i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split(" ");
				String label = split[split.length-1];
				String snippet = arrLines[i].substring(0, arrLines[i].length()-(label).length()).trim();
				List<Annotation> lstAnnotations = new ArrayList<>();
				System.out.println(snippet);
				if (label.contains("-")) {
					String[] splitLabel = label.split("-");
					for (int j = 0; j < splitLabel.length; j++) {
						System.err.println(wiki.getCategoryByTitle(StringUtils.capitalize(splitLabel[j])));
					}
				}
				else
				{
					Category category = wiki.getCategoryByTitle(StringUtils.capitalize(label));
					System.err.println(category);
				}
				
				HeuristicApproach.getBestMatchingCategory(snippet);
				
//				service.annotate(snippet, lstAnnotations);
//				for(Annotation a :lstAnnotations )
//				{
//					StringBuilder strBuild = new StringBuilder();
//					strBuild.append("Mention: "+a.getMention()+", title: "+a.getTitle()+", weight: "+a.getWeight()+", category:" 
//							+ " "+EmbeddingsService.getMostSimilarCategory(a,Model_LINE.LINE_COMBINED));
//					System.err.println(strBuild.toString());
//
//					//					System.out.println(a.getDisplayName());
//					//					System.out.println(a.getTitle());
//					//					System.out.println(a.getURL());
//					//					System.out.println(a.getWeight());
//					//					System.out.println(a.getMention());
//				}
			}

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
