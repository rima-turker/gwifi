package org.fiz.ise.gwifi.dataset.category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.fiz.ise.gwifi.dataset.LabelsOfTheTexts;
import org.fiz.ise.gwifi.dataset.shorttext.test.TestBasedOnAnnotatedDocument;
import org.fiz.ise.gwifi.longText.TestBasedonLongTextDatasets;
import org.fiz.ise.gwifi.model.Dataset;

import edu.kit.aifb.gwifi.model.Category;

public class Categories {

	public static List<String> getCategoryList(Dataset t)
	{
		switch (t) {
		case AG:  return getCategories_Ag();
		case WEB_SNIPPETS:  return getCategories_Web();
		case YAHOO:  return getCategories_Yahoo();
		case DBLP: return getCategories_DBLP();
		case TWENTYNEWS: return getCategories_20News();
		case YOVISTO: return getCategories_YOVISTO();
		case YOVISTO_SENTENCEBYSENTENCE_sentence: return getCategories_YOVISTOSENTENCES_sentence();
		case YOVISTO_SENTENCEBYSENTENCE_entities: return getCategories_YOVISTOSENTENCES_entities();
		default: 
			System.out.println("Invalid Dataset Type");
			return null;
		}
	}
	private static List<String> getCategories_Web() {
//		final List<String> dummySeeds = Arrays.asList("Business","Computers","Culture","Arts","Entertainment","Education",
//				"Science","Engineering","Health","Politics","Society","Sports");
//		final List<String> dummySeeds = Arrays.asList("Business","Computer hardware","Computer networking","Culture","Arts","Entertainment","Education",
//				"Science","Engineering","Health","Politics","Society","Sports");
//		final List<String> dummySeeds = Arrays.asList("Business","Computer hardware","Culture","Arts","Entertainment","Hypotheses",
//				"Science","Engineering","Health","Politics","Society","Sports");		
//		final List<String> dummySeeds = Arrays.asList("Business","Computer hardware","Culture","Arts","Entertainment","Education",
//				"Science","Engineering","Health","Politics","Society","Sports");
		
//		final List<String> dummySeeds = Arrays.asList("Business","Software","Music",
//				"Science","Automotive industry","Health","Politics","Sports");
		
		final List<String> dummySeeds = Arrays.asList("Business","Computers","Culture","Arts","Entertainment","Education",
				"Science","Engineering","Health","Politics","Society","Sports");
		return Collections.unmodifiableList(dummySeeds);
	}
	private static List<String> getCategories_YOVISTO() {
		List<String> categoryList = new ArrayList<>(TestBasedonLongTextDatasets.getLstCategory());
		return Collections.unmodifiableList(categoryList);
	}
	private static List<String> getCategories_YOVISTOSENTENCES_sentence() {
		List<String> categoryList = new ArrayList<>(TestBasedonLongTextDatasets.getLstCategory());
		return Collections.unmodifiableList(categoryList);
	}
	private static List<String> getCategories_YOVISTOSENTENCES_entities() {
		List<String> categoryList = new ArrayList<>(TestBasedOnAnnotatedDocument.getLstCategory());
		return Collections.unmodifiableList(categoryList);
	}
	private static List<String> getCategories_20News() {
		Map<String, List<Category>> categories = new HashMap<>(LabelsOfTheTexts.getLables_20News());
		List<String> dummySeeds = new ArrayList<>();
		categories.forEach((key, articles) -> {
          articles.forEach(a -> {
        	  dummySeeds.add(a.getTitle());
          
          });
		});
	return dummySeeds;
	}
	
	private static List<String> getCategories_Yahoo() {
		final List<String> dummySeeds = Arrays.asList("Society","Culture","Science","Mathematics","Health","Education",
				"Reference","Computers","Internet","Sports","Trade","Finance","Entertainment","Music","Family","Intimate relationships","Politics","Government");		
		return Collections.unmodifiableList(dummySeeds);
	}
	public static List<Category> getCategories_AgCats() {
		Map<Integer, Category> map = new HashMap<>(LabelsOfTheTexts.getLables_AG_category());
		final List<Category> dummySeeds = new ArrayList<>();
		for(Entry <Integer, Category> e: map.entrySet()) {
			dummySeeds.add(e.getValue());
		}
	//	final List<String> dummySeeds = Arrays.asList("World","Sports","Business","Science","Technology");
//			final List<String> dummySeeds = Arrays.asList("Sports","Science","Technology","World","Trade");//0.80
	
		
		//		final List<String> dummySeeds = Arrays.asList("Sports","Science","Technology","World","Business");
		//	final List<String> dummySeeds = Arrays.asList("World","Business");
	//	final List<String> dummySeeds = Arrays.asList("Business","World");
		return Collections.unmodifiableList(dummySeeds);
	}
	public static List<String> getCategories_Ag() {
		Map<Integer, Category> map = new HashMap<>(LabelsOfTheTexts.getLables_AG_category());
		final List<String> dummySeeds = new ArrayList<>();
		for(Entry <Integer, Category> e: map.entrySet()) {
			dummySeeds.add(e.getValue().getTitle());
		}
	//	final List<String> dummySeeds = Arrays.asList("World","Sports","Business","Science","Technology");
//			final List<String> dummySeeds = Arrays.asList("Sports","Science","Technology","World","Trade");//0.80
	
		
		//		final List<String> dummySeeds = Arrays.asList("Sports","Science","Technology","World","Business");
		//	final List<String> dummySeeds = Arrays.asList("World","Business");
	//	final List<String> dummySeeds = Arrays.asList("Business","World");
		return Collections.unmodifiableList(dummySeeds);
	}
	private static List<String> getCategories_DBLP() {
			final List<String> dummySeeds = Arrays.asList("Databases","Artificial intelligence","Computer hardware",
					"Systems Network Architecture","Programming languages","Theory of computation","Theoretical computer science");
			return Collections.unmodifiableList(dummySeeds);
		}
	
	

}
