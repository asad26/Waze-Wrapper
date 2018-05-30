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
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final String PROCESS_SEGMENTS       = "-processSegments";
    private static final String PROCESS_SEGMENTS_SHORT = "-ps";

    private static final Logger  LOG             = LoggerFactory.getLogger(Main.class);
    private static       Boolean processSegments = Boolean.FALSE;

    public static void main(String[] args) {
        processArgs(args);
        // Load and get properties from the file
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("resources/config.properties"));
        } catch (IOException e1) {
            LOG.error("Error loading properties.", e1);
        }
        String consumerKey = prop.getProperty("consumer_key");
        String consumerSecret = prop.getProperty("consumer_secret");

        WazeData wazeObject = new WazeData(prop);

        // Generating an access token
        String accessToken = null;
        try {
            accessToken = wazeObject.getAccessToken(consumerKey, consumerSecret);
        } catch (JSONException e1) {
            LOG.error("Unable to get access token.", e1);
        }

        if (processSegments) {
            LOG.debug("-ps flag detected");
            processSegments(accessToken, wazeObject);
        }

        String wazeAlertsUrl = prop.getProperty("waze_alerts");
        String wazeJamsUrl = prop.getProperty("waze_jams");

        while (true) {
            try {
                // For getting waze alerts and parse it
                accessToken = wazeObject.getAccessToken(consumerKey, consumerSecret);
                LOG.debug("Data access has been granted...!");
                String jsonDataA = wazeObject.getJsonData(wazeAlertsUrl, accessToken);
                JSONObject jObjectA = new JSONObject(jsonDataA);
                JSONObject wazeAlertsA = (JSONObject) jObjectA.get("waze_alerts");
                JSONArray wazeArrayA = (JSONArray) wazeAlertsA.get("waze_alert");

                // For getting waze jams and parse it
                String jsonDataJ = wazeObject.getJsonData(wazeJamsUrl, accessToken);
                JSONObject jObjectJ = new JSONObject(jsonDataJ);
                JSONObject wazeAlertsJ = (JSONObject) jObjectJ.get("waze_jams");
                JSONArray wazeArrayJ = (JSONArray) wazeAlertsJ.get("waze_jam");

                LOG.debug("Total alerts that need to be processed: {}", wazeArrayA.length());
                wazeObject.parseArray(wazeArrayA, 1);
                LOG.info("*****Done*****");

                LOG.debug("Total jams that need to be processed: {}", wazeArrayJ.length());
                wazeObject.parseArray(wazeArrayJ, 1);
                LOG.debug("*****Done*****");

                LOG.debug("Wait for 4 minutes...");
                Thread.sleep(240000);
            } catch (Throwable e) {
                LOG.error("Error during process.", e);
            }
        }
    }

    private static void processSegments(String accessToken, WazeData wazeObject) {
        int offset = 0;
        while (wazeObject.count < 22780) {
            String segmentsURL =
                    "https://api.irisnetlab.be:443/api/biotope-datasources/0.0.1/biotope_street_axis2/axis?limit=1000&offset="
                            + Integer.toString(offset);
            try {
                String segmentsData = wazeObject.getJsonData(segmentsURL, accessToken);
                JSONObject segmentsObject = new JSONObject(segmentsData);
                JSONObject jObjectS = (JSONObject) segmentsObject.get("data_list");
                JSONArray segmentsArray = (JSONArray) jObjectS.get("data");
                wazeObject.parseArray(segmentsArray, 0);
                wazeObject.sendPartialSegments();
                offset = offset + 1000;
                LOG.debug("{} segments processed.", wazeObject.count);
            } catch (Throwable e) {
                LOG.error("Error while processing segments.", e);
            }
        }
        LOG.info("The ODF structure with all segments has been processed.");
    }

    private static void processArgs(Object... args) {
        for (Object arg : args) {
            switch (arg.toString()) {
                case PROCESS_SEGMENTS:
                case PROCESS_SEGMENTS_SHORT:
                    processSegments = Boolean.TRUE;
                    break;
            }
        }
    }
}
