/*
 * Created by Asad Javed on 28/08/2017
 * Aalto University project
 * 
 * Last modified 06/09/2017
 */

package api.wrapper.WrapperOmi;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.json.JSONArray;
import org.json.JSONObject;


public class Main {

	public static void main(String[] args) {

		// Load and get properties from the file
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("resources/config.properties"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String consumerKey = prop.getProperty("consumer_key");
		String consumerSecret = prop.getProperty("consumer_secret");

		WazeData wazeObject = new WazeData(prop);

		// Generating an access token
		String accessToken = null;

		/*try {
			accessToken = wazeObject.getAccessToken(consumerKey, consumerSecret);
		} catch (JSONException e1) {
			e1.printStackTrace();
		}

		int offset = 0;
		while (wazeObject.count < 22780) {
			String segmentsURL = "https://api.irisnetlab.be:443/api/biotope-datasources/0.0.1/biotope_street_axis2/axis?limit=1000&offset=" + Integer.toString(offset);
			try {
				String segmentsData = wazeObject.getJsonData(segmentsURL, accessToken);
				JSONObject segmentsObject = new JSONObject(segmentsData);
				JSONObject jObjectS = (JSONObject) segmentsObject.get("data_list");
				JSONArray segmentsArray = (JSONArray) jObjectS.get("data");
				wazeObject.parseArray(segmentsArray, 0);
				//wazeObject.sendPartialSegments();
				offset = offset + 1000;
				System.out.println("Segments processed: " + wazeObject.count);
			} catch (Throwable e) {
				e.printStackTrace();
			}	
		}
		System.out.println("*** The ODF structure with all segments has been processed!");*/

		String wazeAlertsUrl = prop.getProperty("waze_alerts");
		String wazeJamsUrl = prop.getProperty("waze_jams");
		
		while (true) {
			try {
				// For getting waze alerts and parse it
				accessToken = wazeObject.getAccessToken(consumerKey, consumerSecret);
				System.out.println("Data access has been granted...!");
				String jsonDataA = wazeObject.getJsonData(wazeAlertsUrl, accessToken);
				JSONObject jObjectA = new JSONObject(jsonDataA);
				JSONObject wazeAlertsA = (JSONObject) jObjectA.get("waze_alerts");
				JSONArray wazeArrayA = (JSONArray) wazeAlertsA.get("waze_alert");
				
				// For getting waze jams and parse it
				String jsonDataJ = wazeObject.getJsonData(wazeJamsUrl, accessToken);
				JSONObject jObjectJ = new JSONObject(jsonDataJ);
				JSONObject wazeAlertsJ = (JSONObject) jObjectJ.get("waze_jams");
				JSONArray wazeArrayJ = (JSONArray) wazeAlertsJ.get("waze_jam");
				
				System.out.println("Total alerts that need to be processed: " + wazeArrayA.length());
				wazeObject.parseArray(wazeArrayA, 1);
				System.out.println("*****Done*****");
				
				System.out.println("Total jams that need to be processed: " + wazeArrayJ.length());
				wazeObject.parseArray(wazeArrayJ, 1);
				System.out.println("*****Done*****");
				
				System.out.println("Wait for 4 minutes...");
				Thread.sleep(240000);
			} catch (Throwable e) {
				System.out.println(e.getMessage());
			}	
		}
	}
}
