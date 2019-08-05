package org.fiz.ise.gwifi.dataset.train.generation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections15.map.HashedMap;
import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.GoogleModelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_2modelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.dataset.test.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.test.ReadDataset;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.test.afterESWC.HeuristicBasedOnEntitiyVectorSimilarity;
import org.fiz.ise.gwifi.test.afterESWC.TestBasedonSortTextDatasets;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.SynchronizedCounter;
import org.fiz.ise.gwifi.util.TimeUtil;
import org.fiz.ise.gwifi.util.WriteToAFile;
import org.apache.commons.io.FileUtils;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

public class GenerateDatasetForNN {
	private final static TestDatasetType_Enum TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private final static String TRAIN_SET_AG = Config.getString("DATASET_TRAIN_AG","");
	private final static Double THRESHOLD = Config.getDouble("THRESHOLD",1.0);
	private final static String TRAIN_SET_WEB = Config.getString("DATASET_TRAIN_WEB","");
	private final static Integer NUMBER_OF_THREADS= Config.getInt("NUMBER_OF_THREADS",-1);
	public static Map<String, String> mapRedirectPages;  //new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);//

	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");

	static int numberOfSamples=50;

	private static SynchronizedCounter countCorrectSyn;
	private static SynchronizedCounter countWrongSyn;
	private static SynchronizedCounter countNullSyn;

	private static Map<String, Integer> truePositive = new ConcurrentHashMap<>();
	private static Map<String, Integer> falsePositive = new ConcurrentHashMap<>();
	private static Map<String, Integer> mapMissClassified = new ConcurrentHashMap<>();
	private static Map<String, Integer> mapCount = new ConcurrentHashMap<>();

	private ExecutorService executor;

	private static Map<Article , List<String>> mapEstimated = new ConcurrentHashMap<>();
	private static List<String> listEstimated = Collections.synchronizedList(new ArrayList<String>());
	final public static Map<String, Article> map_results_doc2vec= new HashMap<String, Article>();
	public static void main(String[] args) throws IOException {
		long now = TimeUtil.getStart();
		CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE));
		AnnotationSingleton.getInstance();
		mapRedirectPages= new HashMap<>(RedirectPageAnalysis.loadRedirectPages());

