package org.fiz.ise.gwifi.util;

import java.util.List;

import org.deeplearning4j.models.word2vec.Word2Vec;
import org.nd4j.linalg.api.ndarray.INDArray;

public class VectorUtil {
	
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
	public static double[] getSentenceVector(List<String> words, Word2Vec model,String sentence) {        
        INDArray a = null;
        try{
            a = model.getWordVectorsMean(words);
        }catch(Exception e) {
        	System.out.println("I am in getSentenceVector "+ words.size()+" "+a+"\n"+sentence);
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
	
	public static double[] getSentenceVector(List<String> words, Word2Vec model) {        
        INDArray a = null;
        try{
            a = model.getWordVectorsMean(words);
        }catch(Exception e) {
        	System.out.println("I am in getSentenceVector "+ words.size()+" "+a);
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

}
