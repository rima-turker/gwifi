package org.fiz.ise.gwifi.test.afterESWC;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Test2 {

	public static void main(String[] args) {
		try (BufferedReader br = Files.newBufferedReader(Paths.get("sample"))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] split = line.split("\t")[1].split(",");
				
				List<Integer> lst=  Arrays.asList(2825899, 55458, 731976, 2892840, 1644925, 2288615, 2990475, 78694, 3088902, 2815712, 957317, 3024609, 2003185, 2647259, 2429366, 2847837, 1060999, 1510351, 1299337, 1887143, 724567, 1775239, 1262318);
				for (int i :lst) {
					System.out.println(split[i]);
				}
				System.out.println();
				for (int i = 0; i < split.length; i++) {
					if (split[i].equals("1")) {
						System.out.println(i);
					}
				}
				
			}

		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}

	}

}
