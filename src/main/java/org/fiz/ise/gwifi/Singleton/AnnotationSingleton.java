package org.fiz.ise.gwifi.Singleton;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.annotation.preprocessing.HtmlPreprocessor;
import edu.kit.aifb.gwifi.annotation.preprocessing.WikiPreprocessor;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;
import edu.kit.aifb.gwifi.service.Service.DisambiguationModel;
import edu.kit.aifb.gwifi.service.Service.KB;
import edu.kit.aifb.gwifi.service.Service.LinkFormat;
import edu.kit.aifb.gwifi.service.Service.MentionMode;
import edu.kit.aifb.gwifi.service.Service.NLPModel;
import edu.kit.aifb.gwifi.service.Service.RepeatMode;
import edu.kit.aifb.gwifi.service.Service.ResponseFormat;
import edu.kit.aifb.gwifi.service.Service.ResponseMode;
import edu.kit.aifb.gwifi.service.Service.SourceMode;
import edu.kit.aifb.gwifi.util.nlp.Language;

public class AnnotationSingleton {
	 private static AnnotationSingleton single_instance = null;
	 
	    // variable of type String
	 public NLPAnnotationService service;
	    // private constructor restricted to this class itself
	    private AnnotationSingleton()
	    {
	    	try {
	    		service = new NLPAnnotationService("configs/hub-template.xml",
	    				"configs/wikipedia-template-en.xml", "configs/NLPConfig.properties", Language.EN, Language.EN,
	    				KB.WIKIPEDIA, NLPModel.NGRAM, DisambiguationModel.PRIOR, MentionMode.NON_OVERLAPPED, ResponseMode.BEST,
	    				RepeatMode.FIRST);
			} catch (Exception e) {
				System.out.println("Exception initializing Wikipedia: "+e.getMessage());
				System.exit(1);
			}
	    }
	    // static method to create instance of Singleton class
	    public static AnnotationSingleton getInstance()
	    {
	        if (single_instance == null)
	            single_instance = new AnnotationSingleton();
	 
	        return single_instance;
	    }
	    
}
