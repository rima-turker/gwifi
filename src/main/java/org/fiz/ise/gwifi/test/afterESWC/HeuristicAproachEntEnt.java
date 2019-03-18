package org.fiz.ise.gwifi.test.afterESWC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.Model_LINE;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.EmbeddingsService;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Request_LINEServer;

import com.mongodb.util.Hash;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class HeuristicAproachEntEnt {
	private final static TestDatasetType_Enum TEST_DATASET_TYPE = Config.getEnum("TEST_DATASET_TYPE");
	private static boolean LOAD_MODEL = Config.getBoolean("LOAD_MODEL", false);
	private static final Logger LOG = Logger.getLogger(HeuristicAproachEntEnt.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	/*
	 * The input of the function is a short text( List<Category> gtList is just for printing purpose)
	 *  
	 * The main purpose of this class is the calculate the similarity for each predefined category just as {World,Business,Sports,Technology}
	 * and return the most similar category to given text
	 * 
	 */

	
	public static String getBestMatchingCategoryWithAvg(String shortText, List<Category> gtList) {

		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		HeuristicAproachEntEnt heuristic = new HeuristicAproachEntEnt();
		Map<String, Map<String, Double>> mapScoreForCounting = new HashMap<String, Map<String, Double>>();
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
			Map<Integer, Map<Integer, Double>> contextSimilarity = new HashMap<>(
					calculateContextEntitySimilarities(filteredAnnotations));//the similarity between entities present in the text are calculated 
			double totalScore=0.0;
			for (Category mainCat : setMainCategories) { //iterate over categories and calculate a score for each of them
				double score = 0.0; 
				for (Annotation a : filteredAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())&&WikipediaSingleton.getInstance().getArticle(a.getTitle())!=null) { //we had so many noisy entities therefore filtering required
						double tempScore=0;
						if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.AG)) {
							tempScore = heuristic.calculateScore_AG(a, WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle()), contextSimilarity);
							//mainBuilder.append(a.getMention()+"\t"+a.getTitle()+"\t"+mainCat.getTitle()+"\t"+tempScore+"\n");

							if (mapScoreForCounting.containsKey(a.getTitle())) {
								Map<String, Double> temp = new HashMap<String, Double>(mapScoreForCounting.get(a.getTitle()));
								for (Entry<String, Double> e : temp.entrySet()) {
									if (e.getValue()<tempScore) {
										temp.remove(e.getKey());
										temp.put(mainCat.getTitle(), tempScore);
									}
								}
								mapScoreForCounting.put(a.getTitle(), temp);
							}
							else 
							{
								Map<String, Double> temp = new HashMap<String, Double>();
								temp.put(mainCat.getTitle(), tempScore);
								mapScoreForCounting.put(a.getTitle(), temp);
							}
						}
						else if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.WEB_SNIPPETS)) {
							tempScore = heuristic.calculateScore_WEB(a, WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle()), contextSimilarity);
						}
						score +=tempScore ;
					} 
				}  
				totalScore+=score;
				mapScore.put(mainCat, score);
			}
			Map<Category, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			List<Double> valuesList = new LinkedList<Double>(sortedMap.values());

			Category firstElement = MapUtil.getFirst(sortedMap).getKey();
			int index=1;
			double countCorrect=0;
			for (Entry<String, Map<String, Double>> e : mapScoreForCounting.entrySet()) {
				Map<String, Double> temp = new HashMap<String, Double>(e.getValue());
				Map.Entry<String,Double> entry = temp.entrySet().iterator().next();
				String key = entry.getKey();
				if (key.equals(firstElement.getTitle())) {
					countCorrect++;
				}
			}
			double avg = countCorrect/filteredAnnotations.size();
			return firstElement.getTitle()+"\t"+avg;

		} catch (Exception e) {
			System.out.println();
			e.printStackTrace();
		}
		return null;
	}
	public static Category getBestMatchingCategory(String shortText, List<Category> gtList) {

		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		HeuristicAproachEntEnt heuristic = new HeuristicAproachEntEnt();
		Map<String, Map<String, Double>> mapScoreForCounting = new HashMap<String, Map<String, Double>>();
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
			Map<Integer, Map<Integer, Double>> contextSimilarity = new HashMap<>(
					calculateContextEntitySimilarities(filteredAnnotations));//the similarity between entities present in the text are calculated 
			double totalScore=0.0;
			for (Category mainCat : setMainCategories) { //iterate over categories and calculate a score for each of them
				double score = 0.0; 
				for (Annotation a : filteredAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())&&WikipediaSingleton.getInstance().getArticle(a.getTitle())!=null) { //we had so many noisy entities therefore filtering required
						double tempScore=0;
						if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.AG)) {
							tempScore = heuristic.calculateScore_AG(a, WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle()), contextSimilarity);
							//mainBuilder.append(a.getMention()+"\t"+a.getTitle()+"\t"+mainCat.getTitle()+"\t"+tempScore+"\n");

							if (mapScoreForCounting.containsKey(a.getTitle())) {
								Map<String, Double> temp = new HashMap<String, Double>(mapScoreForCounting.get(a.getTitle()));
								for (Entry<String, Double> e : temp.entrySet()) {
									if (e.getValue()<tempScore) {
										temp.remove(e.getKey());
										temp.put(mainCat.getTitle(), tempScore);
									}
								}
								mapScoreForCounting.put(a.getTitle(), temp);
							}
							else 
							{
								Map<String, Double> temp = new HashMap<String, Double>();
								temp.put(mainCat.getTitle(), tempScore);
								mapScoreForCounting.put(a.getTitle(), temp);
							}
						}
						else if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.WEB_SNIPPETS)) {
							tempScore = heuristic.calculateScore_WEB(a, WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle()), contextSimilarity);
						}
						score +=tempScore ;
					} 
				}  
				totalScore+=score;
				mapScore.put(mainCat, score);
				//mainBuilder.append(mainCat.getTitle()+"\t"+score/filteredAnnotations.size()+"\n"+"\n");
			}
			//mainBuilder.append("\n");
			Map<Category, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));

			List<Double> valuesList = new LinkedList<Double>(sortedMap.values());

			Category firstElement = MapUtil.getFirst(sortedMap).getKey();
			int index=1;

