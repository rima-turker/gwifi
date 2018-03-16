package org.fiz.ise.gwifi.dataset.LINE.CategoryBased;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.fiz.ise.gwifi.Singleton.CategorySingleton;
import org.fiz.ise.gwifi.Singleton.FilteredWikipediaPagesSingleton;
import org.fiz.ise.gwifi.Singleton.WikipediaSingleton;
import org.fiz.ise.gwifi.categoryTree.CategorySeedLoaderFromMemory;
import org.fiz.ise.gwifi.categoryTree.CategorySeedloader;
import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;
import org.fiz.ise.gwifi.util.Global;
import org.fiz.ise.gwifi.util.TimeUtil;
import org.xml.sax.SAXException;

import com.sleepycat.je.EnvironmentLockedException;

import edu.kit.aifb.gwifi.comparison.ArticleComparer;
import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Page.PageType;
import edu.kit.aifb.gwifi.util.PageIterator;
import edu.kit.aifb.gwifi.yxu.textcategorization.CategoryTree;

@Deprecated
public class FilterWikipediaPages {

	private Wikipedia wikipedia=null;
	PageIterator pageIterator = null;
	private static String CATEGORY_TREE_FOLDER = Config.getString("CATEGORY_TREE_FOLDER", "");
	private final Integer DEPTH_OF_CAT_TREE= Config.getInt("DEPTH_OF_CAT_TREE", 0);

	public FilterWikipediaPages() {
//		  WikipediaSingleton singleton = WikipediaSingleton.getInstance();
//		  wikipedia = singleton.wikipedia;
		FilteredWikipediaPagesSingleton singleton = FilteredWikipediaPagesSingleton.getInstance();
		
	}
	public Set<String>  getCategoryTress() {
		Set<String> setCategories = new HashSet<>();
		final File[] listOfFiles = new File(CATEGORY_TREE_FOLDER).listFiles();
		Arrays.sort(listOfFiles);
		for(File f:listOfFiles) {
			try(BufferedReader br = new BufferedReader(new FileReader(f)))
			{
				String line=null;
				while ((line = br.readLine()) != null) 
				{
					String category = line.substring(0, line.indexOf("\t")).trim();
					System.out.println(category);
					setCategories.add(category);
				} 

			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		System.out.println("size of the all the categories "+setCategories.size()+" depth "+DEPTH_OF_CAT_TREE);
		return setCategories;
	}
	public void generateCategoryTreeOnTheFly()
	{
		final CategorySeedloader seedLoader = new CategorySeedLoaderFromMemory();
		seedLoader.loadSeeds();
		List<String> categories = new ArrayList<>(seedLoader.getSeeds());
		Category[] cats = new Category[categories.size()];
		int[] catIDs = new int[categories.size()];
		int id=0;
		for (String category: categories) {
			cats[id] = wikipedia.getCategoryByTitle(category);
			catIDs[id]=wikipedia.getCategoryByTitle(category).getId();
			id++;
			//System.out.println(wikipedia.getCategoryByTitle(category));
		}
		for (int i = 0; i < cats.length; i++) {
			List<Category> lstParentCats = new ArrayList<>(Arrays.asList(cats[i].getChildCategories()));
			for(Category category: lstParentCats ) {
				System.out.println(category);
			}
			System.out.println(lstParentCats.size());
			System.out.println(new ArrayList<>(Arrays.asList(cats[i].getChildCategories())));
		}

	}
	public Set getFilteredWikipagesArticle(Set<Category> categories) {
		Set<Article> set = new HashSet<>();
		try {
			final long now = System.currentTimeMillis();
			final File databaseDirectory = new File("configs/wikipedia-template-en.xml");
			Wikipedia wikipedia = new Wikipedia(databaseDirectory, false);
			System.out.println("The Wikipedia environment has been initialized.");
			PageIterator pageIterator = wikipedia.getPageIterator();
			List<Article> localList = new ArrayList<>();
			int i = 1;
			while (pageIterator.hasNext()) {
				Page page = pageIterator.next();
				if (page.getType().equals(PageType.article)) {
					Article article = getArticle(page.getTitle());

				}
				//System.out.println("We have " + countCategoryPerArticle.value() + " categories per article");
				//			System.out.println("Total time minutes " + TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - now));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}


		return categories;

	}

	public  Article getArticle(String title) {
		if (wikipedia.getArticleByTitle(title) == null) {
			System.out.println("Could not find exact match of an article for a given title " + title);
			return null;
		}
		return wikipedia.getArticleByTitle(title);
	}
}
