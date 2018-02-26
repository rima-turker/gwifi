package org.fiz.ise.gwifi.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

public class WriteToAFile 
{
	public void writeAnchorsToFile(List<String> lst, String fileName)
	{
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(fileName), "utf-8"))) {
				for (String str: lst) {
					writer.write(str);
					writer.write("\n");	
				}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
