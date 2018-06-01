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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import api.wrapper.db.SegmentsDB;

public class WazeData {

    private static final Logger LOG = LoggerFactory.getLogger(WazeData.class);
    private static ApiForOmi    obj;
    private static String       omiURL;
    private static String       odfComplete;
    private static SegmentsDB   database;
    private static List<String> wazeSegments;
    private final String consumerKey;
    private final String consumerSecret;
    public         int          count;
    private        AccessToken  accessToken;

    /* Constructor for initializing variables */
    public WazeData(Properties prop) {
        count = 0;
        consumerKey = prop.getProperty("consumer_key");
        consumerSecret = prop.getProperty("consumer_secret");
        obj = new ApiForOmi();
        omiURL = prop.getProperty("omi_node");
        odfComplete = "";
        database = new SegmentsDB();
        wazeSegments = new ArrayList<>();
        //		try {
        //			database.createTable();
        //		} catch (ClassNotFoundException e) {}
    }

    public static void createOdfStructure(String sid) throws JSONException {
        String infoItems = "";
        String[] relatedData = database.queryData(sid);

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

    /**
     * Get items from Waze JSON array, create object, and send to the sand box one at a time
     *
     * @param jObject
     * @throws JSONException
     */
    public static void createOmi(JSONObject jObject) throws JSONException {
        String infoItems = "";

        if (!(jObject.get("sid") instanceof JSONObject)) {
            LOG.debug("Creating OMI.");

            String sid = "Sa." + jObject.getString("sid");

            if (!(wazeSegments.contains(sid))) {
                wazeSegments.add(sid);
                createOdfStructure(sid);
            }

            Iterator<?> iterator = jObject.keys();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();

                if (key.equals("type")) {
                    String newKey = "Status";
                    infoItems = infoItems + obj.createInfoItem(newKey, jObject.getString(key));
                } else if (key.equals("pubdate")) {
                    String newKey = "Publication-Date";
                    infoItems = infoItems + obj.createInfoItem(newKey, jObject.getString(key));
                } else if (key.equals("reportdescription")) {
                    String newKey = "Cause";
                    String value = jObject.get(key).toString();
                    String newValue;
                    if (!(jObject.get(key) instanceof JSONObject)) {
                        if (value.equals("Travaux")) {
                            newValue = "Works";
                        } else {
                            newValue = value;
                        }
                        infoItems = infoItems + obj.createInfoItem(newKey, newValue);
                    }
                } else if (key.equals("speed")) {
                    String newKey = "Measured-Speed";
                    infoItems = infoItems + obj.createInfoItem(newKey, jObject.getString(key));
                } else if (key.equals("reliability")) {
                    String newKey = "Information-Reliability";
                    infoItems = infoItems + obj.createInfoItem(newKey, jObject.getString(key));
                } else if (key.equals("confidence")) {
                    String newKey = "Information-Confidence";
                    infoItems = infoItems + obj.createInfoItem(newKey, jObject.getString(key));
                } else {
                }
            }
            String sidObject = obj.createOdfObject(sid, infoItems);
            String streetObject = obj.createOdfObject("Brussels-Smart-City", sidObject);
            String finalMessage = obj.createWriteMessage(obj.createOdfObjects(streetObject));
            sendData(omiURL, finalMessage);
        }
    }

