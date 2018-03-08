package org.fiz.ise.gwifi.Singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fiz.ise.gwifi.categoryTree.CategorySeedLoaderFromMemory;
import org.fiz.ise.gwifi.categoryTree.CategorySeedloader;
import org.fiz.ise.gwifi.util.Config;

import edu.kit.aifb.gwifi.model.Category;
import edu.kit.aifb.gwifi.model.Wikipedia;
/*
 * The purpose of this class is to create a Category tree and a categorySet comes from all the merge of the cat
 * trees 
 *we will use the set and tree for filtering the articles sake of reducing the complexity
 *we will deal only with the pages under the categories we consider
 */
public class CategorySingleton {
	private static CategorySingleton single_instance = null;
	private final Integer DEPTH_OF_CAT_TREE = Config.getInt("DEPTH_OF_CAT_TREE", 0);
	// variable of type String
	public Map<Category, Set<Category>> map;
	public Set<Category> setAll;
	public Set<Category> set;
	
	private CategorySingleton() {
		map = new HashMap<>();
		setAll = new HashSet<>();
		set = new HashSet<>();
		
		WikipediaSingleton singleton = WikipediaSingleton.getInstance();
		Wikipedia wikipedia = singleton.wikipedia;
		System.out.println("Depth of the category Tree is "+DEPTH_OF_CAT_TREE );
		final CategorySeedloader seedLoader = new CategorySeedLoaderFromMemory();
		seedLoader.loadSeeds();
		List<String> categories = new ArrayList<>(seedLoader.getSeeds());
		Category[] mainCats = new Category[categories.size()];
		List<Category> mainCategories = new ArrayList<>();
		int id = 0;
		for (String category : categories) {
			Category cat = wikipedia.getCategoryByTitle(category);
			mainCats[id] = cat;
			mainCategories.add(cat);
			set.add(cat);
			setAll.add(cat);
			id++;
		}
		//System.out.println(setAll);
		Map<Category, Set<Category>> latest = new HashMap<>();
		for (int j = 0; j < DEPTH_OF_CAT_TREE; j++) {
			//System.out.println("J: " + j);
			if (j == 0) {
				for (int i = 0; i < mainCats.length; i++) {
					Set<Category> child = new HashSet<>(
							getChildCategoriesSet(new HashSet<>(Arrays.asList(mainCats[i]))));
					map.put(mainCats[i], child);
					setAll.addAll(child);
					latest.put(mainCats[i], child);
				}
			} else {
				for (Entry<Category, Set<Category>> entry : latest.entrySet()) {
					Set<Category> parent = new HashSet<>(entry.getValue());
					// System.out.println("parent "+entry.getKey()+" "+parent.size());
					Set<Category> child = new HashSet<>(getChildCategoriesSet(parent));
					// System.out.println("child "+child.size());
					// System.out.println("Before originalSet parent "+parent.size());
					latest.put(entry.getKey(), child);
					List<Category> lstoriginalSet = new ArrayList<>(parent);
					List<Category> lstchildSet = new ArrayList<>(child);
					lstoriginalSet.addAll(lstchildSet);
					parent.addAll(child);// with the previous elements
					// System.out.println("After originalSet parent"+parent.size());
					// System.out.println("After lstoriginalSet "+lstoriginalSet.size());
					map.put(entry.getKey(), parent);
					setAll.addAll(parent);
					//System.out.println(setAll.size());
					// System.out.println("******************************************************************************");
				}
			}
		}
	}

	private Set<Category> getChildCategoriesSet(Set<Category> setParent) {
		Set<Category> child = new HashSet<>();
		for (Category category : setParent) {
			Set<Category> temp = new HashSet<>(Arrays.asList(category.getChildCategories()));
			child.addAll(temp);
		}
		return child;
	}

	public static CategorySingleton getInstance() {
		if (single_instance == null)
			single_instance = new CategorySingleton();

		return single_instance;
	}
}
