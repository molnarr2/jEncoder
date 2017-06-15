package jEncoder.media;

import org.json.simple.JSONObject;

public class ProcessParameters {
	
	private static final String RECORDING_PATH = "/opt/recordings/";
	private static final String ARCHIVE_PATH = "/opt/archive/clients/";
	private static final String BACKUP_PATH = "/opt/recordings_backup/";
	private static final String DOWNLOAD_PATH = "/opt/downloads/";
	
	/** PRERECORDING: The file needs to be processed so that it can be viewed in recording. 
	 * RECORDING: The file is from the recording path.
	 *  ARCHIVE: The file is from the archive path.
	 *  IN: The file for an "in" video. Ie the video that is played before the archived video is played.
	 *  OUT: The file for an "out" video. Ie the video that is played after the archived video is played.
	 *  DOWNLOAD: The file is from the archive path and needs to be converted back to an .mp4 file to be downloadable.
	 * */
	public enum COMMAND {
			RECORDING(1), ARCHIVE(2), IN(3), OUT(4), DOWNLOAD(5);
			private int value;    
			private COMMAND(int value) { this.value = value; }
			public int getValue() { return value; }
		};
		
	/** ENCODE: Encode the in/out video.
	 * REFRESH: Refresh all videos and turn off this in/out video.
	 * */
	public enum COMMAND_INOUT {
		ENCODE, REFRESH
	}
	
	/** ClientInfo.client_info_id. */
	public int cid;
	/** ChannelInfo.channel_no. */
	public int cno;
	/** Command type. */
	public COMMAND cmd;
	/** Command type for an in/out video. */
	COMMAND_INOUT cmdInOut;
	/** VideoInfo.video_info_id. */
	public int viid;
	/** InOutVideoInfo.in_out_video_info_id. */
	public int ioviid;
	/** DownloadHistory.download_history_id. */
	public int dhid;
	/** MP3 title. */
	public String mp3_title;
	/** MP3 artist. */
	public String mp3_artist;
	/** MP3 album. */
	public String mp3_album;
	/** Clipping start time in seconds. */
	public int clip_start_seconds;
	/** Clipping end time in seconds. 0=to the end of the video. */
	public int clip_end_seconds;
	/** Relative input filename. */
	public String filename;
	/** Relative output filename. Currently ARCHIVE videos are the only one that uses this. */
	public String outputFilename = "";
	/** If a recorded file should be forced to re-encode. Useful for uploaded videos should be re-encoded. */
	public boolean force_reencoding;
	/** an Unique Job ID number assigned to this. */
	public String jobIdNumber;
	
	
	private ProcessParameters(String filename) {
		// The filename needs to be filtered out. Only numbers, letters, dot, underscore are valid.
		this.filename = filename.replaceAll("[^A-Za-z0-9\\._]", "");
	}
	
	private ProcessParameters(String filename, String outputFilename) {
		// The filename needs to be filtered out. Only numbers, letters, dot, underscore are valid.
		this.filename = filename.replaceAll("[^A-Za-z0-9\\._]", "");
		this.outputFilename = outputFilename.replaceAll("[^A-Za-z0-9\\._]", "");
	}
	
