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
		default: 
			System.out.println("Invalid Dataset Type");
			return null;
		}
	}
	private static List<String> getCategories_Web() {
		final List<String> dummySeeds = Arrays.asList("Business","Computers","Culture","Arts","Entertainment","Education",
				"Science","Engineering","Health","Politics","Society","Sports");
		return Collections.unmodifiableList(dummySeeds);
	}
	private static List<String> getCategories_Ag() {
		final List<String> dummySeeds = Arrays.asList("World","Politics and sports","Business","Science","Technology");
		return Collections.unmodifiableList(dummySeeds);
	}
}
