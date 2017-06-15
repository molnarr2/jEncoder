package jEncoder.media;

import jEncoder.apple_hls.M3U8ChildPlaylist;
import jEncoder.util.LogSystem;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

public class FFmpeg {
    public static final String FFMPEG_PATH = "/opt/gomedia/bin/programs/ffmpeg";

    // Client information.
    int clientId;
    int channelNo;
    
    /** This is the input file into FFMPEG. */
    public String inputFile;
    
    /** The job ID number. */
    public String jobIdNumber;
    
    /** @param inputFile is the absolute path to file that will be processed.
     * */
    public FFmpeg(int clientId, int channelNo, String inputFile, String jobIdNumber) {
    	this.clientId = clientId;
    	this.channelNo = channelNo;
    	this.inputFile = inputFile;
    	this.jobIdNumber = jobIdNumber;
    }

	private String formatMessageForLog(String message) {
		return "Job [" + jobIdNumber + "] " + message; 
	}

	/** @param outputMp4 is the output MP4 file.
	 * */
	public void encodeMP4(String outputMp4) {
    	CommandLine cmdLine = new CommandLine(FFMPEG_PATH);

    	cmdLine.addArgument("-analyzeduration");
    	cmdLine.addArgument("2147483647");

    	cmdLine.addArgument("-probesize");
    	cmdLine.addArgument("2147483647");
    	
    	cmdLine.addArgument("-n");
    	cmdLine.addArgument("-loglevel");
    	cmdLine.addArgument("quiet");

    	// Set the input file.
    	cmdLine.addArgument("-i");
    	cmdLine.addArgument(inputFile);
    	cmdLine.addArgument("-threads");
    	cmdLine.addArgument("3");
    	
		cmdLine.addArgument("-codec");
		cmdLine.addArgument("copy");
		
		cmdLine.addArgument("-bsf:a");
		cmdLine.addArgument("aac_adtstoasc");
		 
    	cmdLine.addArgument(outputMp4);

    	LogSystem.getInstance().printInfo(clientId, channelNo, formatMessageForLog("encodeMP4 cmd:[" + cmdLine + "] "));
    	
		// Set up the streams to get the out/err from the executable.
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		ByteArrayOutputStream errStream = new ByteArrayOutputStream();
		PumpStreamHandler streamHandler = new PumpStreamHandler(outStream, errStream);

    	DefaultExecutor executor = new DefaultExecutor();
    	executor.setStreamHandler(streamHandler);
    	int exitValues[] = {0,1};
    	executor.setExitValues(exitValues);

    	try {
    		executor.execute(cmdLine);
    	} catch (Exception e) {    		
    		// Executing the FFMPEG it will output error messages for things we don't care about. Just verify that the duration times.
    		    		  
    		// If the file does not exist than error or if durations do not match.
    		if (!Files.exists(Paths.get(outputMp4))) {
    			LogSystem.getInstance().printError(clientId, channelNo, formatMessageForLog("Output file does not exist."), e);
    		}
    	}
	}

