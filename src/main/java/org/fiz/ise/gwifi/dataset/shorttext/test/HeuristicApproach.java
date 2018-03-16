package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Print;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;
import edu.kit.aifb.gwifi.util.PageIterator;

public class HeuristicApproach {

	private static WikipediaSingleton wikiSing = WikipediaSingleton.getInstance();
	private static Wikipedia wikipedia = wikiSing.wikipedia;
	private static CategorySingleton singCategory = CategorySingleton.getInstance();
	private static Set<Category> setMainCategories = new HashSet<>(singCategory.setMainCategories);
	private static Set<Category> setAllCategories = new HashSet<>(singCategory.setAllCategories);
	private static Map<Category, Set<Category>> mapCategories = new HashMap<>(singCategory.map);
	/*
	 * The main purpose of this class is the calculate the similarity and decide 
	 * which category a text belongs to based on the probability
	 * 
	 */
	public static void getBestMatchingCategory(String shortText) {

		List<Annotation> lstAnnotations = new ArrayList<>();
		AnnotationSingleton singleton = AnnotationSingleton.getInstance();
		NLPAnnotationService service = singleton.service;
		try {
			Map<Category, Double> mapScore = new HashMap<>(); 
			service.annotate(shortText, lstAnnotations);
			for (Category mainCat : setMainCategories) {
				double score = 0.0; 
				for(Annotation a:lstAnnotations) {
					List<Annotation> contextAnnotation = lstAnnotations.stream()
						    .filter(p -> p.getId()==a.getId()).collect(Collectors.toList());
					score+=(get_P_c_(mainCat)*get_P_e_c(a.getId(), mainCat)*a.getWeight()*get_P_Ce_e(a.getId(),contextAnnotation));
				}
				mapScore.put(mainCat, score);
			}

			Map<Category, Double> sortedMap = new HashMap<>(MapUtil.sortByValueDescending(mapScore));
			Print.printMap(sortedMap);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	/*
	 * Popularity of the category
	 */
	private static int get_P_c_(Category c){
		PageIterator pageIterator = wikipedia.getPageIterator();
		int result =0;
		try {
			while (pageIterator.hasNext()) {
				Page page = pageIterator.next();
				if (page.getType().equals(PageType.article)) {
					Article article = wikipedia.getArticleByTitle(page.getTitle());
					if(article==null) {
						continue;
					}
					Set<Category> categories = new HashSet<>(Arrays.asList(article.getParentCategories()));
					if (categories.contains(c)) {
						result++;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	private static double get_P_e_c(int articleID,Category mainCat) {
	
		Set<Category> childCategories = new HashSet<>(mapCategories.get(mainCat));
		double result =0.0;
		for(Category c:childCategories) {
			result+=(EmbeddingsService.getSimilarity(String.valueOf(articleID), String.valueOf(c.getId()))*
					EmbeddingsService.getSimilarity(String.valueOf(mainCat.getId()), String.valueOf(c.getId())));
		}
		return result;
	}

	private static double get_P_Se_c(Annotation a) {//comes from EL system weight value because we calculate the confidence based on the prior prob
		return a.getWeight();
	}

	/**
	 * This method takes the all the context entities and tries to calculate the probabilities of the given an 
	 * entitiy and all the other context entities and sums them up
	 * @return
	 */
	private static double get_P_Ce_e(Integer mainId,List<Annotation> contextEntities){ //Context entities an the entity(already disambiguated) 
		double result =0.0;
		for(Annotation a: contextEntities){
			result+=(EmbeddingsService.getSimilarity(String.valueOf(mainId), String.valueOf(a.getId())));
		}
		return result;
	}
}
