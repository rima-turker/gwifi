package org.fiz.ise.gwifi.dataset.LINE.Category;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.fiz.ise.gwifi.model.TestDatasetType_Enum;

public class Categories {

	public static List<String> getCategoryList(TestDatasetType_Enum t)
	{
		switch (t) {
		case AG:  return getCategories_Ag();
		case WEB_SNIPPETS:  return getCategories_Web();
		case DBLP: return getCategories_DBLP();
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
		final List<String> dummySeeds = Arrays.asList("Business","Computer hardware","Culture","Arts","Entertainment","Hypotheses",
				"Science","Engineering","Health","Politics","Society","Sports");		
		return Collections.unmodifiableList(dummySeeds);
	}
	private static List<String> getCategories_Ag() {
	//	final List<String> dummySeeds = Arrays.asList("World","Sports","Business","Science and technology");
	//	final List<String> dummySeeds = Arrays.asList("World","Sports","Business","Science","Technology");
			final List<String> dummySeeds = Arrays.asList("Sports","Science","Technology","World","Trade");
		//final List<String> dummySeeds = Arrays.asList("Sports","Business","Science","Technology");
		return Collections.unmodifiableList(dummySeeds);
	}
	private static List<String> getCategories_DBLP() {
			final List<String> dummySeeds = Arrays.asList("Databases","Artificial intelligence","Computer hardware",
					"Systems Network Architecture","Programming languages","Theory of computation","Theoretical computer science");
			return Collections.unmodifiableList(dummySeeds);
		}

}
