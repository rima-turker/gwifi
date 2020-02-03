package org.fiz.ise.gwifi.dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.apache.log4j.Logger;
import org.apache.tools.ant.types.CommandlineJava.SysProperties;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.MapUtil;
import org.fiz.ise.gwifi.util.SynchronizedCounter;

import riotcmd.printtokens;


public class AnalysePatentData {
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger thirdLOG = Logger.getLogger("reportsLogger");
	private static ExecutorService executor;
	private final static Integer NUMBER_OF_THREADS= Config.getInt("NUMBER_OF_THREADS",-1);
	private final static String fileNamePatent=Config.getString("DATASET_PATENT","");
	private final static String fileNamePublicationsHaveAbstract=Config.getString("DATASET_PUBLICATIONS_ABSTRACT","");
	private final static String fileNamePatentCPC=Config.getString("DATASET_PATENT_CPC","");

	private static Set<String> set_publications= Collections.synchronizedSet(new HashSet<>());
	private static Set<String> set_abstracts= Collections.synchronizedSet(new HashSet<>());

	private static Map<String, String> map_publication_abstract = new ConcurrentHashMap<>();
	private static Map<String, String> map_abstract_publication = new ConcurrentHashMap<>();
	private static Map<String, String> map_publication_application = new ConcurrentHashMap<>();
	private static Map<String, List<String>> map_application_cpc= new ConcurrentHashMap<>();

	private static SynchronizedCounter count_dublicate_publications_applications=new SynchronizedCounter();
	private static SynchronizedCounter count_application_no_CPC=new SynchronizedCounter();
	private static SynchronizedCounter count_null_app=new SynchronizedCounter();
	private static SynchronizedCounter count_null_cpc=new SynchronizedCounter();
	private static Set<String> setMostCommonCats;


	public static void main(String[] args) throws Exception  {
		//		compareLabels();
		setMostCommonCats = findMostCommonCats("patent_publication_abstract_cpc.txt", 300);
		generateTrainandTestData("patent_publication_abstract_cpc.txt");
	}
	private static void generate_cpc_hierarchy(String fName, int num) throws Exception {
		
	}
	

	private static Set<String> findMostCommonCats(String fName, int num) throws Exception {
		Set<String> set_common_cats= new HashSet<String>();
		Map<String, Integer> map_result = new HashMap<String, Integer>();

		List<String> lines = FileUtils.readLines(new File(fName), "utf-8");
		for(String line: lines) {
			String[] split = line.split("\t");
			String label= split[2];
			String[] split_label = label.split(",");
			Set<String> set_label = new HashSet<String>();
			for (int i = 0; i < split_label.length; i++) {
				String first_Chars=split_label[i].substring(0,4);
				set_label.add(first_Chars);
			}
			for(String c : set_label) {
				map_result.merge(c, 1, Integer::sum);
			}
		}
		Map<String, Integer> sortByValueDescending = MapUtil.sortByValueDescending(map_result);
		int count=0;
		for(Entry<String, Integer> e : sortByValueDescending.entrySet()) {
			set_common_cats.add(e.getKey());
			count++;
			if (count==num) {
				break;
			}
		}
		System.out.println("Size of the most common cats: "+set_common_cats.size());
		return set_common_cats;
	}

	private static void generateTrainandTestData(String fName) throws Exception {
		List<String> lines = FileUtils.readLines(new File(fName), "utf-8");
		int lastIndex= (lines.size()*80)/100;

		List<String> list_train = new ArrayList<String>(lines.subList(0, lastIndex));
		List<String> list_test = new ArrayList<String>(lines.subList(lastIndex, lines.size()));

		System.out.println("Size the original list: " + lines.size());
//		System.out.println("Size the  list_train: " + list_train.size());
//		System.out.println("Size the  list_test: " + list_test.size());
//		System.out.println("Size the total: " + (list_test.size()+list_train.size()));

		writeDataToFile_filter(lines);
	}

