package org.fiz.ise.gwifi.Singleton;

import java.util.concurrent.TimeUnit;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.TimeUtil;

public class LINE_modelSingleton {
    private static LINE_modelSingleton single_instance = null;
    public Word2Vec line_Combined;
    private static final String ADDRESS_OF_LINE_MODEL = Config.getString("ADDRESS_OF_LINE_MODEL","");
    private LINE_modelSingleton()
    {
    	try {
    		long now = TimeUtil.getStart();
    		line_Combined=WordVectorSerializer.readWord2VecModel(ADDRESS_OF_LINE_MODEL);
    		System.out.println("Time took to load model minutes :"+ TimeUnit.SECONDS.toMinutes(TimeUtil.getEnd(TimeUnit.SECONDS, now)));
		} catch (Exception e) {
			System.out.println("Exception initializing LINE_modelSingleton");
		}
    }
    public static LINE_modelSingleton getInstance()
    {
        if (single_instance == null)
        	single_instance = new LINE_modelSingleton();
 
        return single_instance;
    }
}
