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

public class DatasetGeneration_directedToUndirected {

	private final String DIRECTED_GRAPH_FILE = Config.getString("DIRECTED_GRAPH_FILE","");

	public static void main(String[] args) {
		//data.initializeVariables();
		//		data.generateDatasetEntityEntiy_parallel();
	}
	private void convertDirectedToUndirected() {
		final File[] listOfFolders = new File(DIRECTED_GRAPH_FILE).listFiles();
		Arrays.sort(listOfFolders);
		final List<File> files = new ArrayList<>();
		int i=0;
		for(File f:listOfFolders) {
			try(BufferedReader br = new BufferedReader(new FileReader(f))){
				String line=null;
				while ((line = br.readLine()) != null) {
					String[] split = line.split("\t\t");
					String[] entities = split[0].split("\t");
					String weight = split[1];
					String entity
					

				}
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}

	}
}