	/** @param copyOnly is true if only copy the audio to the MP3 file.
	 * */
    public void encodeMP3(String outputFileMP3, int startingPositionSeconds, int durationSeconds, String title, String artist, String album, boolean copyOnly) {
    	CommandLine cmdLine = new CommandLine(FFMPEG_PATH);

    	cmdLine.addArgument("-analyzeduration");
    	cmdLine.addArgument("2147483647");

    	cmdLine.addArgument("-probesize");
    	cmdLine.addArgument("2147483647");
    	
    	cmdLine.addArgument("-n");
    ///	cmdLine.addArgument("-loglevel");
    //	cmdLine.addArgument("quiet");

    	// Set the start position.
    	if (startingPositionSeconds > 0) {
    		cmdLine.addArgument("-ss");
    		cmdLine.addArgument(secondsToFFmpeg(startingPositionSeconds));
    	}

    	// Set the input file.
    	cmdLine.addArgument("-i");
    	cmdLine.addArgument(inputFile);
    	cmdLine.addArgument("-threads");
    	cmdLine.addArgument("3");

    	cmdLine.addArgument("-metadata");
    	cmdLine.addArgument("title=" + title, false);
    	cmdLine.addArgument("-metadata");
    	cmdLine.addArgument("artist=" + artist, false);
    	cmdLine.addArgument("-metadata");
    	cmdLine.addArgument("album=" + album, false);
    	cmdLine.addArgument("-metadata");
    	cmdLine.addArgument("genre=Podcast", false);

    	// Set the duration.
    	if (durationSeconds > 0) {
    		cmdLine.addArgument("-t");
    		cmdLine.addArgument(secondsToFFmpeg(durationSeconds));
    	}
    	
    	if (copyOnly) {
	    	cmdLine.addArgument("-acodec");
	    	cmdLine.addArgument("copy");    
	    	cmdLine.addArgument("-vn");
	    	
    	} else {    	
	    	cmdLine.addArgument("-acodec");
	    	cmdLine.addArgument("libmp3lame");
	    	cmdLine.addArgument("-vn");
	    	cmdLine.addArgument("-ar");
	    	cmdLine.addArgument("22050");
	    	cmdLine.addArgument("-ab");
	    	cmdLine.addArgument("64k");
	    	cmdLine.addArgument("-ac");
	    	cmdLine.addArgument("1");
    	}
    	
    	cmdLine.addArgument(outputFileMP3);

    	LogSystem.getInstance().printInfo(clientId, channelNo, formatMessageForLog("encodeMP3 cmd:[" + cmdLine + "] "));
    	
		// Set up the streams to get the out/err from the executable.
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		ByteArrayOutputStream errStream = new ByteArrayOutputStream();
		PumpStreamHandler streamHandler = new PumpStreamHandler(outStream, errStream);

    	DefaultExecutor executor = new DefaultExecutor();
    	executor.setStreamHandler(streamHandler);
    	int exitValues[] = {0,1};
    	executor.setExitValues(exitValues);

    	try {
    		executor.execute(cmdLine);
    	} catch (Exception e) {    		
    		// Executing the FFMPEG it will output error messages for things we don't care about. Just verify that the duration times.
    		    		
    		// Get the encoding duration.
    		int newEncodingDuration = durationSeconds;
    		if (newEncodingDuration == 0) {
    			newEncodingDuration = getDuration();
    		}
  
    		// If the file does not exist than error or if durations do not match.
    		if (!Files.exists(Paths.get(outputFileMP3))) {
    			LogSystem.getInstance().printError(clientId, channelNo, formatMessageForLog("Output file does not exist."), e);
    		} else {
	    		FFmpeg ffmpeg = new FFmpeg(clientId, channelNo, outputFileMP3, jobIdNumber);
	    		int mp3Duration = ffmpeg.getDuration();
	  
	    		// Since there is a variance of more than 5 seconds.
	    		if (Math.abs(mp3Duration - newEncodingDuration) > 5.0) {
	    			LogSystem.getInstance().printError(clientId, channelNo, formatMessageForLog("MP3 Encoding error."), e);
	    		}    	
    		}
    	}

//    	System.out.println("OUTSTREAM: " + outStream.toString());
//    	System.out.println("ERRSTREAM: " + errStream.toString());

    }
    
