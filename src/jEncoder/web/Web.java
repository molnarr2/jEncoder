package jEncoder.web;

import java.io.IOException;
import java.net.URI;

import jEncoder.Configuration;
import jEncoder.util.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Web {
	
	/** The web server addresss. */
	protected String serverAddress;

	private String ENCODING_JOBS = "/encoding_jobs.php";
	
	private String GET_CHANNEL_INOUT = "/inout.php";
			
	
    // The only instance of this class.
    private static Web instance = null;
    
    // [start] Methods: Constructor and getInstance()

    protected Web() {
    	String tmp;
    	serverAddress = Configuration.getInstance().getWebServerName();
    	
    	tmp = Configuration.getInstance().getWebServerRootPath() + GET_CHANNEL_INOUT;
    	GET_CHANNEL_INOUT = tmp;
    	    	
    }
    
    /** @return the only instance of this class.
     */
    public static Web getInstance() {
        if(instance == null) {
            instance = new Web();
        }
        return instance;
    }
	
	public Web(String serverAddress) {
		this.serverAddress = serverAddress;
	}
	
	/** @return the JSONobject of the encoding jobs.
	 * */
	public JSONObject getEncodingJobs() {				
		String ipAddress = Configuration.getInstance().getEvostreamServerName();	
		String preMessageInfo = "Notify Web [GET_ENCODING_JOBS] {ipaddress:" + ipAddress + "}";

		// Get data from web site.
		String url = "http://" + serverAddress + ENCODING_JOBS + "?cmd=1&ipaddress=" + ipAddress;
		JSONObject root = null;
		try {
			Content content = Request.Get(url).execute().returnContent();			
			root = (JSONObject)JSONValue.parse(content.asString());
			
			// LogSystem.getInstance().printInfo(0, 0, preMessageInfo);
			
		} catch (Exception e) {
			LogSystem.getInstance().printError(preMessageInfo, e);
		}	
				
		return root;
	}
		
	/** Notify the web server the archive video does not exist and therefore cannot encode it.
	 * */
	public void notifyDownloadDone(String jobIdNumber, int clientId, int channel_no, int dhid, long fileSize) {

		String preMessageInfo = "Job [" + jobIdNumber + "] Notify Web [ENCODING_DOWNLOAD] {dhid:"+ dhid + "}";
			 		
        URI uri;
        try {
            uri = new URIBuilder()
            .setScheme("http")
            .setHost(serverAddress)
            .setPath(ENCODING_JOBS)
            .setParameter("cmd", "5")
            .setParameter("download_history_id", "" + dhid)
            .setParameter("video_file_size", "" + fileSize)            
            .build();
            
			sendRequest(clientId, channel_no, preMessageInfo, uri);
            
        } catch (Exception e) {
            LogSystem.getInstance().printError(clientId, channel_no, preMessageInfo, e);
        }

	}
	
	
	/** Notify the web server the archive video does not exist and therefore cannot encode it.
	 * */
	public void notifyRecordedFileDoesNotExist(String jobIdNumber, int clientId, int channel_no, int viid) {

		String preMessageInfo = "Job [" + jobIdNumber + "] Notify Web [ENCODING_JOB_ERROR] {viid:"+ viid + "}";
			 		
        URI uri;
        try {
            uri = new URIBuilder()
            .setScheme("http")
            .setHost(serverAddress)
            .setPath(ENCODING_JOBS)
            .setParameter("cmd", "4")
            .setParameter("video_info_id", "" + viid)
            .build();
            
			sendRequest(clientId, channel_no, preMessageInfo, uri);
            
        } catch (Exception e) {
            LogSystem.getInstance().printError(clientId, channel_no, preMessageInfo, e);
        }

	}
	
	/** Notify the web server that the archive video is done being encoded and ready to be viewed or edited.
	 * */
	public void notifyArchiveVideoDone(String jobIdNumber, int clientId, int channel_no, int viid, int audioDuration, int videoDuration, long audioSize) {

		String preMessageInfo = "Job [" + jobIdNumber + "] Notify Web [ENCODING_JOB_FINISHED] {viid:"+ viid + ", audio duration:" + audioDuration + ", audio size:" + audioSize + ", video duration:" + videoDuration + "}";
			 		
        URI uri;
        try {
            uri = new URIBuilder()
            .setScheme("http")
            .setHost(serverAddress)
            .setPath(ENCODING_JOBS)
            .setParameter("cmd", "2")
            .setParameter("video_info_id", "" + viid)
            .setParameter("audio_duration", "" + audioDuration)
            .setParameter("video_duration", "" + videoDuration)
            .setParameter("audio_file_size", "" + audioSize)
            .build();
            
			sendRequest(clientId, channel_no, preMessageInfo, uri);
            
        } catch (Exception e) {
            LogSystem.getInstance().printError(clientId, channel_no, preMessageInfo, e);
        }
	}	
	
	/** Notify the web server the in/out video has been encoded.
	 * @param ioviid is the InOutVideoInfo.in_out_video_id.
	 * */
	public void notifyInOutVideo(String jobIdNumber, int clientId, int channel_no, int ioviid) {
				
		String preMessageInfo = "Job [" + jobIdNumber + "] Notify Web [ENCODING_JOB_FINISHED] {ioviid:" + ioviid + "}";
			 		
        URI uri;
        try {
            uri = new URIBuilder()
            .setScheme("http")
            .setHost(serverAddress)
            .setPath(ENCODING_JOBS)
            .setParameter("cmd", "3")            
            .setParameter("in_out_video_info_id", "" + ioviid)
            .build();

			sendRequest(clientId, channel_no, preMessageInfo, uri);
            
        } catch (Exception e) {
            LogSystem.getInstance().printError(clientId, channel_no, preMessageInfo, e);
        }
	}	
	
	/** This will get the in/out JSON data. 
	 * */
	public JSONObject getInOutDetails(String jobIdNumber, int clientId, int channel_no) {
				
		String preMessageInfo = "Job [" + jobIdNumber + "] Notify Web [GET_IN_OUT_FLAGS] {}";
    	
    	// Get data from web site.
		String url = "http://" + serverAddress + GET_CHANNEL_INOUT + "?cid=" + clientId + "&cno=" + channel_no;
		JSONObject root = null;
		
		try {
			Content content = Request.Get(url).execute().returnContent();
			
			// Get in/out information.
			root = (JSONObject)JSONValue.parse(content.asString());
			
			LogSystem.getInstance().printInfo(clientId, channel_no, preMessageInfo);
			
		} catch (Exception e) {
			LogSystem.getInstance().printError(clientId, channel_no, preMessageInfo, e);
		}	
    			
		return root;
	}
		
	/** This will perform a request to jEvoSystem.
	 * */
	private String sendRequest(int clientId, int channelNo, String preMessageInfo, URI uri) {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		String responseBody = null;
        try {
            HttpGet httpget = new HttpGet(uri);

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                @Override
                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };
            responseBody = httpclient.execute(httpget, responseHandler);

    	    // Log it.
	    	LogSystem.getInstance().printInfo(clientId, channelNo, preMessageInfo + " = SUCCESS");

        } catch (Exception e) {
	    	LogSystem.getInstance().printError(clientId, channelNo, preMessageInfo + " ERROR.", e);

        } finally {
        	try {
        		httpclient.close();
        	} catch (Exception e) {
    	    	LogSystem.getInstance().printError(clientId, channelNo, preMessageInfo + " ERROR.", e);        		
        	}
        }
		
        return responseBody;		
	}	
}