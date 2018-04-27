package org.fiz.ise.gwifi.dataset.shorttext.baseline;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.bytedeco.javacpp.presets.opencv_core.Str;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.dataset.shorttext.test.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.shorttext.test.TestBasedonDatasets;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.Document;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.MergeTwoFiles;
import org.fiz.ise.gwifi.util.Print;
import org.fiz.ise.gwifi.util.WikipediaFilesUtil;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;
import weka.gui.SysErrLog;

public class DatasetGeneration_TFIDF {

	private final static Integer NUMBER_OF_ARTICLES_RANDOM_PER_LABEL = Config.getInt("NUMBER_OF_ARTICLES_RANDOM_PER_LABEL", 0);
	private final static TestDatasetType_Enum TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private final static String WIKI_ALL_FILES= Config.getString("WIKI_ALL_FILES","");
	private final static String DATASET_TEST_AG= Config.getString("DATASET_TEST_AG","");
	private final static String DATASET_TEST_WEB= Config.getString("DATASET_TEST_WEB","");
	private final static Integer NUMBER_OF_ARTICLES_PER_LABEL=Config.getInt("NUMBER_OF_ARTICLES_PER_LABEL",0);
	public static void main(String[] args) {
		//generateTestSetTFIDF_AG();
		//generateTrainSetTFIDF();
		generateTestSetTFIDF_WEB();
	}
	private static void generateTrainSetTFIDF() {
		DatasetGeneration_TFIDF test = new DatasetGeneration_TFIDF();
		Map<Category, Set<Integer>> randomArticles = new HashMap<>(test.generateRandomArticleIDs());
		List<Integer> IDs = new ArrayList<>();
		for (Entry<Category, Set<Integer>> e: randomArticles.entrySet()) {
			IDs.addAll(e.getValue());
		}
		System.out.println("IDs size "+IDs.size());

		//		for(Entry<Integer,Document> e: wikipediaDocuments.entrySet()) {
		//			System.out.println(e.getKey()+" "+e.getValue().getId()+" "+e.getValue().getContent());
		//		}
		Map<Integer,Document> wikipediaDocuments = new HashMap<>(test.readWikipediaDocumentsBasedOnIds(IDs));
		test.compareWriteToFile(wikipediaDocuments,randomArticles);
	}
	private static void generateTestSetTFIDF_WEB() {
		try {
			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_WEB), "utf-8");
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			for (int i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split(" ");
				String label = split[split.length-1];
				String snippet = arrLines[i].substring(0, arrLines[i].length()-(label).length()).trim();
				String folderName = label;
				File directory = new File("TestDataset_TFIDF_WEB"+File.separator+folderName);
				if (! directory.exists()){
					directory.mkdir();
				}
				FileUtil.writeDataToFile(Arrays.asList(snippet), directory+File.separator+i,false);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	private static void generateTestSetTFIDF_AG() {
		try {
			List<String> lines = FileUtils.readLines(new File(DATASET_TEST_AG), "utf-8");
			Map<Integer, Category> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_AG());
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			int i=0;
			for (i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split("\",\"");
				String label = split[0].replace("\"", "");
				String title = split[1].replace("\"", "");
				String description = split[2].replace("\"", "");
				String folderName = mapLabel.get(Integer.parseInt(label)).getTitle();
				File directory = new File("TestDataset"+File.separator+folderName);
				if (! directory.exists()){
					directory.mkdir();
				}
				FileUtil.writeDataToFile(Arrays.asList(title+" "+description), directory+File.separator+i,false);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	private void compareWriteToFile(Map<Integer,Document> wikipediaDocuments,Map<Category, Set<Integer>> randomArticles) {
		int countNotContain =0;
		int countContains=0;
		String folder ="TrainTFIDF";
		Map<Category, Integer> map = new HashMap<>();
		try {
			File directory = new File(folder);
			FileUtils.deleteDirectory(directory);
			directory.mkdir();
			System.out.println("Wikipedia size "+wikipediaDocuments.size());
			for(Entry<Category, Set<Integer>> e : randomArticles.entrySet() ) {
				directory = new File(folder+File.separator+e.getKey().getTitle());
				directory.delete();
				directory.mkdir();
				for(Integer i : e.getValue()) {
					if (wikipediaDocuments.containsKey(i)) {
						if (map.containsKey(e.getKey())) {
							int count = map.get(e.getKey());
							if (count<(NUMBER_OF_ARTICLES_PER_LABEL+1)) {
								FileUtil.writeDataToFile(Arrays.asList(wikipediaDocuments.get(i).toString()), directory+File.separator+i+".txt",false);
								map.put(e.getKey(), ++count);
							}
						}
						else {
							map.put(e.getKey(), 1);
							FileUtil.writeDataToFile(Arrays.asList(wikipediaDocuments.get(i).toString()), directory+File.separator+i+".txt",false);
						}
						countContains++;
					}
					else {
						countNotContain++;
					}
				}
				Print.printMap(map);
				System.out.println("Does not contain size "+countNotContain);
				System.out.println("contains "+ countContains);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	private Map<Integer,Document> readWikipediaDocumentsBasedOnIds(List<Integer> IDs) {
		Map<Integer,Document> result= new HashMap<>();
		final List<Document> resultDocuments = new ArrayList<>(WikipediaFilesUtil.getDocuments(WIKI_ALL_FILES))	;//all wikipedia article is in the list
		System.out.println("Total wikipedia article size "+ resultDocuments.size());
		for(Document d : resultDocuments) { //Based on random article IDs we get their corresponding wikipedia article 
			if (IDs.contains(d.getId())) {
				result.put(d.getId(),d);
			}
		}
		System.out.println("Total wikipedia article size after reading the all wikipedia "+ result.size());
		return result;
	}
	private Map<Category, Set<Integer>> generateRandomArticleIDs(){
		Map<Category, Set<Integer>> result = new HashMap<>();
		Map<Category, Set<Category>> mapCategories = new HashMap<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).map);
		for(Entry<Category, Set<Category>> e : mapCategories.entrySet()) {//iterate over all the main cates and get their child articles
			List<Article> dirtyChildArticles = new ArrayList<>();
			dirtyChildArticles.addAll(Arrays.asList(e .getKey().getChildArticles()));
			for(Category cCat : e.getValue()) {
				dirtyChildArticles.addAll(Arrays.asList(cCat.getChildArticles()));//per category all child articles are stored in childArticles
			}
			List<Article> cleanChildArticles = new ArrayList<>();
			for(Article a: dirtyChildArticles) {
				if (a.getType().equals((PageType.article))) {
					cleanChildArticles.add(a);
				}
			}
			System.out.println("For category "+e.getKey().getTitle()+" number of dirtyList(contains disambiguation) articles: "+dirtyChildArticles.size()+" "
					+ "\nafter filtering the disambiguation pages the size is "+cleanChildArticles.size());
			List<Integer> random = new ArrayList<>(MergeTwoFiles.random(0, cleanChildArticles.size()-1, NUMBER_OF_ARTICLES_RANDOM_PER_LABEL));//Based on number of child articles and and the maximum sizes of the random articles
			Set<Integer> temp = new HashSet<>();//fist generate random unique numbers and then get the corresponding articles 
			for(Integer i : random) {
				temp.add(cleanChildArticles.get(i).getId());
			}
			result.put(e.getKey(), temp);//add to map each category and its random articles 
		}
		System.out.println("Random article size "+ result.size()); 
		if (result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science"))&&result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Education"))) {
			List<Integer> sci = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science")));
			int size=sci.size();
			List<Integer> tech = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Education")));
			Set<Integer> merge = new HashSet<>(sci.subList(0, size/2));
			merge.addAll(tech.subList(0, size/2));
			System.out.println("Size of the merger after merging the science and education random IDs: "+merge.size());
			result.remove(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science"));
			result.put(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Education"), merge);
		}
		else {
			System.err.println("Random Map does not contain all the categories");
			System.exit(1);
		}
		if (result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Politics"))&&result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Society"))) {
			List<Integer> sci = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Politics")));
			int size=sci.size();
			List<Integer> tech = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Society")));
			Set<Integer> merge = new HashSet<>(sci.subList(0, size/2));
			merge.addAll(tech.subList(0, size/2));
			System.out.println("Size of the merger after merging the Society and Politics random IDs: "+merge.size());
			result.remove(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Politics"));
			result.put(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Society"), merge);
		}
		else {
			System.err.println("Random Map does not contain all the categories");
			System.exit(1);
		}
		if (result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Culture"))&&result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Arts"))&&result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Entertainment"))) {
			List<Integer> sci = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Culture")));
			int size=sci.size();
			List<Integer> ent = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Entertainment")));
			List<Integer> arts = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Arts")));
			Set<Integer> merge = new HashSet<>(sci.subList(0, size/3));
			merge.addAll(ent.subList(0, size/3));
			merge.addAll(arts.subList(0, size/3));
			System.out.println("Size of the merger after merging the Entertainment, Culture and Arts random IDs: "+merge.size());
			result.remove(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Culture"));
			result.remove(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Entertainment"));
			result.put(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Arts"), merge);
		}
		else {
			System.err.println("Random Map does not contain all the categories");
			System.exit(1);
		}




		//		/*
		//		 * Since science and technology is considered as a one class then we can devide each class elemnets of list into half and then merge them
		//		 */
		//		if (result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science"))&&result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Technology"))) {
		//			List<Integer> sci = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science")));
		//			int size=sci.size();
		//			List<Integer> tech = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Technology")));
		//			Set<Integer> merge = new HashSet<>(sci.subList(0, size/2));
		//			merge.addAll(tech.subList(0, size/2));
		//			System.out.println("Size of the merger after merging the science and technology random IDs: "+merge.size());
		//			result.remove(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science"));
		//			result.put(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Technology"), merge);
		//		}
		//		else {
		////			System.err.println("Random Map does not contain all the categories");
		////			System.exit(1);
		//		}
		return result;
	}
}
