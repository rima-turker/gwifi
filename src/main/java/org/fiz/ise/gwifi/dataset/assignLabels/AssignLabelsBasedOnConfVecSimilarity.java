package org.fiz.ise.gwifi.dataset.assignLabels;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.GoogleModelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.ReadDataset;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.dataset.train.generation.GenerateDatasetForNN;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.model.EmbeddingModel;
import org.fiz.ise.gwifi.test.afterESWC.GenerateFeatureSet;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Print;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class AssignLabelsBasedOnConfVecSimilarity {
	private static final String DATASET_DBP_TRAIN = Config.getString("DATASET_DBP_TRAIN","");
	private static final String DATASET_DBP_TRAIN_CATEGORIZED_LINE = Config.getString("DATASET_DBP_TRAIN_CATEGORIZED_LINE","");
	private static final String DATASET_DBP_TRAIN_CATEGORIZED_D2Vec = Config.getString("DATASET_DBP_TRAIN_CATEGORIZED_D2Vec","");
	private static final String DATASET_DBP_TRAIN_CATEGORIZED_GOOGLE = Config.getString("DATASET_DBP_TRAIN_CATEGORIZED_GOOGLE","");
	
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	Map<String, List<Article>> map_AG_test_gt = null;//ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TEST_AG",""));

	final String pathFolderLabelResult= "ResultLabelAssignmentDifferentModels";
	public static void main(String[] args) throws Exception {
		AssignLabelsBasedOnConfVecSimilarity assign = new AssignLabelsBasedOnConfVecSimilarity();
		//assign.obtainLabelForEachSample(Dataset.DBpedia, EmbeddingModel.LINE_Ent_Ent, new ArrayList<Article>(LabelsOfTheTexts.getLables_DBP_article().values()));
		//assign.generateDatasetOneHotEncoding(Dataset.AG,Config.getString("DATASET_TEST_AG",""));


		assign.generateDatasetBasedOnConfidenceForEachModel(Dataset.AG,Config.getString("DATASET_TRAIN_AG",""));
		//		assign.getIntersectedLabel(Dataset.AG);

	}
	public void generateDatasetOneHotEncoding( Dataset dname, String fileName) {
		if (dname.equals(Dataset.AG)) {
			Map<String, List<Article>> dataset = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,fileName);
			for(Entry<String, List<Article>> e : dataset.entrySet()) {
				Map<String, Double> temp= new HashMap<String, Double>();
				List<Article> lstCats = new ArrayList<Article>(LabelsOfTheTexts.getArticleValue_AG().keySet());
				for(Article a : lstCats) {
					temp.put(a.getTitle(), 0.0);
				}
				String label = e.getValue().get(0).getTitle();
				temp.put(label, 1.);
				//Print.printMap(temp);
				LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();
				temp.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey())
				.forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

				StringBuilder labels=new StringBuilder();
				for(Double s : sortedMap.values()) {
					labels.append(s+",");
				}
				secondLOG.info(e.getKey());
				resultLog.info(labels.substring(0,labels.length()-1));
			}
		}
	}
	public String getLabelBasedOnConfidenceForEachModel(Dataset dname,String str ) throws Exception {


		if (dname.equals(Dataset.AG)) {
			double conf_LINE=0.9384;
			double conf_GOOGLE=0.7667;
			double conf_Doc2Vec=0.8247583333333334;

			Map<String, String> map_DOC2VEC = new HashMap<String, String>(readLabelAssignment(Dataset.AG, EmbeddingModel.Doc2Vec));
			Map<String, String> map_GOOGLE = new HashMap<String, String>(readLabelAssignment(Dataset.AG, EmbeddingModel.GOOGLE));
			Map<String, String> map_LINE = new HashMap<String, String>(readLabelAssignment(Dataset.AG, EmbeddingModel.LINE_Ent_Ent));


			Map<String, Double> temp= new HashMap<String, Double>();
			List<Article> lstCats = new ArrayList<Article>(LabelsOfTheTexts.getArticleValue_AG().keySet());
			for(Article a : lstCats) {
				temp.put(a.getTitle(), 0.0);
			}
			//DELETE THIS PART
			//temp.put(e.getValue().get(0).getTitle(), 1.);
			String str_DOC2VEC = map_DOC2VEC.get(str);
			String str_LINE = map_LINE.get(str);
			String str_GOOGLE = map_GOOGLE.get(str);

			if ((str_DOC2VEC!=null&&str_LINE!=null )&& str_DOC2VEC.equals(str_GOOGLE)&&str_DOC2VEC.equals(str_LINE)) {
				//					temp.put(str_GOOGLE, 1.);
				temp.put(str_GOOGLE, conf_Doc2Vec+conf_GOOGLE+conf_LINE);
			}
			else if(str_DOC2VEC!=null&&(str_DOC2VEC.equals(str_LINE))) {
				//					temp.put(str_DOC2VEC, (conf_Doc2Vec+conf_LINE)/2.0);
				temp.put(str_DOC2VEC, (conf_Doc2Vec+conf_LINE));
				temp.put(str_GOOGLE, conf_GOOGLE);
			}
			else if(str_DOC2VEC!=null&&str_DOC2VEC.equals(str_GOOGLE)) {
				//					temp.put(str_DOC2VEC, (conf_Doc2Vec+conf_GOOGLE)/2.0);
				temp.put(str_DOC2VEC, (conf_Doc2Vec+conf_GOOGLE));
				temp.put(str_LINE, conf_LINE);
			}
			else if(str_LINE!=null&&str_LINE.equals(str_GOOGLE)) {
				//					temp.put(str_LINE, (conf_GOOGLE+conf_LINE)/2.0);
				temp.put(str_LINE, (conf_GOOGLE+conf_LINE));
				temp.put(str_DOC2VEC, conf_Doc2Vec);
			}
			else {

				if (str_LINE!=null) {
					temp.put(str_LINE, conf_LINE);
				}
				if (str_GOOGLE!=null) {
					temp.put(str_GOOGLE, conf_GOOGLE);
				}
				if (str_DOC2VEC!=null) {
					temp.put(str_DOC2VEC, conf_Doc2Vec);
				}
			}
			double sum = temp.entrySet().stream().mapToDouble //to normalize the final
					(l->l.getValue()).sum();

			//LinkedHashMap preserve the ordering of elements in which they are inserted
			LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();
			temp.entrySet()
			.stream()
			.sorted(Map.Entry.comparingByKey())
			.forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

			StringBuilder labels=new StringBuilder();
			for(Double s : sortedMap.values()) {
				labels.append((double)s/sum+",");
			}
			return labels.substring(0,labels.length()-1);
		}
		else if (dname.equals(Dataset.AG_test)) {
			Map<String, Double> temp= new HashMap<String, Double>();
			List<Article> lstCats = new ArrayList<Article>(LabelsOfTheTexts.getArticleValue_AG().keySet());
			for(Article a : lstCats) {
				temp.put(a.getTitle(), 0.0);
			}
			temp.put(map_AG_test_gt.get(str).get(0).getTitle(), 1.);

			LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();
			temp.entrySet()
			.stream()
			.sorted(Map.Entry.comparingByKey())
			.forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

			StringBuilder labels=new StringBuilder();
			for(Double s : sortedMap.values()) {
				labels.append(s+",");
			}
			return labels.substring(0,labels.length()-1);
		}
		return null;
	}

	public void generateDatasetBasedOnConfidenceForEachModel(Dataset dname, String fileName) throws Exception {
		double conf_LINE=0.9384;
		double conf_GOOGLE=0.7667;
		double conf_Doc2Vec=0.8247583333333334;

		if (dname.equals(Dataset.AG)) {
			Map<String, List<Article>> dataset = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,fileName);
			Map<String, String> map_DOC2VEC = new HashMap<String, String>(readLabelAssignment(Dataset.AG, EmbeddingModel.Doc2Vec));
			Map<String, String> map_GOOGLE = new HashMap<String, String>(readLabelAssignment(Dataset.AG, EmbeddingModel.GOOGLE));
			Map<String, String> map_LINE = new HashMap<String, String>(readLabelAssignment(Dataset.AG, EmbeddingModel.LINE_Ent_Ent));

			for(Entry<String, List<Article>> e : dataset.entrySet()) {
				Map<String, Double> temp= new HashMap<String, Double>();
				List<Article> lstCats = new ArrayList<Article>(LabelsOfTheTexts.getArticleValue_AG().keySet());
				for(Article a : lstCats) {
					temp.put(a.getTitle(), 0.0);
				}
				//DELETE THIS PART
				//temp.put(e.getValue().get(0).getTitle(), 1.);
				String str_DOC2VEC = map_DOC2VEC.get(e.getKey());
				String str_LINE = map_LINE.get(e.getKey());
				String str_GOOGLE = map_GOOGLE.get(e.getKey());

				if ((str_DOC2VEC!=null&&str_LINE!=null )&& str_DOC2VEC.equals(str_GOOGLE)&&str_DOC2VEC.equals(str_LINE)) {
					//					temp.put(str_GOOGLE, 1.);
					temp.put(str_GOOGLE, conf_Doc2Vec+conf_GOOGLE+conf_LINE);
				}
				else if(str_DOC2VEC!=null&&(str_DOC2VEC.equals(str_LINE))) {
					//					temp.put(str_DOC2VEC, (conf_Doc2Vec+conf_LINE)/2.0);
					temp.put(str_DOC2VEC, (conf_Doc2Vec+conf_LINE));
					temp.put(str_GOOGLE, conf_GOOGLE);
				}
				else if(str_DOC2VEC!=null&&str_DOC2VEC.equals(str_GOOGLE)) {
					//					temp.put(str_DOC2VEC, (conf_Doc2Vec+conf_GOOGLE)/2.0);
					temp.put(str_DOC2VEC, (conf_Doc2Vec+conf_GOOGLE));
					temp.put(str_LINE, conf_LINE);
				}
				else if(str_LINE!=null&&str_LINE.equals(str_GOOGLE)) {
					//					temp.put(str_LINE, (conf_GOOGLE+conf_LINE)/2.0);
					temp.put(str_LINE, (conf_GOOGLE+conf_LINE));
					temp.put(str_DOC2VEC, conf_Doc2Vec);
				}
				else {

					if (str_LINE!=null) {
						temp.put(str_LINE, conf_LINE);
					}
					if (str_GOOGLE!=null) {
						temp.put(str_GOOGLE, conf_GOOGLE);
					}
					if (str_DOC2VEC!=null) {
						temp.put(str_DOC2VEC, conf_Doc2Vec);
					}
				}
				double sum = temp.entrySet().stream().mapToDouble //to normalize the final
						(l->l.getValue()).sum();

				//LinkedHashMap preserve the ordering of elements in which they are inserted
				Print.printMap(temp);
				LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<>();
				temp.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey())
				.forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

				StringBuilder labels=new StringBuilder();
				for(Double s : sortedMap.values()) {
					labels.append((double)s/sum+",");
				}
				String vector=GenerateFeatureSet.featureSet_EntitiyVectorMeanAsString(e.getKey());
				if (vector!=null) {
					secondLOG.info(e.getKey());
					resultLog.info(labels.substring(0,labels.length()-1));
				}
			}
		}

	}

	public void calculateConfidenceForEachModel(Dataset dname) {
		Map<String, List<String>> mapModelOverlapConflict = new HashMap<String, List<String>>();
		Map<String, List<Article>> dataset=null;
		Map<String,  List<Article>> map_DOC2VEC = null;
		Map<String,  List<Article>> map_GOOGLE = null;
		Map<String, List<Article>> map_LINE= null;
		if (dname.equals(Dataset.AG)) {
//			dataset = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TRAIN_AG",""));
//			map_DOC2VEC = new HashMap<String, String>(readLabelAssignment(Dataset.AG, EmbeddingModel.Doc2Vec));
//			map_GOOGLE = new HashMap<String, String>(readLabelAssignment(Dataset.AG, EmbeddingModel.GOOGLE));
//			map_LINE = new HashMap<String, String>(readLabelAssignment(Dataset.AG, EmbeddingModel.LINE_Ent_Ent));
		}
		else if (dname.equals(Dataset.DBpedia)) {
			dataset = ReadDataset.read_dataset_DBPedia_SampleLabel(DATASET_DBP_TRAIN);
			map_DOC2VEC = new HashMap<String,List<Article>>(ReadDataset.read_dataset_Doc2Vec_categorized(Dataset.DBpedia, DATASET_DBP_TRAIN_CATEGORIZED_D2Vec));
			map_GOOGLE = new HashMap<String,List<Article>>( ReadDataset.read_dataset_DBPedia_SampleLabel(DATASET_DBP_TRAIN_CATEGORIZED_GOOGLE));
			map_LINE = new HashMap<String,List<Article>>(ReadDataset.read_dataset_DBPedia_SampleLabel(DATASET_DBP_TRAIN_CATEGORIZED_LINE));
		}
		for(Entry<String, List<Article>> e : dataset.entrySet()) {
			String str_DOC2VEC = map_DOC2VEC.get(e.getKey()).get(0).getTitle();
			String str_LINE = map_LINE.get(e.getKey()).get(0).getTitle();
			String str_GOOGLE = map_GOOGLE.get(e.getKey()).get(0).getTitle();
			String key = null;
			List<String> lstKey= new ArrayList<String>();
			if ((str_DOC2VEC!=null&&str_LINE!=null)&&(str_LINE.equals(str_GOOGLE)&&str_LINE.equals(str_DOC2VEC))) {
				key = EmbeddingModel.Doc2Vec.name() +EmbeddingModel.GOOGLE.name()+EmbeddingModel.LINE_Ent_Ent.name();
			}
			else if(str_DOC2VEC!=null&&(str_DOC2VEC.equals(str_LINE))) {
				key = EmbeddingModel.Doc2Vec.name()+EmbeddingModel.LINE_Ent_Ent.name();
			}
			else if(str_DOC2VEC!=null&&str_DOC2VEC.equals(str_GOOGLE)) {
				key = EmbeddingModel.Doc2Vec.name()+EmbeddingModel.GOOGLE.name();
			}
			else if(str_LINE!=null&&str_LINE.equals(str_GOOGLE)) {
				key = EmbeddingModel.GOOGLE.name()+EmbeddingModel.LINE_Ent_Ent.name();
			}
			else {
				if (str_LINE!=null) {
					lstKey.add(EmbeddingModel.LINE_Ent_Ent.name());
				}
				if (str_GOOGLE!=null) {
					lstKey.add(EmbeddingModel.GOOGLE.name());
				}
				if (str_DOC2VEC!=null) {
					lstKey.add(EmbeddingModel.Doc2Vec.name());
				}
			}
			if (lstKey.size()==0) {
				mapModelOverlapConflict=new HashMap<>(addElementToMapModelOverlapConflict(mapModelOverlapConflict, key, e.getKey()));
			}
			else {
				for(String k:lstKey) {
					mapModelOverlapConflict=new HashMap<>(addElementToMapModelOverlapConflict(mapModelOverlapConflict, k, e.getKey()));
				}
			}
		}
		int countLINE=0;
		int countGOOGLE=0;
		int countDOC2VEC=0;
		for (Entry<String, List<String>> e :mapModelOverlapConflict.entrySet()) {
			System.out.println(e.getKey()+": "+e.getValue().size());
			if (e.getKey().contains(EmbeddingModel.LINE_Ent_Ent.name())) {
				countLINE+=e.getValue().size();
			}
			if (e.getKey().contains(EmbeddingModel.GOOGLE.name())) {
				countGOOGLE+=e.getValue().size();
			}
			if (e.getKey().contains(EmbeddingModel.Doc2Vec.name())) {
				countDOC2VEC+=e.getValue().size();
			}
		}
		System.out.println();
		System.out.println("countLINE: "+countLINE+" confidence:"+countLINE*1./1.0*dataset.size());
		System.out.println("countGOOGLE: "+countGOOGLE+" confidence:"+countGOOGLE*1./1.0*dataset.size());
		System.out.println("countDOC2VEC: "+countDOC2VEC+" confidence:"+countDOC2VEC*1./1.0*dataset.size());
		//Print.printMap(mapModelOverlapConflict);

	}

	private Map<String, List<String>> addElementToMapModelOverlapConflict(Map<String, List<String>> mapModelOverlapConflict, String key, String value) {
		Map<String, List<String>> mapResult = new HashMap<String, List<String>>(mapModelOverlapConflict);
		if (mapResult.containsKey(key)) {
			List<String> temp = new ArrayList<String>(mapResult.get(key));
			temp.add(value);
			mapResult.put(key,temp);
		}
		else {
			List<String> temp = new ArrayList<String>();
			temp.add(value);
			mapResult.put(key,temp);
		}
		return mapResult; 
	}
	public void obtainLabelForEachSample(Dataset dname, EmbeddingModel model, List<Article> labels) {
		if (dname.equals(Dataset.AG)) {
			if (model.equals(EmbeddingModel.LINE_Ent_Ent)) {
				LINE_modelSingleton.getInstance();
				GenerateDatasetForNN generate = new GenerateDatasetForNN();
				Map<String, Article> mapResultLabelAssignment = new HashMap<String, Article>(generate.labelTrainSetParalel(model, dname,labels));
				String fname = "LabelAssignment_AG_LINE";
				FileUtil.writeDataToFile(mapResultLabelAssignment, fname);
			}
		}
		if (model.equals(EmbeddingModel.GOOGLE)) {
			GoogleModelSingleton.getInstance();
			GenerateDatasetForNN generate = new GenerateDatasetForNN();
			Map<String, Article> mapResultLabelAssignment = new HashMap<String, Article>(generate.labelTrainSetParalel(EmbeddingModel.GOOGLE, dname,labels));
			String fname = "LabelAssignment_"+dname.toString()+"_GOOGLE";
			FileUtil.writeDataToFile(mapResultLabelAssignment, fname);
		}
	}
	public Map<String, String> readLabelAssignment(Dataset dname, EmbeddingModel model) {
		Map<String, String> result = new HashMap<String, String>();
		try {										 
			List<String> lines = FileUtils.readLines(new File(pathFolderLabelResult+"/"+dname+"/"+"LabelAssignment_"+dname+"_"+model), "utf-8");
			for(String line : lines) {
				String[] split = line.split("\t");
				Article articleByTitle = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(split[1]);
				if (articleByTitle==null) {
					articleByTitle = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(split[1].split(": ")[1]);
				}
				result.put(split[0],articleByTitle.getTitle());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	public void getIntersectedLabel(Dataset dname) {
		int count_3_Agree=0;
		int count_2_Agree=0;
		int count_No_Agree=0;

		int count_3_Agree_Correct=0;
		int count_3_Agree_Wrong=0;

		int count_2_Agree_Correct=0;
		int count_2_Agree_Wrong=0;

		if (dname.equals(Dataset.AG)) {
			Map<String, List<Article>> dataset = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TRAIN_AG",""));

			String fName_LINE="ResultLabelAssignmentDifferentModels/LabelAssignment_AG_LINE_";
			String fName_GOOGLE="ResultLabelAssignmentDifferentModels/LabelAssignment_AG_GOOGLE_";
			String fName_DOC2VEC="ResultLabelAssignmentDifferentModels/LabelAssignment_AG_DOC2VEC";

			Map<String, Article> result_LINE = new HashMap<>(read_categorization_file(fName_LINE));
			Map<String, Article> result_GOOGLE = new HashMap<>(read_categorization_file(fName_GOOGLE));
			Map<String, Article> result_DOC2VEC = new HashMap<>(read_categorization_file(fName_DOC2VEC));

			for(Entry<String, List<Article>> e : dataset.entrySet()) {
				Article best_LINE = result_LINE.get(e.getKey());
				Article best_google = result_GOOGLE.get(e.getKey());
				Article best_doc2vec = result_DOC2VEC.get(e.getKey());

				Map<Article, Integer> map = new HashMap<Article, Integer>();
				if (best_LINE!=null ) {
					Integer integer = map.get(best_LINE);
					if (integer==null) {
						map.put(best_LINE, 1);
					}
					else {
						map.put(best_LINE, integer+1);
					}
				}
				if (best_google!=null ) {
					Integer integer = map.get(best_google);
					if (integer==null) {
						map.put(best_google, 1);
					}
					else {
						map.put(best_google, integer+1);
					}
				}
				if (best_doc2vec!=null ) {
					Integer integer = map.get(best_doc2vec);
					if (integer==null) {
						map.put(best_doc2vec, 1);
					}
					else {
						map.put(best_doc2vec, integer+1);
					}
				}
				if(map.isEmpty()) {
					System.out.println("Element could not be found:\n"+e.getKey());
				}
				Map<Article, Integer> sortedMap = new LinkedHashMap<>(MapUtil.sortByValueDescending(map));
				Entry<Article, Integer> firstElement = MapUtil.getFirst(sortedMap);

				if (firstElement.getValue()==3||firstElement.getValue()==2) {
					count_3_Agree++;
					if (e.getValue().contains(firstElement.getKey())) {
						count_3_Agree_Correct++;
					}
					else {
						count_3_Agree_Wrong++;
					}
				}else if(firstElement.getValue()==2) {
					count_2_Agree++;
					if (e.getValue().contains(firstElement.getKey())) {
						count_2_Agree_Correct++;
					}
					else {
						count_2_Agree_Wrong++;
					}
				}
				else {
					count_No_Agree++;
				}

			}
			System.out.println("count_3_Agree:"+count_3_Agree+", count_2_Agree:"+count_2_Agree+" count_No_Agree:"+count_No_Agree);
			System.out.println("Accuracy_3_Agree:"+(double)((double)count_3_Agree_Correct/(double)count_3_Agree)+", accuracy_2_Agree:"+(double)((double)count_2_Agree_Correct/(double)count_2_Agree*1.0));
			System.out.println("count_3_Agree_Correct:"+count_3_Agree_Correct+", count_2_Agree_Correct:"+count_2_Agree_Correct);
			System.out.println("count_3_Agree_Wrong:"+count_3_Agree_Wrong+", count_2_Agree_Wrong:"+count_2_Agree_Wrong);
		}



	}
	public Map<String, Article> read_categorization_file(String fileName) {
		Map<String, Article> result = new HashMap<>();
		try {
			List<String> lines = FileUtils.readLines(new File(fileName), "utf-8");
			for(String line : lines) {
				String[] split = line.split("\t");
				Article articleByTitle = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(split[1]);
				if (articleByTitle==null) {
					articleByTitle = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(split[1].split(": ")[1]);
				}
				result.put(split[0],articleByTitle );
			}
			System.out.println("fileName:"+fileName+", size"+result.size());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;

	}
}