    /** This will perform an encoding of the .m3u8 and .ts files for the inputFile.
     * @param outputM3U8File is the .m3u8 file that will be created. The directory does not have to exist as this will create the directories.
	 * @param startingPositionSeconds the starting position in seconds to be encoding at.
 	 * @param durationSeconds the duration in seconds the video should be cropped to. 0 = use full video.
 	 * @param reEncodeBitRate is the video bitrate it should be encoded at, ex: 500000. 0 = copy video over.
	 * @param audio64kOnly is true if should encode audio only and at 64k.
	 * @param textTag is a label for this encoding in the log file.
     * */
    public void encodeM3U8(String outputFileM3U8, int startingPositionSeconds, int durationSeconds, int reEncodeBitRate, boolean audio64kOnly, String textTag) {
    	// Create the parent directories if need be.
    	File parentFilePath = new File(outputFileM3U8).getParentFile();
    	    
    	// Make sure parent directories are created.
    	parentFilePath.mkdirs();
    	
    	// The ts files wild card.
    	String tsFiles = parentFilePath.toString() + "/%04d.ts";
    	
    	// WARNING: ffmpeg options are dependant on where they are placed before or after the -i <input_file> command.
    	CommandLine cmdLine = new CommandLine(FFMPEG_PATH);
    	
    	cmdLine.addArgument("-analyzeduration");
    	cmdLine.addArgument("2147483647");

    	cmdLine.addArgument("-probesize");
    	cmdLine.addArgument("2147483647");
    	
    	cmdLine.addArgument("-n");
    	cmdLine.addArgument("-loglevel");
    	cmdLine.addArgument("quiet");
	   	 
	  	// Set the start position.
	  	if (startingPositionSeconds > 0) {
	  		cmdLine.addArgument("-ss");
	  		cmdLine.addArgument(secondsToFFmpeg(startingPositionSeconds));
	  	}

    	// Set the duration.
    	if (durationSeconds > 0) {
    		cmdLine.addArgument("-t");
    		cmdLine.addArgument(secondsToFFmpeg(durationSeconds));
    	}

    	// Set the input file.
    	cmdLine.addArgument("-i");
    	cmdLine.addArgument(inputFile);
    	
    	cmdLine.addArgument("-threads");
    	cmdLine.addArgument("2");
    	
    	// encoding commands.
    	if (audio64kOnly) {
    		cmdLine.addArgument("-f");
    		cmdLine.addArgument("segment");
    		cmdLine.addArgument("-vn");
    		cmdLine.addArgument("-acodec");
    		
    		if (reEncodeBitRate > 0) {        		
        		cmdLine.addArgument("libfdk_aac");
        		cmdLine.addArgument("-b:a");
        		cmdLine.addArgument("56k");    			
    		} else {
        		cmdLine.addArgument("copy");    			
    		}
    		
    	} else if (reEncodeBitRate > 0) {
    		reEncodeBitRate /= 1000;
    		cmdLine.addArgument("-f");
    		cmdLine.addArgument("ssegment");
    		cmdLine.addArgument("-vcodec");
    		cmdLine.addArgument("libx264");
    		cmdLine.addArgument("-acodec");
    		cmdLine.addArgument("libfdk_aac");
    		cmdLine.addArgument("-b:v");
    		cmdLine.addArgument("" + reEncodeBitRate + "k");
    		cmdLine.addArgument("-profile:v");
    		cmdLine.addArgument("main");
    		cmdLine.addArgument("-level");
    		cmdLine.addArgument("3.1");
        	cmdLine.addArgument("-force_key_frames");
        	cmdLine.addArgument("expr:gte(t,n_forced*2)");    	
    		
    	} else {
    		cmdLine.addArgument("-f");
    		cmdLine.addArgument("segment");
    		cmdLine.addArgument("-codec");
    		cmdLine.addArgument("copy");
    		cmdLine.addArgument("-bsf:v");
    		cmdLine.addArgument("h264_mp4toannexb");
    	}
    	
    	// map and output settings. 
    	
    	LinkedList<String> ltChannels = getFirstVideoAudioStreams();
    	for (String channel : ltChannels) {
        	cmdLine.addArgument("-map");
        	cmdLine.addArgument(channel);
    	}
    	
    	cmdLine.addArgument("-segment_list");
    	cmdLine.addArgument(outputFileM3U8);
    	cmdLine.addArgument("-segment_time");
    	cmdLine.addArgument("10");
    	cmdLine.addArgument(tsFiles);
    	
    	LogSystem.getInstance().printInfo(clientId, channelNo, formatMessageForLog("encodeM3U8 [" + textTag + "] cmd:[" + cmdLine + "] "));

		// Set up the streams to get the out/err from the executable.
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		ByteArrayOutputStream errStream = new ByteArrayOutputStream();
		PumpStreamHandler streamHandler = new PumpStreamHandler(outStream, errStream);

    	DefaultExecutor executor = new DefaultExecutor();
    	executor.setStreamHandler(streamHandler);
    	int exitValues[] = {0,1};
    	executor.setExitValues(exitValues);

    	try {
    		executor.execute(cmdLine);
    	} catch (Exception e) {
    		// Executing the FFMPEG it will output error messages for things we don't care about. Just verify that it created the .ts files.
    		    		
    		// Get the encoding duration.
    		int newEncodingDuration = durationSeconds;
    		if (newEncodingDuration == 0) {
    			newEncodingDuration = getDuration();
    		}
    		
    		// Get the last Ts file that is generated.
    		int totalTSfiles = newEncodingDuration / 10;
    		String lastTsFile = parentFilePath.toString() + "/" + String.format("%04d", totalTSfiles) + ".ts"; 
     
    		if (!Files.exists(Paths.get(lastTsFile))) {
    			LogSystem.getInstance().printError(clientId, channelNo, formatMessageForLog("encodeM3U8 [" + textTag + "] Encoding error. "), e);
    		}    		
    	}
    	
    	// Remove the full path from the .m3u8 file.
    	M3U8ChildPlaylist m3u8 = new M3U8ChildPlaylist(clientId, channelNo);
    	m3u8.setFilename(outputFileM3U8);
    	m3u8.read();
    	m3u8.removeDirectories();
    	
    	// Set duration to 10 seconds.
    	if (reEncodeBitRate > 0) {
    		m3u8.setDurationTimeTo10Seconds();
    	}
    	
    	m3u8.write();
    }
    
