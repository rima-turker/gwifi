package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.model.Model_LINE;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.Request_LINEServer;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class TEST_Meeting {
	private static WikipediaSingleton singleton = WikipediaSingleton.getInstance();
	private static Wikipedia wikipedia = singleton.wikipedia;
	private final static TestDatasetType_Enum TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	//private static CategorySingleton singCategory = CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE));
	static NLPAnnotationService service = AnnotationSingleton.getInstance().service;

	public static void main(String[] args) throws Exception {
//		dataset_WEB();
//		String DATASET_TEST_WEB ="/home/rtue/eclipse-workspace/Dataset_ShortTextClassification/ag_news_csv/test.csv";
//		List<String> lines = FileUtils.readLines(new File(DATASET_TEST_WEB), "utf-8");
//		System.out.println("number of the lines "+lines.size()+" dataset: "+DATASET_TEST_WEB);
//		double count =0;
//		for (String line:lines) {
//			String[] split = line.split("\",\"");
//			String title = split[1].replace("\"", "");
//			String description = split[2].replace("\"", "");
//			String text =title+" "+description;
//			List<Annotation> lstAnnotations = new ArrayList<>();
//			service.annotate(text, lstAnnotations);
//			count+=lstAnnotations.size();
//		}
//		System.out.println("total number of the entities "+count);
//		System.out.println("Avg entities "+count/lines.size());
//		
		//		List<String> catList = new ArrayList<>();
		//		catList.add("Business");
		//		catList.add("Sports");
		//		catList.add("World");
		//		catList.add("Science");
		//		catList.add("Technology");
		//		for(String entity : catList) {
		//			System.out.println(entity+" "+wikipedia.getCategoryByTitle(entity).getId());
		//			for (Model_LINE m : Model_LINE.values()) {
		//			for(String category : catList) {
		//					System.out.println(m+" "+wikipedia.getCategoryByTitle(entity).getTitle()+" "+wikipedia.getCategoryByTitle(category).getTitle()+" "+Request_LINEServer.getSimilarity(String.valueOf(wikipedia.getCategoryByTitle(entity).getId()),String.valueOf(wikipedia.getCategoryByTitle(category).getId()), m));
		//				}
		//			}
		//			
		//		}

		//		
		//		List<String> lines = new ArrayList<>(TestBasedonDatasets.generateRandomDataset_AG());
				NLPAnnotationService service = AnnotationSingleton.getInstance().service;
				String str = "Mozart was a prolific and influential composer.";
				List<Annotation> lstAnnotations = new ArrayList<>();
				service.annotate(str, lstAnnotations);
				
				for(Annotation a : lstAnnotations) {
					System.out.println(a);
				}
				
		//		String[] arrLines = new String[lines.size()];
		//		arrLines = lines.toArray(arrLines);
		//		int count=0;
		//		
		//		for (int i = 0; i < arrLines.length; i++) {
		//			String[] split = arrLines[i].split("\",\"");
		//			String title = split[1].replace("\"", "");
		//			String description = split[2].replace("\"", "");
		//			
		//			List<Annotation> lstAnnotations = new ArrayList<>();
		//			service.annotate(title, lstAnnotations);
		//			if (lstAnnotations.size()==0) {
		//				System.out.println("Size is :" +title);
		//			}
		//			count+=lstAnnotations.size();
		//			
		//		}
		//		double per = count/Double.valueOf(arrLines.length);
		//		System.out.println("per: "+per);


		//		List<String> lst = new ArrayList<>(Categories.getCategoryList(TestDatasetType_Enum.AG));
		//		for(String str: lst) {
		//			for(String str2: lst) {
		//				if (!str.equals(str2)) {
		//					System.out.println(str+" "+str2+" "+EmbeddingsService.getSimilarity(String.valueOf(wikipedia.getCategoryByTitle(str).getId()),String.valueOf(wikipedia.getCategoryByTitle(str2).getId()))); 
		//				}
		//			}
		//		}

		//		
//		List<String> entityList = new ArrayList<>();
//		entityList.add("Germany");
//		entityList.add("Albert Einstein");
//		entityList.add("Mathematics");
//		entityList.add("Paris");
//		entityList.add("Company");
//		entityList.add("London");
//		for(String entity : entityList) {
//			System.out.println(entity);
//			//for (Model_LINE m : Model_LINE.values()) {
//				//System.out.println(m+": "+getMostSimilarCategory(wikipedia.getArticleByTitle(entity).getId(),m));
//				System.out.println(getMostSimilarCategory(wikipedia.getArticleByTitle(entity).getId(), Model_LINE.LINE_COMBINED_2nd));
//			//}
//			//List<String> result = new ArrayList<>(EmbeddingsService.getMostSimilarConcepts(String.valueOf(wikipedia.getArticleByTitle(entity).getId()), null, 12));
//			//			for(String str: result) {
//			//				System.out.println(wikipedia.getPageById(Integer.parseInt(str)).getTitle());
//			//			}
//			//			System.out.println();
//		}

		//		List<String> catList = new ArrayList<>();
		//		catList.add("Business");
		//		catList.add("Sports terminology");
		//		catList.add("Mathematics");
		//		catList.add("Continents");
		//		catList.add("Sports rules and regulations");
		//		for(String entity : catList) {
		//			System.out.println(entity+" "+wikipedia.getCategoryByTitle(entity).getId());
		//			for (Model_LINE m : Model_LINE.values()) {
		//				Map<Category, Double > map = new LinkedHashMap<>(getMostSimilarCategory(wikipedia.getCategoryByTitle(entity),m));
		//				int i=0;
		//				for (Entry<Category, Double > e: map.entrySet()) {
		//					System.out.println(m+" "+entity+" "+e.getKey());
		//					if (++i>11) {
		//						break;	
		//					}
		//				}
		//
		//	
		
//	for(String entity : entityList) {
//		System.out.println(entity);
//		List<String> result = new ArrayList<>(EmbeddingsService.getMostSimilarConcepts(String.valueOf(wikipedia.getArticleByTitle(entity).getId()), null, 12));
//		for(String str: result) {
//			System.out.println(wikipedia.getPageById(Integer.parseInt(str)).getTitle());
//		}
//					System.out.println();
//		//		}
//	}


		//		String category = "Sports";
		//		List<String> result = new ArrayList<>(EmbeddingsService.getMostSimilarConcepts(String.valueOf(wikipedia.getCategoryByTitle(category).getId()), null, 10));
		//		for(String str: result) {
		//
		//			System.out.println(wikipedia.getPageById(Integer.parseInt(str)));
		//			//				
		//			//			
		//			//			
		//		}

	}
	
	public static void dataset_WEB() {
		String DATASET_TEST_WEB ="/home/rtue/eclipse-workspace/Dataset_ShortTextClassification/data-web-snippets/test.txt";
		try {
			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_WEB), "utf-8");
			System.out.println("size of the file "+lines.size());
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			double count=0;
			for (int i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split(" ");
				String label = split[split.length-1];
				String snippet = arrLines[i].substring(0, arrLines[i].length()-(label).length()).trim();
//				String snippet ="IBM adds midrange server to eServer lineup";
				List<Annotation> lstAnnotations = new ArrayList<>();
				service.annotate(snippet, lstAnnotations);
				System.out.println(lstAnnotations);
				count+=lstAnnotations.size();
			}
			System.out.println("total number of the entities "+count);
			System.out.println("Avg entities "+count/lines.size());
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	public static  Map<Category, Double> getMostSimilarCategory(Category c,Model_LINE m)
	{
		Set<Category> categories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setAllCategories);
		Map<Category, Double> map = new HashMap<>();
		for(Category category:categories){
			double similarity = 0.0;
			try {
				similarity=Request_LINEServer.getSimilarity(String.valueOf(c.getId()), String.valueOf(category.getId()), m);
				if (similarity>0) {
					map.put(category, similarity);
				}
			} catch (Exception e) {
				System.out.println("exception finding the similarity: "+similarity);
			}
		}	

		Map<Category, Double> mapSorted = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));
		return mapSorted;
		//		return MapUtil.getFirst(mapSorted);

	}
	public static  Entry<Category, Double> getMostSimilarCategory(Integer id,Model_LINE m)
	{
		Set<Category> categories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setAllCategories);
		Map<Category, Double> map = new HashMap<>();
		for(Category category:categories){
			double similarity = 0.0;
			try {
				similarity=Request_LINEServer.getSimilarity(String.valueOf(id), String.valueOf(category.getId()), m);
				if (similarity>0) {
					map.put(category, similarity);
				}
			} catch (Exception e) {
				System.out.println("exception finding the similarity: "+similarity);
			}
		}	

		Map<Category, Double> mapSorted = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));
		return MapUtil.getFirst(mapSorted);

	}

}
