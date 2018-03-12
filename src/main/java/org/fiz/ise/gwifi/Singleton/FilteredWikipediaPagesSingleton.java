package org.fiz.ise.gwifi.Singleton;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;


import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Page.PageType;

public class FilteredWikipediaPagesSingleton {
	private static FilteredWikipediaPagesSingleton single_instance = null;
	public Set<Article> articles;
	private FilteredWikipediaPagesSingleton()
	{
		articles = new HashSet<>();
		CategorySingleton categories = CategorySingleton.getInstance();
		Set<Category> setMainCat = new HashSet<>(categories.setAll);
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
