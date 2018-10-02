package org.fiz.ise.gwifi.dataset.LINE.Category;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.shorttext.test.HeuristicApproachConsideringCategorySemantics;
import org.fiz.ise.gwifi.dataset.shorttext.test.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.shorttext.test.TestBasedonSortTextDatasets;
import org.fiz.ise.gwifi.dataset.test.read.ReadTestDataset;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.TimeUtil;
import org.fiz.ise.gwifi.util.VectorUtil;
import org.nd4j.linalg.api.ndarray.INDArray;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;
import weka.filters.supervised.instance.Resample;

public class KBCorrespondingCategory {
	private final static TestDatasetType_Enum TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private final static Category catFindMatching = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Technology"); 
	static final Logger secondLOG = Logger.getLogger("debugLogger");

	public static void main(String[] args) {

		KBCorrespondingCategory test = new KBCorrespondingCategory();
		List<Category> lstCat = new ArrayList<>();
		lstCat.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("World"));
		lstCat.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Sports"));
		lstCat.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Business"));
		lstCat.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science and technology"));
		Set<Category> setAllCategories = CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setAllCategories;
		List<Category> lstAllCategories= new ArrayList<>(setAllCategories);
		System.out.println("lstAllCategories.size: "+lstAllCategories.size());
		for(Category c : lstCat ) {
			secondLOG.info("Start"+c.getTitle()+"------------------------------------------\n");
			System.out.println("start processing texts"+c.getTitle());
			List<String> dataset = new ArrayList<>(ReadTestDataset.read_AG_BasedOnCategory(c));

//			test.findMatchingCatgeoryBasedOnSimForAllCats(dataset, lstAllCategories);
			if (c.getTitle().equals("Science and technology")) {
				test.findMatchingCatgeoryBasedOnSimDocVecForAllCats(dataset, "dataset_"+c.getTitle());
			}

		}

	}
	public Map<Category, Integer> findMatchingCatgeoryBasedOnFreq(List<String> lstText) {
		Map<Category, Integer> mapResult = new HashMap<>();
		Map<Category, Integer> sortedMap = new HashMap<>();
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		try {
			for(String text:lstText) {
				List<Annotation> lstAnnotations = new ArrayList<>();
				service.annotate(text, lstAnnotations);
				for(Annotation a:lstAnnotations) {
					if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())!=null) {
						List<Category> catsOfEntity = new ArrayList<>(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()).getParentCategories()));
						for(Category c : catsOfEntity) {
							if (mapResult.containsKey(c)) {
								mapResult.put(c, (mapResult.get(c)+1));
							}
							else{
								mapResult.put(c, 1);
							}
						}
					}

				}
			}
			sortedMap= new LinkedHashMap<>(MapUtil.sortByValueDescending(mapResult));
			int count =0;
			for(Entry<Category,Integer> e : sortedMap.entrySet()) {
				System.out.println(e.getKey()+" "+e.getValue());
				if (++count>50) {
					break;
				}
			}
			//Print.printMap(sortedMap);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return sortedMap;
	}

	public  void findMatchingCatgeoryBasedOnSimDocVecForAllCats(List<String> lstText,String nameOfTheDateSet) {
		Word2Vec model = LINE_modelSingleton.getInstance().lineModel; 
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		Map<String, Double>  sortedMap = null; 
		Map<String, Double> result = new HashMap<>();
		try {
			Double sim =0.0;
			long now = TimeUtil.getStart();
			List<String> allEntities = new ArrayList<>();
			for(String text:lstText) {
				List<Annotation> lstAnnotations = new ArrayList<>();
				service.annotate(text, lstAnnotations);
				for(Annotation a:lstAnnotations) {
					if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())!=null) {
						allEntities.add(String.valueOf(a.getId()));
					}
				}
			}
			double[] documentVec = VectorUtil.getSentenceVector(allEntities,model);
			System.out.println("Document vec created");
			int countCat = 0;
			List<String> categoryLines = new ArrayList<>(FileUtils.readLines(new File("/home/rtue/eclipse-workspace/gwifi/Models/category.node"), "utf-8"));
			for(String c : categoryLines) {
				sim = VectorUtil.cosineSimilarity(documentVec, model.getWordVector(c));
				if (!Double.isNaN(sim)) {
					result.put(WikipediaSingleton.getInstance().wikipedia.getCategoryById(Integer.valueOf(c)).getTitle(),sim);
				}
				System.out.println("Processed cat number "+ ++countCat);
			}
			System.out.println("resultSize "+result.size());
			System.out.println("Finished iteration for one dataset " +TimeUtil.getEnd(TimeUnit.SECONDS, now)+" seconds");

			sortedMap= new LinkedHashMap<>(MapUtil.sortByValueDescending(result));
			System.out.println("sortedMap size"+sortedMap.size());
			int count =0;
			for(Entry<String,Double> e : sortedMap.entrySet()) {
				secondLOG.info(nameOfTheDateSet+" "+e.getKey()+" "+e.getValue());
				System.out.println(nameOfTheDateSet+" "+e.getKey()+" "+e.getValue());
				if (++count>50) {
					break;
				}
			}
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	public  void findMatchingCatgeoryBasedOnSimForAllCats(List<String> lstText, List<Category> lstCat) {
		Word2Vec model = LINE_modelSingleton.getInstance().lineModel; 
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		Map<Category, Double>  sortedMap = null; 
		Map<Category, Double> result = new HashMap<>();
		int countProcessedCat=0;
		System.out.println("Total size of Cats "+lstCat.size());
		try {
			for(Category mainCat :lstCat ) {
				int countLine=0;
				Double sim =0.0;
				long now = TimeUtil.getStart();
				System.out.println("start Processing: "+mainCat+" total Category "+ lstCat.size());
				for(String text:lstText) {
					List<Annotation> lstAnnotations = new ArrayList<>();
					service.annotate(text, lstAnnotations);
					for(Annotation a:lstAnnotations) {
						if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())!=null) {
							Double tempSim = model.similarity(String.valueOf(mainCat.getId()), String.valueOf(a.getId()));
							if (!Double.isNaN(tempSim)) {
								sim+=tempSim ;
							}
						}
					}
					//System.out.println(++countLine+" similarity:"+sim);
				}
				if (!Double.isNaN(sim)) {
					if (result.containsKey(mainCat)) {
						result.put(mainCat, (result.get(mainCat)+sim));
						System.out.println(mainCat+" "+(result.get(mainCat)+sim));
					}
					else{
						result.put(mainCat, sim);
						System.out.println(mainCat+" "+sim);
					}
					System.out.println("resultSize "+result.size());
				}
				else
				{
					System.out.println("Sim is nan");
				}

				System.out.println("Finished total category: "+ ++countProcessedCat);
				System.out.println("Took time to process one cat "+TimeUtil.getEnd(TimeUnit.SECONDS, now)+" seconds");
			}
			System.out.println("Finished iteration for one dataset");

			sortedMap= new LinkedHashMap<>(MapUtil.sortByValueDescending(result));
			System.out.println("sortedMap size"+sortedMap.size());
			int count =0;
			for(Entry<Category,Double> e : sortedMap.entrySet()) {
				secondLOG.info(e.getKey()+" "+e.getValue());
				if (++count>50) {
					break;
				}
			}
			secondLOG.info("Finish------------------------------------------");
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	public  void findMatchingCatgeory(List<String> lstText) {
		Word2Vec model = LINE_modelSingleton.getInstance().lineModel; 
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		List<Category> lstCats = new ArrayList<>();
		lstCats.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle(getCatfindmatching().getTitle()));
		Map<Category, Double>  sortedMap = null; 
		for(Category mainCat :lstCats ) {
			Set<Category> subCats = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).map.get(mainCat));
			if (getCatfindmatching().getTitle().equals("Science")) {
				subCats.addAll(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).map.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Technology")));
			}
			Map<Category, Double> result = new HashMap<>();
			int countLine=0;
			try {
				for(String text:lstText) {
					List<Annotation> lstAnnotations = new ArrayList<>();
					service.annotate(text, lstAnnotations);
					for(Annotation a:lstAnnotations) {
						if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())!=null) {
							for(Category subCat : subCats) {
								Double sim = model.similarity(String.valueOf(subCat.getId()), String.valueOf(a.getId()));
								if (!Double.isNaN(sim)) {
									if (result.containsKey(subCat)) {
										result.put(subCat, (result.get(subCat)+sim));
									}
									else{
										result.put(subCat, sim);
									}
								}
							}
						}

					}
					System.out.println(++countLine);
				}
				//				if (mainCat.getTitle().equals("Business")) {
				//					sortedMapBusiness = new LinkedHashMap<>(MapUtil.sortByValueDescending(result));
				//				}
				//				if (mainCat.getTitle().equals("World")) {
				//					sortedMapWorld = new LinkedHashMap<>(MapUtil.sortByValueAscendingGeneric(result));
				//				}
				//			
				//				if (sortedMapWorld!=null&&sortedMapBusiness!=null) {
				//					int countCommon = 0;
				//					int countIterate=0;
				//					for(Entry<Category, Double> b:sortedMapBusiness.entrySet() ) {
				//						System.out.println(++countIterate+"iteration:");
				//						for(Entry<Category, Double> w:sortedMapWorld.entrySet() ) {
				//							countCommon++;
				//							if (b.getKey().equals(w.getKey())) {
				//								System.out.println("countCommon: "+countCommon);
				//								System.out.println(b.getKey()+", Business "+b.getValue()+", World "+w.getValue());
				//							}
				//						}
				//					}
				//				}
				//Countries by form of government, Biological globalization,Countries, Nationalist movements
				sortedMap= new LinkedHashMap<>(MapUtil.sortByValueDescending(result));
				//				for (Entry<Category, Double> e : sortedMap.entrySet()) {
				//					System.out.println(e.getKey()+" "+e.getValue());
				//				}

				Print.printMap(sortedMap);
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}
	public static void findMostCommonCats(List<String> lstText) {
		//Word2Vec model = LINE_modelSingleton.getInstance().lineModel; 
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		Map<Category, Integer> result = new HashMap<>();
		int countLine=0;
		try {
			for(String text:lstText) {
				List<Annotation> lstAnnotations = new ArrayList<>();
				service.annotate(text, lstAnnotations);
				for(Annotation a:lstAnnotations) {
					if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())!=null) {
						List<Category> cats = new ArrayList<>(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId()).getParentCategories()));
						if (cats!=null) {
							for(Category c:cats) {
								if (result.containsKey(c)) {
									result.put(c, (result.get(c)+1));
								}
								else{
									result.put(c, 1);
								}
							}
						}
					}

					//					List<String> temp = new LinkedList<>(model.wordsNearest(String.valueOf(a.getId()), 100));
					//					int countCat=0;
					//					for(String s : temp) {
					//						if (WikipediaSingleton.getInstance().wikipedia.getCategoryById(Integer.parseInt(s))!=null) {
					//							result.merge(WikipediaSingleton.getInstance().wikipedia.getCategoryById(Integer.parseInt(s)), 1, (l, b) -> l + b);
					//							if (++countCat>10) {
					//								break;
					//							}
					//
					//						}
					//					}
				}
				//System.out.println(++countLine);
			}
			Map<Category, Integer>  sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(result));
			for (Entry<Category, Integer> e : sortedMap.entrySet()) {
				System.out.println(e.getKey()+" "+e.getValue());
			}

			Print.printMap(sortedMap);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public static Category getCatfindmatching() {
		return catFindMatching;
	}
}
