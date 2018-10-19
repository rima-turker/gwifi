package org.fiz.ise.gwifi.original.dataless;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fiz.ise.gwifi.dataset.LINE.Category.Categories;
import org.fiz.ise.gwifi.model.TestDatasetType_Enum;
import org.fiz.ise.gwifi.util.Config;


public class PrepareDatasetForDataless {
	private final static TestDatasetType_Enum TEST_DATASET_TYPE= Config.getEnum("TEST_DATASET_TYPE");
	static final Logger secondLOG = Logger.getLogger("debugLogger");
	static final Logger thirdLOG = Logger.getLogger("resultLogger");
	public static void main(String[] args) {
		try {
			List<String> lines = FileUtils.readLines(new File(Config.getString("DATASET_TEST_WEB","")), "utf-8");
			List<String> lstCat = new ArrayList<>(Categories.getCategoryList(TEST_DATASET_TYPE));
			int count =0;
			for(String s : lines) {
				secondLOG.info(count++ +"\t"+s);
			}
			for(String c : lstCat) {
				thirdLOG.info(c);
			}
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