    /** This will convert seconds to ffmpeg formatted seconds.
     * @param seconds is the number of seconds.
     * @return ffmpeg formatted seconds. Example: 65 seconds = "00:01:05.0"
     * */
    public String secondsToFFmpeg(int seconds) {
    	int hour = seconds / 3600;
    	int leftOvers = seconds - (hour * 3600);
    	
    	int min = leftOvers / 60;
    	int sec = leftOvers - (min * 60);
    	
    	return String.format("%02d:%02d:%02d.0", hour, min, sec);
    }

    /** This will get the duration from the input file.  
     * @return duration in seconds for the input file.
     * */
    public int getDuration() {
    	String theInputFile = this.inputFile;
    	
		// It is an m3u8 file.
		String last4Characters = inputFile.substring(inputFile.length() - 4); 
		if (last4Characters.equals("m3u8")) {
			theInputFile = Paths.get(inputFile).getParent().toString() + "/0000.ts";
		} 	
    	
    	Map<String, Object> map = new HashMap<String, Object>();
    	map.put("file", new File(theInputFile));
    	
    	CommandLine cmdLine = new CommandLine(FFMPEG_PATH);

    	cmdLine.addArgument("-analyzeduration");
    	cmdLine.addArgument("2147483647");

    	cmdLine.addArgument("-probesize");
    	cmdLine.addArgument("2147483647");
    	
    	cmdLine.addArgument("-i");
    	cmdLine.addArgument("${file}");
    	cmdLine.setSubstitutionMap(map);
    	    	
    	// Set up the streams to get the out/err from the executable.
    	ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    	ByteArrayOutputStream errStream = new ByteArrayOutputStream();
    	PumpStreamHandler streamHandler = new PumpStreamHandler(outStream, errStream);
    	
    	DefaultExecutor executor = new DefaultExecutor();
    	executor.setStreamHandler(streamHandler);
    	int exitValues[] = {0,1};
    	executor.setExitValues(exitValues);
    	
    	try {
    		executor.execute(cmdLine);
    	} catch (Exception e) {
    		// Do Nothing.
    		// e.printStackTrace();
    	}
    	
    	try {
	    	BufferedReader rdr = new BufferedReader(new StringReader(errStream.toString()));
	    	for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {
	    		
	    		// The line that has the Duration is what we are interested in.
	    		if (line.indexOf("Duration") > -1) {
	    			
	    			//   Duration: 01:40:24.05, start: -7.105011, bitrate: 2064 kb/s
	    			int start = line.indexOf(":");
	    			int end = line.indexOf(",");
	    			String []tokens = line.substring(start+2, end).split(":");
	    			
	    			// Convert it to seconds.
	    			int hours = Integer.parseInt(tokens[0]);
	    			int minutes = Integer.parseInt(tokens[1]);
	    			float seconds = Float.parseFloat(tokens[2]);	    			
	    			return hours * (60 * 60) + minutes * 60 + (int)seconds;
	    		}
	    		
	    	}
	    	rdr.close();
    	} catch (Exception e) {
			LogSystem.getInstance().printError(clientId, channelNo, formatMessageForLog("Get duration error. cmdLine: [" + cmdLine + "] "), e);
    	}
    	
    	// lines now contains all the strings between line breaks
    	
    	return 0;
    }
    
