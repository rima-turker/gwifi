package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.LINE_2modelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.model.NewsgroupsArticle;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.test.longDocument.NewsgroupParser;
import org.fiz.ise.gwifi.test.longDocument.YovistoParser;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.SynchronizedCounter;

import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.WordToSentenceProcessor;

public class TestBasedonLongTextDatasets {

	private final String DATASET_TEST_20NEWS = Config.getString("DATASET_TEST_20NEWS_RANDOM100","");
	private final String DATASET_TEST_YOVISTO = Config.getString("DATASET_TEST_YOVISTO","");
	private final static Integer NUMBER_OF_THREADS=  Config.getInt("NUMBER_OF_THREADS",-1);
	private static boolean LOAD_MODEL = Config.getBoolean("LOAD_MODEL", false);
	private final static TestDatasetType_Enum TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE"); 
	private static Wikipedia wikipedia = WikipediaSingleton.getInstance().wikipedia;
	private static CategorySingleton singCategory;
	private static SynchronizedCounter counterTruePositive;
	private static SynchronizedCounter counterFalsePositive;
	private static SynchronizedCounter counterProcessed;
	private static Map<Category, Integer> numberOfSamplesPerCategory = new ConcurrentHashMap<>();
	private static Map<Category, Integer> truePositive = new ConcurrentHashMap<>();
	private static Map<Category, Integer> falsePositive = new ConcurrentHashMap<>();
	private static Map<String, Category> falsePositiveResult = new ConcurrentHashMap<>();
	private static Map<String, Integer> mapMissClassified = new ConcurrentHashMap<>();
	private ExecutorService executor;
	private static List<String> lstCategory;

	private static Map<Category, Set<Category>> mapCategories;
	//static final Logger resultLog = Logger.getLogger("reportsLogger");
	long now = System.currentTimeMillis();