	private static void compareLabels() throws IOException {
		List<String> labels_train  = FileUtils.readLines(new File("/home/rima/playground/Datasets/Features/LabelInformation/patent/patent_train_labels_no_dublicate.txt"), "utf-8");
		List<String> labels_test  = FileUtils.readLines(new File("/home/rima/playground/Datasets/Features/LabelInformation/patent/patent_test_labels_no_dublicate.txt"), "utf-8");

		Set<String> s_train = new HashSet<String>();
		Set<String> s_test = new HashSet<String>();

		for(String str: labels_train) {
			String[] split = str.split(" ");
			for (int i = 0; i < split.length; i++) {
				s_train.add(split[i]);
			}
		}
		for(String str: labels_test) {
			String[] split = str.split(" ");
			for (int i = 0; i < split.length; i++) {
				if (!s_train.contains(split[i])) {
					System.out.println(split[i]);
				}
				s_test.add(split[i]);
			}
		}

		System.out.println("Size of the train : "+s_train.size());
		System.out.println("Size of the test : "+s_test.size());
	}
	private static void writeDataToFile_filter(List<String> lst) {
		Set<String> set_dublicates = new HashSet<String>();
		for(String str : lst) {
			String[] split = str.split("\t");
			String s_abstract=split[1];
			String label= split[2];

			String key = s_abstract+" "+label;
			if (!set_dublicates.contains(key)) {
				String[] split_label = label.split(",");
				Set<String> set_label = new HashSet<String>();
				for (int i = 0; i < split_label.length; i++) {
					String first_Chars=split_label[i].substring(0,4);
					if (setMostCommonCats.contains(first_Chars)) {
						set_label.add(first_Chars);
					}
				}
				if (set_label.size()>0) {
					StringBuilder strBuild = new StringBuilder();
					for (String str_label: set_label) {
						strBuild.append(str_label+" ");
					}
					if (set_abstracts.contains(s_abstract)) {
						System.out.println(str);
					}
					secondLOG.info(s_abstract);
					thirdLOG.info(strBuild.substring(0, strBuild.length()-1));
				}
				set_dublicates.add(key);
			}
		}
		System.out.println("After converting to set: "+set_dublicates.size());
	}
	private static void writeDataToFile(List<String> lst) {
		Set<String> set_dublicates = new HashSet<String>();
		for(String str : lst) {
			String[] split = str.split("\t");
			String s_abstract=split[1];
			String label= split[2];

			String key = s_abstract+" "+label;
			if (!set_dublicates.contains(key)) {
				String[] split_label = label.split(",");
				Set<String> set_label = new HashSet<String>();
				for (int i = 0; i < split_label.length; i++) {
					String first_Chars=split_label[i].substring(0,4);
					set_label.add(first_Chars);
				}
				StringBuilder strBuild = new StringBuilder();
				for (String str_label: set_label) {
					strBuild.append(str_label+" ");
				}
				if (set_abstracts.contains(s_abstract)) {
					System.out.println(str);
				}
				secondLOG.info(s_abstract);
				thirdLOG.info(strBuild.substring(0, strBuild.length()-1));
				set_dublicates.add(key);
			}
		}
		System.out.println("After converting to set: "+set_dublicates.size());
	}
	private static void analyseExtractedResults(String fName) throws Exception {
		Set<String> set_first_chars= new HashSet<String>();
		Map<String, Set<String>> map_dataset = new HashMap<>();
		List<String> lines = FileUtils.readLines(new File(fName), "utf-8");
		int count_single_label=0;
		int count_multiple_label=0;

		for(String str : lines) {
			String[] split = str.split("\t");
			String label= split[2];

			String[] split_label = label.split(",");

			Set<String> set_label = new HashSet<String>();
			for (int i = 0; i < split_label.length; i++) {
				String first_Chars=split_label[i].substring(0,4);
				set_label.add(first_Chars);
			}
			if (set_label.size()>1) {
				count_multiple_label++;
			}
			else if (set_label.size()==1) {
				count_single_label++;
			}
		}
		System.out.println("Total lines: "+lines.size());

		System.out.println("count_single_label: "+count_single_label);
		System.out.println("count_multiple_label: "+count_multiple_label);
		System.out.println("The size of the set set_first_char: "+set_first_chars.size());
	}
	private static void get_patent_id_abstract_labels() throws IOException, Exception {
		map_publication_abstract=new HashMap<String, String>(readExtractedData("patent_publicationID_abstract_clean.txt"));
		set_publications= new HashSet<String>(map_publication_abstract.keySet());
		System.out.println("Size of the publications: "+map_publication_abstract.size());
		//extractPublications();
		map_publication_application= new HashMap<String, String>(readExtractedData("patent_publicationID_application_clean.txt"));
		System.out.println("Size of the publications and applications: "+		map_publication_application.size());

		//extractApplication();

		extractCPCCodeApplications();
		System.out.println("Size of the applications and cpc: "+		map_application_cpc.size());



		//		System.out.println("Count of the applications null: "+		count_null_app.value());
		//		System.out.println("Count of the  cpc null: "+		count_null_cpc.value());

		//		System.out.println("**********************************");
		//		System.out.println(map_publication_application.get("1892927/A1"));
		//		System.out.println("**********************************");
		//		System.out.println(map_application_cpc.get("07021657").size());
		//		System.out.println("**********************************");
		//		System.out.println(map_application_cpc.get("07021657"));

		mergeResults();
		System.out.println("count_application_no_CPC: "+count_application_no_CPC.value() );
	}

