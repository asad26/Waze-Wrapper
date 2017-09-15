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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WazeData {

	private static ApiForOmi obj;
	private static int count;
	public static String omiURL;

	private static List<String> allSegments;
	private static String odfComplete;
	public int total;

	/* Constructor for initializing variables */
	public WazeData(Properties prop) {
		obj = new ApiForOmi();
		count = 0;
		omiURL = prop.getProperty("omi_node");
		allSegments = new ArrayList<String>();
		odfComplete = "";
		total = 0;
	}


	/* Get items from JSON array, create object, and send to the sandbox one at a time */
	/*public static void createOmi(JSONObject jObject) throws JSONException {
		String infoItems = "";
		String sidId = null;
		String cityId = null;
		Iterator<?> iterator = jObject.keys();
		while (iterator.hasNext()) {

			String key = (String) iterator.next();
			if (key.equals("sid")) {
				if (!(jObject.get(key) instanceof JSONObject)) {
					sidId = jObject.getString(key);
				}
				else {
					sidId = "Outside-city";
				}
			}
			else if (key.equals("city")) {
				if (!(jObject.get(key) instanceof JSONObject)) {
					cityId = jObject.getString(key).replaceAll("\\W", " ");
				}
				else {
					cityId = "No-city";
				}

			}
			else if (key.equals("type")) {
				String newKey="Status";
				infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
			}
			else if (key.equals("street")) {
				String newKey="Street";
				infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
			}
			else if (key.equals("reportdescription")){
				String newKey="Cause";
				String value=jObject.get(key).toString();
				String newValue;
				if (!(jObject.get(key) instanceof JSONObject)) {
					if (value.equals("Travaux")) {
						newValue = "Works";
					}
					else {
						newValue = value;
					}
					infoItems = infoItems + obj.createInfoItem(newKey, newValue);
				}
			}
			else if (key.equals("speed")){
				String newKey="Measured-Speed";
				infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
			}
			else if (key.equals("reliability")){
				String newKey="Information-Reliability";
				infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
			}
			else if (key.equals("confidence")){
				String newKey="Information-Confidence";
				infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
			}
			else if (key.equals("line")){
				String newKey="Position";
				infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
			}
			else {}
		}

		if (sidId != null) {
			String sidObject = obj.createOdfObject(sidId, infoItems);
			String cityObject = obj.createOdfObject(cityId, sidObject);
			String topObject = obj.createOdfObject("Brussels-Smart-City", cityObject);
			count ++;
			String finalMessage = obj.createWriteMessage(obj.createOdfObjects(topObject));
			//System.out.println(finalMessage);
			System.out.println(count);
			sendData(omiURL, finalMessage);
		}
	}*/


	/* Get json array index one at a time and send to createOmi function */
	public void parseArray(Object object, int select) throws JSONException {
		JSONArray jArray = (JSONArray) object;
		for (int i=0; i<jArray.length(); i++) {
			if (jArray.get(i) instanceof JSONObject) {
				if (select == 0) {
					createOmi((JSONObject) jArray.get(i));
				}
				else {
					createCompleteOdf((JSONObject) jArray.get(i));
					total ++;
				}
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
			Thread.sleep(20);			

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

	public static void createOmi(JSONObject jObject) throws JSONException {
		String infoItems = "";

		String sid = jObject.getString("sid");

		if (allSegments.contains(sid)) {
			Iterator<?> iterator = jObject.keys();
			while (iterator.hasNext()) {
				String key = (String) iterator.next();

				if (key.equals("type")) {
					String newKey="Status";
					infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
				}

				else if (key.equals("street")) {
					String newKey="Street";
					infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
				}
				else if (key.equals("reportdescription")){
					String newKey="Cause";
					String value=jObject.get(key).toString();
					String newValue;
					if (!(jObject.get(key) instanceof JSONObject)) {
						if (value.equals("Travaux")) {
							newValue = "Works";
						}
						else {
							newValue = value;
						}
						infoItems = infoItems + obj.createInfoItem(newKey, newValue);
					}
				}
				else if (key.equals("speed")){
					String newKey="Measured-Speed";
					infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
				}
				else if (key.equals("reliability")){
					String newKey="Information-Reliability";
					infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
				}
				else if (key.equals("confidence")){
					String newKey="Information-Confidence";
					infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
				}
				else {
					System.out.println("Do not do anything with this parameter!" + key);
				} 
			}

			String sidObject = obj.createOdfObject(sid, infoItems);
			String streetObject = obj.createOdfObject("Brussels-Smart-City", sidObject);
			count ++;
			String finalMessage = obj.createWriteMessage(obj.createOdfObjects(streetObject));
			System.out.println(finalMessage);
			System.out.println(count);
			sendData(omiURL, finalMessage);
		}
		else {
			System.out.println(sid + "Segment is not in Waze");
		}
	}


	public void createCompleteOdf(JSONObject jObject) throws JSONException {
		String infoItems = "";
		String sidId = null;
		Iterator<?> iterator = jObject.keys();
		while (iterator.hasNext()) {

			String key = (String) iterator.next();
			if (key.equals("id")) {
				sidId = "Sa." + jObject.getString(key);
				allSegments.add(sidId);
			}
			else if (key.equals("shape")) {
				String newKey="Position";
				infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
			}
			else if (key.equals("PN_NAME_FRE")) {
				String newKey="Street-Name-FRE";
				infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
			}
			else if (key.equals("PN_NAME_DUT")) {
				String newKey="Street-Name-DUT";
				infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
			}
			else {}
		}
		odfComplete = odfComplete + obj.createOdfObject(sidId, infoItems);
		count ++;
		System.out.println(count);
//		String sidObject = obj.createOdfObject(sidId, infoItems);
//		String streetObject = obj.createOdfObject("Brussels-Smart-City", sidObject);
//		count ++;
//		String finalMessage = obj.createWriteMessage(obj.createOdfObjects(streetObject));
//		System.out.println(count);
//		sendData(omiURL, finalMessage);

	}

	public void odfPrint() {
		String cityObject = obj.createOdfObject("Brussels-Smart-City", odfComplete);
		String finalMessage = obj.createWriteMessage(obj.createOdfObjects(cityObject));
		//System.out.println(finalMessage);
		sendData(omiURL, finalMessage);
		odfComplete = "";
	}
}
