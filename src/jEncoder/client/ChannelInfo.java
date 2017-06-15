package jEncoder.client;

import jEncoder.util.LogSystem;
import jEncoder.web.Web;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.simple.JSONObject;

import jEncoder.apple_hls.M3U8ChildPlaylist;
import jEncoder.apple_hls.M3U8MasterPlaylist;

public class ChannelInfo {
	
	private final String CONFIG_ARCHIVE_PATH = "/opt/clients/";
	
	/** ChannelInfo.channel_no. */
	private int channelNo;
	    
	/** ClientInfo.client_info_id. */
	private int clientInfoId;
    
	/** The in-video's m3u8 playlist. HD, SD, LOW, and audio64k. */
	private M3U8ChildPlaylist inVideo[] = new M3U8ChildPlaylist[4];
	
	/** The out-video's m3u8 playlist. HD, SD, LOW, and audio64k. */
	private M3U8ChildPlaylist outVideo[] = new M3U8ChildPlaylist[4];
	
    // [start] Methods: Constructor and getInstance()

    public ChannelInfo(int clientInfoId, int channelNo) {
    	this.clientInfoId = clientInfoId;
    	this.channelNo = channelNo;
    }
    
    // [end]    
    
    /** This will update all HLS archive videos for a given channel #.
     * */
    synchronized public void updateAllHLS(String jobIdNumber) {
    	
    	// Log this call.
    	LogSystem.getInstance().printInfo(clientInfoId, channelNo, formatMessageForLog(jobIdNumber, "Update all HLS."));
    	
		// Get the files that need to be processed.
		Path pathDirectory = Paths.get(CONFIG_ARCHIVE_PATH + clientInfoId);

		// Load in the in/out videos for the channel.
		loadInOutVideos(jobIdNumber);
		
		try (DirectoryStream<Path> paths = Files.newDirectoryStream(pathDirectory, "*_*")) {
    		for (Path path : paths) {
    			
    			// Skip non-directories.
    			if(!Files.isDirectory(path))
    				continue;
    			
    			// process the HLS.
    			updateSingleHLS(path, jobIdNumber);
    			
    		}
    	} catch (IOException e) {
			LogSystem.getInstance().printError(0, 0, formatMessageForLog(jobIdNumber, "Unable to get directory of files to process."), e);
    	}			

    	LogSystem.getInstance().printInfo(clientInfoId, channelNo, "Done.");
    }
    
    /** This will update a single video HLS archive.
     * @param filename is the name of the file to update it's HLS archive. ex: 1_1_20150505145533.mp4.
     * */
    synchronized public void updateSingleHLS(String filename, String jobIdNumber) {
		// Load in the in/out videos for the channel.
		loadInOutVideos(jobIdNumber);
		
		// Process the single file.
		String[] components = filename.split("\\.");
		String dirFilename = CONFIG_ARCHIVE_PATH + clientInfoId + "/" + components[0];
		updateSingleHLS(Paths.get(dirFilename), jobIdNumber);
    }
    
    /** @return true if clientInfoId is equal to this clientInfoId.
     * */
    public boolean isChannelNo(int channelNo) {
    	 return this.channelNo == channelNo;
    }