//			for (Entry<Category, Double> e : sortedMap.entrySet()) {
//				mainBuilder.append(e.getKey() + " " + e.getValue() + "\n");
//			}
			//mainBuilder.append(firstElement+"\n");
			//mainBuilder.append("\n");
			//mainBuilder.append("\n");

//			for (Entry<String, Map<String, Double>> e : mapScoreForCounting.entrySet()) {
//				mainBuilder.append(e.getKey() + " " + e.getValue() + "\n");
//			}
//			mainBuilder.append(firstElement+"\n");

			double countCorrect=0;
			for (Entry<String, Map<String, Double>> e : mapScoreForCounting.entrySet()) {
				Map<String, Double> temp = new HashMap<String, Double>(e.getValue());
				Map.Entry<String,Double> entry = temp.entrySet().iterator().next();
				String key = entry.getKey();
				if (key.equals(firstElement.getTitle())) {
					countCorrect++;
				}
			}
			double avg = countCorrect/filteredAnnotations.size();
			
			mainBuilder.append("\n Avg:\t"+firstElement.getTitle()+"\t"+countCorrect+"\t"+filteredAnnotations.size()+"\t"+avg+"\n");

			if (lstAnnotations.size()<1) {
				secondLOG.info("Could not find any annpotation");
			}
			if (!gtList.contains(firstElement)) {
				secondLOG.info(mainBuilder.toString());
			}
			else {
				resultLog.info(mainBuilder.toString());
			}



			//			for (Entry<Category, Double> e : sortedMap.entrySet()) {
			//				if (index<setMainCategories.size()) {
			//					mainBuilder.append(e.getKey() + " " +e.getValue()+" " + (e.getValue()/totalScore) +" "+((e.getValue()/totalScore)-(valuesList.get(index)/totalScore)) +"\n");
			//				}
			//				index++;
			//			}
			//			if (lstAnnotations.size()<1) {
			//				secondLOG.info("Could not find any annotation");
			//			}
			//			if (!gtList.contains(firstElement)) {
			////				secondLOG.info(firstElement+"\t"+sortedMap.get(firstElement)/totalScore);
			//				secondLOG.info(firstElement+"\t"+sortedMap.get(firstElement)/totalScore+"\t"+((sortedMap.get(firstElement)/totalScore)-(valuesList.get(index)/totalScore)+"\t"+(sortedMap.get(firstElement)/totalScore)/(valuesList.get(index)/totalScore)));
			//			}
			//			else {
			////				resultLog.info(firstElement+"\t"+sortedMap.get(firstElement)/totalScore);
			//				resultLog.info(firstElement+"\t"+sortedMap.get(firstElement)/totalScore+"\t"+((sortedMap.get(firstElement)/totalScore)-(valuesList.get(index)/totalScore)+"\t"+(sortedMap.get(firstElement)/totalScore)/(valuesList.get(index)/totalScore)));
			//			}

			return firstElement;

		} catch (Exception e) {
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
	private double calculateScore_WEB(Annotation a, Article cArticle, Map<Integer, Map<Integer, Double>> contextSimilarity) {
		double P_e_c=0; 
		double P_Se_c=1;
		//		if(a.getId()==30653) {
		if(a.getMention().getTerm().toLowerCase().trim().equals("consumption")||a.getMention().getTerm().toLowerCase().trim().equals("consumption")) {
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Consumption (economics)"), cArticle); 
		}
		if(a.getId()==13930) {//House band --> white house
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().wikipedia.getArticleById(33057), cArticle); 
		}

		else {
			P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle(a.getTitle()), cArticle);
			P_Se_c=get_P_Se_c(a);
		}
		double P_Ce_e=1;
		if (contextSimilarity.size()>1) {
			P_Ce_e=get_P_Ce_e_efficient(a.getId(),contextSimilarity);
		}
		return (P_e_c*P_Se_c*P_Ce_e);
	}
	/*
	 * This function calculates a score for a given annotation and a main category
	 */
	private double calculateScore_AG(Annotation a, Article cArticle,
			Map<Integer, Map<Integer, Double>> contextSimilarity) {
		//		double P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle(a.getTitle()), mainCat); 
		//		double P_e_c = get_P_e_c(WikipediaSingleton.getInstance().getArticle(a.getTitle()), mainCat); 
		double P_e_c=0; 
		double P_Se_c=1;
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
			P_Se_c=get_P_Se_c(a);
		}
		double P_Ce_e=1;
		//		if (contextSimilarity.size()>1) {
		//			P_Ce_e=get_P_Ce_e_efficient(a.getId(),contextSimilarity);
		//		}
		return P_e_c;
		//return (P_e_c*P_Se_c*P_Ce_e);
	}
	private static Map<Integer, Map<Integer, Double>> calculateContextEntitySimilarities(List<Annotation> annotations) {
		Map<Integer, Map<Integer, Double>> mapContextSimilarity = new HashMap<>();
		for (Annotation a : annotations) {
			Map<Integer, Double> temp = new HashMap<>();
			for (Annotation c : annotations) {
				double similarity = .0;
				if (LOAD_MODEL) {
					similarity = (LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(a.getId()),
							String.valueOf(c.getId())));
				} else {
					similarity = (EmbeddingsService.getSimilarity(String.valueOf(a.getId()),
							String.valueOf(c.getId())));
				}
				temp.put(c.getId(), similarity);
			}
			mapContextSimilarity.put(a.getId(), temp);
		}
		return mapContextSimilarity;
	}

	public static double get_P_e_c(Article article, Article cArticle) {
		//		System.out.println(article+" "+cArticle);
		//		for(Entry<String,List<String>> e : TestBasedonSortTextDatasets.CACHE_nearestWords.entrySet()) {
		//			System.out.println(e.getKey()+" "+e.getValue());
		//		}
		//		List<String> lst = new ArrayList<>(TestBasedonSortTextDatasets.CACHE_nearestWords.get(cArticle.getTitle()));
		//		System.out.println("List of most sim:" + lst);
		//		double score=LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(article.getId()), String.valueOf(cArticle.getId()));
		//		System.out.println(score);
		//		for(String s: lst) {
		//			score+=LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(article.getId()), String.valueOf(s));
		//		}
		//		System.out.println("return score "+score);
		return LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(article.getId()), String.valueOf(cArticle.getId()));
	}
	//	public static double get_P_e_c(Article article, Article cArticle) {
	//		return LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(article.getId()), String.valueOf(cArticle.getId()));
	//	}

	private static double get_P_Se_c(Annotation a) {// comes from EL system weight value because we calculate the
		return a.getWeight();
	}

	/**
	 * This method takes the all the context entities and tries to calculate the
	 * probabilities of the given an entitiy and all the other context entities and
	 * sums them up and takes the avarage
	 * 
	 * @return the avarage result
	 */
	private static double get_P_Ce_e_efficient(Integer mainId,
			Map<Integer, Map<Integer, Double>> mapContextSimilarity) { // Context entities an the entity(already
		// disambiguated)
		double result = 0.0;
		// double result =1.0;
		double countNonZero = 0;
		Map<Integer, Double> temp = new HashMap<>(mapContextSimilarity.get(mainId));
		for (Entry<Integer, Double> e : temp.entrySet()) {
			double similarity = e.getValue();
			if (!Double.isNaN(similarity) && similarity > 0.0 && similarity != 1.0) {
				countNonZero++;
				result += similarity;
				// result*=similarity;
			}
		}
		if (countNonZero == 0) {
			return 0.0;
		}
		return result / countNonZero;
		// return result;
	}

	public static Entry<Category, Double> getMostSimilarCategory(Annotation annotation, Category mainCategory) {
		if (annotation != null) {
			Set<Category> categories = new HashSet<>(
					CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapMainCatAndSubCats
					.get(mainCategory));
			categories.add(mainCategory);
			Map<Category, Double> map = new HashMap<>();
			for (Category category : categories) {
				if (LOAD_MODEL) {
					if (LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(category.getId()))
							&& LINE_modelSingleton.getInstance().lineModel
							.hasWord(String.valueOf(annotation.getId()))) {
						double similarity = 0.0;
						try {
							similarity = LINE_modelSingleton.getInstance().lineModel
									.similarity(String.valueOf(annotation.getId()), String.valueOf(category.getId()));
							map.put(category, similarity);
						} catch (Exception e) {
							System.out.println("exception finding the similarity: " + similarity);
						}
					} else {
						LOG.info(
								"LINE model does not contain the category: " + category + " or " + annotation.getURL());
					}
				} else {
					double similarity = 0.0;
					try {
						similarity = Request_LINEServer.getSimilarity(String.valueOf(annotation.getId()),
								String.valueOf(category.getId()), Model_LINE.LINE_1st_Complex);
						if (similarity > 0) {
							map.put(category, similarity);
						}
					} catch (Exception e) {
						System.out.println("exception finding the similarity: " + similarity);
					}
				}
			}

			Map<Category, Double> mapSorted = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));
			return MapUtil.getFirst(mapSorted);
		}
		return null;
	}

	public static Entry<Category, Double> getMostSimilarCategory(Annotation annotation) {
		if (annotation != null) {
			Set<Category> categories = new HashSet<>(
					CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setAllCategories);
			Map<Category, Double> map = new HashMap<>();
			for (Category category : categories) {
				if (LOAD_MODEL) {
					if (LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(category.getId()))
							&& LINE_modelSingleton.getInstance().lineModel
							.hasWord(String.valueOf(annotation.getId()))) {
						double similarity = 0.0;
						try {
							similarity = LINE_modelSingleton.getInstance().lineModel
									.similarity(String.valueOf(annotation.getId()), String.valueOf(category.getId()));
							map.put(category, similarity);
						} catch (Exception e) {
							System.out.println("exception finding the similarity: " + similarity);
						}
					} else {
						LOG.info(
								"LINE model does not contain the category: " + category + " or " + annotation.getURL());
					}
				} else {
					double similarity = 0.0;
					try {
						similarity = Request_LINEServer.getSimilarity(String.valueOf(annotation.getId()),
								String.valueOf(category.getId()), Model_LINE.LINE_COMBINED_2nd);
						if (similarity > 0) {
							map.put(category, similarity);
						}
					} catch (Exception e) {
						System.out.println("exception finding the similarity: " + similarity);
					}
				}
			}
			Map<Category, Double> mapSorted = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));
			return MapUtil.getFirst(mapSorted);
		}
		return null;
	}
}

