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
import edu.kit.aifb.gwifi.model.Wikipedia;
import weka.gui.SysErrLog;

public class DatasetGeneration_TFIDF {

	private final static Integer NUMBER_OF_ARTICLES_RANDOM_PER_LABEL = Config.getInt("NUMBER_OF_ARTICLES_RANDOM_PER_LABEL", 0);
	private final static TestDatasetType_Enum TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private final static String WIKI_ALL_FILES= Config.getString("WIKI_ALL_FILES","");
	private final static String AG_TEST_DATASET_TFIDF= Config.getString("AG_TEST_DATASET_TFIDF","");

	public static void main(String[] args) {
		//generateTestSetTFIDF();
		generateTrainSetTFIDF();
	}

	private static void generateTrainSetTFIDF() {

		DatasetGeneration_TFIDF test = new DatasetGeneration_TFIDF();
		Map<Category, List<Integer>> randomArticles = new HashMap<>(test.generateRandomArticleIDs());
		List<Integer> IDs = new ArrayList<>();
		for (Entry<Category, List<Integer>> e: randomArticles.entrySet()) {
			IDs.addAll(e.getValue());
		}
		System.out.println("IDs size "+IDs.size());
		Map<Integer,Document> wikipediaDocuments = new HashMap<>(test.readWikipediaDocumentsBasedOnIds(IDs));

		//		for(Entry<Integer,Document> e: wikipediaDocuments.entrySet()) {
		//			System.out.println(e.getKey()+" "+e.getValue().getId()+" "+e.getValue().getContent());
		//		}
		test.compareWriteToFile(wikipediaDocuments,randomArticles);
	}
	private static void generateTestSetTFIDF() {
		try {
			List<String> lines = FileUtils.readLines(new File(AG_TEST_DATASET_TFIDF), "utf-8");
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
				FileUtil.writeDataToFile(Arrays.asList(description), directory+File.separator+i,false);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	private void compareWriteToFile(Map<Integer,Document> wikipediaDocuments,Map<Category, List<Integer>> randomArticles) {
		int countNotContain =0;
		int countContains=0;
		String folder ="TrainTFIDF";
		Map<Category, Integer> map = new HashMap<>();
		try {
			File directory = new File(folder);
			FileUtils.deleteDirectory(directory);
			directory.mkdir();
			System.out.println("Wikipedia size "+wikipediaDocuments.size());
			for(Entry<Category, List<Integer>> e : randomArticles.entrySet() ) {
				directory = new File(folder+File.separator+e.getKey().getTitle());
				directory.delete();
				directory.mkdir();
				for(Integer i : e.getValue()) {
					if (wikipediaDocuments.containsKey(i)) {
						if (map.containsKey(e.getKey())) {
							int count = map.get(e.getKey());
							if (count<NUMBER_OF_ARTICLES_RANDOM_PER_LABEL) {
								FileUtil.writeDataToFile(Arrays.asList(wikipediaDocuments.get(i).toString()), directory+File.separator+i+".txt",false);
								map.put(e.getKey(), ++count);
							}
						}
						else {
							map.put(e.getKey(), 1);
							FileUtil.writeDataToFile(Arrays.asList(wikipediaDocuments.get(i).toString()), directory+File.separator+i+".txt",false);
						}
					}
				}
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
		final List<Document> resultDocuments = new ArrayList<>(WikipediaFilesUtil.getDocuments(WIKI_ALL_FILES))	;
		for(Document d : resultDocuments) {
			if (IDs.contains(d.getId())) {
				result.put(d.getId(),d);
			}
		}
		System.out.println("wikipedia article size "+ result.size());
		return result;
	}
	private Map<Category, List<Integer>> generateRandomArticleIDs(){
		Map<Category, List<Integer>> result = new HashMap<>();
		Map<Category, Set<Category>> mapCategories = new HashMap<>(CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE)).map);
		for(Entry<Category, Set<Category>> e : mapCategories.entrySet()) {
			List<Article> childArticles = new ArrayList<>();
			childArticles.addAll(Arrays.asList(e .getKey().getChildArticles()));
			for(Category cCat : e.getValue()) {
				childArticles.addAll(Arrays.asList(cCat.getChildArticles()));
			}
			List<Integer> random = new ArrayList<>(MergeTwoFiles.random(0, childArticles.size()-1, NUMBER_OF_ARTICLES_RANDOM_PER_LABEL));
			List<Integer> temp = new ArrayList<>();
			for(Integer i : random) {
				temp.add(childArticles.get(i).getId());
			}
			result.put(e.getKey(), temp);
		}
		System.out.println("Random article size "+ result.size());
		if (result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science"))||result.containsKey(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Technology"))) {
			List<Integer> sci = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science")));
			int size=sci.size();
			List<Integer> tech = new ArrayList<>(result.get(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Technology")));
			List<Integer> merge = new ArrayList<>(sci.subList(0, size/2));
			merge.addAll(tech.subList(0, size/2));
			System.out.println("Size of the merger "+merge.size());
			result.remove(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Science"));
			result.put(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Technology"), merge);
		}
		else {
			System.err.println("Random Map does not contain all the categories");
			System.exit(1);
		}
		return result;
	}
}
