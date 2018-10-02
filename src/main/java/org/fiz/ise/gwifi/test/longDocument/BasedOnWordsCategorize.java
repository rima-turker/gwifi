package org.fiz.ise.gwifi.test.longDocument;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.MapUtil;
import org.nd4j.linalg.api.ndarray.INDArray;

import edu.kit.aifb.gwifi.model.Category;

public class BasedOnWordsCategorize {
	private final static TestDatasetType_Enum TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE"); //here you get the name of the dataset
	private static Set<Category> setMainCategories = new HashSet<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	
	public static Category getBestMatchingCategory(String text,List<Category> gtList, Map<Category, Set<Category>> map) {
		try {
			Map<Category, Double> mapScore = new HashMap<>(); 
			//System.out.println(text);
			for (Category mainCat : setMainCategories) {
				double score=getSimilarity(text,mainCat.getTitle());
				//System.out.println(mainCat.getTitle()+" "+score);
				mapScore.put(mainCat, score);
			}
			Map<Category, Double>  sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(mapScore));
			Category firstElement = MapUtil.getFirst(sortedMap).getKey();
			return firstElement;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public static double getSimilarity(String text,String category) {
		Word2Vec model= LINE_modelSingleton.getInstance().lineModel;
		final String[] words = text.split(" ");
		final String[] cats = category.split(" ");
		double[] catVec2;
		
		double[] docVec= getSentenceVector(Arrays.asList(words), model);
		double[] catVec = getSentenceVector(Arrays.asList(category), model);
//		double[] catVec = model.getWordVector(category);
//		if(cats.length==1) {
//			if (cosineSimilarity(docVec, catVec)!=cosineSimilarity(docVec, catVec2)) {
//				System.out.println("ERROR :" + category);
//			}
//		}
		return cosineSimilarity(docVec, catVec);
	}
	private static double[] getSentenceVector(List<String> words, Word2Vec model) {        
		if (words.size()==1) {
			return model.getWordVector(words.get(0));
		}
		INDArray a = null;
		try{
			a = model.getWordVectorsMean(words);
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		int cols = a.columns();
		double[] result = new double[cols];
		for(int i=0;i<cols;i++) {
			result[i] = a.getDouble(i);
		}
		return result;
	}
	public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
		double dotProduct = 0.0;
		double normA = 0.0;
		double normB = 0.0;
		for (int i = 0; i < vectorA.length; i++) {
			dotProduct += vectorA[i] * vectorB[i];
			normA += Math.pow(vectorA[i], 2);
			normB += Math.pow(vectorB[i], 2);
		}   
		return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	}
}
