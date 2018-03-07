package org.fiz.ise.gwifi.dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.fiz.ise.gwifi.util.Config;
import org.fiz.ise.gwifi.util.FileUtil;

public class DatasetGenerationLINE_directedToUndirected {

	private final String DIRECTED_GRAPH_FOLDER = Config.getString("DIRECTED_GRAPH_FOLDER","");
	
	public static void main(String[] args) {
		//data.initializeVariables();
//		data.generateDatasetEntityEntiy_parallel();
	}
	private void convertDirectedToUndirected() {
		final File[] listOfFolders = new File(DIRECTED_GRAPH_FOLDER).listFiles();
		Arrays.sort(listOfFolders);
		final List<File> files = new ArrayList<>();
		int i=0;
		for(File f:listOfFolders) {
			try(BufferedReader br = new BufferedReader(new FileReader(f)))
			{
				String line=null;
				while ((line = br.readLine()) != null) 
				{
					
					
				}
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
		
	}
}
