package jEncoder.apple_hls;

import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jEncoder.util.*;

public class M3U8MasterPlaylist {
	
	/** The clientId and channelNo for this m3u8 file. */
	int clientId;
	int channelNo;
	
	/** This is the name of the file which the m3u8 is located at. Must be full path to the file. */
	String filename;

	LinkedList<PlaylistInfo> ltPlaylistInfo = new LinkedList<PlaylistInfo>();
	
	public M3U8MasterPlaylist (int clientId, int channelNo) {
		this.clientId = clientId;
		this.channelNo = channelNo;
	}

	/** This is the filename to read or write to.
	 * */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/** Add a PlaylistInfo to this Master Playlist.
	 * */
	public void add(PlaylistInfo info) {
		ltPlaylistInfo.add(info);
	}
	
	/** Loads in the m3u8 file (ie, the filename set in setFilename() function).
	 * */
	public void read() {
		
		try {
			List<String> lines = Files.readAllLines(Paths.get(filename), Charset.defaultCharset());

			Iterator<String> itr = lines.iterator(); 
			while (itr.hasNext()) {
				String line = itr.next();
				
				if (line.startsWith("#EXT-X-STREAM-INF")) {
					
					if (itr.hasNext()) {
						String nextLine = itr.next();
						
						// Get the streamName.
						String streamName = "";
						int colonIndex = nextLine.indexOf('/');
						if (colonIndex > -1) {
							streamName = nextLine.substring(0, colonIndex);
							ltPlaylistInfo.add(new PlaylistInfo(line, streamName));
						}
					}
				}
			}					
		} catch (Exception e) {
			LogSystem.getInstance().printError(clientId, channelNo, "Unable to read m3u8 file: " + filename, e);
		}
	}

	/** This will write out the m3u8 file as PC compatible.
	 * */
	public void writePC() {
		write(false);
	}	

	/** This will write out the m3u8 file as iOS compatible.
	 * */
	public void writeiOS() {
		write(true);
	}	

	/** This will write out the m3u8 file.
	 * @param writeStream64 is true if to write out stream64.
	 * */
	private void write(boolean writeStream64) {				
		try {
			// Get a temporary file.
	    	Path dir = Paths.get(filename).getParent();
			Path temp = Files.createTempFile(dir, "temp", ".3u8");		
			BufferedWriter writer =  Files.newBufferedWriter( temp, Charset.defaultCharset(), StandardOpenOption.CREATE);

			// Write out to the temporary file.
			writer.write("#EXTM3U\n");

			for (PlaylistInfo playlistInfo : ltPlaylistInfo) {
				if (!writeStream64) {
					if (!playlistInfo.isStream64())
						playlistInfo.writeUpdated(writer);
				} else
					playlistInfo.writeUpdated(writer);
			} 
			writer.close();
			
			// Set it so that it can be read.
			Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
			perms.add(PosixFilePermission.OWNER_READ);
			perms.add(PosixFilePermission.OWNER_WRITE);
			perms.add(PosixFilePermission.GROUP_READ);
			perms.add(PosixFilePermission.GROUP_WRITE);
			perms.add(PosixFilePermission.OTHERS_READ);			
		    Files.setPosixFilePermissions(temp, perms);
			
			// Move the file over and delete the temporary file.
			Files.move(temp, Paths.get(filename), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			temp.toFile().delete();
			
		} catch (Exception e) {
			LogSystem.getInstance().printError(clientId, channelNo, "Unable to write m3u8 file out: " + this, e);
		}
	}	

	/** This will write out the m3u8 file as an inout version.
	 * */
	public void writeInout() {				
		try {
			// Get a temporary file.
	    	Path dir = Paths.get(filename).getParent();
			Path temp = Files.createTempFile(dir, "temp", ".3u8");		
			BufferedWriter writer =  Files.newBufferedWriter( temp, Charset.defaultCharset(), StandardOpenOption.CREATE);

			// Write out to the temporary file.
			writer.write("#EXTM3U\n");

			for (PlaylistInfo playlistInfo : ltPlaylistInfo) {
				playlistInfo.writeInout(writer);
			} 
			writer.close();
			
			// Set it so that it can be read.
			Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
			perms.add(PosixFilePermission.OWNER_READ);
			perms.add(PosixFilePermission.OWNER_WRITE);
			perms.add(PosixFilePermission.GROUP_READ);
			perms.add(PosixFilePermission.GROUP_WRITE);
			perms.add(PosixFilePermission.OTHERS_READ);			
		    Files.setPosixFilePermissions(temp, perms);
			
			// Move the file over and delete the temporary file.
			Files.move(temp, Paths.get(filename), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			temp.toFile().delete();
			
		} catch (Exception e) {
			LogSystem.getInstance().printError(clientId, channelNo, "Unable to write m3u8 file out: " + this, e);
		}
	}	

	/** This will write out the m3u8 file using the childplaylist field in the PlaylistInfo.
	 * */
	public void writeManual() {				
		try {
			// Get a temporary file.
	    	Path dir = Paths.get(filename).getParent();
			Path temp = Files.createTempFile(dir, "temp", ".3u8");		
			BufferedWriter writer =  Files.newBufferedWriter( temp, Charset.defaultCharset(), StandardOpenOption.CREATE);

			// Write out to the temporary file.
			writer.write("#EXTM3U\n");

			for (PlaylistInfo playlistInfo : ltPlaylistInfo) {
				playlistInfo.writeChildPlaylist(writer);
			} 
			writer.close();
			
			// Set it so that it can be read.
			Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
			perms.add(PosixFilePermission.OWNER_READ);
			perms.add(PosixFilePermission.OWNER_WRITE);
			perms.add(PosixFilePermission.GROUP_READ);
			perms.add(PosixFilePermission.GROUP_WRITE);
			perms.add(PosixFilePermission.OTHERS_READ);			
		    Files.setPosixFilePermissions(temp, perms);
			
			// Move the file over and delete the temporary file.
			Files.move(temp, Paths.get(filename), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			temp.toFile().delete();
			
		} catch (Exception e) {
			LogSystem.getInstance().printError(clientId, channelNo, "Unable to write m3u8 file out: " + this, e);
		}
	}	
	
/*
	#EXTM3U
	#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=1531904
	livestreamHD/live.m3u8
	#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=125952
	livestreamLOW/live.m3u8
	#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=559104
	livestreamSD/live.m3u8
	#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=1000
	stream64/live.m3u8
	*/
	
}
