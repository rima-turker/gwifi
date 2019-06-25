package org.fiz.ise.gwifi.test.afterESWC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.xpath.operations.Gt;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.VectorUtil;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class HeuristicBasedOnEntitiyVectorSimilarity {
	private final static TestDatasetType_Enum TEST_DATASET_TYPE = Config.getEnum("TEST_DATASET_TYPE");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	private final static Integer NUMBER_OF_MOST_SIMILAR_ARTICLES= Config.getInt("NUMBER_OF_MOST_SIMILAR_ARTICLES",-1);
	private final static Map<Article, double[]> CACHE_mostSimilarNArticles = new HashMap<>();

	public static void initializeMostSimilarCache() {
		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		System.out.println(setMainCategories);
		for (Category mainCat : setMainCategories) { //iterate over categories and calculate a score for each of them
			Article amainCat = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle());
			Collection<String> wordsNearest = new ArrayList<String>(LINE_modelSingleton.getInstance().lineModel.wordsNearest(String.valueOf(amainCat.getId()), NUMBER_OF_MOST_SIMILAR_ARTICLES));
			wordsNearest.add(String.valueOf(amainCat.getId()));
			List<String> list = new ArrayList<String>(wordsNearest);
			double[] documentVec = VectorUtil.getSentenceVector(list,LINE_modelSingleton.getInstance().lineModel);
			CACHE_mostSimilarNArticles.put(amainCat, documentVec);
		}
		System.out.println("Finished initializing the cache");
		Print.printMap(CACHE_mostSimilarNArticles);
	}		

	/*
	 * One time experiment: instead of assigning one single category for each training sample this helps to assign N most similar category/article
	 */
	public static List<Article> getBestMatchingNArticles(String shortText, int n) {
		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		HeuristicBasedOnEntitiyVectorSimilarity heuristic = new HeuristicBasedOnEntitiyVectorSimilarity();
		StringBuilder mainBuilder = new StringBuilder();
		try {
			Map<Article, Double> mapScore = new HashMap<>();
			mainBuilder.append(shortText + "\n");
			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);//annotate the given text
			List<Annotation> filteredAnnotations = new ArrayList<>(filterEntitiesNotInVectorSpace(lstAnnotations));

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

			List<Article> lst = new ArrayList<Article>();
			int count=0;
			for(Entry <Article, Double> e :sortedMap.entrySet()) {
				if (count<n) {
					lst.add(e.getKey());
					count++;
				}
				else
					break;
			}
			return lst;
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}
	public static Article getBestMatchingArticlewithThreshold(String shortText, double threshold) {

		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		HeuristicBasedOnEntitiyVectorSimilarity heuristic = new HeuristicBasedOnEntitiyVectorSimilarity();
		StringBuilder mainBuilder = new StringBuilder();
		try {
			Map<Article, Double> mapScore = new HashMap<>();
			mainBuilder.append(shortText + "\n");
			StringBuilder strBuild = new StringBuilder();

			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);//annotate the given text
			List<Annotation> filteredAnnotations = new ArrayList<>(filterEntitiesNotInVectorSpace(lstAnnotations));

			mainBuilder.append(strBuild.toString() + "\n" + "\n");

			for (Category mainCat : setMainCategories) { //iterate over categories and calculate a score for each of them
				Article amainCat = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle());
				double score = 0.0; 
				int count =0;
				for (Annotation a : filteredAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())&&WikipediaSingleton.getInstance().getArticle(a.getTitle())!=null) { //we had so many noisy entities therefore filtering required
						double tempScore=0;
						tempScore+= heuristic.calculateScore_AG(a, amainCat);
						score +=tempScore ;
						count++;
					} 
				}
				mapScore.put(amainCat, score/count);

			}
			Map<Article, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Article firstElement = MapUtil.getFirst(sortedMap).getKey();
			if (sortedMap.get(firstElement)>=threshold) {
				return firstElement;
			}
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}

	public static Article getBestMatchingArticleByExtendingCategory(String shortText, List<Article> gtList) {
		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		HeuristicBasedOnEntitiyVectorSimilarity heuristic = new HeuristicBasedOnEntitiyVectorSimilarity();
		StringBuilder mainBuilder = new StringBuilder();
		try {
			Map<Article, Double> mapScore = new HashMap<>();
			mainBuilder.append(shortText + "\n");
			StringBuilder strBuild = new StringBuilder();
			for (Article c : gtList) {
				strBuild.append("Ground Truth"+c + " ");
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
							double ttempScore = heuristic.calculateScore_AG_withExtention(a, amainCat, NUMBER_OF_MOST_SIMILAR_ARTICLES);
							tempScore+=ttempScore;
							mainBuilder.append(a.getMention()+" "+a.getTitle()+":"+mainCat.getTitle()+":"+ttempScore+"\n" );
						}
						else if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.WEB_SNIPPETS)) {
							tempScore+= heuristic.calculateScore_WEB(a, amainCat);
						}
						score +=tempScore ;
					} 
				}
				mapScore.put(amainCat, score);
				mainBuilder.append(amainCat+": "+score+"\n\n");
			}
			Map<Article, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Article firstElement = MapUtil.getFirst(sortedMap).getKey();
			mainBuilder.append("predicted:"+firstElement.getTitle());
			if ((firstElement.getTitle().equals("Business")&&gtList.get(0).getTitle().equals("Software"))||
					firstElement.getTitle().equals("Software")&&gtList.get(0).getTitle().equals("Business")) {
				secondLOG.info(mainBuilder.toString()+"\n--------------------------------------------");
			}

			return firstElement;
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}

	public static Article getBestMatchingArticle(String shortText, List<Article> gtList) {
		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		HeuristicBasedOnEntitiyVectorSimilarity heuristic = new HeuristicBasedOnEntitiyVectorSimilarity();
		StringBuilder mainBuilder = new StringBuilder();
		try {
			Map<Article, Double> mapScore = new HashMap<>();
			mainBuilder.append(shortText + "\n");
			StringBuilder strBuild = new StringBuilder();
			for (Article c : gtList) {
				strBuild.append("Ground Truth"+c + " ");
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
							double ttempScore = heuristic.calculateScore_AG(a, amainCat);
							tempScore+=ttempScore;
							mainBuilder.append(a.getMention()+" "+a.getTitle()+":"+mainCat.getTitle()+":"+ttempScore+"\n" );
						}
						else if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.WEB_SNIPPETS)) {
							tempScore+= heuristic.calculateScore_WEB(a, amainCat);
						}
						score +=tempScore ;
					} 
				}
				mapScore.put(amainCat, score);
				mainBuilder.append(amainCat+": "+score+"\n\n");
			}
			Map<Article, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Article firstElement = MapUtil.getFirst(sortedMap).getKey();
