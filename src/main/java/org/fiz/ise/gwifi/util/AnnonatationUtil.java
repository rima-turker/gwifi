package org.fiz.ise.gwifi.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingletonAnnotationFiltering;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class AnnonatationUtil {
	private final static TestDatasetType_Enum TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	public static String getAnnotationsXML(String shortText) {
		try {
			NLPAnnotationService service = AnnotationSingleton.getInstance().service;
			List<Annotation> lstAnnotations = new ArrayList<>();
			String xmlResponce= service.annotate(shortText, lstAnnotations);
			return xmlResponce;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}
	public static List<Annotation> findAnnotationAll_FilterWEB(List<String> lst) {
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		List<Annotation> result = new ArrayList<>();
		try {
			for(String text:lst) {
				List<Annotation> lstAnnotations = new ArrayList<>();
				service.annotate(text, lstAnnotations);
				for(Annotation a : lstAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_WebSnippets().contains(a.getId())) { //we had so many noisy entities therefore filtering required
						result.add(a);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	public static List<Annotation> findAnnotationAll_FilterAG(List<String> lst) {
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		List<Annotation> result = new ArrayList<>();
		try {
			for(String text:lst) {
				List<Annotation> lstAnnotations = new ArrayList<>();
				service.annotate(text, lstAnnotations);
				for(Annotation a : lstAnnotations) {
					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())) { //we had so many noisy entities therefore filtering required
						result.add(a);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	public static List<Annotation> findAnnotationAll(List<String> lst) {
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		List<Annotation> result = new ArrayList<>();
		try {
			for(String text:lst) {
				List<Annotation> lstAnnotations = new ArrayList<>();
				service.annotate(text, lstAnnotations);
				result.addAll(lstAnnotations);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	public static int countALinkToMainCat(String str, Category c, int depth) {
		Set<Category> categories = new HashSet<>(CategoryUtil.generateCategoryTree(c, depth));
		categories.add(c);
		List<Annotation> allAnnotation = new ArrayList<>(findAnnotationAll(Arrays.asList(str)));
		int count=0;
		for(Annotation a : allAnnotation) {
			if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())!=null) {
				Set<Category> entityCats = new HashSet<>(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()).getParentCategories()));
				entityCats.retainAll(categories);
				if (entityCats.size()>0) {
					count++;
				}
			}
		}
		return count;
	}
	public static boolean hasALink(Annotation a, Category c) {
		Set<Category> categories = new HashSet<>(CategorySingletonAnnotationFiltering.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapMainCatAndSubCats.get(c));
		//		if (c.getTitle().equals("Science")||c.getTitle().equals("Technology")) {
		//			categories.addAll(CategoryUtil.generateCategoryTree(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science and technology"), depth));
		//		}
		if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())!=null) {
			Set<Category> entityCats = new HashSet<>(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()).getParentCategories()));
			for(Category t: entityCats) {
				if (categories.contains(t)) {
					return true;
				}
			}
		}
		return false;
	}
	public static boolean hasALink(String str, Category c, int depth) {
		Set<Category> categories = new HashSet<>(CategorySingletonAnnotationFiltering.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).mapMainCatAndSubCats.get(c));
		List<Annotation> allAnnotation = new ArrayList<>(findAnnotationAll(Arrays.asList(str)));
		for(Annotation a : allAnnotation) {


			if (c.getTitle().equals("Science")||c.getTitle().equals("Technology")||c.getTitle().equals("Science and technology")) {
				//categories = new HashSet<>(CategoryUtil.generateCategoryTree(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Business"), depth));
				//categories.addAll(CategoryUtil.generateCategoryTree(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Companies listed on NASDAQ"), depth));
				categories.addAll(CategoryUtil.generateCategoryTree(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science and technology"), depth));
				//categories.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Companies listed on NASDAQ"));
			}

			else if (c.getTitle().equals("World")) {
				categories = new HashSet<>(CategoryUtil.generateCategoryTree(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Countries by continent"), depth));
				Set<Category> sTemp = new HashSet<>(CategoryUtil.generateCategoryTree(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("World government"), depth));
				categories.addAll(sTemp);
			}
			if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())!=null) {
				Set<Category> entityCats = new HashSet<>(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()).getParentCategories()));
				for(Category t: entityCats) {
					if (categories.contains(t)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	public static void findFreqOfWord(List<String> lst ,String fileName) {
		Map<String, Integer> resultFreq = new HashMap<>();
		for(String a :lst  ) {
			if (resultFreq.containsKey(a)) {
				resultFreq.put(a, (resultFreq.get(a)+1));
			}
			else{
				resultFreq.put(a, 1);
			}
		}
		Map<String, Integer> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(resultFreq));
		FileUtil.writeDataToFile(sortedMap,fileName);
		System.out.println("Finished one dataset writing: " + fileName);
	}

	public static Map<String, Integer> findFreqOfEntitySortedMap(List<Annotation> lst ) {
		Map<String, Integer> resultFreq = new HashMap<>();
		for(Annotation a :lst  ) {
			if (resultFreq.containsKey(a.getTitle()+"\t"+a.getId())) {
				resultFreq.put(a.getTitle()+"\t"+a.getId(), (resultFreq.get(a.getTitle()+"\t"+a.getId())+1));
			}
			else{
				resultFreq.put(a.getTitle()+"\t"+a.getId(), 1);
			}
		}
		Map<String, Integer> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(resultFreq));
		return sortedMap;
	}
	public static void findFreqOfEntity(List<Annotation> lst ,String fileName) {
		Map<String, Integer> resultFreq = new HashMap<>();
		for(Annotation a :lst  ) {
			if (resultFreq.containsKey(a.getTitle()+"-"+a.getId())) {
				resultFreq.put(a.getTitle()+"-"+a.getId(), (resultFreq.get(a.getTitle()+"-"+a.getId())+1));
			}
			else{
				resultFreq.put(a.getTitle()+"-"+a.getId(), 1);
			}
		}
		Map<String, Integer> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(resultFreq));
		FileUtil.writeDataToFile(sortedMap,fileName);
		System.out.println("Finished one dataset writing: " + fileName);
	}
	public Map<Annotation, Double> analizeWeightOfAnnotations(List<Annotation> lst) {
		Map<Annotation, Double> annotations = new HashMap<>();
		for(Annotation a : lst) {
			annotations.put(a, a.getWeight());
		}
		Map<Annotation, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(annotations));
		return sortedMap;
	}
	public static List<Integer> getEntityBlackList_AGNews(){
		List<Integer> lstidBlack = new ArrayList<>();
		lstidBlack.add(18935732);
		lstidBlack.add(60534); //Shilling
		lstidBlack.add(18998750); //Reuters
		lstidBlack.add(3434750);//Inited States
		lstidBlack.add(54635);//Tuesday
		lstidBlack.add(54407);

		//New black list
		lstidBlack.add(54634);//Wednesday
		lstidBlack.add(266139);//ThursdayBand
		lstidBlack.add(169788);//FridayFilm
		lstidBlack.add(145418);//CompanyMilitary Unit
		lstidBlack.add(35524);//2004
		lstidBlack.add(4249942);//Dollar (band)-4249942
		lstidBlack.add(557667);//Face (professional wrestling)
		lstidBlack.add(1220573);//UPDATE news
		lstidBlack.add(13623073);//CUT film
		lstidBlack.add(2605250);//CUTS
		lstidBlack.add(1691433);//NEW (TV station)
		lstidBlack.add(471981);//Agence France-Presse

		return lstidBlack;

	}
	public static List<Integer> getEntityBlackList_WebSnippets(){
		List<Integer> lstidBlack = new ArrayList<>();
		lstidBlack.add(5043734); //Wikipedia
		return lstidBlack;

	}
	public static List<Annotation> filterAnnotation(List<Annotation> lst) {
		List<Integer> lstidBlack = new ArrayList<>(getEntityBlackList_AGNews());
		List<Annotation> filteredList = new ArrayList<>();
		int countFiltered=0;

		for(Annotation a : lst) {
			if (!lstidBlack.contains(a.getId())) {
				filteredList.add(a);
			}
			else {
				countFiltered++;
			}
		}
		System.out.println("Filtered number of elements "+countFiltered);
		return filteredList;
	}
	private List<Annotation> findMaxWeightedAnnotation(List<Annotation> contextAnnotations) {
		double max = 0.0;
		Annotation result = null;
		for (Annotation a : contextAnnotations) {
			if (a.getWeight() > max) {
				max = a.getWeight();
				result = a;
			}
		}
		return Arrays.asList(result);
	}

}
