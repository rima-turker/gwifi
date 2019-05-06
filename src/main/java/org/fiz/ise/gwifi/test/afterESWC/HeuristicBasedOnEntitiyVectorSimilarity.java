package org.fiz.ise.gwifi.test.afterESWC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.MapUtil;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class HeuristicBasedOnEntitiyVectorSimilarity {
	private final static TestDatasetType_Enum TEST_DATASET_TYPE = Config.getEnum("TEST_DATASET_TYPE");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");

	public static Article getBestMatchingArticle(String shortText, List<Category> gtList) {

		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		HeuristicBasedOnEntitiyVectorSimilarity heuristic = new HeuristicBasedOnEntitiyVectorSimilarity();
		StringBuilder mainBuilder = new StringBuilder();
		try {
			Map<Article, Double> mapScore = new HashMap<>();
			mainBuilder.append(shortText + "\n");
			StringBuilder strBuild = new StringBuilder();
			for (Category c : gtList) {
				strBuild.append(c + " ");
			}
			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);//annotate the given text
			List<Annotation> filteredAnnotations = new ArrayList<>(filterEntitiesNotInVectorSpace(lstAnnotations));

			mainBuilder.append(strBuild.toString() + "\n" + "\n");

			for (Category mainCat : setMainCategories) { //iterate over categories and calculate a score for each of them
				Article amainCat = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle());
				double score = 0.0; 
				for (Annotation a : filteredAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())&&WikipediaSingleton.getInstance().getArticle(a.getTitle())!=null) { //we had so many noisy entities therefore filtering required
						double tempScore=0;
						if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.AG)) {
							tempScore+= heuristic.calculateScore_AG(a, amainCat);
						}
						else if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.WEB_SNIPPETS)) {
							tempScore+= heuristic.calculateScore_WEB(a, amainCat);
						}
						score +=tempScore ;
					} 
				}
				mapScore.put(amainCat, score);
			}
			Map<Article, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Article firstElement = MapUtil.getFirst(sortedMap).getKey();
			return firstElement;
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}
	
	public static Category getBestMatchingCategory(String shortText, List<Category> gtList) {

		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		HeuristicBasedOnEntitiyVectorSimilarity heuristic = new HeuristicBasedOnEntitiyVectorSimilarity();
		StringBuilder mainBuilder = new StringBuilder();
		try {
			Map<Category, Double> mapScore = new HashMap<>();
			mainBuilder.append(shortText + "\n");
			StringBuilder strBuild = new StringBuilder();
			for (Category c : gtList) {
				strBuild.append(c + " ");
			}
			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);//annotate the given text
			List<Annotation> filteredAnnotations = new ArrayList<>(filterEntitiesNotInVectorSpace(lstAnnotations));

			mainBuilder.append(strBuild.toString() + "\n" + "\n");

			for (Category mainCat : setMainCategories) { //iterate over categories and calculate a score for each of them
				double score = 0.0; 
				for (Annotation a : filteredAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())&&WikipediaSingleton.getInstance().getArticle(a.getTitle())!=null) { //we had so many noisy entities therefore filtering required
						double tempScore=0;
						if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.AG)) {
							tempScore+= heuristic.calculateScore_AG(a, WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle()));
						}
						score +=tempScore ;
					} 
				}
				mapScore.put(mainCat, score);
			}
			Map<Category, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Category firstElement = MapUtil.getFirst(sortedMap).getKey();
			//resultLog.info(shortText+"\t\t"+firstElement.getTitle());
			return firstElement;
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}

	public static List<Annotation> filterEntitiesNotInVectorSpace(List<Annotation> lstAnnotations) {
		List<Annotation> result = new ArrayList<>();
		for(Annotation a : lstAnnotations) {
			if (LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(a.getId()))) {
				result.add(a);
			}
		}
		return result;
	}
		private double calculateScore_WEB(Annotation a, Article cArticle) {
			double P_e_c=0; 
			//		if(a.getId()==30653) {
			if(a.getMention().getTerm().toLowerCase().trim().equals("consumption")||a.getMention().getTerm().toLowerCase().trim().equals("consumption")) {
				P_e_c = get_P_e_c(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Consumption (economics)"), cArticle); 
			}
			if(a.getId()==13930) {//House band --> white house
				P_e_c = get_P_e_c(WikipediaSingleton.getInstance().wikipedia.getArticleById(33057), cArticle); 
			}
	
			else {
				P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle(a.getTitle()), cArticle);
			}
			
			return P_e_c;
		}
	//	/*
	//	 * This function calculates a score for a given annotation and a main category
	//	 */
	private double calculateScore_AG(Annotation a, Article cArticle) {
		double P_e_c=0; 
		if(a.getId()==25614) {
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Racing"), cArticle); 
		}
		else if(a.getId()==12240) {
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Gold medal"), cArticle); 
		}
		else if(a.getMention().getTerm().toLowerCase().trim().equals("enterprise")||a.getMention().getTerm().toLowerCase().trim().equals("enterprises")) 
		{
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Enterprise (computer)"), cArticle); 
		}
		else if(a.getId()==870936) 
		{
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Coach (sport)"), cArticle); 
		}
		else if(a.getId()==60930) 
		{
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("New product development"), cArticle); 
		} 
		else if(a.getId()==2532101) 
		{
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Profit (accounting)"), cArticle); 
		}
		else if(a.getId()==16888425) 
		{
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Job"), cArticle); 
		}
		else if(a.getId()==770846) 
		{
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle("Personal trainer"), cArticle); 
		}
		else {
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle(a.getTitle()), cArticle);
		}
		return P_e_c;
	}

	public static double get_P_e_c(Article article, Article cArticle) {
		return LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(article.getId()), String.valueOf(cArticle.getId()));
	}
}
