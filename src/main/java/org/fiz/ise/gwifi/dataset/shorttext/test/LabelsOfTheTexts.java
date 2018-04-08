package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;

import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;

public class LabelsOfTheTexts {
	private static Wikipedia wikipedia = WikipediaSingleton.getInstance().wikipedia;
	
	public static Map<Integer, Category> getLables_AG()
	{
		Map<Integer, Category> mapLabel = new HashMap<>();
		mapLabel.put(1, wikipedia.getCategoryByTitle("World"));
		mapLabel.put(2, wikipedia.getCategoryByTitle("Sports"));
		mapLabel.put(3, wikipedia.getCategoryByTitle("Business"));
//		mapLabel.put(4, wikipedia.getCategoryByTitle("Science and technology"));
		mapLabel.put(4, wikipedia.getCategoryByTitle("Science"));
		mapLabel.put(5, wikipedia.getCategoryByTitle("Technology"));
		
		return mapLabel;
	}

}
