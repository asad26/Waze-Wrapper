/*
 * Created by Asad Javed on 28/08/2017
 * Aalto University project
 * 
 * Last modified 06/09/2017
 */

package api.wrapper.WrapperOmi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WazeData {

	private static ApiForOmi obj;
	private static int count;
	public static String omiURL;

	/* Constructor for initializing variables */
	public WazeData(Properties prop) {
		obj = new ApiForOmi();
		count = 0;
		omiURL = prop.getProperty("omi_node");
	}


	/* Get items from JSON array, create object, and send to the sandbox one at a time */
	public static void createOmi(JSONObject jObject) throws JSONException {
		String infoItems = "";
		String sidId = null;
		String topCityId = null;
		String streetId = null;
		Iterator<?> iterator = jObject.keys();
		while (iterator.hasNext()) {

			String key = (String) iterator.next();
			if (key.equals("sid")) {
				if (!(jObject.get(key) instanceof JSONObject)) {
					sidId = jObject.getString(key).replaceAll("\\W", " ");
				}
			}
			else if (key.equals("city")) {
				topCityId = jObject.getString(key).replaceAll("\\W", " ");
			}
			else if (key.equals("street")) {
				streetId = jObject.getString(key).replaceAll("\\W", " ");
			}
			else {
				if ((!(key.equals("waze_alert")) || !(key.equals("waze_jam"))) && !(key.equals("title")) && !(key.equals("current")) && !(key.equals("created")) && !(key.equals("country"))) {
					infoItems = infoItems + obj.createInfoItem(key, jObject.get(key).toString());
				}
			}
		}

		if (topCityId == null || topCityId.isEmpty()) {
			topCityId = "No-City";
		}

		if (streetId == null || streetId.isEmpty()) {
			streetId = "No-Street";
		}

		if (sidId != null) {
			String sidObject = obj.createOdfObject(sidId, infoItems);
			String streetObject = obj.createOdfObject(streetId, sidObject);
			String cityObject = obj.createOdfObject(topCityId, streetObject);
			count ++;
			String finalMessage = obj.createWriteMessage(obj.createOdfObjects(cityObject));
			//System.out.println(finalMessage);
			System.out.println(count);
			sendData(omiURL, finalMessage);
		}
	}


	/* Get json array index one at a time and send to createOmi function */
	public void parseArray(Object object) throws JSONException {
		JSONArray jArray = (JSONArray) object;
		for (int i=0; i<jArray.length(); i++) {
			if (jArray.get(i) instanceof JSONObject) {
				createOmi((JSONObject) jArray.get(i));
			}
		}
	}

	/* Method call to create access token for authorization */
	public String getAccessToken(String key, String secret) {

		String data = "grant_type=client_credentials&client_id=" + key + "&client_secret=" + secret;
		HttpURLConnection httpcon = null;
		String at = null;
		BufferedReader br = null;
		OutputStream os = null;
		try {
			httpcon = (HttpURLConnection) ((new URL("https://api.irisnetlab.be:443/api/token").openConnection()));
			httpcon.setDoOutput(true);
			httpcon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			httpcon.setRequestMethod("POST");
			httpcon.connect();
			byte[] outputBytes = data.getBytes("UTF-8");
			os = httpcon.getOutputStream();
			os.write(outputBytes);
			System.out.println(httpcon.getResponseMessage());

			br = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
			StringBuffer response = new StringBuffer();

			String inputLine;
			while ((inputLine = br.readLine()) != null) {
				response.append(inputLine);
			}

			JSONObject jObject = new JSONObject(response.toString());
			at = jObject.get("access_token").toString();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {}
			}

			if (httpcon != null) {
				httpcon.disconnect();
			}

			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {}
			}
		}

		return at;
	}

	/* Method call to return data in JSON */
	public String getJsonData(String url, String token) {

		HttpURLConnection httpcon = null;
		StringBuffer response = null;
		BufferedReader br = null;
		try {
			httpcon = (HttpURLConnection) ((new URL(url).openConnection()));
			httpcon.setDoOutput(false);
			String authorization="Bearer " + token;
			httpcon.setRequestProperty("Authorization", authorization);
			httpcon.setRequestProperty("Accept", "application/json");
			httpcon.setRequestMethod("GET");

			//System.out.println(httpcon.getResponseMessage());
			br = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
			response = new StringBuffer();
			String inputLine;
			while ((inputLine = br.readLine()) != null) {
				response.append(inputLine);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {}
			}

			if (httpcon != null) {
				httpcon.disconnect();
			}
		}

		return response.toString();
	}


	/* Method call to send Omi write envelope to the sandbox */
	private static void sendData(String url, String finalMessage) {
		HttpURLConnection httpcon = null;
		OutputStream os =  null;
		try {
			httpcon = (HttpURLConnection) ((new URL(url).openConnection()));
			httpcon.setDoOutput(true);
			httpcon.setRequestProperty("Content-Type", "text/xml");
			httpcon.setRequestMethod("POST");
			//httpcon.connect();
			byte[] outputBytes = finalMessage.getBytes("UTF-8");
			os = httpcon.getOutputStream();
			os.write(outputBytes);
			System.out.println(httpcon.getResponseMessage());
			Thread.sleep(10);			

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {}
			}

			if (httpcon != null) {
				httpcon.disconnect();
			}
		}
	}
}