	/** This will create a new ProcessParameters
	 * */
	public static ProcessParameters newProcessParametersUsingJSON(JSONObject obj, String jobIdNumber) {
		
		// Can be New, Archive, In, Out.
		String type = (String)obj.get("type");
		
		if (type.equals("New") || type.equals("Archive")) {
			Integer videoInfoId = Integer.parseInt((String)obj.get("video_info_id"));
			Integer clientInfoId = Integer.parseInt((String)obj.get("client_info_id"));
			Integer channelNo = Integer.parseInt((String)obj.get("channel_no"));
			String inputFileName = (String)obj.get("input_filename");
			String outputFileName = (String)obj.get("output_filename");
			Integer clipStartPos = Integer.parseInt((String)obj.get("clip_start_pos"));
			Integer clipEndPos = Integer.parseInt((String)obj.get("clip_end_pos"));
			String mp3_title = (String)obj.get("mp3_title");
			String mp3_artist = (String)obj.get("mp3_artist");
			String mp3_album = (String)obj.get("mp3_album");
			
			String forceReencoding = (String)obj.get("force_reencode");
			boolean force_reencoding = false;
			if (forceReencoding.equals("Y"))
				force_reencoding = true;
			
			if (type.equals("New")) {
				return newRecording(clientInfoId, channelNo, videoInfoId, mp3_title, mp3_artist, mp3_album, clipStartPos, clipEndPos, inputFileName, outputFileName, force_reencoding, jobIdNumber);
			} else {
				return newArchive(clientInfoId, channelNo, videoInfoId, mp3_title, mp3_artist, mp3_album, clipStartPos, clipEndPos, inputFileName, outputFileName, force_reencoding, jobIdNumber);				
			}
		} else if (type.equals("In") || type.equals("Out")) {
			Integer inOutVideoInfoId = Integer.parseInt((String)obj.get("in_out_video_info_id"));
			Integer clientInfoId = Integer.parseInt((String)obj.get("client_info_id"));
			Integer channelNo = Integer.parseInt((String)obj.get("channel_no"));
			String cmd = (String)obj.get("cmd");
			String filename = (String)obj.get("input_filename");
			
			if (type.equals("In"))
				return newIn(clientInfoId, channelNo, inOutVideoInfoId, filename, cmd, jobIdNumber);
			else
				return newOut(clientInfoId, channelNo, inOutVideoInfoId, filename, cmd, jobIdNumber);
		} else if (type.equals("Download")) {
			Integer downloadhistoryId = Integer.parseInt((String)obj.get("download_history_id"));
			Integer clientInfoId = Integer.parseInt((String)obj.get("client_info_id"));
			Integer channelNo = Integer.parseInt((String)obj.get("channel_no"));
			String inputFileName = (String)obj.get("input_filename");

			return newDownload(clientInfoId, channelNo, downloadhistoryId, inputFileName, jobIdNumber);
		}
		
		return null;
	}

	public static ProcessParameters newDownload(int cid, int cno, int dhid, String filename, String jobIdNumber) {
		ProcessParameters pp = new ProcessParameters(filename, filename);
		pp.cmd = COMMAND.DOWNLOAD;
		pp.dhid = dhid;
		pp.cid = cid;
		pp.cno = cno;
		pp.jobIdNumber = jobIdNumber;
		return pp;		
	}
	
	/** This will create a ProcessParameter for a recording file.
	 * */
	public static ProcessParameters newRecording(int cid, int cno, int viid, String mp3_title, String mp3_artist, String mp3_album, int clip_start_seconds, int clip_end_seconds, String filename, String outputFilename, boolean force_reencoding, String jobIdNumber) {
		ProcessParameters pp = new ProcessParameters(filename, outputFilename);
		pp.cmd = COMMAND.RECORDING;		
		pp.cid = cid;
		pp.cno = cno;
		pp.viid = viid;
		pp.mp3_title = mp3_title;
		pp.mp3_artist = mp3_artist;
		pp.mp3_album = mp3_album;
		pp.clip_start_seconds = clip_start_seconds;		
		pp.clip_end_seconds = clip_end_seconds;		
		pp.jobIdNumber = jobIdNumber;
		pp.force_reencoding = force_reencoding;		
		return pp;
	}
	
	/** This will create a ProcessParameter for an archive file.
	 * */
	public static ProcessParameters newArchive(int cid, int cno, int viid, String mp3_title, String mp3_artist, String mp3_album, int clip_start_seconds, int clip_end_seconds, String filename, String outputFilename, boolean force_reencoding, String jobIdNumber) {
		ProcessParameters pp = new ProcessParameters(filename, outputFilename);
		pp.cmd = COMMAND.ARCHIVE;
		pp.cid = cid;
		pp.cno = cno;
		pp.viid = viid;
		pp.mp3_title = mp3_title;
		pp.mp3_artist = mp3_artist;
		pp.mp3_album = mp3_album;
		pp.clip_start_seconds = clip_start_seconds;		
		pp.clip_end_seconds = clip_end_seconds;		
		pp.jobIdNumber = jobIdNumber;
		pp.force_reencoding = force_reencoding;
		return pp;
	}
	
	/** This will create a ProcessParameter for an in video.
	 * */
	public static ProcessParameters newIn(int cid, int cno, int ioviid, String filename, String cmdInOut, String jobIdNumber) {
		ProcessParameters pp = new ProcessParameters(filename);
		pp.cmd = COMMAND.IN;
		pp.cmdInOut = stringToCommandInOut(cmdInOut);
		pp.cid = cid;
		pp.cno = cno;
		pp.ioviid = ioviid;
		pp.jobIdNumber = jobIdNumber;
		return pp;
	}
	
