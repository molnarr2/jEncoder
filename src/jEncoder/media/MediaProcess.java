package jEncoder.media;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jEncoder.apple_hls.HLSupdater;
import jEncoder.apple_hls.M3U8MasterPlaylist;
import jEncoder.apple_hls.PlaylistInfo;
import jEncoder.util.LogSystem;
import jEncoder.web.Web;

/** This class is used to handle the processing of the media.
 * */
public class MediaProcess {
	
	/** This is the cap on HD bitrates. Any higher and the file will be re-encoded down to this bitrate. */
	private static final int BITRATE_HD_MAX_CAP = 1750000;
	/** This is the cap on SD bitrates. Any higher and the file will be re-encoded at this bitrate for SD. */
	private static final int BITRATE_SD_MAX_CAP = 700000;
	/** This is the cap on LOW bitrates. Any higher and the file will be re-encoded at this bitrate for LOW. */
	private static final int BITRATE_LOW_MAX_CAP = 140000;
	/** This is the path to the export of the recorded file. */
	private static final String EXPORT_PATH = "/opt/export/";
	
	/** The parameters to process.
	 * */
	private ProcessParameters pp;
	
	public MediaProcess(ProcessParameters pp) {
		this.pp = pp;
	}
	
	private String formatMessageForLog(String message) {
		return "Job [" + pp.jobIdNumber + "] " + message; 
	}
	
	/** This will process the passed in parameters.
	 * */
	public void process() {
		try {
			LogSystem.getInstance().printInfo(pp.cid, pp.cno, formatMessageForLog("Encoding job started."));				
			
			if (pp.cmd == ProcessParameters.COMMAND.RECORDING)
				processRecording();
			else if (pp.cmd == ProcessParameters.COMMAND.ARCHIVE)
				processArchive();
			else if (pp.cmd == ProcessParameters.COMMAND.IN || pp.cmd == ProcessParameters.COMMAND.OUT)
				processInOut();
			else if (pp.cmd == ProcessParameters.COMMAND.DOWNLOAD)
				processDownload();
			
			LogSystem.getInstance().printInfo(pp.cid, pp.cno, formatMessageForLog("Encoding job finished."));
		} catch (Exception e) {
			LogSystem.getInstance().printError(formatMessageForLog("Unable to process job."), e);
		}
	}
	
	/** This will convert the archive video file to an mp4 into the download.
	 * */
	private void processDownload() {
		// Determine which m3u8 encoding to use.
		String inputDir = pp.getAbsoluteFilePathNoExtensions();
		String inputM3U8 = "";
										
		// Determine which encoding to use for input.
		if (Files.exists(Paths.get(inputDir + "/HD/HD.m3u8")))
			inputM3U8 = inputDir + "/HD/HD.m3u8";
		else if (Files.exists(Paths.get(inputDir + "/SD/SD.m3u8")))
			inputM3U8 = inputDir + "/SD/SD.m3u8";			
		else if (Files.exists(Paths.get(inputDir + "/LOW/LOW.m3u8")))
			inputM3U8 = inputDir + "/LOW/LOW.m3u8";
		else if (Files.exists(Paths.get(inputDir + "/64k/64k.m3u8")))
			inputM3U8 = inputDir + "/64k/64k.m3u8";

		// Determine which one will be used for audio.
		if ("".equals(inputM3U8)) {
			System.out.println("NO VALID input encoding. There does not exist an HD, SD, A64k, or LOW m3u8 in the archive folder.");
			return;			
		}
		
		LogSystem.getInstance().printInfo(pp.cid, pp.cno, formatMessageForLog("DOWNLOAD using these files for input: [" + inputM3U8 + "]"));
		
		// Output mp4 file name. 
		String outputMp4 = pp.getAbsoluteFilePathNoExtensionsForOutput() + "/" + pp.filename;
		
		FFmpeg ffmpeg = new FFmpeg(pp.cid, pp.cno, inputM3U8, pp.jobIdNumber);
		ffmpeg.encodeMP4(outputMp4);
				
		long fileSize = 0;
		try {fileSize = Files.size(Paths.get(outputMp4));} catch (Exception e) {LogSystem.getInstance().printError(pp.cid, pp.cno, formatMessageForLog("Unable to get mp4 file[" + outputMp4 + "] size."), e);}
		
		// Restorecon the files.
//		Restorecon restorecon = new Restorecon(pp.cid, pp.cno, pp.getAbsoluteFilePathNoExtensionsForOutput(), pp.jobIdNumber);
//		restorecon.run();
		
		// Notify the web server the recording has been processed.
		Web.getInstance().notifyDownloadDone(pp.jobIdNumber, pp.cid, pp.cno, pp.dhid, fileSize);
	}
	
