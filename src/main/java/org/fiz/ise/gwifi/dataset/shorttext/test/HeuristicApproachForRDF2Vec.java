package org.fiz.ise.gwifi.dataset.shorttext.test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

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
import edu.kit.aifb.gwifi.annotation.detection.NLPTopicDetector;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.mingyuzuo.Wiki;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;
import gnu.trove.impl.hash.THashIterator;

public class HeuristicApproachForRDF2Vec {
	private final static TestDatasetType_Enum TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private static Set<Category> setMainCategories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
	private static boolean LOAD_MODEL = Config.getBoolean("LOAD_MODEL", false);
	private final static Integer DEPTH_OF_CAT_TREE = Config.getInt("DEPTH_OF_CAT_TREE", 0);
	private static Map<Category, Set<Category>> mapCategories;
	private static final Logger LOG = Logger.getLogger(HeuristicApproach_CONLL.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	private final static Map<String, Set<Category>> mapDepthCategory = new HashMap<>(
			CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapCategoryDept);

	/*
	 * The main purpose of this class is the calculate the similarity and decide 
	 * which category a text belongs to based on the probability
	 */
	public static Category getBestMatchingCategory(String shortText,List<Category> gtList) {
		Set<Category> setMainCategories = new HashSet<>(
				CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories); //get predefined cats
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		HeuristicApproachForRDF2Vec heuristic = new HeuristicApproachForRDF2Vec();
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
					//if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())) { //we had so many noisy entities therefore filtering required
					if (!AnnonatationUtil.getEntityBlackList_WebSnippets().contains(a.getId())) {
						double tempScore = heuristic.calculateScore(a, mainCat, contextSimilarity);
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
			e.printStackTrace();
		}
		return null;
	}

	public static List<Annotation> filterEntitiesNotInVectorSpace(List<Annotation> lstAnnotations) {
		List<Annotation> result = new ArrayList<>();
		for(Annotation a : lstAnnotations) {
			if (LINE_modelSingleton.getInstance().lineModel.hasWord(convertURIToRDF2VecBased(a.getURL(),true))) {
				result.add(a);
			}
		}
		return result;
	}
	private double calculateScore(Annotation a, Category mainCat,
			Map<Integer, Map<Integer, Double>> contextSimilarity) {
		double P_e_c=0; 
		double P_Se_c=1;
		P_e_c = get_P_e_c(a, mainCat);
		P_Se_c=get_P_Se_c(a);
		double P_Ce_e=1;
		if (contextSimilarity.size()>1) {
			P_Ce_e=get_P_Ce_e_efficient(a.getId(),contextSimilarity);
		}
		return (P_e_c*P_Se_c*P_Ce_e);
	}
	public static String convertURIToRDF2VecBased(String uri,boolean entity) {
		if (entity) {
			return "e_"+uri.replace("http://en.wikipedia.org/wiki/", "").toLowerCase().replaceAll(" ", "_");
		}
		return "c_"+uri.replace(" ", "_").toLowerCase();
//		if (entity) {
//			return "<http://dbpedia.org/resource/"+uri.replace("http://en.wikipedia.org/wiki/", "").replaceAll(" ", "_")+">";
//		}
//		return "<http://dbpedia.org/resource/Category:"+uri.replace("http://en.wikipedia.org/wiki/", "").replaceAll(" ", "_")+">";
	}
	private static String convertURIToConllBased(String uri,boolean entity) {
		if (entity) {
			return "e_"+uri.replace("http://en.wikipedia.org/wiki/", "").toLowerCase().replaceAll(" ", "_");
		}
		return "c_"+uri.replace(" ", "_").toLowerCase();
	}
	private static Map<Integer, Map<Integer, Double>>  calculateContextEntitySimilarities(List<Annotation> annotations) {
		Map<Integer, Map<Integer, Double>> mapContextSimilarity = new HashMap<>();
		for(Annotation a: annotations){
			Map<Integer, Double> temp = new HashMap<>();
			for(Annotation c: annotations){
				double similarity=.0;
				if (LOAD_MODEL) {
					similarity=(LINE_modelSingleton.getInstance().lineModel.similarity(convertURIToRDF2VecBased(a.getTitle(),true), convertURIToRDF2VecBased(c.getTitle(),true)));

					if (!LINE_modelSingleton.getInstance().lineModel.hasWord(convertURIToRDF2VecBased(a.getTitle(),true))) {
						secondLOG.info(convertURIToRDF2VecBased(a.getTitle(),true));
					}
					else {
						resultLog.info(convertURIToRDF2VecBased(a.getTitle(),true));
					}
					if (!LINE_modelSingleton.getInstance().lineModel.hasWord(convertURIToRDF2VecBased(c.getTitle(),true))) {
						secondLOG.info(convertURIToRDF2VecBased(c.getTitle(),true));
					}
					else {
						resultLog.info(convertURIToRDF2VecBased(c.getTitle(),true));
					}
				}
				temp.put(c.getId(), similarity);
			}
			mapContextSimilarity.put(a.getId(), temp);
		}
		return mapContextSimilarity;
	}
	/*
	 * Popularity of the category
	 */
	private static int get_P_c_(Category c){
		return c.getChildArticles().length;
	}
	private static double get_P_e_c(Annotation an, Category mainCat) {
		Article article = WikipediaSingleton.getInstance().getArticle(an.getTitle());
		double result = 0.0;
		final Set<Article> cArticle = new HashSet<>(
				PageCategorySingleton.getInstance().mapMainCatAndArticles.get(mainCat));
		final Set<Article> setOfArticleWithCategoryAndEntity = new HashSet<>();
		for (final Article art : cArticle) {
			List<Article> linksOutList = TestBasedonSortTextDatasets.CACHE.get(art);
			if(linksOutList!=null) {
				if(linksOutList.contains(article)) {
					setOfArticleWithCategoryAndEntity.add(art);
				}
			}else {
				linksOutList = Arrays.asList(art.getLinksOut());
				TestBasedonSortTextDatasets.CACHE.put(art,new ArrayList<>(linksOutList));
				if(linksOutList.contains(article)) {
					setOfArticleWithCategoryAndEntity.add(art);
				}
			}
		}
		if (setOfArticleWithCategoryAndEntity.size() > 0) {
			final HashSet<Category> setOfArticleCategory = new HashSet<>();
			for (final Article a : setOfArticleWithCategoryAndEntity) {
				final HashSet<Category> temp = new HashSet<>(Arrays.asList(a.getParentCategories()));
				setOfArticleCategory.addAll(temp);
			}
			boolean check = false;
			for (Category c : setOfArticleCategory) {
				int depth = -1;

				if (c.getTitle().equals(mainCat.getTitle())) {
					depth = 0;
					check = true;
				}
				else {
					for (Entry<String, Set<Category>> e : mapDepthCategory.entrySet()) {
						if (e.getKey().contains(mainCat.getTitle()) && e.getValue().contains(c)) {
							String Sdept = e.getKey().split("\t")[1];
							depth = Integer.valueOf(Sdept)+1;
							check = true;
							break;
						}
					}
				}
				if (depth == -1) {
					continue;
				}
				double P_cm_c = 1.0 / (depth + 1.0);
				double P_e_c = LINE_modelSingleton.getInstance().lineModel.similarity(convertURIToRDF2VecBased(an.getURL(),true),convertURIToRDF2VecBased(c.getTitle(),false));
				//double P_e_c = getSimilarity(String.valueOf(article.getId()), String.valueOf(c.getId()));
				double temp = P_cm_c * P_e_c;

				if (!Double.isNaN(temp)) {
					result += temp;
				} else {
					LOG.info("similarity could not be calculated category: " + c.getTitle() + " "
							+ c.getChildArticles().length);
				}
			}
			if (check == false) {
				System.out.println("The depth is zero could not find the category in the category tree");
				System.out.println(mainCat);
				System.out.println(setOfArticleCategory);
				System.exit(0);
			}
			return result;
		}
		return 0.0;
	}
	private static double get_P_Se_c(Annotation a) {//comes from EL system weight value because we calculate the confidence based on the prior prob
		return a.getWeight();
	}
	/**
	 * This method takes the all the context entities and tries to calculate the probabilities of the given an 
	 * entitiy and all the other context entities and sums them up and takes the avarage
	 * @return the avarage result
	 */
	private static double get_P_Ce_e_efficient(Integer mainId,Map<Integer, Map<Integer, Double>> mapContextSimilarity){ //Context entities an the entity(already disambiguated) 
		double result =0.0;
		double countNonZero=0;
		Map<Integer, Double> temp = new HashMap<>(mapContextSimilarity.get(mainId));
		for (Entry<Integer, Double> e: temp.entrySet()) {
			double similarity = e.getValue();
			if (!Double.isNaN(similarity)&&similarity>0.0&&similarity!=1.0) {
				countNonZero++;
				result+=similarity;
			}
		}
		if (countNonZero==0) {
			return 0.0;
		}
		return result/countNonZero;
	}

	/**
	 * This method takes the all the context entities and tries to calculate the probabilities of the given an 
	 * entitiy and all the other context entities and sums them up
	 * @return
	 */
	private static double get_P_Ce_e(Integer mainId,List<Annotation> contextEntities){ //Context entities an the entity(already disambiguated) 
		double result =0.0;
		double countNonZero=0;
		for(Annotation a: contextEntities){
			double temp=.0;
			if (LOAD_MODEL) {
				temp=(LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(mainId), String.valueOf(a.getId())));
			}
			else {
				temp =(EmbeddingsService.getSimilarity(String.valueOf(mainId), String.valueOf(a.getId())));
			}
			if (!Double.isNaN(temp)&&temp>0.0) {
				countNonZero++;
				result+=temp;
			}
			else{
				LOG.info("similarity could not be calculated entity-entity: "+mainId+" "+a.getURL());
			}
		}
		return result/countNonZero;
	}
	public static Entry<Category, Double> getMostSimilarCategory(Annotation annotation,Category mainCategory)
	{
		if (annotation!=null) {
			Set<Category> categories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapMainCatAndSubCats.get(mainCategory));
			categories.add(mainCategory);
			Map<Category, Double> map = new HashMap<>();
			for(Category category:categories){
				if (LOAD_MODEL) {
					if (LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(category.getId()))&&LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(annotation.getId()))) {
						double similarity = 0.0;
						try {
							similarity=LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(annotation.getId()), String.valueOf(category.getId()));
							map.put(category, similarity);
						} catch (Exception e) {
							System.out.println("exception finding the similarity: "+similarity);
						}
					}
					else {
						LOG.info("LINE model does not contain the category: "+category+" or "+annotation.getURL());
					}
				}
				else {
					double similarity = 0.0;
					try {
						similarity=Request_LINEServer.getSimilarity(String.valueOf(annotation.getId()), String.valueOf(category.getId()), Model_LINE.LINE_1st_Complex);
						if (similarity>0) {
							map.put(category, similarity);
						}
					} catch (Exception e) {
						System.out.println("exception finding the similarity: "+similarity);
					}
				}
			}	

			Map<Category, Double> mapSorted = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));
			return MapUtil.getFirst(mapSorted);
		}
		return null;
	}
	//	public static Map<Annotation,Map<Category, Double>> initilazeAnnotationCategoryRelation(List<Annotation> annotations){
	//		Map<Annotation,Map<Category, Double>> result = new HashMap<>();
	//		Set<Category> allCategories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setAllCategories);
	//		for(Annotation annotation: annotations) {
	//			if (annotation!=null) {
	//				Map<Category, Double> mapTempCat = new HashMap<>();
	//				for(Category category:allCategories){
	//						double similarity=Request_LINEServer.getSimilarity(String.valueOf(annotation.getId()), String.valueOf(category.getId()), Model_LINE.LINE_COMBINED_2nd);
	//						if (similarity>0) {
	//							mapTempCat.put(category,similarity);
	//						}
	//					}
	//					Map<Category, Double> mapSorted = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapTempCat));
	//					Entry<Category, Double> firstElement = MapUtil.getFirst(mapSorted);
	//					mapTempCat.put(firstElement.getKey(), firstElement.getValue());
	//				}	
	//				result.put(annotation, mapTempCat);
	//			}
	//		}
	//		return result;
	//	}
	public static Entry<Category, Double> getMostSimilarCategory(Annotation annotation)
	{
		if (annotation!=null) {
			Set<Category> categories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setAllCategories);
			Map<Category, Double> map = new HashMap<>();
			for(Category category:categories){
				if (LOAD_MODEL) {
					if (LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(category.getId()))&&LINE_modelSingleton.getInstance().lineModel.hasWord(String.valueOf(annotation.getId()))) {
						double similarity = 0.0;
						try {
							similarity=LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(annotation.getId()), String.valueOf(category.getId()));
							map.put(category, similarity);
						} catch (Exception e) {
							System.out.println("exception finding the similarity: "+similarity);
						}
					}
					else {
						LOG.info("LINE model does not contain the category: "+category+" or "+annotation.getURL());
					}
				}
				else {
					double similarity = 0.0;
					try {
						similarity=Request_LINEServer.getSimilarity(String.valueOf(annotation.getId()), String.valueOf(category.getId()), Model_LINE.LINE_COMBINED_2nd);
						if (similarity>0) {
							map.put(category, similarity);
						}
					} catch (Exception e) {
						System.out.println("exception finding the similarity: "+similarity);
					}
				}
			}	
			Map<Category, Double> mapSorted = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));
			return MapUtil.getFirst(mapSorted);
		}
		return null;
	}
}


