package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.util.Arrays;
import java.util.Collections;
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
//		mapLabel.put(3, wikipedia.getCategoryByTitle("Business"));
		mapLabel.put(3, wikipedia.getCategoryByTitle("Trade"));
//		mapLabel.put(4, wikipedia.getCategoryByTitle("Science"));
		mapLabel.put(4, wikipedia.getCategoryByTitle("Science"));
//		mapLabel.put(5, wikipedia.getCategoryByTitle("Technology"));
		mapLabel.put(5, wikipedia.getCategoryByTitle("Technology"));
		
		
		return mapLabel;
	}
	
	public static Map<Integer, String> getLables_Yahoo()
	{
		Map<Integer, String> mapLabel = new HashMap<>();
		mapLabel.put(1, "Society-Culture");
		mapLabel.put(2, "Science-Mathematics");
		mapLabel.put(3,"Health");
		mapLabel.put(4,"Education-Reference");
		mapLabel.put(5,"Computers-Internet");
		mapLabel.put(6,"Sports");
		mapLabel.put(7,"Business-Finance");
		mapLabel.put(8,"Entertainment-Music");
		mapLabel.put(9,"Family-Intimate relationships");
		mapLabel.put(10,"Politics-Government");
		return mapLabel;
	}
	public static Map<Integer, Category> getLables_DBLP()
	{
		Map<Integer, Category> mapLabel = new HashMap<>();
		//mapLabel.put(1, wikipedia.getCategoryByTitle);
		//mapLabel.put(2, wikipedia.getCategoryByTitle("Artificial intelligence"));
		mapLabel.put(3, wikipedia.getCategoryByTitle("Computer hardware"));
		mapLabel.put(4, wikipedia.getCategoryByTitle("Systems Network Architecture"));
		mapLabel.put(5, wikipedia.getCategoryByTitle("Programming languages"));
		mapLabel.put(6, wikipedia.getCategoryByTitle("Theory of computation"));
		mapLabel.put(7, wikipedia.getCategoryByTitle("Theoretical computer science"));
		return mapLabel;
	}

}