	/** This will process the archive video file. No need to copy the video file to be saved as it should already be saved.
	 * */
	private void processArchive() {
		// Determine which m3u8 encoding to use.
		String inputDir = pp.getAbsoluteFilePathNoExtensions();
		String inputM3U8HD = "";
		String inputM3U8SD = "";
		String inputM3U8LOW = "";
		String inputM3U864k = "";
										
		// Determine which encoding to use for input.
		if (Files.exists(Paths.get(inputDir + "/HD/HD.m3u8")))
			inputM3U8HD = inputDir + "/HD/HD.m3u8";
		if (Files.exists(Paths.get(inputDir + "/SD/SD.m3u8")))
			inputM3U8SD = inputDir + "/SD/SD.m3u8";			
		if (Files.exists(Paths.get(inputDir + "/LOW/LOW.m3u8")))
			inputM3U8LOW = inputDir + "/LOW/LOW.m3u8";
		if (Files.exists(Paths.get(inputDir + "/64k/64k.m3u8")))
			inputM3U864k = inputDir + "/64k/64k.m3u8";

		// Determine which one will be used for audio.
		if ("".equals(inputM3U8SD) && "".equals(inputM3U8HD) && "".equals(inputM3U8LOW) && "".equals(inputM3U864k)) {
			System.out.println("NO VALID input encoding. There does not exist an HD, SD, A64k, or LOW m3u8 in the archive folder.");
			return;			
		}
		
		LogSystem.getInstance().printInfo(pp.cid, pp.cno, formatMessageForLog("ARCHIVE using these files for input: [" + inputM3U8HD + " " + inputM3U8SD + " " + inputM3U8LOW +  " " + inputM3U864k + "]"));
		
		// Output directory. 
		String outputPathDir = pp.getAbsoluteFilePathNoExtensionsForOutput();
		
		// Encode the M3U8 versions.		
		encodeM3U8(inputM3U8HD, inputM3U8SD, inputM3U8LOW, inputM3U864k, outputPathDir);
		
		// Encode the MP3 version.
		String outputFileMP3 = outputPathDir + ".mp3";			
		FFmpeg ffmpeg = new FFmpeg(pp.cid, pp.cno, inputDir + ".mp3", pp.jobIdNumber);
		ffmpeg.encodeMP3(outputFileMP3, pp.clip_start_seconds, pp.getDurationSeconds(), pp.mp3_title, pp.mp3_artist, pp.mp3_album, true);
		long mp3FileSize = 0;
		try {mp3FileSize = Files.size(Paths.get(outputFileMP3));} catch (Exception e) {LogSystem.getInstance().printError(pp.cid, pp.cno, formatMessageForLog("Unable to get mp3 file[" + outputFileMP3 + "] size."), e);}
		ffmpeg = new FFmpeg(pp.cid, pp.cno, outputFileMP3, pp.jobIdNumber);		
		int mp3Duration = ffmpeg.getDuration();
		
		// Generate the M3U8 with in/out videos.
		HLSupdater.getInstance().updateSingleHLS(pp.cid, pp.cno, pp.outputFilename, pp.jobIdNumber);
		
		// Restorecon the files.
		Restorecon restorecon = new Restorecon(pp.cid, pp.cno, outputPathDir, pp.jobIdNumber);
		restorecon.run();
		
		// Notify the web server the recording has been processed.
		Web.getInstance().notifyArchiveVideoDone(pp.jobIdNumber, pp.cid, pp.cno, pp.viid, pp.getDurationSeconds(), mp3Duration, mp3FileSize);
	}
	
