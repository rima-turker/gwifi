package org.fiz.ise.gwifi.dataset.shorttext.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;

public class TEST {

	public static void main(String[] args) throws Exception {
		Wikipedia wikipedia = WikipediaSingleton.getInstance().wikipedia;
//		Map <Category, Integer> truePositive = new HashMap<>();
//		Map <Category, Integer> falsePositive = new HashMap<>();
//		Map <Category, Integer> numberOfElements = new HashMap<>();
//		truePositive.put(wikipedia.getCategoryByTitle("Business"), 12);
//		truePositive.put(wikipedia.getCategoryByTitle("Computers"), 50);
//		falsePositive.put(wikipedia.getCategoryByTitle("Business"), 9);
//		falsePositive.put(wikipedia.getCategoryByTitle("Computers"), 23);
//		numberOfElements.put(wikipedia.getCategoryByTitle("Business"), 15);
//		numberOfElements.put(wikipedia.getCategoryByTitle("Computers"), 59);
//		CalculateClassificationMetrics metrics = new CalculateClassificationMetrics();
//		metrics.evaluateResults( truePositive,falsePositive,numberOfElements);
		
		NLPAnnotationService service = AnnotationSingleton.getInstance().service;
		List<Annotation> lstAnnotations = new ArrayList<>();
		String shortText ="Albert Einstein was born in Germany";
		service.annotate(shortText, lstAnnotations);
		
		for(Annotation a:lstAnnotations) {
			System.out.println("e_"+a.getURL().replace("http://en.wikipedia.org/wiki/", "").toLowerCase());
		}
		//System.out.println((LINE_modelSingleton.getInstance().lineModel.similarity(String.valueOf(a.getId()), String.valueOf(aFilteredContext.getId())));
		
				
	}

}