//		LINE_modelSingleton.getInstance();
//		GoogleModelSingleton.getInstance();


		GenerateDatasetForNN generate = new GenerateDatasetForNN();

		ArrayList<String> lst_file_names = new ArrayList<String>();

		lst_file_names.add("/home/rima/playground/PyCharProjects/UnsupervisedTextCategorization/LabeledDataGeneration/categorization_iteration_rsv_dbow_afterCorrecting.txt");
		lst_file_names.add("/home/rima/playground/PyCharProjects/UnsupervisedTextCategorization/LabeledDataGeneration/categorization_iteration_rsv_dbow_afterCorrecting_2018_redirectionResolved.txt");
		
		lst_file_names.add("/home/rima/playground/PyCharProjects/UnsupervisedTextCategorization/LabeledDataGeneration/categorization_iteration_rsv_dm_afterCorrecting.txt");
		lst_file_names.add("/home/rima/playground/PyCharProjects/UnsupervisedTextCategorization/LabeledDataGeneration/categorization_iteration_rsv_dm_afterCorrecting_2018_redirectionResolved.txt");
		
		lst_file_names.add("/home/rima/playground/PyCharProjects/UnsupervisedTextCategorization/LabeledDataGeneration/categorization_iteration_dbow_afterCorrecting.txt");
		lst_file_names.add("/home/rima/playground/PyCharProjects/UnsupervisedTextCategorization/LabeledDataGeneration/categorization_iteration_dbow_afterCorrecting_2018_redirectionResolved.txt");
		
		lst_file_names.add("/home/rima/playground/PyCharProjects/UnsupervisedTextCategorization/LabeledDataGeneration/categorization_iteration_dm_afterCorrecting.txt");
		lst_file_names.add("/home/rima/playground/PyCharProjects/UnsupervisedTextCategorization/LabeledDataGeneration/categorization_iteration_dm_afterCorrecting_2018_redirectionResolved.txt");
		for (String file_name : lst_file_names) {
//			
			List<String> lines = FileUtils.readLines(new File(file_name), "utf-8");
			for(String line : lines) {
				String[] split = line.split("\t");
				map_results_doc2vec.put(split[0], WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(split[1]));
			}
			countCorrectSyn=new SynchronizedCounter();
			countWrongSyn=new SynchronizedCounter();
			countNullSyn=new SynchronizedCounter();

			generate.datasetGenerateFromTrainSetParalel();
			//generate.calculate_accuracy_for_doc2Vec(file_name);
			System.out.println("Total time minutes :"+ TimeUnit.SECONDS.toMinutes(TimeUtil.getEnd(TimeUnit.SECONDS, now)));
//			resultLog.info(file_name+":True Positives: "+countCorrectSyn.value());
//			resultLog.info(file_name+":False Positives: "+countWrongSyn.value());
//			resultLog.info(file_name+":Null: "+countNullSyn.value());
//			System.out.println(file_name+":True Positives: "+countCorrectSyn.value());
//			System.out.println(file_name+":False Positives: "+countWrongSyn.value());
//			System.out.println(file_name+":Null: "+countNullSyn.value());
			
		}

		/*
		List<String> lstCat = new ArrayList<>(Categories.getCategoryList(TEST_DATASET_TYPE));
		List<Category> lstDatasetCatList = new ArrayList<>();
		for(String c : lstCat) {
			lstDatasetCatList.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle(c));
		}
		String fileName="AG_TitleDesc_FreqWord";
		for(Category c : lstDatasetCatList ) {
		List<String> dataset = new ArrayList<>(ReadDataset.read_AG_BasedOnCategory(c, (AG_DataType.TITLEANDDESCRIPTION)));
		findFreqOfWords(dataset,fileName+"_"+c.getTitle());
		}
		 */
	}
	public void calculate_accuracy_for_doc2Vec(String fileName) {
		List<String> lines;
		try {
			lines = FileUtils.readLines(new File(fileName), "utf-8");
			Map<String, List<Article>> dataset = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION);
			int correct =0;
			int wrong=0;
			for(String line : lines) {
				String[] split = line.split("\t");
				String sentence=split[0];
				String cArticle=split[1];
				if (dataset.get(sentence).get(0).getTitle().equals(cArticle)) {
					correct++;
				}
				else {
					wrong++;
				}
				
			}
			System.out.println(fileName);
			System.out.println("correct: "+correct);
			System.out.println("wrong: "+wrong);
			int total=(correct+wrong);
			System.out.println("total: "+total);
			System.out.println("accuracy: "+(double)correct/(correct+wrong)+"\n\n");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private static void findFreqOfWords(List<String> dataset,String fileName) {
		final List<CoreLabel> tokens = new ArrayList<CoreLabel>();
		final List<String> tokensStr = new ArrayList<String>();

		final LexedTokenFactory<CoreLabel> tokenFactory = new CoreLabelTokenFactory();
		for(String line: dataset) {
			final PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(line), tokenFactory,
					"untokenizable=noneDelete");
			while (tokenizer.hasNext()) {
				tokensStr.add(tokenizer.next().toString());
			}
		}
		AnnonatationUtil.findFreqOfWord(tokensStr, fileName);

	}
	public void datasetGenerateFromTrainSetParalel() {
		try {
			Map<String, List<Article>> dataset = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION);
			int count =0;
			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			for(Entry<String, List<Article>> e: dataset.entrySet()) {
				executor.execute(runFindBestMachingArticle(e.getKey(),e.getValue(),++count));
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);



			System.out.println("countCorrect "+countCorrectSyn.value()+"\nWrongly assigned labels: "+countWrongSyn.value()+"\nNull assigned labels: "+countNullSyn.value());
			System.out.println("Total classified "+(countCorrectSyn.value()+countWrongSyn.value()));
			//			System.out.println("Accuracy "+(countCorrectSyn.value()/(dataset.size()*1.0)));
			System.out.println("Accuracy "+(countCorrectSyn.value()/((countCorrectSyn.value()+countWrongSyn.value())*1.0)));

			System.out.println("True Positives");
			Print.printMap(truePositive);
			System.out.println("\nFalse Positives");
			Print.printMap(falsePositive);
			System.out.println("\nMissClassified");
			Print.printMap(mapMissClassified);
			System.out.println("\nPredicted Count");
			Print.printMap(mapCount);
			System.out.println("\nList Count:"+listEstimated.size());
			
			//filterEntitiesWritecsv(dataset);

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private Runnable runFindBestMachingArticle(String description, List<Article> gtList, int i ) {
		return () -> {
//			Article bestMatchingCategory = HeuristicBasedOnEntitiyVectorSimilarity.getBestMatchingArticle_resolve_redirect(description, gtList);
//			Article bestMatchingCategory = HeuristicBasedOnEntitiyVectorSimilarity.getBestMatchingArticle(description, gtList);
			//			Article bestMatchingCategory = HeuristicBasedOnEntitiyVectorSimilarity.getBestMatchingArticlewithEuclineDistance(description, gtList);
			//			Article bestMatchingCategory = HeuristicBasedOnEntitiyVectorSimilarity.getBestMatchingArticlewithManhattenDistance(description, gtList);

			//Article bestMatchingCategory = HeuristicBasedOnEntitiyVectorSimilarity.getBestMatchingArticleFromWordVectorModel(description, gtList);


			//			Article bestMatchingCategory = HeuristicBasedOnEntitiyVectorSimilarity.getBestMatchingArticlewithTwoDifferentApproachAgreement(description, gtList);
			//Article bestMatchingCategory = HeuristicBasedOnEntitiyVectorSimilarity.getBestMatchingArticlewithTwoDifferentSimilarityMetricAgreement(description, gtList);
			Article bestMatchingCategory = HeuristicBasedOnEntitiyVectorSimilarity.getBestMatchingArticlewith_3_DifferentApproachAgreement(description, gtList);
//			Article bestMatchingCategory = HeuristicBasedOnEntitiyVectorSimilarity.getBestMatchingArticlewith_3_DifferentApproachAgreement_categorize_all_dataset_write(description, gtList);

			if(bestMatchingCategory==null) {
				countNullSyn.increment();
			}
			else {
				mapCount.put(bestMatchingCategory.getTitle(), mapCount.getOrDefault(bestMatchingCategory.getTitle(), 0) + 1); 
				listEstimated.add(bestMatchingCategory.getTitle()+"\t"+description);

				if (gtList.contains(bestMatchingCategory)) {
					countCorrectSyn.increment();
					truePositive.put(gtList.get(0).getTitle(), truePositive.getOrDefault(gtList.get(0).getTitle(), 0) + 1);
				}
				else 
				{
					countWrongSyn.increment();
					falsePositive.put(gtList.get(0).getTitle(), falsePositive.getOrDefault(gtList.get(0).getTitle(), 0) + 1);
					String key=gtList.get(0).getTitle()+"-->"+bestMatchingCategory.getTitle();
					mapMissClassified.put(key, mapMissClassified.getOrDefault(key, 0) + 1);
				}
			}
			//System.out.println(i+" files are processed. Correctly: "+countCorrectSyn.value()+" Wrongly: "+countWrongSyn.value()+" Null: "+countNullSyn.value());

		};
	}
	private static void filterEntitiesWritecsv(Map<String, List<Article>> groundTruth){
		List<String> lst_result_to_write_file = new ArrayList<String>();
		String file_name="data_generated_3_models_agreement_dbow_doc2vec_2018_redirect.txt";
		
//		Integer min = Collections.min(mapCount.values());
//		System.out.println("The size of the samples for each label: "+min);
		System.out.println("Size of the list off all the classified samples: "+listEstimated.size());


		Map<Article , List<String>> mapResult = new HashedMap<Article , List<String>>();

		for(String s : listEstimated) {
			String[] split = s.split("\t");
			Article a = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(split[0]);
			String description = split[1];

			if (mapResult.containsKey(a)) {

				//if (mapResult.get(a).size()<min) { //enable this part when you want an evenly distributed dataset
					List<String> temp = new ArrayList<String>(mapResult.get(a));
					temp.add(description);
					mapResult.put(a, temp);
				//}
			}
			else {
				List<String> temp = new ArrayList<String>();
				temp.add(description);
				mapResult.put(a, temp);
			}
		}

		for(Entry <Article , List<String>> entry : mapResult.entrySet()) {
			List<String> lst = new ArrayList<String>(entry.getValue());

			for(String s : lst) {
				if (entry.getKey().getTitle().equals("Sport")) {
//					resultLog.info("\""+LabelsOfTheTexts.getCatValue_AG().get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Sports"))+
//							"\",\""+s+"\"");
					lst_result_to_write_file.add("\""+LabelsOfTheTexts.getCatValue_AG().get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Sports"))+
							"\",\""+s+"\"");
				}
				else {
//					resultLog.info("\""+LabelsOfTheTexts.getCatValue_AG().get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle(entry.getKey().getTitle()))+
//							"\",\""+s+"\"");
					lst_result_to_write_file.add("\""+LabelsOfTheTexts.getCatValue_AG().get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle(entry.getKey().getTitle()))+
							"\",\""+s+"\"");

				}
			}
			System.out.println("Start writing to a file, size of the list: "+lst_result_to_write_file.size());
			
			FileUtil.writeDataToFile(lst_result_to_write_file, file_name,false);
			int countCorrect=0;
			int size=0;
			int wrong=0;

			System.out.println("Start Calculating the accuracy from filtered list");
			for(Entry<Article, List<String>> e : mapResult.entrySet()) {
				Article estimatedLabel = e.getKey();
				size+=e.getValue().size();
				for(String s : e.getValue()) {

					Article gtLabel = groundTruth.get(s).get(0);
					if (estimatedLabel.equals(gtLabel)) {
						countCorrect++;
					}
					else {
						wrong++;
					}
					//System.out.println("Size "+ size+" correct: "+countCorrect+" wrong:"+wrong);
				}
			}

			System.out.println("\n");
			System.out.println("Size "+ size+" correct: "+countCorrect+" wrong:"+wrong+" total:"+mapResult.size());
		}


		//		for(Entry<String, Integer> e : sortByValueAscendingGeneric.entrySet()) {
		//			int
		//			
		//			System.out.println(min); // 
		//			rows.add(Arrays.asList(String.valueOf(e.getValue()),e.getKey()));
		//		}
		//			try {
		//				FileWriter csvWriter = new FileWriter("new.csv");  
		//				for (List<String> rowData : rows) {  
		//				    csvWriter.append(String.join(",", rowData));
		//				    csvWriter.append("\n");
		//				}
		//
		//				csvWriter.flush();  
		//				csvWriter.close(); 
		//			} catch (IOException e) {
		//				// TODO Auto-generated catch block
		//				e.printStackTrace();
		//			}  
	}
	/*
	private static void datasetGenerateFromTrainSet() {
		Map<String, Integer> mapResult = new HashMap<String, Integer>();
		try {
			Set<Category> setMainCategories = new HashSet<>(
					CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).setMainCategories);	
			//			for(Category c : setMainCategories) {
			//				File directory = new File(c.getTitle());
			//				if (! directory.exists()){
			//					directory.mkdir();
			//				}
			//			}
			TestBasedonSortTextDatasets datasetRead =  new TestBasedonSortTextDatasets();
			Map<String, List<Category>> dataset = null;
			Map<String, List<Category>> map_result_To_Compare = new HashedMap<String, List<Category>>();
			Category bestMatchingCategory=null;
			if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.AG)) {
				dataset = datasetRead.read_dataset_AG(AG_DataType.TITLEANDDESCRIPTION, TRAIN_SET_AG);
				int i =0;
				for(Entry<String, List<Category>> e: dataset.entrySet()) {
					bestMatchingCategory = HeuristicBasedOnEntitiyVectorSimilarity.getBestMatchingCategory(e.getKey(),e.getValue());
					i++;
					//FileUtil.writeDataToFile(Arrays.asList(e.getKey()), bestMatchingCategory.getTitle()+File.separator+ i,false);

					if (e.getValue().contains(bestMatchingCategory)) {
						countCorrect++;
					}
					else if(e.getValue().get(0).getTitle().equals("Sports")&&bestMatchingCategory.getTitle().equals("Sport")) {
						countCorrect++;
					}
					else
					{
						//						String key = bestMatchingCategory.getTitle();
						String key = bestMatchingCategory.getTitle()+"-"+e.getValue().get(0).getTitle();
						int count = mapResult.containsKey(key) ? mapResult.get(key) : 0;
						mapResult.put(key, count + 1);
						resultLog.info("wrong classified: "+ bestMatchingCategory.getTitle()+"\t"+i+"\t"+e.getKey()+"\n");
						//						System.out.println("wrong classified: "+ bestMatchingCategory.getTitle()+"\t"+i+"\t"+e.getKey());
						countWrong++;
					}
					//System.out.println(i+" files are processed. Correctly: "+countCorrect+" Wrongly: "+countWrong);
					if (bestMatchingCategory.getTitle().equals("Sport")) {
						map_result_To_Compare.put(e.getKey(), Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Sports")));
					}
					else {
						map_result_To_Compare.put(e.getKey(), Arrays.asList(bestMatchingCategory));
					}
				}
			}
			else if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.WEB_SNIPPETS)) {
				dataset = datasetRead.read_dataset_WEB_for_DatasetGeneration(TRAIN_SET_WEB);
				System.out.println("Dataset size: "+dataset.size());
				int i =0;
				for(Entry<String, List<Category>> e: dataset.entrySet()) {
					Article bestMatchingArticle = HeuristicBasedOnEntitiyVectorSimilarity.getBestMatchingArticle(e.getKey(),e.getValue());
					bestMatchingCategory= WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle(bestMatchingArticle.getTitle());
					i++;
					//FileUtil.writeDataToFile(Arrays.asList(e.getKey()), bestMatchingCategory.getTitle()+File.separator+ i,false);

					if (e.getValue().get(0).equals(bestMatchingCategory)) {
						secondLOG.info("classified: "+ bestMatchingArticle.getTitle()+"\t"+i+"\t"+e.getKey()+"\n");
						countCorrect++;
					}
					else if(e.getValue().get(0).getTitle().equals("Sports")&&bestMatchingCategory.getTitle().equals("Sport")) {
						countCorrect++;
					}
					else//wrong classified
					{
						String key = bestMatchingCategory.getTitle()+"-"+e.getValue().get(0).getTitle();
						int count = mapResult.containsKey(key) ? mapResult.get(key) : 0;
						mapResult.put(key, count + 1);
						resultLog.info("wrong classified: "+ bestMatchingArticle.getTitle()+"\t"+i+"\t"+e.getKey()+"\t"+e.getValue().get(0)+"\n");
						countWrong++;
					}

					//For comparison
					if (bestMatchingCategory.getTitle().equals("Sport")) {
						map_result_To_Compare.put(e.getKey(), Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Sports")));
					}
					else {
						map_result_To_Compare.put(e.getKey(), Arrays.asList(bestMatchingCategory));
					}
				}
			}
			System.out.println("Test calculation is started...");
			int matchingSentences =0;
			for(Entry<String, List<Category>> e : dataset.entrySet()) {
				if (map_result_To_Compare.get(e.getKey()).contains(e.getValue().get(0))) {
					matchingSentences++;
				}
			}
			System.out.println("matching sentences between artificial ds and the original"+matchingSentences);
			System.out.println("countCorrect "+countCorrect+"\nWrongly assigned labels: "+countWrong);
			System.out.println("Total classified "+(countCorrect+countWrong));
			System.out.println("Accuracy "+(countCorrect/(dataset.size()*1.0)));
			Print.printMap(mapResult);

		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	 */
}