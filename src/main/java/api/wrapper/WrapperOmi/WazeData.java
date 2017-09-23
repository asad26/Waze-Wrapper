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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import api.wrapper.db.SegmentsDB;

public class WazeData {

	private static ApiForOmi obj;
	public int count;
	private static String omiURL;
	private static String odfComplete;
	private static SegmentsDB database;

	/* Constructor for initializing variables */
	public WazeData(Properties prop) {
		obj = new ApiForOmi();
		count = 0;
		omiURL = prop.getProperty("omi_node");
		odfComplete = "";
		database = new SegmentsDB();
//		try {
//			database.createTable();
//		} catch (ClassNotFoundException e) {}
	}


	/* Get JSON array index one at a time and send for parsing and creating ODF */
	public void parseArray(Object object, int select) throws JSONException {
		JSONArray jArray = (JSONArray) object;
		for (int i=0; i<jArray.length(); i++) {
			if (jArray.get(i) instanceof JSONObject) {
				if (select == 0) {
					//createCompleteOdf((JSONObject) jArray.get(i));
					storeSegmentsData((JSONObject) jArray.get(i));
				}
				else if (select == 1) {
					createOdfStructure((JSONObject) jArray.get(i));
				}
				else {
					createOmi((JSONObject) jArray.get(i));
				}
			}
		}
	}
	
	/* Get segments from biotope API and create object structure */
	public void storeSegmentsData(JSONObject jObject) throws JSONException {
		String sid = "Sa." + jObject.getString("id");
		String pos = jObject.getString("shape").toString();
		String nameFre = jObject.getString("PN_NAME_FRE").toString();
		String nameDut = jObject.getString("PN_NAME_DUT");

		try {
			database.insert(sid, nameDut, nameFre, pos);
		} catch (ClassNotFoundException e) {}
		count ++;
	}

	public static void createOdfStructure(JSONObject jObject) throws JSONException {
		String infoItems = "";

		String[] relatedData = new String[3];
		if (!(jObject.get("sid") instanceof JSONObject)) {

			String sid = "Sa." + jObject.getString("sid");

			try {
				relatedData = database.queryData(sid);
			} catch (ClassNotFoundException e) {}
			
			infoItems = infoItems + obj.createInfoItem("Street-Name-DUT", relatedData[0]);
			infoItems = infoItems + obj.createInfoItem("Street-Name-FRE", relatedData[1]);
			infoItems = infoItems + obj.createInfoItem("Position", relatedData[2]);
			infoItems = infoItems + obj.createInfoItem("Status", "NotApplicable");
			infoItems = infoItems + obj.createInfoItem("Publication-Date", "NotApplicable");
			infoItems = infoItems + obj.createInfoItem("Cause", "NotApplicable");
			infoItems = infoItems + obj.createInfoItem("Measured-Speed", "NotApplicable");
			infoItems = infoItems + obj.createInfoItem("Information-Reliability", "NotApplicable");
			infoItems = infoItems + obj.createInfoItem("Information-Confidence", "NotApplicable");

			String sidObject = obj.createOdfObject(sid, infoItems);
			String streetObject = obj.createOdfObject("Brussels-Smart-City", sidObject);
			String finalMessage = obj.createWriteMessage(obj.createOdfObjects(streetObject));
			sendData(omiURL, finalMessage);
		}
	}