	/** This will create a ProcessParameter for an out video.
	 * */
	public static ProcessParameters newOut(int cid, int cno, int ioviid, String filename, String cmdInOut, String jobIdNumber) {
		ProcessParameters pp = new ProcessParameters(filename);
		pp.cmd = COMMAND.OUT;		
		pp.cmdInOut = stringToCommandInOut(cmdInOut);
		pp.cid = cid;
		pp.cno = cno;
		pp.ioviid = ioviid;
		pp.jobIdNumber = jobIdNumber;
		return pp;
	}
	
	/** Convert string to in/out video command.
	 * */
	private static COMMAND_INOUT stringToCommandInOut(String cmdInOut) {
		if ("Encode".equals(cmdInOut))
			return COMMAND_INOUT.ENCODE;
		else if ("Refresh".equals(cmdInOut))
			return COMMAND_INOUT.REFRESH;
		return null;
	}
	
	/** @return the absolute file path to the file.
	 * */
	public String getAbsoluteFilePath() {
		if (cmd == COMMAND.RECORDING || cmd == COMMAND.IN || cmd == COMMAND.OUT)
			return RECORDING_PATH + cid + "/" + filename;
		else if (cmd == COMMAND.ARCHIVE || cmd == COMMAND.DOWNLOAD)
			return ARCHIVE_PATH + cid + "/" + filename;
		
		return "";
	}

	/** @return the absolute file path to the file from the backup.
	 * */
	public String getAbsoluteFilePathBackup() {
		return BACKUP_PATH + filename;					
	}
	
	public String getAbsoluteFilePathNoExtensions() {
		String extensionRemoved = filename.split("\\.")[0];
		if (cmd == COMMAND.RECORDING || cmd == COMMAND.IN || cmd == COMMAND.OUT)
			return RECORDING_PATH + cid + "/" + extensionRemoved;
		else if (cmd == COMMAND.ARCHIVE || cmd == COMMAND.DOWNLOAD)
			return ARCHIVE_PATH + cid + "/" + extensionRemoved;
		
		return "";
	}
	
	/** @return the absolute file path with no extensions for output.
	 * */
	public String getAbsoluteFilePathNoExtensionsForOutput() {
		String extensionRemoved = outputFilename.split("\\.")[0];			
		
		if (cmd == COMMAND.RECORDING)
			return ARCHIVE_PATH + cid + "/" + extensionRemoved;
		else if (cmd == COMMAND.ARCHIVE) {
			return ARCHIVE_PATH + cid + "/" + extensionRemoved;
		} else if (cmd == COMMAND.IN)
			return ARCHIVE_PATH + cid + "/inout/" + cno + "/" + ioviid;
		else if (cmd == COMMAND.OUT)
			return ARCHIVE_PATH + cid + "/inout/" + cno + "/" + ioviid;
		else if (cmd == COMMAND.DOWNLOAD)
			return DOWNLOAD_PATH;
		
		return "";
	}
	
	public int getDurationSeconds() {
		return clip_end_seconds - clip_start_seconds;
	}
	
	public String toString() {
		if (cmd == COMMAND.ARCHIVE || cmd == COMMAND.RECORDING)		
			return "{ProcessParameters: cid:[" + cid + "] cno:[" + cno + "] cmd:[" + cmd + "] viid:[" + viid + "] mp3_title:[" + mp3_title + "] mp3_artist:[" + mp3_artist 
				+ "] mp3_album:[" + mp3_album + "] clip_start_seconds:[" + clip_start_seconds + "] clip_end_seconds:[" + clip_end_seconds + "] filename:[" + filename 
				+ "] outputFilename:[" + outputFilename + "] force_reencoding:[" + force_reencoding + "] jobIdNumber:[" + jobIdNumber + "]}";
		else if (cmd == COMMAND.IN || cmd == COMMAND.OUT)
			return "{ProcessParameters: cid:[" + cid + "] cno:[" + cno + "] cmd:[" + cmd + "] cmdInOut:[" + cmdInOut + "] ioviid:[" + ioviid + "] filename:[" + filename + "] jobIdNumber:[" + jobIdNumber + "]}";
		else if (cmd == COMMAND.DOWNLOAD)
			return "{ProcessParameters: cid:[" + cid + "] cno:[" + cno + "] cmd:[" + cmd + "] dhid:[" + dhid + "] filename:[" + filename + "] jobIdNumber:[" + jobIdNumber + "]}";
		return "{ProcessParameters UNKNOWN}";
	}
}		