    /** This will get the bitrate from the input file. 
     * @return bitrate in raw bits, ie, will return 500000 for a 500k bitrate.
     * */
    public int getBitrate() {
    	String theInputFile = this.inputFile;
    	
		// It is an m3u8 file.
		String last4Characters = inputFile.substring(inputFile.length() - 4); 
		if (last4Characters.equals("m3u8")) {
			theInputFile = Paths.get(inputFile).getParent().toString() + "/0000.ts";
		} 	
    	
    	Map<String, Object> map = new HashMap<String, Object>();
    	map.put("file", new File(theInputFile));
    	
    	CommandLine cmdLine = new CommandLine(FFMPEG_PATH);

    	cmdLine.addArgument("-analyzeduration");
    	cmdLine.addArgument("2147483647");

    	cmdLine.addArgument("-probesize");
    	cmdLine.addArgument("2147483647");
    	
    	cmdLine.addArgument("-i");
    	cmdLine.addArgument("${file}");
    	cmdLine.setSubstitutionMap(map);
    	    	
    	// Set up the streams to get the out/err from the executable.
    	ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    	ByteArrayOutputStream errStream = new ByteArrayOutputStream();
    	PumpStreamHandler streamHandler = new PumpStreamHandler(outStream, errStream);
    	
    	DefaultExecutor executor = new DefaultExecutor();
    	executor.setStreamHandler(streamHandler);
    	int exitValues[] = {0,1};
    	executor.setExitValues(exitValues);

    	try {
    		executor.execute(cmdLine);
    	} catch (Exception e) {
    		// Do nothing.
    		// e.printStackTrace();
    	}

    	try {
	    	BufferedReader rdr = new BufferedReader(new StringReader(errStream.toString()));
	    	for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {	    		
	    		if (line.indexOf("Duration") > -1) {
	    			// Duration: 01:40:24.05, start: -7.105011, bitrate: 2064 kb/s
	    			int start = line.lastIndexOf(":");
	    			String []tokens = line.substring(start+2).split(" ");
	    			int bitrate = Integer.parseInt(tokens[0]);
	    			return bitrate * 1000;
	    		}
	    		
	    	}
	    	rdr.close();
    	} catch (Exception e) {
			LogSystem.getInstance().printError(clientId, channelNo, formatMessageForLog("Get bitrate error. cmdLine: [" + cmdLine + "] "), e);
    	}
    	
    	return 0;
    }
    