    /** This will update the HLS for a given archive video. 
     * @param pathArchiveDir is the path to the archive's video directory that contains the HLS for the video. 
     * Example: /opt/gomedia/media/archive/clients/1/1_2_20150206125348
     * */
    private void updateSingleHLS(Path pathArchiveDir, String jobIdNumber) {    	
    	StringBuffer sBufferOut = new StringBuffer();
    	sBufferOut.append("Updating in/out HLS for file:[" + pathArchiveDir.getFileName().toString() + "]  bitrates:[");
    	
    	/** The transformations going on:
    	 * <filename>/64k/64k.m3u8 => <filename>/64k/inout.m3u8
    	 * <filename>/LOW/LOW.m3u8 => <filename>/LOW/inout.m3u8
    	 * <filename>/SD/SD.m3u8   => <filename>/SD/inout.m3u8
    	 * <filename>/HD/HD.m3u8   => <filename>/HD/inout.m3u8
    	 * <filename>/pc.m3u8      => <filename>/pc_inout.m3u8
    	 * <filename>/ios.m3u8      => <filename>/ios_inout.m3u8
    	 * */
    	    	
    	// Create the inout.m3u8 with the in/out videos.
    	String []bitrates = {"HD", "SD", "LOW", "64k"};
    	for (int i=0; i < bitrates.length; i++) {
    		
	    	// Read in the archive video.
	    	String pathToOriginalM3U8 = pathArchiveDir.toString() + "/" + bitrates[i] + "/" + bitrates[i] + ".m3u8";
	    	if (!Files.exists(Paths.get(pathToOriginalM3U8)))
	    		continue;
	    	
	    	// Log the operation.
	    	sBufferOut.append(" " + bitrates[i]);
	    	
	    	M3U8ChildPlaylist archiveVideo = new M3U8ChildPlaylist(clientInfoId, channelNo);
	    	archiveVideo.setFilename(pathToOriginalM3U8);
	    	archiveVideo.read();	    
	    	
	    	// Add in the intro/ending videos. Check for the current bitrate and if it doesn't exist than add a lower version in.
	    	for (int j=i; j < bitrates.length; j++) {
	    		if (inVideo[j] != null) {
	    			archiveVideo.addIntroVideo(inVideo[j]);
	    			break;
	    		}
	    	}	    	
	    	for (int j=i; j < bitrates.length; j++) {
	    		if (outVideo[j] != null) {
	    			archiveVideo.addEndingVideo(outVideo[j]);
	    			break;
	    		}
	    	}
	    	
	    	// Write out the new m3u8 file.
	    	archiveVideo.setFilename(pathArchiveDir.toString() + "/" + bitrates[i] + "/inout.m3u8");
	    	archiveVideo.write();
    	}
    	sBufferOut.append("] m3u8 created:[");
    	
    	// Only needs to be written if the pc.m3u8 exists and if it the pc_inout.m3u8 does not.
    	String pathAbsolute = pathArchiveDir.toString() + "/pc.m3u8";
    	String pathAbsoluteInout = pathArchiveDir.toString() + "/pc_inout.m3u8";    	
    	if (Files.exists(Paths.get(pathAbsolute)) && !Files.exists(Paths.get(pathAbsoluteInout))) {
        	M3U8MasterPlaylist master = new M3U8MasterPlaylist(clientInfoId, channelNo);    	    
        	master.setFilename(pathAbsolute);
        	master.read();
        	master.setFilename(pathAbsoluteInout);
        	master.writeInout();   
	    	
	    	// Log the operation.
	    	sBufferOut.append("pc_inout.m3u8 ");        	
    	}
    	
    	// Only needs to be written if the ios.m3u8 exists and if it the ios_inout.m3u8 does not. The master.m3u8 is required by
    	// the iOS app.
    	pathAbsolute = pathArchiveDir.toString() + "/ios.m3u8";
    	pathAbsoluteInout = pathArchiveDir.toString() + "/ios_inout.m3u8";    	
    	String pathAbsoluteInoutMaster = pathArchiveDir.toString() + "/master.m3u8";    	
    	if (Files.exists(Paths.get(pathAbsolute)) && !Files.exists(Paths.get(pathAbsoluteInout))) {
        	M3U8MasterPlaylist master = new M3U8MasterPlaylist(clientInfoId, channelNo);    	    
        	master.setFilename(pathAbsolute);
        	master.read();
        	master.setFilename(pathAbsoluteInout);
        	master.writeInout();    	
        	master.setFilename(pathAbsoluteInoutMaster);
        	master.writeInout();
        	
	    	// Log the operation.
	    	sBufferOut.append("ios_inout.m3u8 master.m3u8");        	
    	}
    	sBufferOut.append("]");

    	LogSystem.getInstance().printInfo(clientInfoId, channelNo, formatMessageForLog(jobIdNumber, sBufferOut.toString()));    	
    }
    
