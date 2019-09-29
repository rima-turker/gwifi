package org.fiz.ise.gwifi.dataset.train.generation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.Singleton.AnnotationSingleton;
import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.GoogleModelSingleton;
import org.fiz.ise.gwifi.Singleton.LINE_modelSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.ReadDataset;
import org.fiz.ise.gwifi.dataset.category.Categories;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.test.afterESWC.GenerateWideFeatureSet;
import org.fiz.ise.gwifi.test.afterESWC.TestBasedonSortTextDatasets;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.SynchronizedCounter;
import org.fiz.ise.gwifi.util.TimeUtil;

import edu.kit.aifb.gwifi.annotation.Annotation;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Page.PageType;

public class PrepareDateForPython {
	private static final String DATASET_TRAIN_AG = Config.getString("DATASET_TRAIN_AG","");
	private final static Integer NUMBER_OF_THREADS= Config.getInt("NUMBER_OF_THREADS",-1);
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger resultLog = Logger.getLogger("reportsLogger");
	private static Map<String, String> mapRedirectPages; //= new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);//new HashMap<>();
	private static SynchronizedCounter totalRedirect;
	private static SynchronizedCounter resolvedRedirect;
	private static SynchronizedCounter couldNotResolved;

	private static ExecutorService executor;
	public static void main(String[] args) throws Exception {
		long now = TimeUtil.getStart();
		totalRedirect=new SynchronizedCounter();
		resolvedRedirect=new SynchronizedCounter();
		couldNotResolved=new SynchronizedCounter();

		mapRedirectPages= new HashMap<>(AnalysisEmbeddingandRedirectDataset.loadRedirectPages());
		AnnotationSingleton.getInstance();
		Map<String, List<Article>> dataset = ReadDataset.read_dataset_AG_LabelArticle(AG_DataType.TITLEANDDESCRIPTION,Config.getString("DATASET_TRAIN_AG",""));

		PrepareDateForPython generate = new PrepareDateForPython();
		generate.extractEntities(dataset);
		System.out.println("Total time minutes :"+ TimeUnit.SECONDS.toMinutes(TimeUtil.getEnd(TimeUnit.SECONDS, now)));

	}
	private void extractEntities(Map<String, List<Article>> dataset ) throws Exception {
		int count =0;
		//String str = "Is Google Page Rank Still Important? Is Google Page Rank Still Important?\\\\Since 1998 when Sergey Brin and Larry Page developed the Google search engine, it has relied (and continues to rely) on the Page Rank Algorithm. Googles reasoning behind this is, the higher the number of inbould links pointing to a website, the more valuable that ...\n";
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);		
		for(Entry<String, List<Article>> e : dataset.entrySet()) {
			//		List<Article> m = new ArrayList<Article>();
			//		m.add(WikipediaSingleton.getInstance().wikipedia.getArticleByTitle("Business"));
			//			executor.execute(handleExtractEntities(str, m,count++));
			executor.execute(handleExtractEntities(e.getKey(), e.getValue(),count++));
		}
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
	}

	private Runnable handleExtractEntities(String str,List<Article> gt,int count)  {
		return () -> {
			List<Annotation> lstAnnotations = new ArrayList<>();
			try {
				AnnotationSingleton.getInstance().service.annotate(str, lstAnnotations);
				StringBuilder strBuild = new StringBuilder(str+"\t\t"+gt.get(0).getTitle()+"\t\t");

				for(Annotation a : lstAnnotations) {

					if (!AnnonatationUtil.getEntityBlackList_AGNews().contains(a.getId())){
						if (WikipediaSingleton.getInstance().wikipedia.getArticleById(a.getId())==null) {
							Page p = new Page(WikipediaSingleton.getInstance().wikipedia.getEnvironment(), a.getId());
							if (p.getType().equals(Page.PageType.redirect)) {
								totalRedirect.increment();
								String key = a.getURL().replace("http://en.wikipedia.org/wiki/", "");
								if(mapRedirectPages.containsKey(key)) {
									String tName = mapRedirectPages.get(key).replace("_", " ");
									Article article = WikipediaSingleton.getInstance().wikipedia.getArticleByTitle(tName);
									if (article!=null) {
										strBuild.append(article.getTitle()+"\t");
										resolvedRedirect.increment();
									}
									else {
										couldNotResolved.increment();
										secondLOG.info(key);
									}
								}
							}
						}
						else {
							strBuild.append(a.getTitle()+"\t");
						}
					}
				}
				resultLog.info(strBuild.toString().subSequence(0, strBuild.toString().length()-1));
				System.out.println(count+" files are processed. totalRedirect: "+totalRedirect.value()+" resolvedRedirect: "+resolvedRedirect.value()
				+" couldNotResolved: "+couldNotResolved.value());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			}//annotate the given text


		};
	}
}
