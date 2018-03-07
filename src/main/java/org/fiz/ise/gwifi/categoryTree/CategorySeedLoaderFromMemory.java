package org.fiz.ise.gwifi.categoryTree;

import java.util.Arrays;
import java.util.List;

public class CategorySeedLoaderFromMemory extends CategorySeedloader {

	public CategorySeedLoaderFromMemory() {

	}

	@Override
	public void loadSeeds() {
//		final List<String> dummySeeds = Arrays.asList("Chief_executive_officers", "Heads_of_state",
//				"Chancellors_(government)", "Popes", "Monarchy");
		
		final List<String> dummySeeds = Arrays.asList("Business","Computers","Culture","Arts","Entertainment","Education",
				"Science","Engineering","Health","Politics","Society","Sports");
		//Google search snippets
		/*Business
	    Computers
	    Culture-Arts-Entertainment
	    Education-Science
	    Engineering
	    Health
	    Politics-Society
	    Sports*/
//		final List<String> dummySeeds = Arrays.asList("Monarchy");
		for (String s : dummySeeds) {
			getSeeds().add(s);
		}
	}
}