    /** This will load the in/out videos. It will query the database on the web server to determine if in/out videos should be loaded.
     * */
    private void loadInOutVideos(String jobIdNumber) {
    	// Pull from database if in/out videos should be loaded.
    	boolean inVideoActive = false;
    	boolean outVideoActive = false;
    	
    	// Get the in/out flag.
    	JSONObject obj = Web.getInstance().getInOutDetails(jobIdNumber, clientInfoId, channelNo);    	
    	if (obj.get("in").toString().equals("Y"))
    		inVideoActive = true;
    	if (obj.get("out").toString().equals("Y"))
    		outVideoActive = true;
    	int in_video_info_id = Integer.parseInt(obj.get("inid").toString());
    	int out_video_info_id = Integer.parseInt(obj.get("outid").toString());
    	
    	StringBuffer sBufferOut = new StringBuffer();
    	sBufferOut.append("Loading In/Out Videos");    	    	
    	sBufferOut.append("  In Flag:[" + inVideoActive + "]  Out Flag:[" + outVideoActive + "]");    	
    	
    	// Load in the inVideo and update it.
    	String []bitrates = {"HD", "SD", "LOW", "64k"};		
    	if (inVideoActive) {

        	sBufferOut.append("  In Bitrate:[");

    		for (int i=0; i < bitrates.length; i++) {
    			// Get the filename and see if it exists.
    			String filename = CONFIG_ARCHIVE_PATH + clientInfoId + "/inout/" + channelNo + "/" + in_video_info_id + "/" + bitrates[i] + "/" + bitrates[i] + ".m3u8";
    			if (!Files.exists(Paths.get(filename))) {    			
    		    	LogSystem.getInstance().printInfo(clientInfoId, channelNo, formatMessageForLog(jobIdNumber, "loadInOutVideos() bitrate:[" + bitrates[i] + "] filename: " + filename + " DOES NOT EXIST."));	    				
    				inVideo[i] = null;
    				continue;
    			}

            	sBufferOut.append(" " + bitrates[i]);
    			
    			// Load in the in video.
    			inVideo[i] = new M3U8ChildPlaylist(clientInfoId, channelNo);
    			inVideo[i].setFilename(filename);
    			inVideo[i].read();
    			inVideo[i].updateTSInfoPath("/video/clients/" + clientInfoId + "/inout/" + channelNo + "/" + in_video_info_id + "/" + bitrates[i] + "/");    			
    		}
    		sBufferOut.append("]");
    	} else {
    		for (int i=0; i < inVideo.length; i++)
    			inVideo[i] = null;
    	}
    	
    	// Load in the outVideo and update it.
    	if (outVideoActive) {
    		
        	sBufferOut.append("  Out Bitrate:["); 
    		
    		for (int i=0; i < bitrates.length; i++) {
    			// Get the filename and see if it exists.
    			String filename = CONFIG_ARCHIVE_PATH + clientInfoId + "/inout/" + channelNo + "/" + out_video_info_id + "/" + bitrates[i] + "/" + bitrates[i] + ".m3u8";
    			if (!Files.exists(Paths.get(filename))) {
    				outVideo[i] = null;
    				continue;
    			}
    			
            	sBufferOut.append(" " + bitrates[i]);
    			
    			// Load in the out video.
    			outVideo[i] = new M3U8ChildPlaylist(clientInfoId, channelNo);
    			outVideo[i].setFilename(filename);
    			outVideo[i].read();
    			outVideo[i].updateTSInfoPath("/video/clients/" + clientInfoId + "/inout/" + channelNo + "/" + out_video_info_id + "/" + bitrates[i] + "/");
    			
    		}
    		sBufferOut.append("]");
    		
    	} else {
    		for (int i=0; i < outVideo.length; i++)
    			outVideo[i] = null;
    	}

    	LogSystem.getInstance().printInfo(clientInfoId, channelNo, formatMessageForLog(jobIdNumber, sBufferOut.toString()));
    }
    
	private String formatMessageForLog(String jobIdNumber, String message) {
		return "Job [" + jobIdNumber + "] " + message; 
	}
}
