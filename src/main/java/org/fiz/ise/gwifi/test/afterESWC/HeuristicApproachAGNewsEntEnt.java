package org.fiz.ise.gwifi.test.afterESWC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.PageCategorySingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.model.Model_LINE;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.EmbeddingsService;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Request_LINEServer;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;
@Deprecated
public class HeuristicApproachAGNewsEntEnt {

	private final static TestDatasetType_Enum TEST_DATASET_TYPE = Config.getEnum("TEST_DATASET_TYPE");
	private static boolean LOAD_MODEL = Config.getBoolean("LOAD_MODEL", false);
	private final static Integer DEPTH_OF_CAT_TREE = Config.getInt("DEPTH_OF_CAT_TREE", 0);
	private static final Logger LOG = Logger.getLogger(HeuristicApproachAGNewsEntEnt.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	private final static Map<String, Set<Category>> mapDepthCategory = new HashMap<>(
			CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapCategoryDept);

	/*
	 * The input of the function is a short text( List<Category> gtList is just for printing purpose)
	 *  
	 * The main purpose of this class is the calculate the similarity for each predefined category just as {World,Business,Sports,Technology}
	 * and return the most similar category to given text
	 * 
	 */
	public static Category getBestMatchingCategory(String shortText, List<Category> gtList) {
		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		HeuristicApproachAGNewsEntEnt heuristic = new HeuristicApproachAGNewsEntEnt();
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
			for (Category mainCat : setMainCategories) { //iterate over categories and calculate a score for each of them
				double score = 0.0; 
				for (Annotation a : filteredAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())&&WikipediaSingleton.getInstance().getArticle(a.getTitle())!=null) { //we had so many noisy entities therefore filtering required
						double tempScore;
						if (mainCat.getTitle().equalsIgnoreCase("Sports")) {
							tempScore = heuristic.calculateScore(a, WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Sport"), contextSimilarity);
						}
						else{
							tempScore = heuristic.calculateScore(a, WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(mainCat.getTitle()), contextSimilarity);
						}
						score +=tempScore ;
					} 
				}  
				mapScore.put(mainCat, score);
			}
			mainBuilder.append("\n");
			Map<Category, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));

			Category firstElement = MapUtil.getFirst(sortedMap).getKey();

			for (Entry<Category, Double> e : sortedMap.entrySet()) {
				mainBuilder.append(e.getKey() + " " + e.getValue() + "\n");
			}
			if (lstAnnotations.size()<1) {
				secondLOG.info("Could not find any annpotation");
			}
			if (!gtList.contains(firstElement)) {
				secondLOG.info(mainBuilder.toString());
			}
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
	private static void findCandidates(String shortText) {
		System.out.println(shortText);
		Map<String, Page> m = new HashMap<>();
		for (Topic t : AnnotationSingleton.getInstance().getCandidates(shortText)) {
			System.out.println(t.getReferences().get(0).getLabel() + " " + t.getDisplayName() + " " + t.getWeight());
		}
	}
	private double calculateScoreBasedWeightMaxCatSimilarity(Annotation a, Category mainCat) {
		Entry<Category, Double> entry = getMostSimilarCategory(a, mainCat);
		double P_e_c = 0.0;
		if (entry != null) {
			P_e_c = entry.getValue();
		}
		double P_Se_c = get_P_Se_c(a);
		return (P_e_c * P_Se_c);
	}
	/*
	 * This function calculates a score for a given annotation and a main category
	 */
	private double calculateScore(Annotation a, Article cArticle,
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
		//		System.out.println(LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(article.getId())));
		//		System.out.println(LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(cArticle.getId())));
		//		System.out.println(LINE_modelSingleton.getInstance().lineModel.getWordVector(String.valueOf(article.getId())));
		//		System.out.println("similarity"+" "+article.getTitle()+" "+cArticle.getTitle()+" "+LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(article.getId()), String.valueOf(cArticle.getId())));
		return LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(article.getId()), String.valueOf(cArticle.getId()));
	}
	private static double getSimilarity(String id, String id2 ) {
		if (LOAD_MODEL) {
			return LINE_modelSingleton.getInstance().lineModel.similarity(id, id2);
		}
		return EmbeddingsService.getSimilarity(id, id2);
	}
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

	/**
	 * This method takes the all the context entities and tries to calculate the
	 * probabilities of the given an entitiy and all the other context entities and
	 * sums them up
	 * 
	 * @return
	 */
	private static double get_P_Ce_e(Integer mainId, List<Annotation> contextEntities) { // Context entities an the
		// entity(already
		// disambiguated)
		double result = 0.0;
		double countNonZero = 0;
		for (Annotation a : contextEntities) {
			double temp = .0;
			if (LOAD_MODEL) {
				temp = (LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(mainId),
						String.valueOf(a.getId())));
			} else {
				temp = (EmbeddingsService.getSimilarity(String.valueOf(mainId), String.valueOf(a.getId())));
			}
			if (!Double.isNaN(temp) && temp > 0.0) {
				countNonZero++;
				result += temp;
			} else {
				LOG.info("similarity could not be calculated entity-entity: " + mainId + " " + a.getURL());
			}
		}
		return result / countNonZero;
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
