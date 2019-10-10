package org.fiz.ise.gwifi.dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.StringUtil;

import edu.kit.aifb.gwifi.model.Article;


public class AnalyseSnippetsDataset {

	private static final String DATASET_TRAIN_WEB = Config.getString("DATASET_TRAIN_WEB","");
	private static final String DATASET_SNIPPETS_TRAIN_ANNOTATIONS = Config.getString("DATASET_SNIPPETS_TRAIN_ANNOTATIONS","");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	public static void main(String[] args) {
		System.out.println("Running Snippet analyses");
		List<String> dataset = ReadDataset.read_snippets(DATASET_TRAIN_WEB);
		System.out.println("Size of the dataset:"+dataset.size());
		//AnnonatationUtil.writeAnnotationFile(dataset);
		Map<String, List<String>> map_dbp_annotations_sentences = AnalyseDBPediaDataset.read_annotations_sentences(Dataset.WEB_SNIPPETS, DATASET_SNIPPETS_TRAIN_ANNOTATIONS);
		System.out.println("Size of the annotated sentences: "+map_dbp_annotations_sentences.size());
		List<Article> labels = new ArrayList<Article>(Categories.getLabels_Snippets());
		int count=0;
		for(Article a : labels) {
			List<String> read_WEB_BasedOnCategory = ReadDataset.read_WEB_BasedOnCategory(a.getTitle(), DATASET_TRAIN_WEB);
			List<String> allAnnotations = new ArrayList<String>();
			for(String sentence: read_WEB_BasedOnCategory) {
				if (map_dbp_annotations_sentences.containsKey(sentence)) {
					allAnnotations.addAll(map_dbp_annotations_sentences.get(sentence));
				}
				else {
					count++;
					System.out.println("Number of sentences are not in  the map so far: "+count);
				}
			}
			AnalyseDataset.findMostSimilarEntitesIDsForDatasetBasedOnDatasetVector(allAnnotations, "snippets_most_similar_entities"+a.getTitle()+"_datasetVec");
			
		}
		System.out.println("Number of sentences are not in  the map : "+count);
		
		//		AnalyseDBPediaDataset.categorizeDataset(Dataset.WEB_SNIPPETS,EmbeddingModel.LINE_Ent_Ent,DATASET_SNIPPETS_TRAIN_ANNOTATIONS,false, null);
	}
}