    /** This will get the first video and audio streams from the video file. 
     * @return Linkedlist of strings of the first video and audio streams. Example: 0:0, 0:1
     * */
    public LinkedList<String> getFirstVideoAudioStreams() {
    	LinkedList<String> ltChannels = new LinkedList<String>();
    	
    	String theInputFile = this.inputFile;
    	
    	/* Input #0, mov,mp4,m4a,3gp,3g2,mj2, from '/opt/gomedia/media/recordings/17/17_1_U201592085447.mp4':
  Metadata:
    major_brand     : qt
    minor_version   : 537199360
    compatible_brands: qt
    creation_time   : 2015-10-19 18:50:02
  Duration: 01:05:59.29, start: 0.000000, bitrate: 356 kb/s
    Stream #0:0(eng): Audio: aac (mp4a / 0x6134706D), 32000 Hz, stereo, fltp, 78 kb/s
    Metadata:
      creation_time   : 2015-10-19 18:50:02
      handler_name    : Apple Alias Data Handler
    Stream #0:1(eng): Video: h264 (Main) (avc1 / 0x31637661), yuv420p, 420x280, 230 kb/s, 29.97 fps, 29.97 tbr, 2997 tbn, 5994 tbc
    Metadata:
      creation_time   : 2015-10-19 18:50:02
      handler_name    : Apple Alias Data Handler
    Stream #0:2(eng): Data: none (rtp  / 0x20707472), 32 kb/s
    Metadata:
      creation_time   : 2015-10-19 18:50:02
      handler_name    : Apple Alias Data Handler
    Stream #0:3(eng): Data: none (rtp  / 0x20707472), 6 kb/s
    Metadata:
      creation_time   : 2015-10-19 18:50:02
      handler_name    : Apple Alias Data Handler. */    	
    	
		// It is an m3u8 file.
		String last4Characters = inputFile.substring(inputFile.length() - 4); 
		if (last4Characters.equals("m3u8")) {
			theInputFile = Paths.get(inputFile).getParent().toString() + "/0000.ts";
		} 	
    	
    	Map<String, Object> map = new HashMap<String, Object>();
    	map.put("file", new File(theInputFile));
    	
    	CommandLine cmdLine = new CommandLine(FFMPEG_PATH);

    	cmdLine.addArgument("-analyzeduration");
    	cmdLine.addArgument("2147483647");

    	cmdLine.addArgument("-probesize");
    	cmdLine.addArgument("2147483647");
    	
    	cmdLine.addArgument("-i");
    	cmdLine.addArgument("${file}");
    	cmdLine.setSubstitutionMap(map);
    	    	
    	// Set up the streams to get the out/err from the executable.
    	ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    	ByteArrayOutputStream errStream = new ByteArrayOutputStream();
    	PumpStreamHandler streamHandler = new PumpStreamHandler(outStream, errStream);
    	
    	DefaultExecutor executor = new DefaultExecutor();
    	executor.setStreamHandler(streamHandler);
    	int exitValues[] = {0,1};
    	executor.setExitValues(exitValues);

    	try {
    		executor.execute(cmdLine);
    	} catch (Exception e) {
    		// Do nothing.
    		// e.printStackTrace();
    	}

    	// Get output of ffmpeg and parse the data.
    	try {
    		boolean bFoundVideo = false;
    		boolean bFoundAudio = false;
	    	BufferedReader rdr = new BufferedReader(new StringReader(errStream.toString()));
	    	for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {
	    		// The line that has the stream for the Video has been found.
	    		if (!bFoundVideo && line.indexOf("Stream") > -1 && line.indexOf("Video") > -1) {
	    			bFoundVideo = true;

	    			// Add the video channel in.
	    			int channelNo = stream2ChannelNo(line);
	    			ltChannels.add("0:" + channelNo);
	    		}

	    		// The line that has the stream for the Audio has been found.
	    		if (!bFoundAudio && line.indexOf("Stream") > -1 && line.indexOf("Audio") > -1) {
	    			bFoundAudio = true;

	    			// Add the audio channel in.
	    			int channelNo = stream2ChannelNo(line);
	    			ltChannels.add("0:" + channelNo);
	    		}
	    	}
	    	rdr.close();
    	} catch (Exception e) {
			LogSystem.getInstance().printError(clientId, channelNo, formatMessageForLog("Get first audio stream error. cmdLine: [" + cmdLine + "] "), e);
    	}
    	
    	return ltChannels;
    }

    /** This will retrieve the stream's channel no.
     * */
    private int stream2ChannelNo(String line) {
		// Example of this token "#0:0(eng)"
		//    Stream #0:0[0x100]: Video: h264 (Main) ([27][0][0][0] / 0x001B), yuv420p, 640x360, 30 fps, 30 tbr, 90k tbn, 60 tbc
		//    Stream #0:1(eng): Video: h264 (Main) (avc1 / 0x31637661), yuv420p, 420x280, 230 kb/s, 29.97 fps, 29.97 tbr, 2997 tbn, 5994 tbc
		//    Stream #0:1[0x101](und): Audio: aac (LC) ([15][0][0][0] / 0x000F), 44100 Hz, stereo, fltp, 126 kb/s
		//    Stream #0:1: Audio: aac (LC) ([15][0][0][0] / 0x000F), 44100 Hz, stereo, fltp, 126 kb/s

    	// The input will always be 0 for the first number.
		String []tokens = line.split(":");
		String channelString = tokens[1];

		int index = channelString.length();
    	for (int i=0; i < channelString.length(); i++) {
    		if (!Character.isDigit(channelString.charAt(i))) {
    			index = i;
    			break;
    		}
    	}    		
    	
    	String parsed = channelString.substring(0, index);
    	return Integer.parseInt(parsed);
    }
    
}