    /**
     * Method call to send OMI write envelope to the sand box
     *
     * @param url
     * @param finalMessage
     */
    private static boolean sendData(String url, String finalMessage) {
        HttpURLConnection httpcon = null;
        OutputStream os = null;
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
            return true;
        } catch (Exception e) {
            LOG.error("Error while sending data to O-MI.", e);
            return false;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
            if (httpcon != null) {
                httpcon.disconnect();
            }
        }
    }

    /**
     * Method call to send OMI write envelope to the sand box
     *
     * @param url
     * @param finalMessage
     * @throws IOException
     */
    private static void sendOdfData(String url, String finalMessage) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(url);

        HttpEntity entity = new ByteArrayEntity(finalMessage.getBytes("UTF-8"));
        post.addHeader("Content-Type", "text/xml");
        post.setEntity(entity);
        HttpResponse response = client.execute(post);
        String result = EntityUtils.toString(response.getEntity());
        LOG.trace("Result : {} | Response status : {}", result, response.getStatusLine());
    }

    /**
     * Get JSON array index one at a time and send for parsing and creating ODF
     *
     * @param object
     * @param select
     * @throws JSONException
     */
    public void parseArray(Object object, int select) throws JSONException {
        JSONArray jArray = (JSONArray) object;
        long timeMs;
        for (int i = 0; i < jArray.length(); i++) {
            if (jArray.get(i) instanceof JSONObject) {
                timeMs = System.currentTimeMillis();
                if (select == 0) {
                    createCompleteOdf((JSONObject) jArray.get(i));
                    //storeSegmentsData((JSONObject) jArray.get(i));
                } else {
                    createOmi((JSONObject) jArray.get(i));
                }
                LOG.debug("Treated JSONObject in {}ms. Remaining : {}", System.currentTimeMillis() - timeMs,
                        jArray.length() - i);
            }

        }
    }

    /**
     * Get segments from biotope API and create object structure
     *
     * @param jObject
     * @throws JSONException
     */
    public void storeSegmentsData(JSONObject jObject) throws JSONException {
        String sid = "Sa." + jObject.getString("id");
        String pos = jObject.getString("shape").toString();
        String nameFre = jObject.getString("PN_NAME_FRE");
        String nameDut = jObject.getString("PN_NAME_DUT");

        try {
            database.insert(sid, nameDut, nameFre, pos);
        } catch (ClassNotFoundException e) {
        }
        count++;
    }

    /**
     * Get segments from biotope API and create object structure
     *
     * @param jObject
     * @throws JSONException
     */
    public void createCompleteOdf(JSONObject jObject) throws JSONException {
        LOG.debug("Creating ODF.");
        String infoItems = "";

        // Just for database storage
        //		String nameFre = null;
        //		String nameDut = null;
        //		String pos = null;

        int group = Integer.valueOf(jObject.getString("id")) % 500;

        String sid = "Sa." + jObject.getString("id");
        //infoItems = infoItems + obj.createInfoItem("Status", "n/a");
        //infoItems = infoItems + obj.createInfoItem("Measured-Speed", "n/a");
        //infoItems = infoItems + obj.createInfoItem("Information-Reliability", "n/a");
        //infoItems = infoItems + obj.createInfoItem("Information-Confidence", "n/a");
        //infoItems = infoItems + obj.createInfoItem("Cause", "n/a");

        infoItems = infoItems + obj.createInfoItem("Position", jObject.getString("shape"));
        //pos = jObject.get(key).toString();

        infoItems = infoItems + obj.createInfoItem("Street-Name-FRE", jObject.getString("PN_NAME_FRE"));
        //nameFre = jObject.get(key).toString();

        infoItems = infoItems + obj.createInfoItem("Street-Name-DUT", jObject.getString("PN_NAME_DUT"));
        //nameDut = jObject.get(key).toString();

        //		try {
        //			database.insert(sid, nameDut, nameFre, pos);
        //		} catch (ClassNotFoundException e) {}
        //odfComplete = odfComplete + obj.createOdfObject(sid, infoItems);
        String segmentObject = obj.createOdfObject(sid, infoItems);
        String groupObject = obj.createOdfObject("Group-" + Integer.toString(group), segmentObject);
        String cityObject = obj.createOdfObject("Brussels-Smart-City", groupObject);
        String finalMessage = obj.createWriteMessage(obj.createOdfObjects(cityObject));
        sendData(omiURL, finalMessage);
        count++;
    }

    public void sendPartialSegments() throws ClientProtocolException, IOException {
        String cityObject = obj.createOdfObject("Brussels-Smart-City", odfComplete);
        String finalMessage = obj.createWriteMessage(obj.createOdfObjects(cityObject));
        sendOdfData(omiURL, finalMessage);
        odfComplete = "";
        LOG.trace("Count : {}", count);
    }

    /**
     * Method call to create access token for authorization
     *
     * @return
     * @throws JSONException
     */
    public String getAccessToken() throws JSONException {
        if (accessToken == null || accessToken.tokenLastCreated.plusSeconds(accessToken.expiresIn)
                .isBefore(LocalDateTime.now())) {
            String data = "grant_type=client_credentials&client_id=" + consumerKey + "&client_secret=" + consumerSecret;
            HttpURLConnection httpcon = null;
            BufferedReader br = null;
            OutputStream os = null;
            JSONObject jObject = null;

            try {
                httpcon = (HttpURLConnection) ((new URL("https://api.irisnetlab.be:443/api/token").openConnection()));
                httpcon.setDoOutput(true);
                httpcon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                httpcon.setRequestMethod("POST");
                byte[] outputBytes = data.getBytes("UTF-8");
                os = httpcon.getOutputStream();
                os.write(outputBytes);
                LOG.trace("Connection : {}", httpcon.getResponseMessage());
                br = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
                StringBuffer response = new StringBuffer();
                String inputLine;
                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }
                jObject = new JSONObject(response.toString());
            } catch (Exception e) {
                LOG.error("Exception while getting access token.", e);
                throw new IllegalStateException("Cannot build token");
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                    }
                }
                if (httpcon != null) {
                    httpcon.disconnect();
                }

                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                    }
                }
            }
            accessToken = new AccessToken(jObject.get("access_token").toString(),
                    new Long(jObject.get("expires_in").toString()));
        }

        return accessToken.token;
    }

    /**
     * Method call to return data in JSON
     *
     * @param url
     * @param token
     * @return
     */
    public String getJsonData(String url, String token) {

        HttpURLConnection httpcon = null;
        StringBuffer response = null;
        BufferedReader br = null;
        try {
            httpcon = (HttpURLConnection) ((new URL(url).openConnection()));
            httpcon.setDoOutput(false);
            String authorization = "Bearer " + token;
            httpcon.setRequestProperty("Authorization", authorization);
            httpcon.setRequestProperty("Accept", "application/json");
            httpcon.setRequestMethod("GET");
            LOG.trace("Connection status : ", httpcon.getResponseMessage());
            br = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
            response = new StringBuffer();
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
        } catch (Exception e) {
            LOG.error("Exception while getting JSON data.", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }

            if (httpcon != null) {
                httpcon.disconnect();
            }
        }

        return response.toString();
    }

    /**
     * Store a token with some time data which helps with token caching.
     */
    private static class AccessToken {

        String        token;
        Long          expiresIn;
        LocalDateTime tokenLastCreated;

        AccessToken(String token, Long expiresIn) {
            this.token = token;
            this.expiresIn = expiresIn;
            this.tokenLastCreated = LocalDateTime.now();
        }
    }
}
