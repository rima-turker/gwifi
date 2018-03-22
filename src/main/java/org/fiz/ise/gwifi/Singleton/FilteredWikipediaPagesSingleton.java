package org.fiz.ise.gwifi.Singleton;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.util.Config;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page.PageType;

public class FilteredWikipediaPagesSingleton {
	private final static TestDatasetType_Enum TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	private static FilteredWikipediaPagesSingleton single_instance = null;
	public Set<Article> articles;
	
	private FilteredWikipediaPagesSingleton()
	{
		articles = new HashSet<>();
		CategorySingleton categories = CategorySingleton.getInstance(Categories.getCategoryList(TEST_DATASET_TYPE));
		Set<Category> setMainCat = new HashSet<>(categories.setAllCategories);
		try {
			final long now = System.currentTimeMillis();
			for (Category category : setMainCat) {
				Article[] temp = category.getChildArticles();
				for (int i = 0; i < temp.length; i++) {
					if (temp[i].getType().equals(PageType.article)) {
						articles.add(temp[i]);
					}
				}
			}
			System.out.println("We have " + articles.size()+" articles after category based filtering");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static FilteredWikipediaPagesSingleton getInstance()
	{
		if (single_instance == null)
			single_instance = new FilteredWikipediaPagesSingleton();

		return single_instance;
	}
}