	/** This will process the recording video file. The following will be generated:
	 * MP3: MP3 audio file will be generated.
	 * M3U8: An HD (optional), SD (optional), LOW, and audio 64k plus master playlist of them will be generated.
	 * 
	 * - The ProcessParameters.force_reencoding will force a re-encoding of the HD and SD.
	 * - If the bitrate is higher for a given bitrate type (ie, HD, SD...) then it will re-encoded it to the max bitrate for the type.
	 * */
	private void processRecording() {
		// Input file.
		String inputFile = pp.getAbsoluteFilePath();
		
		// Does the file exist?
		if (!Files.exists(Paths.get(inputFile))) {
			LogSystem.getInstance().printError(pp.cid, pp.cno, formatMessageForLog("File does not exist: " + inputFile));
			
			// Since it doesn't exist try the backup recording path.
			inputFile = pp.getAbsoluteFilePathBackup();
						
			if (!Files.exists(Paths.get(inputFile))) {
				LogSystem.getInstance().printError(pp.cid, pp.cno, formatMessageForLog("File does not exist: " + inputFile + ". Unable to archive recording."));
				
				// Notify the web server the file does not exist.
				Web.getInstance().notifyRecordedFileDoesNotExist(pp.jobIdNumber, pp.cid, pp.cno, pp.viid);
				return;
			}			

			LogSystem.getInstance().printError(pp.cid, pp.cno, formatMessageForLog("Using backup file instead: " + inputFile + "."));
		}
		
		// Copy the file over to the export path.
		try {
			String exportFile = EXPORT_PATH + pp.filename;
			Files.copy(Paths.get(inputFile), Paths.get(exportFile));
		} catch (Exception e) {
			LogSystem.getInstance().printError(pp.cid, pp.cno, formatMessageForLog("Unable to copy file."), e);
		}
					
		// Output file.
		String outputFileNoExtension = pp.getAbsoluteFilePathNoExtensionsForOutput();
					
		// Encode the MP3 version.
		String outputFileMP3 = outputFileNoExtension + ".mp3";			
		FFmpeg ffmpeg = new FFmpeg(pp.cid, pp.cno, inputFile, pp.jobIdNumber);
		ffmpeg.encodeMP3(outputFileMP3, pp.clip_start_seconds, pp.getDurationSeconds(), pp.mp3_title, pp.mp3_artist, pp.mp3_album, false);
		long mp3FileSize = 0;
		try {mp3FileSize = Files.size(Paths.get(outputFileMP3));} catch (Exception e) {LogSystem.getInstance().printError(pp.cid, pp.cno, formatMessageForLog("Unable to get mp3 file[" + outputFileMP3 + "] size."), e);}
		ffmpeg = new FFmpeg(pp.cid, pp.cno, outputFileMP3, pp.jobIdNumber);		
		int mp3Duration = ffmpeg.getDuration();

		// Encode the M3U8 version.
		encodeM3U8(inputFile, outputFileNoExtension);
		
		// Generate the M3U8 with in/out videos.
		HLSupdater.getInstance().updateSingleHLS(pp.cid, pp.cno, pp.outputFilename, pp.jobIdNumber);
		
		// Restorecon the files.
		Restorecon restorecon = new Restorecon(pp.cid, pp.cno, outputFileNoExtension, pp.jobIdNumber);
		restorecon.run();
		
		// Notify the web server the recording has been processed.
		Web.getInstance().notifyArchiveVideoDone(pp.jobIdNumber, pp.cid, pp.cno, pp.viid, mp3Duration, mp3Duration, mp3FileSize);
		
		// Delete the recorded file.
		try {
			Path deletePath = Paths.get(pp.getAbsoluteFilePath());
			if (Files.exists(deletePath)) {
				LogSystem.getInstance().printInfo(pp.cid, pp.cno, formatMessageForLog("Deleting the file: " + deletePath.toString()));		
				Files.delete(deletePath);				
			}
		} catch (Exception e) {
			LogSystem.getInstance().printError(pp.cid, pp.cno, formatMessageForLog("Unable to delete file: " + pp.getAbsoluteFilePath() + "."), e);
		}
		
		// Delete the backup file.
		try {
			Path deletePath = Paths.get(pp.getAbsoluteFilePathBackup());
			if (Files.exists(deletePath)) {
				LogSystem.getInstance().printInfo(pp.cid, pp.cno, formatMessageForLog("Deleting the file: " + deletePath.toString()));		
				Files.delete(deletePath);				
			}
		} catch (Exception e) {
			LogSystem.getInstance().printError(pp.cid, pp.cno, formatMessageForLog("Unable to delete file: " + pp.getAbsoluteFilePathBackup() + "."), e);
		}
			
	}
	
