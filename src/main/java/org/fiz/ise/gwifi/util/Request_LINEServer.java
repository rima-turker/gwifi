package org.fiz.ise.gwifi.util;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.fiz.ise.gwifi.model.Model_LINE;

public class Request_LINEServer {

	private static final String BASE_URL = Config.getString("SERVER_BASE_URL", "");
	

	public static Double getSimilarity(String entity1, String entity2,Model_LINE model) {
		try{
			String url = BASE_URL+"similarity?"+"entity1="+entity1+"&entity2="+entity2+"&model="+model.toString();
			final HttpGet request = new HttpGet(url);
			final CloseableHttpClient client =  HttpClients.createDefault();
			final HttpResponse response = client.execute(request);
			final BufferedReader rd = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));

			final StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			client.close();
			return Double.parseDouble(result.toString());
		}catch(Exception e){
			return null;
		}
	}

	public static double[] getWordVector(String word){
		try{
			String url = BASE_URL+"wordvector?"+"entity1="+word;
			final HttpGet request = new HttpGet(url);
			final CloseableHttpClient client =  HttpClients.createDefault();
			final HttpResponse response = client.execute(request);
			final BufferedReader rd = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));
			String line = rd.readLine();
			line= line.replace("[", "").replace("]", "");
			final String[] split = line.split(",");
			final double[] result = new double[split.length];
			for(int i=0;i<split.length;i++){
				result[i] = Double.parseDouble(split[i].trim());
			}
			client.close();
			return result;
		}catch(Exception e){  
			return null;
		}

	}
}