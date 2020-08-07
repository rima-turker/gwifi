package org.fiz.ise.gwifi.dataset;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.crypto.Data;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.Dataset;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.Print;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class ReadDataset {
	private static Map<Article, Integer> numberOfSamplesPerCategory = new ConcurrentHashMap<>();
	private static final String DATASET_TRAIN_MR_POS = Config.getString("DATASET_TRAIN_MR_POS","");


	public static List<String> get_file_paths_twitter(boolean test) {
		
		String path = Config.getString("DATASET_PATH",""); 
		String  file_buss_train;
		String  file_ent_train;
		String  file_fnl_train;
		String  file_politics_train;
		String  file_sports_train;
		String  file_tech_train;
		if (test) {
			file_buss_train=path+"business_raw_test.txt";
			file_ent_train=path+"ent_raw_test.txt";
			file_fnl_train=path+"fnl_raw_test.txt";
			file_politics_train=path+"politics_raw_test.txt";
			file_sports_train=path+"sports_raw_test.txt";
			file_tech_train=path+"tech_raw_test.txt";
		}
		
		else {
			file_buss_train=path+"business_raw.txt";
			file_ent_train=path+"ent_raw.txt";
			file_fnl_train=path+"fnl_raw.txt";
			file_politics_train=path+"politics_raw.txt";
			file_sports_train=path+"sports_raw.txt";
			file_tech_train=path+"tech_raw.txt";
		}
		List<String> lst_files= new ArrayList<String>();
		lst_files.add(file_buss_train);
		lst_files.add(file_ent_train);
		lst_files.add(file_fnl_train);
		lst_files.add(file_politics_train);
		lst_files.add(file_sports_train);
		lst_files.add(file_tech_train);
		
		return lst_files;
	}
	
	public static Map<String, List<Article>> read_twitter(Dataset dataset) {
		
		HashSet<String> set_buss= new HashSet<String>();
		HashSet<String> set_ent= new HashSet<String>();
		HashSet<String> set_fnl= new HashSet<String>();
		HashSet<String> set_pol= new HashSet<String>();
		HashSet<String> set_sports= new HashSet<String>();
		HashSet<String> set_tech= new HashSet<String>();

		Map<String, Set<String>> map_dataset = new HashMap<String, Set<String>>();
		Map<String, Set<String>> map_removed_dublicates = new HashMap<String, Set<String>>();
		try {
			List<String> f_paths = null;
			if (dataset.equals(Dataset.TWITTER_test)) {
				f_paths = new ArrayList<String>(get_file_paths_twitter(true));
			}
			else if(dataset.equals(Dataset.TWITTER)) {
				f_paths = new ArrayList<String>(get_file_paths_twitter(false));
			}
			for (String f_name:f_paths) {
				List<String> lines = FileUtils.readLines(new File(f_name), "utf-8");
				//						.stream()
				//                        .map(String::toLowerCase)
				//                        .collect(Collectors.toList());
				lines.removeAll(Arrays.asList("", null));
				//Set<String> set_lines = new HashSet<String>();
				System.out.println("-----------------------"+f_name+"-----------------------"+lines.size());
				if (f_name.contains("business_raw")) {
					set_buss = new HashSet<String>(lines);
					map_dataset.put("Business", set_buss);
					System.out.println("Set : " + set_buss.size());
				}
				else if (f_name.contains("ent_raw")) {
					set_ent = new HashSet<String>(lines);
					map_dataset.put("Film", set_ent);
					System.out.println("Set : " + set_ent.size());
				}
				else if (f_name.contains("fnl_raw")) {
					set_fnl = new HashSet<String>(lines);
					map_dataset.put("Fashion", set_fnl);
					System.out.println("Set : " + set_fnl.size());
				}
				else if (f_name.contains("politics_raw")) {
					set_pol = new HashSet<String>(lines);
					map_dataset.put("Politics", set_pol);
					System.out.println("Set : " + set_pol.size());
				}
				else if (f_name.contains("sports_raw")) {
					set_sports = new HashSet<String>(lines);
					map_dataset.put("Sports", set_sports);
					System.out.println("Set : " + set_sports.size());
				}
				else if (f_name.contains("tech_raw")) {
					set_tech = new HashSet<String>(lines);
					map_dataset.put("Software", set_tech);
					System.out.println("Set : " + set_tech.size());
				}
			}

			List<HashSet<String>> lst_sets = new ArrayList<HashSet<String>>();
			lst_sets.add(set_tech);
			lst_sets.add(set_buss);
			lst_sets.add(set_ent);
			lst_sets.add(set_fnl);
			lst_sets.add(set_pol);
			lst_sets.add(set_sports);

			

			for (Entry<String, Set<String>> e: map_dataset.entrySet()) {
				System.out.println(e.getKey()+"   "+e.getValue().size());
				Set<String> t_set = new HashSet<String>(e.getValue());

				for (Entry<String, Set<String>> i_e: map_dataset.entrySet()) {


					Set<String> retain_set = new HashSet<String>(e.getValue());
					retain_set.retainAll(i_e.getValue());
					System.out.println(e.getKey()+"   "+e.getValue().size()+"  "+i_e.getKey()+"   "+i_e.getValue().size()+"  "+retain_set.size());

					if (!e.getKey().equals(i_e.getKey())) {

						for (String s : e.getValue()) {
							if (i_e.getValue().contains(s) ) {
								t_set.remove(s);
							}

						}
					}
				}
				System.out.println(e.getKey()+"   "+t_set.size());
				map_removed_dublicates.put(e.getKey(), t_set);

				System.out.println("----------------------------------------------------------");
			}
			//			for (Entry<String, Set<String>> e: map_removed_dublicates.entrySet()) {
			//				//System.out.println("Size of the outer set : "+e.getValue().size());
			//				
			//				for(Set<String> i_set : lst_sets) {
			//					//System.out.println("Size of the inner set : "+i_set.size());
			//					Set<String> t_set = new HashSet<String>(e.getValue());
			//					t_set.retainAll(i_set);
			//					if (t_set.size()!=i_set.size() && t_set.size()!=0) {
			//						System.out.println("retain size: "+ t_set.size());
			////						e.getValue().removeAll(t_set);
			//					}
			//				}
			//				
			//			//	map_removed_dublicates.put(e.getKey(), e.getValue());
			//				System.out.println("----------------------------------------------------------");
			//			}

			for (Entry<String, Set<String>> e: map_removed_dublicates.entrySet()) {
				System.out.println(e.getKey()+"   "+e.getValue().size());
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Map<String, List<Article>> map_result = new HashMap<String, List<Article>>();
		
		for (Entry<String, Set<String>> e: map_removed_dublicates.entrySet()) {
			for (String s : e.getValue()) {
				map_result.put(s, new ArrayList<Article>(Arrays.asList(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(e.getKey()))));
			}
			//AnalyseDataset.findMostSimilarEntitesForDatasetBasedOnDatasetVector(AnnonatationUtil.findAnnotationAll_filter(new ArrayList<String>(e.getValue()), AnnonatationUtil.getEntityBlackList_Twitter()),e.getKey()+"_datasetVec_mostSim.txt");
		}
		
		//AnalyseDataset.findMostSimilarEntitesForDataset(new ArrayList<String>(map_removed_dublicates.get("Food")), Dataset.TWITTER,"Food");
		System.out.println("The size of the twitter dataset: "+ map_result.size());
		return  map_result;
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

	public static List<String> read_dataset_DBP_BasedOnLabel(String fileName, Article a) {
		List<String> dataset = new ArrayList<>();
		Map<Integer, Article> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_DBP_article());
		try {
			List<String> lines = FileUtils.readLines(new File(fileName), "utf-8");
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			int i=0;
			for (i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split(",\"");
				String label = split[0];
				if (mapLabel.containsKey(Integer.valueOf(label))) {
					if (a.equals(mapLabel.get(Integer.valueOf(label)))) {
						String title = split[1].replace("\"", "");
						String description = split[2].replace("\"", "").trim();
						dataset.add(title+" "+description);
					}
				}
				else {
					System.out.println("ERROR the dataset does not contain the predefined label");
					System.exit(1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Size of the dataset label: "+a.getTitle()+": "+dataset.size());
		return dataset;
	}
	public static Map<String,List<Article>> read_dataset_Doc2Vec_categorized(Dataset dName, String fileName) {
		Map<String,List<Article>> dataset = new HashMap<>();
		String eL=null;
		try {
			if (dName.equals(Dataset.DBpedia)) {
				List<String> lines = FileUtils.readLines(new File(fileName), "utf-8");
				for(String line : lines) {
					eL=line;
					List<Article> gtList = new ArrayList<>(); 
					String[] split = line.split("\t");
					gtList.add(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(split[1]));
					dataset.put(split[0], gtList);
				}
			}
		} catch (Exception e) {
			System.out.println("Exception with the line:"+eL);
			e.printStackTrace();

		}
		if (dataset.size()==0) {
			System.out.println("Size of the DBpedia datset read_dataset_Doc2Vec_categorized : "+dataset.size());
			System.out.println(dName+ " "+fileName);
			System.exit(1);
		}
		System.out.println("Size of the DBpedia datset read_dataset_Doc2Vec_categorized : "+dataset.size());
		System.out.println(dName+ " "+fileName);
		return dataset;
	}

	public static Map<String,List<Article>> read_dataset_DBPedia_SampleLabel(String fileName) {
		Map<String,List<Article>> dataset = new HashMap<>();
		Map<Integer, Article> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_DBP_article());
		String eLine=null;
		try {
			List<String> lines = FileUtils.readLines(new File(fileName), "utf-8");
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			int i=0;
			for (i = 0; i < arrLines.length; i++) {
				eLine=arrLines[i];
				List<Article> gtList = new ArrayList<>(); 
				String[] split = arrLines[i].split(",\"");
				String label = split[0];
				if (mapLabel.containsKey(Integer.valueOf(label))&&split.length==3) {
					gtList.add(mapLabel.get(Integer.valueOf(label)));
					String title = split[1].replace("\"", "");
					String description = split[2].replace("\"", "").trim();
					dataset.put(title+" "+description, gtList);
				}
				else if (mapLabel.containsKey(Integer.valueOf(label))&&split.length==2) {
					gtList.add(mapLabel.get(Integer.valueOf(label)));
					String title = split[1].replace("\"", "");
					dataset.put(title, gtList);
				}
				else {
					System.out.println("ERROR the dataset does not contain the predefined label");
					System.exit(1);
				}
			}
		} catch (Exception e) {
			System.out.println("Exception with the line:"+eLine);
			e.printStackTrace();

		}
		if (dataset.size()==0) {
			System.out.println("read_dataset_DBPedia_SampleLabel: Size of the DBpedia datset: "+dataset.size());
			System.exit(1);
		}
		System.out.println("read_dataset_DBPedia_SampleLabel: Size of the DBpedia datset: "+dataset.size());
		return dataset;
	}
	
	public static Map<String,List<Category>> read_dataset_DBPedia_SampleLabel_with_Categories(String fileName) {
		Map<String,List<Category>> dataset = new HashMap<>();
		Map<Integer, Article> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_DBP_article());
		String eLine=null;
		try {
			List<String> lines = FileUtils.readLines(new File(fileName), "utf-8");
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			int i=0;
			for (i = 0; i < arrLines.length; i++) {
				eLine=arrLines[i];
				List<Category> gtList = new ArrayList<>(); 
				String[] split = arrLines[i].split(",\"");
				String label = split[0];
				if (mapLabel.containsKey(Integer.valueOf(label))&&split.length==3) {
					
					Category c = null;
					if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Company")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Companies");
					}
					else if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Educational institution")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Educational institutions");	
					}
					else if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Athlete")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Athletic sports");	
					}
					else if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Office-holder")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Office-holders");	
					}
					else if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Plant")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Plants");
					}
					else if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Animal")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Animals");
					}
					else if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Artist")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Artists");
					}
					else if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Album")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Albums");
					}
					else if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Village")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Villages");
					}
					else {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle(mapLabel.get(Integer.valueOf(label)).getTitle());
					}
					
					if (c==null) {
						System.out.println("Could not find the corresponding cat: "+mapLabel.get(Integer.valueOf(label)).getTitle());
						System.exit(1);
					}
					gtList.add(c);
					String title = split[1].replace("\"", "");
					String description = split[2].replace("\"", "").trim();
					dataset.put(title+" "+description, gtList);
				}
				else if (mapLabel.containsKey(Integer.valueOf(label))&&split.length==2) {
					Category c = null;
					if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Company")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Companies");
					}
					else if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Educational institution")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Educational institutions");	
					}
					else if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Athlete")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Athletic sports");	
					}
					else if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Office-holder")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Office-holders");	
					}
					else if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Plant")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Plants");	
					}
					else if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Animal")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Animals");
					}
					else if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Artist")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Artists");
					}
					else if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Album")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Albums");
					}
					else if (mapLabel.get(Integer.valueOf(label)).getTitle().equals("Village")) {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle("Villages");
					}
					else {
						c = WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle(mapLabel.get(Integer.valueOf(label)).getTitle());
					}
					
					if (c==null) {
						System.out.println("Could not find the corresponding cat: "+mapLabel.get(Integer.valueOf(label)).getTitle());
						System.exit(1);
					}
					gtList.add(c);
					String title = split[1].replace("\"", "");
					dataset.put(title, gtList);
				}
				else {
					System.out.println("ERROR the dataset does not contain the predefined label");
					System.exit(1);
				}
			}
		} catch (Exception e) {
			System.out.println("Exception with the line:"+eLine);
			e.printStackTrace();

		}
		System.out.println("read_dataset_DBPedia_SampleLabel: Size of the DBpedia datset: "+dataset.size());
		return dataset;
	}

	public static List<String> read_dataset_Yahoo_BasedOnLabel(String fileName, int labelID) {
		List<String> dataset = new ArrayList<>();
		Map<Integer, Article> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_Yahoo_article());
		try {
			List<String> lines = FileUtils.readLines(new File(fileName), "utf-8");
			for (String line: lines) {
				String[] split = line.split("\",\"");
				String label = split[0].replace("\"", "");
				if (mapLabel.containsKey(Integer.valueOf(label))) {

					if (labelID == Integer.valueOf(label)) {
						String title = split[1].replace("\"", "").trim();
						String question = split[2].replace("\"", "").trim();
						String answer = split[3].replace("\"", "").trim();
						dataset.add(title+" "+question+" "+answer);
						//						dataset.add(title+" "+question);
					}
				}
				else {
					System.out.println("ERROR the dataset does not contain the predefined label");
					System.exit(1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Yahoo dataset "+mapLabel.get(labelID).getTitle()+" size:"+dataset.size());
		return dataset;
	}
	public static Map<String,List<Article>> read_dataset_Yahoo_LabelArticle(String fileName) {
		Map<String,List<Article>> dataset = new HashMap<>();
		//Map<Integer, Article> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_Yahoo_article());
		Map<Integer, String> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_Yahoo());
		try {
			List<String> lines = FileUtils.readLines(new File(fileName), "utf-8");
			for (String line: lines) {
				List<Article> gtList = new ArrayList<>(); 
				String[] split = line.split("\",\"");
				String label = split[0].replace("\"", "");
				if (mapLabel.containsKey(Integer.valueOf(label))) {
					String sLabels = mapLabel.get(Integer.valueOf(label));
					String[] splitLabel = sLabels.split("-");
					for (int i = 0; i < splitLabel.length; i++) {
						gtList.add(WikipediaSingleton.getInstance().getArticle(splitLabel[i]));
					}
					String title = split[1].replace("\"", "").trim();
					String question = split[2].replace("\"", "").trim();
					String answer = split[3].replace("\"", "").trim();
					dataset.put(title+" "+question+" "+answer, gtList);

				}
				else {
					System.out.println("ERROR the dataset does not contain the predefined label");
					System.exit(1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Size of the Yahoo datset: "+dataset.size());
		return dataset;
	}

	public static Map<String,List<Article>> read_dataset_AG_LabelArticle(AG_DataType type, String fileName) {
		Map<String,List<Article>> dataset = new HashMap<>();
		Map<Integer, Article> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_AG_article());
		try {
			NLPAnnotationService service = AnnotationSingleton.getInstance().service;
			List<String> lines = FileUtils.readLines(new File(fileName), "utf-8");
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			int i=0;
			for (i = 0; i < arrLines.length; i++) {
				List<Article> gtList = new ArrayList<>(); 
				String[] split = arrLines[i].split("\",\"");
				String label = split[0].replace("\"", "");
				if (mapLabel.containsKey(Integer.valueOf(label))) {
					gtList.add(mapLabel.get(Integer.valueOf(label)));
					if (type==AG_DataType.TITLE) {
						String title = split[1].replace("\"", "");
						List<Annotation> lstAnnotations = new ArrayList<>();
						service.annotate(title, lstAnnotations);//annotate the given text
						if (lstAnnotations.size()<1) {
							//System.out.println(title);
						}
						if (dataset.containsKey(title)) {
							if (!gtList.contains(dataset.get(title).get(0))) {
								gtList.addAll(dataset.get(title));
							}
							title=title+" ";
						}
						dataset.put(title, gtList);
					}
					else if (type==AG_DataType.DESCRIPTION) {
						String description = split[2].replace("\"", "");
						dataset.put(description, gtList);

					}
					if (type==AG_DataType.TITLEANDDESCRIPTION) {
						String title = split[1].replace("\"", "");
						String description = split[2].replace("\"", "");
						dataset.put(title+" "+description, gtList);
					}
				}
				else {
					System.out.println("ERROR the dataset does not contain the predefined label");
					System.exit(1);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("File path: "+fileName);
		}
		return dataset;
	}

	public static Map<String,List<Category>> read_dataset_AG(AG_DataType type) {
		Map<String,List<Category>> dataset = new HashMap<>();
		Map<Integer, Category> mapLabel = new HashMap<>(LabelsOfTheTexts.getLables_AG_category());
		try {
			NLPAnnotationService service = AnnotationSingleton.getInstance().service;
			List<String> lines = FileUtils.readLines(new File(Config.getString("DATASET_TEST_AG","")), "utf-8");
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			int i=0;
			for (i = 0; i < arrLines.length; i++) {
				List<Category> gtList = new ArrayList<>(); 
				String[] split = arrLines[i].split("\",\"");
				String label = split[0].replace("\"", "");
				if (mapLabel.containsKey(Integer.valueOf(label))) {
					if (label.equals("4")) {
						//numberOfSamplesPerCategory.put(mapLabel.get(4), numberOfSamplesPerCategory.getOrDefault(mapLabel.get(4), 0) + 1);
						gtList.add(mapLabel.get(4));
					}
					else {
						//numberOfSamplesPerCategory.put(mapLabel.get(Integer.valueOf(label)), numberOfSamplesPerCategory.getOrDefault(mapLabel.get(Integer.valueOf(label)), 0) + 1);
						gtList.add(mapLabel.get(Integer.valueOf(label)));
					}
					if (type==AG_DataType.TITLE) {
						String title = split[1].replace("\"", "");
						List<Annotation> lstAnnotations = new ArrayList<>();
						service.annotate(title, lstAnnotations);//annotate the given text
						if (lstAnnotations.size()<1) {
							//System.out.println(title);
						}
						if (dataset.containsKey(title)) {
							if (!gtList.contains(dataset.get(title).get(0))) {
								gtList.addAll(dataset.get(title));
							}
							title=title+" ";
						}
						dataset.put(title, gtList);
					}
					else if (type==AG_DataType.DESCRIPTION) {
						String description = split[2].replace("\"", "");
						dataset.put(description, gtList);

					}
					if (type==AG_DataType.TITLEANDDESCRIPTION) {
						String title = split[1].replace("\"", "");
						String description = split[2].replace("\"", "");
						dataset.put(title+" "+description, gtList);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dataset;
	}
	public static List<String> read_AG_BasedOnType(String file,AG_DataType type){
		List<String> dataset = new ArrayList<>();
		try {
			List<String>  lines = new ArrayList<>(FileUtils.readLines(new File(file), "utf-8"));
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			int i=0;
			for (i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split("\",\"");
				String title = split[1].replace("\"", "");
				String description = split[2].replace("\"", "");
				if (type.equals(AG_DataType.TITLE)) {
					dataset.add(title);
				}
				else if (type.equals(AG_DataType.DESCRIPTION)) {
					dataset.add(description);
				}
				else if (type.equals(AG_DataType.TITLEANDDESCRIPTION)) {
					dataset.add(title+" "+description);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dataset;
	}
	public static List<String> read_AG_BasedOnCategory(Category c, AG_DataType type){
		List<String> dataset = new ArrayList<>();
		List<String> lines;
		Map<Category, Integer> map = new HashMap<>(LabelsOfTheTexts.getCatValue_AG());
		try {
			lines = new ArrayList<>(FileUtils.readLines(new File(Config.getString("DATASET_TEST_AG","")), "utf-8"));
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			int i=0;
			String valueOfCat = String.valueOf(map.get(c));
			for (i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split("\",\"");
				String label = split[0].replace("\"", "");
				if (label.equals(valueOfCat)) {
					String title = split[1].replace("\"", "");
					String description = split[2].replace("\"", "");
					if (type.equals(AG_DataType.TITLE)) {
						dataset.add(title);
					}
					else if (type.equals(AG_DataType.DESCRIPTION)) {
						dataset.add(description);
					}
					else if (type.equals(AG_DataType.TITLEANDDESCRIPTION)) {
						dataset.add(title+" "+description);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dataset;
	}
	
	public static Map<String, List<Article>> read_twitter_embedding_labeled(String fName){
		try {
			Map<String, List<Article>> dataset = new HashMap<String, List<Article>>();
			List<String> lines = FileUtils.readLines(new File(fName), "utf-8");
			for(String line : lines) {
				Article a=WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(line.split("\t")[0]);
				dataset.put(line.split("\t")[1],Arrays.asList(a));
			}
			return dataset;
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	public static List<String> read_snippets(String fName){
		try {
			List<String> dataset = new ArrayList<>();
			List<String> lines = FileUtils.readLines(new File(fName), "utf-8");
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			for (int i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split(" ");
				String label = split[split.length-1];
				String snippet = arrLines[i].substring(0, arrLines[i].length()-(label).length()).trim();
				dataset.add(snippet);
			}
			return dataset;
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}
	public static List<String> read_WEB_BasedOnCategory(String c,String file){
		try {
			List<String> dataset = new ArrayList<>();
			List<String> lines = FileUtils.readLines(new File(file), "utf-8");
			System.out.println("size of the file "+lines.size());
			String[] arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			for (int i = 0; i < arrLines.length; i++) {
				String[] split = arrLines[i].split(" ");
				String label = split[split.length-1];
				String snippet = arrLines[i].substring(0, arrLines[i].length()-(label).length()).trim();
				if (label.contains(c.toLowerCase())) {
					dataset.add(snippet);
				}
			}
			return dataset;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
	public static List<String> read_dataset_Snippets_list(String fName) {
		String[] arrLines = null;
		Integer index=0;
		try {
			List<String> dataset = new ArrayList<>();
			List<String> lines = FileUtils.readLines(new File(fName), "utf-8");
			System.out.println("size of the file "+lines.size());
			arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			for (int i = 0; i < arrLines.length; i++) {
				index=i;
				String[] split = arrLines[i].split(" ");
				String label = split[split.length-1];
				String snippet = arrLines[i].substring(0, arrLines[i].length()-(label).length()).trim();
				dataset.add(snippet);
			}
			System.out.println("Size of the snippets line: "+ dataset.size());
			return dataset;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.out.println(arrLines[index]);
		}
		return null;
	}
	public static Map<String,List<Article>> read_dataset_Snippets(String fName) {
		String[] arrLines = null;
		Integer index=0;
		try {
			Map<String,List<Article>> dataset = new HashMap<>();
			List<String> lines = FileUtils.readLines(new File(fName), "utf-8");
			System.out.println("size of the file "+lines.size());
			arrLines = new String[lines.size()];
			arrLines = lines.toArray(arrLines);
			for (int i = 0; i < arrLines.length; i++) {
				index=i;
				String[] split = arrLines[i].split(" ");
				String label = split[split.length-1];
				String snippet = arrLines[i].substring(0, arrLines[i].length()-(label).length()).trim();
				List<Article> gtList = new ArrayList<>(); 

				if (label.contains("-")) {
					String[] splitLabel = label.split("-");
					for (int j = 0; j < splitLabel.length; j++) {
						gtList.add(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(StringUtils.capitalize(splitLabel[j])));
						numberOfSamplesPerCategory.put(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(StringUtils.capitalize(splitLabel[j])), numberOfSamplesPerCategory.getOrDefault(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(StringUtils.capitalize(splitLabel[j])), 0) + 1);
					}
				}
				else{
					gtList.add(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(StringUtils.capitalize(label)));
					numberOfSamplesPerCategory.put(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(StringUtils.capitalize(label)), numberOfSamplesPerCategory.getOrDefault(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(StringUtils.capitalize(label)), 0) + 1);
				}
				dataset.put(snippet, gtList);
			}
			//Print.printMap(numberOfSamplesPerCategory);
			return dataset;
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.out.println(arrLines[index]);
		}
		return null;
	}
}