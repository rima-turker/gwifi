package org.fiz.ise.gwifi.dataset.train.generation;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.dataset.test.ReadTestDataset;
import org.fiz.ise.gwifi.model.AG_DataType;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.util.AnnonatationUtil;
import org.fiz.ise.gwifi.util.Config;

import edu.kit.aifb.gwifi.model.Category;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

public class GenerateDatasetForNN {
	private final static TestDatasetType_Enum TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	public static void main(String[] args) {
		List<String> lstCat = new ArrayList<>(Categories.getCategoryList(TEST_DATASET_TYPE));
		List<Category> lstDatasetCatList = new ArrayList<>();
		for(String c : lstCat) {
			lstDatasetCatList.add(WikipediaSingleton.getInstance().wikipedia.getCategoryByTitle(c));
		}
		String fileName="AG_TitleDesc_FreqWord";
		for(Category c : lstDatasetCatList ) {
		List<String> dataset = new ArrayList<>(ReadTestDataset.read_AG_BasedOnCategory(c, (AG_DataType.TITLEANDDESCRIPTION)));
		findFreqOfWords(dataset,fileName+"_"+c.getTitle());
		}
	}

	private static void findFreqOfWords(List<String> dataset,String fileName) {
		final List<CoreLabel> tokens = new ArrayList<CoreLabel>();
		final List<String> tokensStr = new ArrayList<String>();
		
		final LexedTokenFactory<CoreLabel> tokenFactory = new CoreLabelTokenFactory();
		for(String line: dataset) {
			final PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(line), tokenFactory,
					"untokenizable=noneDelete");
			while (tokenizer.hasNext()) {
				tokensStr.add(tokenizer.next().toString());
			}
		}
		AnnonatationUtil.findFreqOfWord(tokensStr, fileName);
		
	}
	
}
