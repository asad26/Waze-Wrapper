/*
 * Created by Asad Javed on 28/08/2017
 * Aalto University project
 * 
 * Last modified 06/09/2017
 */

package api.wrapper.WrapperOmi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Main {

	public static void main(String[] args) throws FileNotFoundException, IOException, JSONException {

		// Load and get properties from the file
		Properties prop = new Properties();
		prop.load(new FileInputStream("resources/config.properties"));
		String consumerKey = prop.getProperty("consumer_key");
		String consumerSecret = prop.getProperty("consumer_secret");
		String urlSelection = prop.getProperty("url_selection");

		WazeData wazeObject = new WazeData(prop);

		// Generating an access token
		String accessToken = wazeObject.getAccessToken(consumerKey, consumerSecret);

		String wazeUrl = null;
		// For getting waze alerts and parse it
		if (urlSelection.equals("0")) {
			wazeUrl = prop.getProperty("waze_alerts");
			String jsonData = wazeObject.getJsonData(wazeUrl, accessToken);
			JSONObject jObject = new JSONObject(jsonData);
			JSONObject wazeAlerts = (JSONObject) jObject.get("waze_alerts");
			JSONArray wazeArray = (JSONArray) wazeAlerts.get("waze_alert");
			System.out.println("Total alerts that need to be processed: " + wazeArray.length());
			wazeObject.parseArray(wazeArray);	
		}
		// For getting waze jams and parse it
		else {
			wazeUrl = prop.getProperty("waze_jams");
			String jsonData = wazeObject.getJsonData(wazeUrl, accessToken);
			JSONObject jObject = new JSONObject(jsonData);
			JSONObject wazeAlerts = (JSONObject) jObject.get("waze_jams");
			JSONArray wazeArray = (JSONArray) wazeAlerts.get("waze_jam");
			System.out.println("Total jams that need to be processed: " + wazeArray.length());
			wazeObject.parseArray(wazeArray);	
		}

		System.out.println("***Done***");

	}

}
