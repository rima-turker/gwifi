package org.fiz.ise.gwifi.dataset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.GoogleModelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.train.generation.GenerateDatasetForNN;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.nd4j.linalg.api.ndarray.INDArray;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;

public class AnalyseMRDataset {

	private static final String DATASET_TRAIN_MR_POS = Config.getString("DATASET_TRAIN_MR_POS","");
	private static final String DATASET_TRAIN_MR_NEG = Config.getString("DATASET_TRAIN_MR_NEG","");
	public static void main(String[] args) throws IOException {
		
//		LINE_modelSingleton.getInstance();
//		AnnotationSingleton.getInstance();
//		
//		GenerateDatasetForNN generate = new GenerateDatasetForNN();
//		generate.labelTrainSetParalel(EmbeddingModel.LINE_Ent_Ent, Dataset.AG);
		
		List<String> lines_pos = FileUtils.readLines(new File(DATASET_TRAIN_MR_POS), "utf-8");
		List<String> lines_neg = FileUtils.readLines(new File(DATASET_TRAIN_MR_NEG), "utf-8");
//		AnnonatationUtil.findFreqOfWord(lines_pos, "mr_freqOfWords_pos");
//		AnnonatationUtil.findFreqOfWord(lines_neg, "mr_freqOfWords_neg");
		
		VocabCache<VocabWord> vocab = GoogleModelSingleton.getInstance().google_model.getVocab();
		System.out.println(vocab.numWords());
		for (int i = 0; i < 20; i++) {
			System.out.println(vocab.elementAtIndex(i).getWord());
		}
//		AnalyseDataset.findMostSimilarEntitesForDataset(lines_pos, Dataset.MR, "pos");
//		AnalyseDataset.findMostSimilarEntitesForDataset(lines_neg, Dataset.MR, "neg");
		
		//AnnonatationUtil.findFreqOfEntity(AnnonatationUtil.findAnnotationAll(lines_pos),"AnnotationFrequency_MR_train_pos");
		//AnnonatationUtil.findFreqOfEntity(AnnonatationUtil.findAnnotationAll(lines_neg),"AnnotationFrequency_MR_train_neg");
		
		//AnalyseDataset.countEntitiesOfDatasets(new ArrayList<String>(read_trec_dataset().keySet()));

		
		
	}
	public static List<String> read_trec_dataset_per_cat(String c) {
		try {
			List<String> dataset = new ArrayList<>();
			List<String> lines = FileUtils.readLines(new File(DATASET_TRAIN_MR_POS), "utf-8");
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			for (int i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split(" ",2);
				String sentence=split[1];
				String mainLabel=split[0].split(":")[0];
				if (mainLabel.equals(c)) {
					dataset.add(sentence);
				}
			}
			System.out.println("Number of samples:"+dataset.size()+", category: "+c);
			return dataset;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
	public static Map<String, List<Article>> read_trec_dataset_aLabel(String datasetName) {
		Map<String, List<Article>> result = new HashMap<String, List<Article>>();
		try {
			List<String> lines = FileUtils.readLines(new File(datasetName), "utf-8");
			for(String line : lines) {
				String[] split = line.split(" ",2);
				String sentence=split[1];
				String mainLabel=split[0].split(":")[0];
				List<Article> t = new ArrayList<Article>();
				t.add(LabelsOfTheTexts.getLables_TREC_article().get(mainLabel));
				result.put(sentence, t);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Size of the trec dataset:"+result.size());
		return result;
	}
	public static Map<String, String> read_trec_dataset(String datasetName) {
		Map<String, String> result = new HashMap<String, String>();
		try {
			List<String> lines = FileUtils.readLines(new File(datasetName), "utf-8");
			for(String line : lines) {
//				
//				if (result.containsKey(split[1])) {
//					System.out.println(split[1]);
//				}
//				result.put(split[1], split[0]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Size of the trec dataset:"+result.size());
		return result;
	}
}
