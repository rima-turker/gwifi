package org.fiz.ise.gwifi.dataset.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.FilteredWikipediaPagesSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.test.longDocument.BasedOnWordsCategorize;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.SentenceSegmentator;

import com.mongodb.util.Hash;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class AnalyseDataset {
	private final static TestDatasetType_Enum TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	public static final Map<String,List<String>> CACHE = new HashMap<>();
	public static void main(String[] args) {
		//		Article at =WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Job");
		//		System.out.println(at.getId()+" "+at.getTitle()+" "+Arrays.asList(at.getParentCategories()));


		//counteWordsOfDatasets();
		//countEntitiesOfDatasets();
//		findFreqOfEntitiesOfDataset(AG_DataType.TITLEANDDESCRIPTION);
//		findFreqOfEntitiesOfDatasetwithSimilarities(AG_DataType.TITLEANDDESCRIPTION);
		//analyseAnchorText(1220573);
		findMostSimilarEntitesForDatasetBasedOnSentenceVector();
	}
	public static void findMostSimilarEntitesForDatasetBasedOnSentenceVector() {
		System.out.println("Start running: "+"findMostSimilarEntitesForDatasetBasedOnSentenceVector");
		List<Category> lstDatasetCatList = new ArrayList<>(LabelsOfTheTexts.getCatValue_AG().keySet());
		Set<Article> articles = FilteredWikipediaPagesSingleton.getInstance().articles;
		int i =0;
		for(Category c : lstDatasetCatList ) {
			System.out.println("category: "+c.getTitle());
			Map<String,Double> result = new HashMap<>();
			List<String> dataset = new ArrayList<>(ReadTestDataset.read_AG_BasedOnCategory(c,AG_DataType.TITLEANDDESCRIPTION));
			List<Annotation> lstAllAnnotation = new ArrayList<>(AnnonatationUtil.findAnnotationAll_FilterAG(dataset));
			List<String> words = new ArrayList<>();
			for(Annotation a: lstAllAnnotation) {
				words.add(String.valueOf(a.getId()));
			}
			double[] docVec= BasedOnWordsCategorize.getSentenceVector(words, LINE_modelSingleton.getInstance().lineModel);
			for(Article a : articles) {
				result.put(a.getTitle(), BasedOnWordsCategorize.getSimilarity(docVec, String.valueOf(a.getId())));
				System.out.println(++i);
			}
			System.out.println("Start writing..");
			Map<String, Double> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(result));
			FileUtil.writeDataToFile(sortedMap, c.getTitle()+"_theMostSimilarArticles_filteredEntities");
		}
	}
	public static void findMostSimilarEntitesForDataset() {

		Map<String,Integer> result = new HashMap<>();
		List<Category> lstDatasetCatList = new ArrayList<>(LabelsOfTheTexts.getCatValue_AG().keySet());
		for(Category c : lstDatasetCatList ) {
			List<String> dataset = new ArrayList<>(ReadTestDataset.read_AG_BasedOnCategory(c,AG_DataType.TITLEANDDESCRIPTION));
			List<Annotation> lstAllAnnotation = new ArrayList<>(AnnonatationUtil.findAnnotationAll(dataset));
			System.out.println("Size of the annotations: "+ lstAllAnnotation.size());
			int cCount=0;

			for(Annotation a : lstAllAnnotation) {
				if (!CACHE.containsKey(a.getTitle())) {
					List<String> lstArt = new ArrayList<> (LINE_modelSingleton.getInstance().lineModel.wordsNearest(String.valueOf(a.getId()), 10));
					if (lstArt.size()==0) {
						System.out.println("Size is zero: "+a.getTitle()+" "+a.getId());
					}
					CACHE.put(a.getTitle(), lstArt);
				}
				System.out.println(c.getTitle()+" CACHE size "+CACHE.size()+" count: "+cCount);
				List<String> lstArt = new ArrayList<String>(CACHE.get(a.getTitle())); 
				for(String s : lstArt) {
					if (WikipediaSingleton.getInstance().wikipedia.getArticleById(Integer.valueOf(s))!=null) {
						String entity = WikipediaSingleton.getInstance().wikipedia.getArticleById(Integer.valueOf(s)).getTitle();
						int count = result.containsKey(entity) ? result.get(entity) : 0;
						System.out.println(a.getTitle()+" "+entity);
						result.put(entity, count + 1);
					}

				}
				cCount++;
			}
			System.out.println("Start writing..");
			FileUtil.writeDataToFile(result, c.getTitle()+"_most similar10EntitiesForEachAn");
			System.out.println("Finished writing: " +c.getTitle()+"_most similar1EntitiesForEachAn");
		}
	}
	public static void analyseAnchorText(int id) {
		List<Category> lstDatasetCatList = new ArrayList<>(LabelsOfTheTexts.getCatValue_AG().keySet());
		for(Category c : lstDatasetCatList ) {
			System.out.println(c.getTitle());
			Category cDataset=c;
			List<String> dataset = new ArrayList<>(ReadTestDataset.read_AG_BasedOnCategory(cDataset,AG_DataType.TITLE));
			List<Annotation> lstAnnotations = new ArrayList<>(AnnonatationUtil.findAnnotationAll(dataset));
			int count=0;
			for(Annotation a : lstAnnotations) {
				if (a.getId()==id) {
					System.out.println(a.getMention()+"\t"+a.getTitle()+"\t"+ ++count);
				}
			}
			System.out.println();
		}
	}
	private static void findFreqOfEntitiesOfDatasetwithSimilarities(AG_DataType type) {
		List<Category> lstDatasetCatList = new ArrayList<>(LabelsOfTheTexts.getCatValue_AG().keySet());
		for(Category c : lstDatasetCatList ) {
			String fileName = "AnnotationFrequency_"+type+"_"+TEST_DATASET_TYPE+"_"+c.getTitle()+"_withSimilarity_filteredEntities";
			List<String> dataset = new ArrayList<>(ReadTestDataset.read_AG_BasedOnCategory(c,type));
			List<Annotation> lstAllAnnotation = new ArrayList<>(AnnonatationUtil.findAnnotationAll_FilterAG(dataset));
			Map<String, Integer> findFreqOfEntitySortedMap = AnnonatationUtil.findFreqOfEntitySortedMap(lstAllAnnotation);
			Map<String, Double> result = new LinkedHashMap<String, Double>();

			for(Entry<String,Integer> e : findFreqOfEntitySortedMap.entrySet()) {
				for(Category cC : lstDatasetCatList ) {
					String aId = e.getKey().split("\t")[1];
					String cId = String.valueOf(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(cC.getTitle()).getId());
					result.put(e.getKey().split("\t")[0]+"\t"+aId+"\t"+e.getValue()+"\t"+cC.getTitle(), BasedOnWordsCategorize.getSimilarity(Arrays.asList(aId), Arrays.asList(cId)));
				}
			}
			FileUtil.writeDataToFile(result, fileName);
		}
	}
	private static void findFreqOfEntitiesOfDataset(AG_DataType type) {
		List<Category> lstDatasetCatList = new ArrayList<>(LabelsOfTheTexts.getCatValue_AG().keySet());
		for(Category c : lstDatasetCatList ) {
			List<String> dataset = new ArrayList<>(ReadTestDataset.read_AG_BasedOnCategory(c,type));
			List<Annotation> lstAllAnnotation = new ArrayList<>(AnnonatationUtil.findAnnotationAll(dataset));
			AnnonatationUtil.findFreqOfEntity(lstAllAnnotation,"AnnotationFrequency_"+type+"_"+TEST_DATASET_TYPE+"_"+c.getTitle());
		}
	}
	private static void countEntitiesOfDatasets() {
		List<String> agNews = new ArrayList<>(ReadTestDataset.read_AG_BasedOnType(AG_DataType.TITLEANDDESCRIPTION));
		List<String> agNewsTitle = new ArrayList<>(ReadTestDataset.read_AG_BasedOnType(AG_DataType.TITLE));
		List<String> snippets = new ArrayList<>(ReadTestDataset.read_WEB());
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		int totalEntityCountAgTitleAndDescriprtion=0;
		int totalEntityCountAgTitle=0;
		int totalEntityCountSnippets=0;
		int countTitleDoesNotContainAnyEntity=0;

		try {
			//			for(String str: agNews) {
			//				List<Annotation> lstAnnotations = new ArrayList<>();
			//				service.annotate(str, lstAnnotations);
			//				totalEntityCountAgTitleAndDescriprtion+=lstAnnotations.size();
			//			}

			for(String str: agNewsTitle) {
				List<Annotation> lstAnnotations = new ArrayList<>();
				service.annotate(str, lstAnnotations);
				totalEntityCountAgTitle+=lstAnnotations.size();
				if (lstAnnotations.size()==0) {
					countTitleDoesNotContainAnyEntity++;
				}
			}
			//			for(String str: snippets) {
			//				List<Annotation> lstAnnotations = new ArrayList<>();
			//				service.annotate(str, lstAnnotations);
			//				totalEntityCountSnippets+=lstAnnotations.size();
			//			}
			//System.out.println("totalEntityCountAgTitleAndDescriprtion: "+totalEntityCountAgTitleAndDescriprtion);
			System.out.println("totalEntityCountAgTitle: "+totalEntityCountAgTitle+" countTitleDoesNotContainAnyEntity: "+countTitleDoesNotContainAnyEntity);
			//System.out.println("totalEntityCountSnippets: "+totalEntityCountSnippets);

			//			System.out.println("AvgEntCountAgTitleDesc: "+(double)totalEntityCountAgTitleAndDescriprtion*1.0/agNews.size()*1.0);
			System.out.println("AvgEntCountAgTitle: "+(double)totalEntityCountAgTitle*1.0/agNewsTitle.size()*1.0);
			System.out.println("AvgEntCountAgTitle: "+(double)totalEntityCountAgTitle*1.0/(agNewsTitle.size()-countTitleDoesNotContainAnyEntity)*1.0);
			//			System.out.println("AvgEntityCountSnippets "+(double)(totalEntityCountSnippets*1.0/snippets.size()*1.0));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private static void counteWordsOfDatasets() {
		List<String> agNews = new ArrayList<>(ReadTestDataset.read_AG_BasedOnType(AG_DataType.TITLEANDDESCRIPTION));
		List<String> agNewsTitle = new ArrayList<>(ReadTestDataset.read_AG_BasedOnType(AG_DataType.TITLE));
		List<String> agNewsDecription = new ArrayList<>(ReadTestDataset.read_AG_BasedOnType(AG_DataType.DESCRIPTION));
		List<String> snippets = new ArrayList<>(ReadTestDataset.read_WEB());

		int totalWordCountAgTitleAndDescriprtion=0;
		int totalWordCountAgTitle=0;
		int totalWordCountAgDecription=0;
		int totalWordSnippets=0;
		System.out.println("Size of dataset Ag Title and Description: "+ agNews.size());
		System.out.println("Size of dataset Ag Title: "+ agNewsTitle.size());
		System.out.println("Size of dataset Ag Decription: "+ agNewsDecription.size());
		for(String str: snippets) {
			totalWordSnippets+=SentenceSegmentator.wordCount(str);
		}

		for(String str: agNews) {
			totalWordCountAgTitleAndDescriprtion+=SentenceSegmentator.wordCount(str);
		}
		for(String str: agNewsTitle) {
			totalWordCountAgTitle+=SentenceSegmentator.wordCount(str);
		}
		for(String str: agNewsDecription) {
			totalWordCountAgDecription+=SentenceSegmentator.wordCount(str);
		}
		System.out.println("totalWordCountAgTitleAndDescriprtion: "+totalWordCountAgTitleAndDescriprtion);
		System.out.println("totalWordCountAgTitle: "+totalWordCountAgTitle);
		System.out.println("totalWordCountAgDecription: "+totalWordCountAgDecription);
		System.out.println("totalWordSnippets: "+totalWordSnippets);
		System.out.println("totalWordCountAgTitleAvg: "+(double)totalWordCountAgTitle*1.0/agNewsTitle.size()*1.0);
		System.out.println("totalWordCountAgDescAvg: "+(double)totalWordCountAgDecription*1.0/agNewsTitle.size()*1.0);
		System.out.println("totalWordCountAgTitleAndDescriprtion: "+(double)(totalWordCountAgTitleAndDescriprtion*1.0/agNews.size()*1.0));
		System.out.println("totalWordSnippetsAvg: "+(double)(totalWordSnippets*1.0/snippets.size()*1.0));
		System.out.println("Total: "+ (totalWordCountAgTitle+totalWordCountAgDecription));
	}

}
