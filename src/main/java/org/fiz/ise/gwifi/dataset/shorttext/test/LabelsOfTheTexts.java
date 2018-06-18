package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.util.ArrayList;
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
		mapLabel.put(3, wikipedia.getCategoryByTitle("Trade"));
		mapLabel.put(4, wikipedia.getCategoryByTitle("Science"));
		mapLabel.put(5, wikipedia.getCategoryByTitle("Technology"));
		return mapLabel;
	}
	
	public static Map<String, List<Category>> getLables_20News()
	{
		Map<String, List<Category>> mapLabel = new HashMap<>();
		List<Category> arrList = new ArrayList<Category>();
//		arrList.add(wikipedia.getCategoryByTitle("Federal Bureau of Investigation"));
		arrList.add(wikipedia.getCategoryByTitle("Weapons"));
		mapLabel.put("talk.politics.guns", arrList);
		
		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Middle East"));
		mapLabel.put("talk.politics.mideast", arrList);
		
		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Politics"));
		mapLabel.put("talk.politics.misc", arrList);
		
		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Atheism"));
		mapLabel.put("alt.atheism", arrList);
		
		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Christianity"));
		arrList.add(wikipedia.getCategoryByTitle("Christians"));
		mapLabel.put("soc.religion.christian", arrList);
		
		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Religion"));
		mapLabel.put("talk.religion.misc", arrList);
		
		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("IBM"));
		arrList.add(wikipedia.getCategoryByTitle("Computer hardware"));
		mapLabel.put("comp.sys.ibm.pc.hardware", arrList);
		
		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Macintosh computers"));
		arrList.add(wikipedia.getCategoryByTitle("Apple Inc."));
		arrList.add(wikipedia.getCategoryByTitle("Apple Inc. hardware"));
		mapLabel.put("comp.sys.mac.hardware", arrList);
		
		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Computer graphics"));
		mapLabel.put("comp.graphics", arrList);
		
		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Windows software"));
		arrList.add(wikipedia.getCategoryByTitle("X Window System"));
		mapLabel.put("comp.windows.x", arrList);
		
		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Operating systems"));
		mapLabel.put("comp.os.ms-windows.misc", arrList);
		
		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Cars"));
		mapLabel.put("rec.autos", arrList);
		
		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Motorcycles"));
		mapLabel.put("rec.motorcycles", arrList);
		
		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Baseball"));
		mapLabel.put("rec.sport.baseball", arrList);
		
		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Hockey"));
		mapLabel.put("rec.sport.hockey", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Electronics"));
		mapLabel.put("sci.electronics", arrList);
		
		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Cryptography"));
		mapLabel.put("sci.crypt", arrList);

		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Medicine"));
		mapLabel.put("sci.med", arrList);
		
		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Space"));
		mapLabel.put("sci.space", arrList);
		
		arrList = new ArrayList<Category>();
		arrList.add(wikipedia.getCategoryByTitle("Sales"));
		mapLabel.put("misc.forsale", arrList);
		return mapLabel;
//		Israel, Arab, Jews, Muslims
//		Human_sexuality, LGBT, Male homosexuality
//		Atheist, Category:Christianity,Atheism, God ,Islam 
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
