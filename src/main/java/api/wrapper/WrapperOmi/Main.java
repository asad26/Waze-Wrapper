/*
 * Created by Asad Javed on 28/08/2017
 * Aalto University project
 * 
 * Last modified 06/09/2017
 */

package api.wrapper.WrapperOmi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Main {

	//private static String segmentsURL = "https://api.irisnetlab.be:443/api/biotope-datasources/0.0.1/biotope_street_axis2/axis";

	public static void main(String[] args) throws FileNotFoundException, IOException, JSONException {

		// Load and get properties from the file
		Properties prop = new Properties();
		prop.load(new FileInputStream("resources/config.properties"));
		String consumerKey = prop.getProperty("consumer_key");
		String consumerSecret = prop.getProperty("consumer_secret");
		//String urlSelection = prop.getProperty("url_selection");

		WazeData wazeObject = new WazeData(prop);

		// Generating an access token
		String accessToken = wazeObject.getAccessToken(consumerKey, consumerSecret);
		
		//String segmentsURL = "https://api.irisnetlab.be:443/api/biotope-datasources/0.0.1/biotope_street_axis2/axis?limit=22780&offset=0";
		//String segmentsData = wazeObject.getJsonData(segmentsURL, accessToken);
		//System.out.println(segmentsData);
		
//		int offset = 0;
//		while (offset < 22780) {
//			String segmentsURL = "https://api.irisnetlab.be:443/api/biotope-datasources/0.0.1/biotope_street_axis2/axis?limit=1&offset=" + Integer.toString(offset);
//			String segmentsData = wazeObject.getJsonData(segmentsURL, accessToken);
//			JSONObject segmentsObject = new JSONObject(segmentsData);
//			JSONObject jObjectS = (JSONObject) segmentsObject.get("data_list");
//			JSONObject jSingle = (JSONObject) jObjectS.get("data");
//			
//			wazeObject.createCompleteOdf(jSingle);
//
//			offset ++;
//			System.out.println("********" + offset);
//		}
//		System.out.println("The ODF structure with all segments has been created!");
		
		int offset = 0;
		while (wazeObject.total < 22780) {
			String segmentsURL = "https://api.irisnetlab.be:443/api/biotope-datasources/0.0.1/biotope_street_axis2/axis?limit=1000&offset=" + Integer.toString(offset);
			String segmentsData = wazeObject.getJsonData(segmentsURL, accessToken);
			JSONObject segmentsObject = new JSONObject(segmentsData);
			JSONObject jObjectS = (JSONObject) segmentsObject.get("data_list");
			JSONArray segmentsArray = (JSONArray) jObjectS.get("data");

			wazeObject.parseArray(segmentsArray, 1);
			wazeObject.odfPrint();
			
			offset = offset + 1000;
			//System.out.println("********" + offset);
		}
		System.out.println("The ODF structure with all segments has been created!");

		// For getting waze alerts and parse it

				String wazeAlertsUrl = prop.getProperty("waze_alerts");
				String jsonDataA = wazeObject.getJsonData(wazeAlertsUrl, accessToken);
				JSONObject jObjectA = new JSONObject(jsonDataA);
				JSONObject wazeAlertsA = (JSONObject) jObjectA.get("waze_alerts");
				JSONArray wazeArrayA = (JSONArray) wazeAlertsA.get("waze_alert");
				System.out.println("Total alerts that need to be processed: " + wazeArrayA.length());
				wazeObject.parseArray(wazeArrayA, 0);	
		//
		//		String wazeJamsUrl = prop.getProperty("waze_jams");
		//		String jsonDataJ = wazeObject.getJsonData(wazeJamsUrl, accessToken);
		//		JSONObject jObjectJ = new JSONObject(jsonDataJ);
		//		JSONObject wazeAlertsJ = (JSONObject) jObjectJ.get("waze_jams");
		//		JSONArray wazeArrayJ = (JSONArray) wazeAlertsJ.get("waze_jam");
		//		System.out.println("Total jams that need to be processed: " + wazeArrayJ.length());
		//		wazeObject.parseArray(wazeArrayJ);	

		System.out.println("***Done***");

	}

}