	/** This will process the "in" or "out" video.
	 * */
	private void processInOut() {
		// Encode the M3U8 version if need be.
		if (pp.cmdInOut == ProcessParameters.COMMAND_INOUT.ENCODE)
			encodeM3U8(pp.getAbsoluteFilePath(), pp.getAbsoluteFilePathNoExtensionsForOutput());
		
		// Generate the M3U8 with in/out videos.
		HLSupdater.getInstance().updateAllHLS(pp.cid, pp.cno, pp.jobIdNumber);
		
		// Notify the web server the in/out videos have been processed.
		Web.getInstance().notifyInOutVideo(pp.jobIdNumber, pp.cid, pp.cno, pp.ioviid);
	}
	
	/** This will not re-encode unless the ProcessParameters.force_reencoding is turned on. It will just crop the input files.
	 * @param inputFileHD is blank or null if not used. The HD file will only be encoded if it exists. Must be an .m3u8 file.
	 * @param inputFileSD is blank or null if not used. The SD file will only be encoded if it exists. Must be an .m3u8 file.
	 * @param inputFileLOW is blank or null if not used. The LOW file will only be encoded if it exists. Must be an .m3u8 file.
	 * @param inputFileA64 is blank or null if not used. The Audio64 file will only be encoded if it exists. Must be an .m3u8 file. REQUIRED
	 * @param outputDir is the output location for the encodings.
	 * */
	private void encodeM3U8(String inputFileHD, String inputFileSD, String inputFileLOW, String inputFileA64, String outputDir) {
		if (inputFileA64 == null || inputFileA64.equals("")) {
			LogSystem.getInstance().printError(pp.cid, pp.cno, formatMessageForLog("The 64k m3u8 file is required to encode copy."));
			return;
		}
		
		try {			
			// Encode an HD version.
			int hdBitrate = 0;
			String hdOutputFileM3U8 = "";
			boolean bHdEncoded = false;
			if (inputFileHD != null && "".equals(inputFileHD) == false) {
				bHdEncoded = true;

				// Get the bitrate.
				String tsFile = Paths.get(inputFileHD).getParent().toString() + "/0000.ts";
				FFmpeg ffTs = new FFmpeg(pp.cid, pp.cno, tsFile, pp.jobIdNumber);
				hdBitrate = ffTs.getBitrate();
				
				// Generate the HD m3u8 file.
				hdOutputFileM3U8 = outputDir + "/HD/HD.m3u8";
				FFmpeg ffmpeg = new FFmpeg(pp.cid, pp.cno, inputFileHD, pp.jobIdNumber);
				ffmpeg.encodeM3U8(hdOutputFileM3U8, pp.clip_start_seconds, pp.getDurationSeconds(), 0, false, "HD");				
			}
			
			// Encode an SD version.
			int sdBitrate = 0;
			String sdOutputFileM3U8 = "";
			boolean bSdEncoded = false;
			if (inputFileSD != null && "".equals(inputFileSD) == false) {
				bSdEncoded = true;

				// Get the bitrate.
				String tsFile = Paths.get(inputFileSD).getParent().toString() + "/0000.ts";
				FFmpeg ffTs = new FFmpeg(pp.cid, pp.cno, tsFile, pp.jobIdNumber);
				sdBitrate = ffTs.getBitrate();
				
				// Generate the SD m3u8 file.
				sdOutputFileM3U8 = outputDir + "/SD/SD.m3u8";
				FFmpeg ffmpeg = new FFmpeg(pp.cid, pp.cno, inputFileSD, pp.jobIdNumber);
				ffmpeg.encodeM3U8(sdOutputFileM3U8, pp.clip_start_seconds, pp.getDurationSeconds(), 0, false, "SD");				
			}

			// Encode an LOW version.
			int lowBitrate = 0;
			String lowOutputFileM3U8 = "";
			boolean bLowEncoded = false;
			if (inputFileLOW != null && "".equals(inputFileLOW) == false) {
				bLowEncoded = true;

				// Get the bitrate.
				String tsFile = Paths.get(inputFileLOW).getParent().toString() + "/0000.ts";
				FFmpeg ffTs = new FFmpeg(pp.cid, pp.cno, tsFile, pp.jobIdNumber);
				lowBitrate = ffTs.getBitrate();
				
				// Generate the LOW m3u8 file.
				lowOutputFileM3U8 = outputDir + "/LOW/LOW.m3u8";
				FFmpeg ffmpeg = new FFmpeg(pp.cid, pp.cno, inputFileLOW, pp.jobIdNumber);
				ffmpeg.encodeM3U8(lowOutputFileM3U8, pp.clip_start_seconds, pp.getDurationSeconds(), 0, false, "LOW");				
			}
			
			// Encode an audio 64k version.
			int audio64kBitrate = 0;
			String a64kOutputFileM3U8 = "";
			boolean b64kEncoded = false;
			if (inputFileA64 != null && "".equals(inputFileA64) == false) {
				b64kEncoded = true;
				audio64kBitrate = 64000;
				
				// Generate the 64k m3u8 file.
				a64kOutputFileM3U8 = outputDir + "/64k/64k.m3u8";
				FFmpeg ffmpeg = new FFmpeg(pp.cid, pp.cno, inputFileA64, pp.jobIdNumber);
				ffmpeg.encodeM3U8(a64kOutputFileM3U8, pp.clip_start_seconds, pp.getDurationSeconds(), 0, true, "64k");				
			}

			// Log the encoding options for HD and SD.
			LogSystem.getInstance().printInfo(pp.cid, pp.cno, formatMessageForLog("Copy and clip m3u8 for HD:[encoded:" + bHdEncoded + "  bitrate:" + hdBitrate + "] SD:[encoded:" + bSdEncoded + "  bitrate:" + sdBitrate + "] LOW:[encoded:" + bLowEncoded + "  bitrate:" + lowBitrate + "] 64k:[encoded:" + b64kEncoded + "  bitrate:" + audio64kBitrate + "]"));
						
			// Create the master playlists.
			M3U8MasterPlaylist masterPlaylist = new M3U8MasterPlaylist(pp.cid, pp.cno);
			if (hdBitrate > 0)
				masterPlaylist.add(new PlaylistInfo(hdBitrate, "HD", "HD/HD.m3u8"));
			if (sdBitrate > 0)
				masterPlaylist.add(new PlaylistInfo(sdBitrate, "SD", "SD/SD.m3u8"));
			if (lowBitrate > 0)
				masterPlaylist.add(new PlaylistInfo(lowBitrate, "LOW", "LOW/LOW.m3u8"));

			// PC.
			String outputFileM3U8 = outputDir + "/pc.m3u8";
			masterPlaylist.setFilename(outputFileM3U8);
			masterPlaylist.writeManual();

			// iOS.
			outputFileM3U8 = outputDir + "/ios.m3u8";
			masterPlaylist.add(new PlaylistInfo(audio64kBitrate, "64k", "64k/64k.m3u8"));
			masterPlaylist.setFilename(outputFileM3U8);
			masterPlaylist.writeManual();
			
			
		} catch (Exception e) {
			LogSystem.getInstance().printError(pp.cid, pp.cno, formatMessageForLog("Unable to encode m3u8."), e);
		}
	}
	
