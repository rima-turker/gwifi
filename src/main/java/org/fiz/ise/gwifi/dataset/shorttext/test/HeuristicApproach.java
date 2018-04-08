package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.util.ArrayList;
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
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.model.Model_LINE;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Request_LINEServer;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.annotation.detection.NLPTopicDetector;
import edu.kit.aifb.gwifi.annotation.detection.Topic;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class HeuristicApproach {

	private final static TestDatasetType_Enum TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private static Set<Category> setMainCategories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
	private static boolean LOAD_MODEL = Config.getBoolean("LOAD_MODEL", false);
	private final static Integer DEPTH_OF_CAT_TREE = Config.getInt("DEPTH_OF_CAT_TREE", 0);
	private static Map<Category, Set<Category>> mapCategories;
	private static final Logger LOG = Logger.getLogger(HeuristicApproach.class);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	/*
	 * The main purpose of this class is the calculate the similarity and decide 
	 * which category a text belongs to based on the probability
	 * 
	 */
	public static Category getBestMatchingCategory(String shortText,List<Category> gtList, Map<Category, Set<Category>> map) {
		//shortText="SINGAPORE -- If you didnt think anybody else could possibly get any more spam than you, then think of Bill Gates. The Microsoft Corp.";
		mapCategories = new HashMap<>(map);
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;

		//		System.out.println(shortText);
		//		Map<String, Page> m = new HashMap<>();
		//		for(Topic t: AnnotationSingleton.getInstance().getCandidates(shortText) ) {
		//			System.out.println(t.getReferences().get(0).getLabel()+" "+t.getDisplayName()+" "+t.getWeight());
		//		}

		StringBuilder mainBuilder = new StringBuilder();
		try {
			//Map<Category, Double> mapScore = new HashMap<>(); 
			Map<Category, Double> mapScoreNoPopularity = new HashMap<>(); 
			Map<Category, Double> mapMostSimilar = new HashMap<>(); 
			mainBuilder.append(shortText+"\n");
			StringBuilder strBuild = new StringBuilder();
			for(Category c: gtList)	{
				strBuild.append(c+" ");
			}
			List<Annotation> lstAnnotations = new ArrayList<>();
			service.annotate(shortText, lstAnnotations);
			mainBuilder.append(strBuild.toString()+"\n"+"\n");
			boolean first =true;
			Map<Integer, Map<Integer, Double>> contextSimilarity = new HashMap<>(calculateContextEntitySimilarities(lstAnnotations));
			for (Category mainCat : setMainCategories) {
				//secondLOG.info(mainCat);
				double score = 0.0; 
				for(Annotation a:lstAnnotations) {
					//					List<Annotation> contextAnnotation = lstAnnotations.stream()
					//							.filter(p -> p.getId()!=a.getId()).collect(Collectors.toList());
					double P_e_c=get_P_e_c(a.getId(), mainCat);
					//	System.out.println(mainCat.getTitle()+" "+a.getTitle()+" P_e_c: "+P_e_c);
					double P_Se_c=get_P_Se_c(a);
					//System.out.println(mainCat.getTitle()+" "+a.getTitle()+" P_Se_c: "+P_Se_c);
					//	double P_Ce_e=get_P_Ce_e(a.getId(),contextAnnotation);
					double P_Ce_e=get_P_Ce_e_efficient(a.getId(),contextSimilarity);
					//System.out.println(mainCat.getTitle()+" "+a.getTitle()+" P_Ce_e: "+P_Ce_e);
					//	System.out.println("result of multiplication: "+(P_e_c*P_Se_c*P_Ce_e));
					//	System.out.println(mainCat+" sim score: "+P_e_c+ " "+a.getTitle());
					score+=(P_e_c*P_Se_c*P_Ce_e);
					//					score+=(P_e_c*P_Ce_e);
					//score+=P_e_c;
					if (first) {
						//System.out.println(mainCat.getTitle()+" EL score: "+(P_Se_c*P_Ce_e)+ " "+a.getTitle());
						Entry<Category, Double> entry = getMostSimilarCategory(a);
						//System.out.println(a.getTitle()+" "+entry.getValue()+" "+entry.getKey().getTitle());
						//						if (mapMostSimilar.containsKey(entry.getKey())) {
						//							mapMostSimilar.put(entry.getKey(), mapMostSimilar.get(entry.getKey())+entry.getValue());
						//						}
						//						else{
						//							mapMostSimilar.put(entry.getKey(), entry.getValue());
						//						}
						//						mainBuilder.append(a.getMention()+", "+a.getTitle()+", "+a.getId()+", weight: "+a.getWeight()+", :"+entry.getKey().getTitle()+", "+entry.getValue()+"\n");
						try {
							if (entry!=null) {
								mainBuilder.append(a.getMention()+", "+a.getTitle()+", EL score: "+(P_Se_c*P_Ce_e)+", :"+entry.getKey().getTitle()+", "+entry.getValue()+"\n");
							}
						} catch (Exception e) {
							System.out.println(e.getMessage()+" "+a.getId()+" "+(P_Se_c*P_Ce_e)+" "+entry);
						}

						//mainBuilder.append(a.getMention()+", "+a.getTitle()+", "+a.getId()+", weight: "+a.getWeight()+"\n");
					}
				}
				first=false;
				//System.out.println(mainCat+" score: "+score);
				//double P_c=get_P_c_(mainCat);
				score=score/lstAnnotations.size();
				mapScoreNoPopularity.put(mainCat, score);
				//				score*=P_c;
				//mapScore.put(mainCat, score);
			}
			mainBuilder.append("\n");
			//Map<Category, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			//			for(Entry<Category, Double> e: sortedMap.entrySet()){			
			//				mainBuilder.append(e.getKey()+" "+e.getValue()+"\n");
			//			}
			Map<Category, Double>  sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScoreNoPopularity));
			Category firstElement = MapUtil.getFirst(sortedMap).getKey();
			//mainBuilder.append("\n" +"without popularity"+"\n");
			for(Entry<Category, Double> e: sortedMap.entrySet()){			
				mainBuilder.append(e.getKey()+" "+e.getValue()+"\n");
			}
			mainBuilder.append(""+"\n");
			mainBuilder.append(""+"\n");
			if (!gtList.contains(firstElement)) {
				secondLOG.info(mainBuilder.toString());
			}
			return firstElement;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	private static Map<Integer, Map<Integer, Double>>  calculateContextEntitySimilarities(List<Annotation> annotations) {
		Map<Integer, Map<Integer, Double>> mapContextSimilarity = new HashMap<>();
		for(Annotation a: annotations){
			Map<Integer, Double> temp = new HashMap<>();
			for(Annotation c: annotations){
				double similarity=.0;
				if (LOAD_MODEL) {
					similarity=(LINE_modelSingleton.getInstance().line_Combined.similarity(String.valueOf(a.getId()), String.valueOf(c.getId())));
				}
				else {
					similarity =(EmbeddingsService.getSimilarity(String.valueOf(a.getId()), String.valueOf(c.getId())));
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
	private static double get_P_e_c(int articleID,Category mainCat) {
		Set<Category> childCategories;
		double result =0.0;
		double countNonZero=0.0;
		if (DEPTH_OF_CAT_TREE==0) {
			double P_Cr_c=0.0;
			double P_e_Cr =0.0;
			if (LOAD_MODEL) {
				P_e_Cr =LINE_modelSingleton.getInstance().line_Combined.similarity(String.valueOf(articleID), String.valueOf(mainCat.getId()));
			}
			else {
				P_e_Cr =EmbeddingsService.getSimilarity(String.valueOf(articleID), String.valueOf(mainCat.getId()));
			}
			if (!Double.isNaN(P_e_Cr)) {
				result+=P_e_Cr;
				countNonZero++;
			}
		}
		else {
			childCategories = new HashSet<>(mapCategories.get(mainCat));
			for(Category c:childCategories) {
				double P_Cr_c=0.0;
				double P_e_Cr =0.0;
				if (LOAD_MODEL) {
					P_Cr_c =1;
					//P_Cr_c = LINE_modelSingleton.getInstance().line_Combined.similarity(String.valueOf(mainCat.getId()), String.valueOf(c.getId()));
					P_e_Cr =LINE_modelSingleton.getInstance().line_Combined.similarity(String.valueOf(articleID), String.valueOf(c.getId()));
				}
				else {
					//P_Cr_c = EmbeddingsService.getSimilarity(String.valueOf(mainCat.getId()), String.valueOf(c.getId()));
					P_Cr_c =1; // here we ignore the relation between root cat and the child cat as we onl care about the relation between an entity and the category
					P_e_Cr =EmbeddingsService.getSimilarity(String.valueOf(articleID), String.valueOf(c.getId()));
				}
				double temp =P_e_Cr*P_Cr_c;
				if (!Double.isNaN(temp)&&temp>0.0) {
					result+=temp;
					countNonZero++;
				}
				else{
					LOG.info("similarity could not be calculated category: "+c.getTitle()+" "+c.getChildArticles().length);
				}
			}
		}
		if (countNonZero==0) {
			return 0.0;
		}
		return result/countNonZero;
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
				temp=(LINE_modelSingleton.getInstance().line_Combined.similarity(String.valueOf(mainId), String.valueOf(a.getId())));
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
	public static Entry<Category, Double> getMostSimilarCategory(Annotation annotation)
	{
		if (annotation!=null) {
			Set<Category> categories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setAllCategories);
			Map<Category, Double> map = new HashMap<>();
			for(Category category:categories){
				if (LOAD_MODEL) {
					if (LINE_modelSingleton.getInstance().line_Combined.hasWord(String.valueOf(category.getId()))&&LINE_modelSingleton.getInstance().line_Combined.hasWord(String.valueOf(annotation.getId()))) {
						double similarity = 0.0;
						try {
							similarity=LINE_modelSingleton.getInstance().line_Combined.similarity(String.valueOf(annotation.getId()), String.valueOf(category.getId()));
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
						similarity=Request_LINEServer.getSimilarity(String.valueOf(annotation.getId()), String.valueOf(category.getId()), Model_LINE.LINE_COMBINED);
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