//			mainBuilder.append("predicted:"+firstElement.getTitle());
//			if ((firstElement.getTitle().equals("Business")&&gtList.get(0).getTitle().equals("Software"))||
//					firstElement.getTitle().equals("Software")&&gtList.get(0).getTitle().equals("Business")) {
//				secondLOG.info(mainBuilder.toString()+"\n--------------------------------------------");
//			}
			return firstElement;
		}
		catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}

	public static Category getBestMatchingCategory(String shortText) {

		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		HeuristicBasedOnEntitiyVectorSimilarity heuristic = new HeuristicBasedOnEntitiyVectorSimilarity();
		try {
			Map<Category, Double> mapScore = new HashMap<>();
			StringBuilder strBuild = new StringBuilder();

			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);//annotate the given text
			List<Annotation> filteredAnnotations = new ArrayList<>(filterEntitiesNotInVectorSpace(lstAnnotations));


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
			System.out.println(shortText);
			Print.printMap(sortedMap);	
			System.out.println();

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

	private Article checkAnnotationCorrectness(Article a) {
		if(a.getId()==25614) {
			return WikipediaSingleton.getInstance().getArticle("Racing"); 
		}
		else if(a.getId()==12240) {
			return WikipediaSingleton.getInstance().getArticle("Gold medal"); 
		}
		//		else if(a.getMention().getTerm().toLowerCase().trim().equals("enterprise")||a.getMention().getTerm().toLowerCase().trim().equals("enterprises")) 
		//		{
		//			return WikipediaSingleton.getInstance().getArticle("Enterprise (computer)"); 
		//		}
		else if(a.getId()==870936) 
		{
			return WikipediaSingleton.getInstance().getArticle("Coach (sport)"); 
		}
		else if(a.getId()==60930) 
		{
			return WikipediaSingleton.getInstance().getArticle("New product development"); 
		} 
		else if(a.getId()==2532101) 
		{
			return WikipediaSingleton.getInstance().getArticle("Profit (accounting)"); 
		}
		else if(a.getId()==16888425) 
		{
			return WikipediaSingleton.getInstance().getArticle("Job"); 
		}
		else if(a.getId()==770846) 
		{
			return WikipediaSingleton.getInstance().getArticle("Personal trainer"); 
		}
		else {
			return a;
		}

	}
	private double calculateScore_AG_withExtention(Annotation a, Article cArticle, int n) {
		//return get_P_e_c_withMostSimilarNCats(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()), cArticle, n);
		return get_P_e_c_withMostSimilarNCats(checkAnnotationCorrectness(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())),cArticle, n);
	}
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
	public static double get_P_e_c_withMostSimilarNCats(Article annotationArticle, Article categoryArticle,int n) {
		
		return VectorUtil.cosineSimilarity(CACHE_mostSimilarNArticles.get(categoryArticle), LINE_modelSingleton.getInstance().lineModel.getWordVector(String.valueOf(annotationArticle.getId())));
	}
	public static double get_P_e_c(Article article, Article cArticle) {
		return LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(article.getId()), String.valueOf(cArticle.getId()));
	}
}