	/** This will encode the input video file to the folder outputfileNoExtension.
	 * @param inputFile is the input video file to encode to m3u8.
	 * @param outputDir is the directory the m3u8 will be encoded to. Should not have a slash at the end. /opt/gomedia/media/archive/clients/1/1_20130516113419
	 * */
	private void encodeM3U8(String inputFile, String outputDir) {
		try {
			// Input file.
			FFmpeg ffmpeg = new FFmpeg(pp.cid, pp.cno, inputFile, pp.jobIdNumber);
			
			// Begin encoding the m3u8 versions.
			int origBitRate = 0;

			// It is an m3u8 file.
			String last4Characters = inputFile.substring(inputFile.length() - 4); 
			if (last4Characters.equals("m3u8")) {
				String tsFile = Paths.get(inputFile).getParent().toString() + "/0000.ts";
				FFmpeg ffTs = new FFmpeg(pp.cid, pp.cno, tsFile, pp.jobIdNumber);
				origBitRate = ffTs.getBitrate();
								
			} else {
				origBitRate = ffmpeg.getBitrate();				
			}

			// Encode an HD version.
			int hdBitrate = 0;
			String hdOutputFileM3U8 = "";
			boolean bHdEncoded = false;
			if (origBitRate > BITRATE_SD_MAX_CAP) {
				// Does the HD version need to be re-encoded?
				if (origBitRate > BITRATE_HD_MAX_CAP)
					hdBitrate = BITRATE_HD_MAX_CAP;
				else if (pp.force_reencoding)
					hdBitrate = origBitRate;

				// Generate the HD m3u8 file.
				hdOutputFileM3U8 = outputDir + "/HD/HD.m3u8";
				ffmpeg.encodeM3U8(hdOutputFileM3U8, pp.clip_start_seconds, pp.getDurationSeconds(), hdBitrate, false, "HD");
				
				// Make sure we save the HD bitrate.
				if (hdBitrate == 0)
					hdBitrate = origBitRate;
				bHdEncoded = true;
			}

			// Encode an SD version.
			int sdBitrate = 0;
			String sdOutputFileM3U8 = "";
			boolean bSdEncoded = false;
			if (origBitRate > BITRATE_LOW_MAX_CAP) {
				// Does the SD version need to be re-encoded?
				if (origBitRate > BITRATE_SD_MAX_CAP)
					sdBitrate = BITRATE_SD_MAX_CAP;
				else if (pp.force_reencoding)
					sdBitrate = origBitRate;

				// Generate the SD m3u8 file.
				sdOutputFileM3U8 = outputDir + "/SD/SD.m3u8";
				ffmpeg.encodeM3U8(sdOutputFileM3U8, pp.clip_start_seconds, pp.getDurationSeconds(), sdBitrate, false, "SD");
				
				// Make sure we save the SD bitrate.
				if (sdBitrate == 0)
					sdBitrate = origBitRate;				
				bSdEncoded = true;
			}
			
			// Log the encoding options for HD and SD.
			LogSystem.getInstance().printInfo(pp.cid, pp.cno, formatMessageForLog("Encoding m3u8 options for HD:[encoded:" + bHdEncoded + "  bitrate:" + hdBitrate + "] SD:[encoded:" + bSdEncoded + "  bitrate:" + sdBitrate + "] for input file:[" + inputFile + "]"));
			
			// Encode an LOW version.
			int lowBitrate = BITRATE_LOW_MAX_CAP;			
			String lowOutputFileM3U8 = outputDir + "/LOW/LOW.m3u8";
			ffmpeg.encodeM3U8(lowOutputFileM3U8, pp.clip_start_seconds, pp.getDurationSeconds(), lowBitrate, false, "LOW");
			
			// Encode an 64k audio only version.
			int audio64kBitrate = 64000;
			String a64kOutputFileM3U8 = outputDir + "/64k/64k.m3u8";
			ffmpeg.encodeM3U8(a64kOutputFileM3U8, pp.clip_start_seconds, pp.getDurationSeconds(), audio64kBitrate, true, "64k");
			
			// Create the master playlists.
			M3U8MasterPlaylist masterPlaylist = new M3U8MasterPlaylist(pp.cid, pp.cno);
			if (hdBitrate > 0)
				masterPlaylist.add(new PlaylistInfo(hdBitrate, "HD", "HD/HD.m3u8"));
			if (sdBitrate > 0)
				masterPlaylist.add(new PlaylistInfo(sdBitrate, "SD", "SD/SD.m3u8"));
			masterPlaylist.add(new PlaylistInfo(lowBitrate, "LOW", "LOW/LOW.m3u8"));

			// PC.
			String outputFileM3U8 = outputDir + "/pc.m3u8";
			masterPlaylist.setFilename(outputFileM3U8);
			masterPlaylist.writeManual();

			// iOS.
			outputFileM3U8 = outputDir + "/ios.m3u8";
			masterPlaylist.add(new PlaylistInfo(audio64kBitrate, "64k", "64k/64k.m3u8"));
			masterPlaylist.setFilename(outputFileM3U8);
			masterPlaylist.writeManual();
			
			
		} catch (Exception e) {
			LogSystem.getInstance().printError(pp.cid, pp.cno, formatMessageForLog("Unable to encode m3u8."), e);
		}
	}
}