	public static void main(String[] args) {
		TestBasedonLongTextDatasets test = new TestBasedonLongTextDatasets();
		test.initializeVariables();

	}
	private void initializeVariables() {
		lstCategory= new ArrayList<>();
		TestBasedonLongTextDatasets test = new TestBasedonLongTextDatasets();
		Map<String,List<Category>> map=null;
		if (LOAD_MODEL) {
			LINE_modelSingleton.getInstance();
		}
		if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.YOVISTO)) {
			System.out.println("The dataset type is YOVISTO");
			map = new HashMap<>(test.dataset_YOVISTO());
			System.out.println("map size "+map.size());
		}
		else if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.TWENTYNEWS)) {
			test.dataset_20News();
		}
		else if (TEST_DATASET_TYPE.equals(TestDatasetType_Enum.YOVISTO_SENTENCEBYSENTENCE)) {
			System.out.println("YOVISTO_SENTENCEBYSENTENCE");
			List<Integer> lstNumberOfSentences = new ArrayList<>();
			lstNumberOfSentences.add(1);
			lstNumberOfSentences.add(2);
			lstNumberOfSentences.add(3);
			lstNumberOfSentences.add(5);
			lstNumberOfSentences.add(100000);
			//for(int i : lstNumberOfSentences) {
			//	map = new HashMap<>(YovistoParser.generateDataset(1));
				System.out.println("map size "+map.size());
				for(Entry<String, List<Category>> e: map.entrySet()) {
					List<Category> temp = new ArrayList<>(e.getValue());
					for(Category c:temp) {
						if (!lstCategory.contains(c)) {
							lstCategory.add(c.getTitle());
						}
					}
				}
				System.out.println("Size of Category list "+lstCategory.size());
			//}
		}
		singCategory= CategorySingleton.getInstance(lstCategory);
		counterProcessed= new SynchronizedCounter();
		counterFalsePositive= new SynchronizedCounter();
		counterTruePositive= new SynchronizedCounter();

		Map<Category, Set<Category>> mapTemp = new HashMap<>(singCategory.map);
		for(Entry<Category, Set<Category>> e: mapTemp.entrySet())
		{
			Category main = e.getKey();
			Set<Category> temp = new HashSet<>();
			for(Category c: e.getValue() ) {
				if (c.getChildArticles().length>0) {
					temp.add(c);
				}
			}
			mapTemp.put(main, temp);
		}
		mapCategories= new HashMap<>(mapTemp);
		if (map!=null) {
			startProcessingData(map);
		}
		else {
			System.out.println("Could not load the dataset");
		}
	}
	public  Map<String,List<Category>> dataset_YOVISTO() {
		Map<String,List<Category>> dataset=null;
		Map<Category, Integer> mapCount = new HashMap<>();
		int numberOfSentencesTotal=0;
		try {
			dataset = new HashMap<>();
			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_YOVISTO), "utf-8");
			for(String line : lines) {
				String[] split = line.split("\t");
				String title = split[0];
				String[] categories = split[1].split(",");
				String content = split[2];
				String sentence=segment2Sentence(content,6);
				if (categories.length==1) {
					Category c = wikipedia.getCategoryByTitle(StringUtils.capitalize(categories[0]));
					if (c!=null) {
						if (!lstCategory.contains(c)) {
							lstCategory.add(c.getTitle());
						}
						mapCount.put(c, mapCount.containsKey(c) ? mapCount.get(c) + 1 : 1);
						numberOfSentencesTotal+=SentenceSegmentator.findNumberOfSentences(content);
						dataset.put(title+" "+sentence, Arrays.asList(c));
					}
				}
			}
			System.out.println("Number of articles: "+dataset.size());
			System.out.println("Start processing");

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		int count =0;
		for (Map.Entry<Category, Integer> entry : mapCount.entrySet()) {
			System.out.println(entry.getKey().getTitle()+"\t"+entry.getValue());
			count+=entry.getValue();
		}
		System.out.println("total Number Of Sentence : "+numberOfSentencesTotal);
		System.out.println("Average number of Sentences : "+numberOfSentencesTotal/count);
		System.out.println(count);
		System.out.println("returned dataset size" + dataset.size());
		return dataset;
	}
	public  void dataset_20News() {
		Map<String,List<Category>> dataset = new HashMap<>();
		Map<String, List<Category>> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_20News());
		try {
			NewsgroupParser parser = new NewsgroupParser(DATASET_TEST_20NEWS);
			parser.parse();
			Map<String, List<NewsgroupsArticle>> mapArticles = new HashMap<String, List<NewsgroupsArticle>>(parser.getArticles());
			for(Entry <String, List<NewsgroupsArticle>> e: mapArticles.entrySet() ) {
				System.out.println(e.getKey()+": "+e.getValue().size());
				e.getValue().forEach(a -> {
					List<Category> gtList = new ArrayList<>(mapLabel.get(e.getKey()));
					//String content =a.getRawText();
					String content = segment2Sentence(a.getRawText(), 1);
					dataset.put(content, gtList);
				});
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		startProcessingData(dataset);
	} 
	public void startProcessingData(Map<String,List<Category>> dataset) {
		int count=0;
		try {
			executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
			for(Entry<String, List<Category>> e : dataset.entrySet()) {
				executor.execute(handle(e.getKey(),e.getValue(),++count));
			}
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
			System.out.println("Number of true positive: "+counterTruePositive.value()+" number of processed: "+counterProcessed.value());
			Double d = (counterTruePositive.value()*0.1)/(counterProcessed.value()*0.1);
			System.out.println("Accuracy: "+d);
			//				Print.printMap(truePositive);
			//				Print.printMap(mapMissClassified);
			//				Print.printMap(falsePositive);
			System.out.println("Calculating F measures");
			CalculateClassificationMetrics calculate = new CalculateClassificationMetrics();
			calculate.evaluateResults(truePositive, falsePositive, numberOfSamplesPerCategory);
			FileUtil.writeDataToFile(truePositive,"TRUE_POSITIVE_RESULTS");
			FileUtil.writeDataToFile(falsePositiveResult,"FALSE_POSITIVE_RESULTS");
			FileUtil.writeDataToFile(mapMissClassified,"MISS_CLASSIFIED_RESULTS");
			//			resultLog.info("Total number processed "+ count+", true positive "+counterTruePositive.value());
			//			resultLog.info("Total number processed "+ counterProcessed.value()+", false positive "+counterFalsePositive.value());
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	private Runnable handle(String description, List<Category> gtList,int i) {
		return () -> {
			Category bestMatchingCategory=null;
			bestMatchingCategory = HeuristicApproach.getBestMatchingCategory(description,gtList,mapCategories);
			counterProcessed.increment();
			if (gtList.contains(bestMatchingCategory)) {
				counterTruePositive.increment();
				truePositive.put(gtList.get(0), truePositive.getOrDefault(gtList.get(0), 0) + 1);
				System.out.println(" total processed: "+i+" True positive "+counterTruePositive.value());
			}
			else{
				try {
					falsePositiveResult.put(description+"\n gt:"+gtList.get(0).getTitle(), bestMatchingCategory);
					falsePositive.put(gtList.get(0), falsePositive.getOrDefault(gtList.get(0), 0) + 1);
				} catch (Exception e) {
					System.out.println("Exception msg "+e.getMessage());
					System.out.println("description "+description+" "+gtList+" "+bestMatchingCategory );
					System.exit(1);
				}
				counterFalsePositive.increment();
				String key=gtList.get(0)+"\t"+"predicted: "+bestMatchingCategory;
				mapMissClassified.put(key, mapMissClassified.getOrDefault(key, 0) + 1);
				System.out.println(" total processed: "+i+" True positive "+counterTruePositive.value());
			}
		};
	}
	public static String segment2Sentence(String text,int numOfSentence) {
		final List<CoreLabel> tokens = new ArrayList<CoreLabel>();
		final LexedTokenFactory<CoreLabel> tokenFactory = new CoreLabelTokenFactory();
		final PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(text), tokenFactory, "untokenizable=noneDelete");
		while (tokenizer.hasNext()) {
			tokens.add(tokenizer.next());
		}
		final List<List<CoreLabel>> sentences = new WordToSentenceProcessor<CoreLabel>().process(tokens);
		int end;
		int start = 0;
		StringBuffer resultSentences = new StringBuffer();
		final ArrayList<String> sentenceList = new ArrayList<String>();
		for (List<CoreLabel> sentence: sentences) {
			end = sentence.get(sentence.size()-1).endPosition();
			sentenceList.add(text.substring(start, end).trim());
			resultSentences.append(text.substring(start, end).trim()+" ");
			if (numOfSentence==sentenceList.size()) {
				return resultSentences.toString();
			}
			start = end;
		}
		return resultSentences.toString();
	}

	public static List<String> getLstCategory() {
		return lstCategory;
	}
}