	/* Get items from Waze JSON array, create object, and send to the sand box one at a time */
	public static void createOmi(JSONObject jObject) throws JSONException {
		String infoItems = "";

		if (!(jObject.get("sid") instanceof JSONObject)) {

			String sid = "Sa." + jObject.getString("sid");
			
			Iterator<?> iterator = jObject.keys();
			while (iterator.hasNext()) {
				String key = (String) iterator.next();

				if (key.equals("type")) {
					String newKey="Status";
					infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
				}
				else if (key.equals("pubdate")){
					String newKey="Publication-Date";
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
				else {} 
			}
			String sidObject = obj.createOdfObject(sid, infoItems);
			String streetObject = obj.createOdfObject("Brussels-Smart-City", sidObject);
			String finalMessage = obj.createWriteMessage(obj.createOdfObjects(streetObject));
			sendData(omiURL, finalMessage);
		}
	}

	/* Get segments from biotope API and create object structure */
	/*public void createCompleteOdf(JSONObject jObject) throws JSONException {
		String infoItems = "";
		String sid = null;

		// Just for database storage
		String nameFre = null;
		String nameDut = null;
		String pos = null;

		Iterator<?> iterator = jObject.keys();
		while (iterator.hasNext()) {

			String key = (String) iterator.next();
			if (key.equals("id")) {
				sid = "Sa." + jObject.getString(key);

				//infoItems = infoItems + obj.createInfoItem("Status", "n/a");
				//infoItems = infoItems + obj.createInfoItem("Measured-Speed", "n/a");
				//infoItems = infoItems + obj.createInfoItem("Information-Reliability", "n/a");
				//infoItems = infoItems + obj.createInfoItem("Information-Confidence", "n/a");
				//infoItems = infoItems + obj.createInfoItem("Cause", "n/a");
			}
			else if (key.equals("shape")) {
				String newKey="Position";
				infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
				pos = jObject.get(key).toString();
			}
			else if (key.equals("PN_NAME_FRE")) {
				String newKey="Street-Name-FRE";
				infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
				nameFre = jObject.get(key).toString();
			}
			else if (key.equals("PN_NAME_DUT")) {
				String newKey="Street-Name-DUT";
				infoItems = infoItems + obj.createInfoItem(newKey, jObject.get(key).toString());
				nameDut = jObject.get(key).toString();
			}
			else {}
		}

		try {
			database.insert(sid, nameDut, nameFre, pos);
		} catch (ClassNotFoundException e) {}
		odfComplete = odfComplete + obj.createOdfObject(sid, infoItems);
		count ++;
	}*/

	public void sendPartialSegments() throws ClientProtocolException, IOException {
		String cityObject = obj.createOdfObject("Brussels-Smart-City", odfComplete);
		String finalMessage = obj.createWriteMessage(obj.createOdfObjects(cityObject));
		//System.out.println(finalMessage);
		sendOdfData(omiURL, finalMessage);
		odfComplete = "";
		System.out.println(count);
	}

	/* Method call to create access token for authorization */
	public String getAccessToken(String key, String secret) throws JSONException {

		String data = "grant_type=client_credentials&client_id=" + key + "&client_secret=" + secret;
		HttpURLConnection httpcon = null;
		BufferedReader br = null;
		OutputStream os = null;
		JSONObject jObject = null;
		try {
			httpcon = (HttpURLConnection) ((new URL("https://api.irisnetlab.be:443/api/token").openConnection()));
			httpcon.setDoOutput(true);
			httpcon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			httpcon.setRequestMethod("POST");
			//httpcon.connect();
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

			jObject = new JSONObject(response.toString());

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

		return jObject.get("access_token").toString();
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


	/* Method call to send OMI write envelope to the sand box */
	private static void sendData(String url, String finalMessage) {
		HttpURLConnection httpcon = null;
		OutputStream os =  null;
		try {
			httpcon = (HttpURLConnection) ((new URL(url).openConnection()));
			httpcon.setDoOutput(true);
			httpcon.setRequestProperty("Content-Type", "text/xml");
			httpcon.setRequestMethod("POST");
			httpcon.setUseCaches(false);
			byte[] outputBytes = finalMessage.getBytes("UTF-8");
			os = httpcon.getOutputStream();
			os.write(outputBytes);
			httpcon.getResponseMessage();

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


	/* Method call to send OMI write envelope to the sand box */
	private static void sendOdfData(String url, String finalMessage) throws ClientProtocolException, IOException {
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(url);

		HttpEntity entity = new ByteArrayEntity(finalMessage.getBytes("ISO-8859-4"));
		post.addHeader("Content-Type", "text/xml");
		post.setEntity(entity);
		HttpResponse response = client.execute(post);
		String result = EntityUtils.toString(response.getEntity());
		System.out.println(result);
		//System.out.println(response.getStatusLine());
	}
}