	private static void mergeResults() {
		for(Entry<String, String> e : map_publication_abstract.entrySet()) {
			String publicationID = e.getKey();
			String publicationAbstract = e.getValue();
			String application = map_publication_application.get(publicationID);



			if (application!=null &&map_application_cpc.containsKey(application)){
				count_application_no_CPC.increment();
				List<String> lst = map_application_cpc.get(application);
				StringBuilder str_cpc=new StringBuilder();
				for(String s : lst) {
					str_cpc.append(s+",");
				}
				secondLOG.info(publicationID+"\t"+publicationAbstract+"\t"+str_cpc.toString().substring(0, str_cpc.length()-1));
			}
			//			if (publicationID.equals("1045302/A1")) {
			//				System.out.println("pub ID: "+publicationID);
			//				
			//				if (map_application_cpc.containsKey(application)){
			//					System.out.println("map_application_cpc contains the application: "+application+" "+publicationID);
			//					System.out.println("*******: "+map_application_cpc.get(application));
			//				}
			//				else {
			//					System.out.println("***********map_application_cpc does not contain the application: "+application+" "+publicationID);
			//					count_null_cpc.increment();
			//					//if (count_null_cpc.value()==2) {
			//						//System.exit(0);
			////					}
			//				}
			//				System.exit(0);
			//			}


			//			if (application!=null && map_application_cpc.containsKey(application)) {
			//				List<String> lst = map_application_cpc.get(application);
			//				StringBuilder str_cpc=new StringBuilder();
			//				for(String s : lst) {
			//					str_cpc.append(s+",");
			//				}
			//				secondLOG.info(publicationID+"\t"+publicationAbstract+"\t"+str_cpc.toString().substring(0, str_cpc.length()-1));
			//			}
			//			else {
			//				count_application_no_CPC.increment();
			//				if (count_application_no_CPC.value()==3) {
			//					System.out.println("application does not have a cpc: "+application+" "+publicationID);
			//					System.out.println("e key and a value: "+e.getKey()+" "+e.getValue());
			//					System.exit(1);
			//				}
			//			}
		}
	}

	private static void extractCPCCodeApplications() throws Exception{
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		int count=0;
		BufferedReader br = new BufferedReader(new FileReader(fileNamePatentCPC)); 
		String st; 
		while ((st = br.readLine()) != null) {
			executor.execute(runextractCPCCodeApplications(st, ++count));
		} 
		br.close();
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		System.out.println("Total number of lines: "+count);
		System.out.println("map_application_cpc: "+map_application_cpc.size());
		//		System.out.println("Total number of lines A1: "+count_A1.value());

	}
	private static Runnable runextractCPCCodeApplications(String line, int i) {
		return () -> {
			if (line.contains("http://data.epo.org/linked-data/def/patent/classificationCPCInventive")) {
				String[] split = line.split("<http://data.epo.org/linked-data/def/patent/classificationCPCInventive>");
				if (split[0].contains("id/application/EP/")) {
					String application=getApplicationID(split[0]);
					try {
						String cpc=getCPC(split[1]);
						synchronized (map_application_cpc) {
							List<String> tmp;
							if(map_application_cpc.containsKey(application)) {
								tmp=new ArrayList<String>(map_application_cpc.get(application));
								//								List<String> tmp =Collections.synchronizedList(new ArrayList<>(map_application_cpc.get(application)));
								//Collections.synchronizedList(tmp);
							}
							else {
								tmp = new ArrayList<String>();
							}
							tmp.add(cpc);
							map_application_cpc.put(application, tmp);
						}
					} catch (Exception e) {
						System.out.println(line);
						System.out.println(split[0]);
						System.exit(1);
					}
				}

			}
		};
	}
	private static void extractApplication() throws IOException, InterruptedException {
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		int count=0;
		BufferedReader br = new BufferedReader(new FileReader(fileNamePatent)); 
		String st; 
		while ((st = br.readLine()) != null) {
			executor.execute(runExtractFeatures(st, ++count));
		} 
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		br.close();
		System.out.println("Total number of lines application: "+count);
		System.out.println("map_publication_application: "+map_publication_application.size());
	}
	private static Runnable runExtractFeatures(String line, int i) {
		return () -> {
			if (line.contains("http://data.epo.org/linked-data/def/patent/application")) {
				String[] split = line.split("<http://data.epo.org/linked-data/def/patent/application>");
				String application=null;
				if (split[0].contains("/publication/EP/")) {
					String publicationID_type=null;
					try {
						publicationID_type=getPublicationID(split[0]);
						if (set_publications.contains(publicationID_type)) {
							application=getApplicationID(split[1]);
							synchronized (count_dublicate_publications_applications) {
								if (map_publication_application.containsKey(publicationID_type) ) {
									count_dublicate_publications_applications.increment();
								}
								map_publication_application.put(publicationID_type, application);
							}
							//							resultLog.info(publicationID_type+"\t"+application);
						}

					} catch (Exception e) {
						System.out.println("publicationID_type: "+publicationID_type);
						System.out.println(line);
						System.out.println(split[0]);
						System.exit(1);
					}
				}
			}
		};
	}
	private static void extractPublications() throws IOException, InterruptedException {
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
		int count=0;
		BufferedReader br = new BufferedReader(new FileReader(fileNamePublicationsHaveAbstract)); 
		String st; 
		while ((st = br.readLine()) != null) {
			executor.execute(extractPublicationIDs(st, ++count));
		} 
		br.close();
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		System.out.println("Total number of publications: "+set_publications.size());
		System.out.println("Total number of publications map: "+map_publication_abstract.size());
		System.out.println("Size of map_abstract_publication: "+map_abstract_publication.size());
	}

	private static Runnable extractPublicationIDs(String line, int i) {
		return () -> {
			if (line.contains("<http://purl.org/dc/terms/abstract>")) {
				String[] split = line.split("<http://purl.org/dc/terms/abstract>");

				if (split[0].contains("/publication/EP/") && split[1].contains("@en")) {
					try {
						String publicationID_type=getPublicationID(split[0]);
						if (publicationID_type!=null) {
							set_publications.add(publicationID_type);
							map_publication_abstract.put(publicationID_type, split[1].trim());
							map_abstract_publication.put(split[1].trim(),publicationID_type);
						}
					} catch (Exception e) {
						System.out.println(line);
						System.out.println(split[0]);
						System.exit(1);
					}
				}
			}
		};
	}
	private static String getPublicationID(String str) {
		int index_begin=str.indexOf("/publication/EP/");
		int index_end=str.indexOf("/-");
		if (index_end>0 && str.contains("/publication/EP/")) {
			return str.substring(index_begin,index_end).replace("/publication/EP/", "");
		}
		else {
			//			System.out.println("********************************");
			//			System.out.println(str);
			return null;
		}
	}
	private static String getApplicationID(String str) {
		try {
			int index_end_app=str.indexOf(">");
			return str.substring(str.indexOf("id/application/EP/"),index_end_app).replace("id/application/EP/","");
		} catch (Exception e) {

			System.out.println("******************");
			System.out.println(str);
			System.exit(1);
			return null;
		}

	}
	private static Map<String, String> readExtractedData(String fName) throws IOException{
		Map<String, String> map = new HashMap<String, String>();
		BufferedReader br = new BufferedReader(new FileReader(fName)); 
		String st; 
		while ((st = br.readLine()) != null) {
			map.put(st.split("\t")[0], st.split("\t")[1]);
		} 
		br.close();
		return map;
	}
	private static String getCPC(String str) {
		int index_end_app=str.indexOf("> .");
		return str.substring(str.indexOf("/cpc/"),index_end_app).replace("/cpc/","");
	}
}
