package jEncoder.apple_hls;

import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
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

public class M3U8ChildPlaylist {

	/** The clientId and channelNo for this m3u8 file. */
	int clientId;
	int channelNo;
	
	/** This is the name of the file which the m3u8 is located at. Must be full path to the file. */
	String filename;
	/** The target duration length in seconds. */
	int targetDuration;
	/** The program date time. "2015-02-10T15:36:01+00:00" */
	String programDateTime = "";
	/** The media sequence. */
	int mediaSequence;
	/** The media version for the m3u8 file. If zero then don't print out. */
	int version = 0;
	/** If allow cache is turned on, YES/NO. If Blank then don't print out. */
	String allowCache = "";
	
	LinkedList<TsInfo> ltTsInfo = new LinkedList<TsInfo>();
	
	public M3U8ChildPlaylist(int clientId, int channelNo) {
		this.clientId = clientId;
		this.channelNo = channelNo;
	}
	
	/** @return the program date time.
	 * */
	public String getProgramDateTime() {
		return programDateTime;
	}
	
	/** This is the filename to read or write to.
	 * */
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	/** This will add an intro video to the m3u8 file.
	 * */
	public void addIntroVideo(M3U8ChildPlaylist intro) {
		// Add a Discontinuity record first between intro video and archive video.
		ltTsInfo.addFirst(TsInfo.newDiscontinuity());
		
		Iterator<TsInfo> i = intro.ltTsInfo.descendingIterator();
		while (i.hasNext()) {
			TsInfo ts = i.next();
			if (ts.getType() == TsInfo.Type.VIDEO_FILE)
				ltTsInfo.addFirst(ts);
		}
	}
	
	/** This will add an ending video to the m3u8 file.
	 * */
	public void addEndingVideo(M3U8ChildPlaylist ending) {
		// Add a Discontinuity record first between ending video and archive video.
		ltTsInfo.add(TsInfo.newDiscontinuity());
		
		Iterator<TsInfo> i = ending.ltTsInfo.iterator();
		while (i.hasNext()) {
			TsInfo ts = i.next();
			if (ts.getType() == TsInfo.Type.VIDEO_FILE)
				ltTsInfo.add(ts);
		}
	}
	
	/** This will update the target duration to the largest time found in the list of TsInfo.
	 * */
	public void updateTargetDuration() {
		Iterator<TsInfo> i = ltTsInfo.iterator();
		targetDuration = 0;
		while (i.hasNext()) {
			TsInfo ts = i.next();
			if (ts.getDuration() > targetDuration)
				targetDuration = (int)Math.ceil(ts.getDuration());
		}
	}
	
	/** This will update each TsInfo that that is a video file type by placing the path before the video name.
	 * @param path is the path to update the TsInfo video file by. Example: /opt/gomedia/media/archive/clients/1/inout/1/in/SD/ which after that 
	 *             is pre-appending it will look like this: /opt/gomedia/media/archive/clients/1/inout/1/in/SD/0000.ts
	 * */
	public void updateTSInfoPath(String path) {
		Iterator<TsInfo> i = ltTsInfo.iterator();
		while (i.hasNext()) {
			TsInfo ts = i.next();
			if (ts.type != TsInfo.Type.VIDEO_FILE)
				continue;			
			ts.updatePath(path);
		}		
	}
	
	/** This will load the m3u8 file and update this with the latest video files from it.
	 * */
	public void updateWith(String m3u8file) {

		if (!Files.exists(FileSystems.getDefault().getPath(m3u8file))) {
			LogSystem.getInstance().printError(clientId, channelNo, "m3u8 file does not exist. " + m3u8file);
			return;
		}

		// Read in the file.
		M3U8ChildPlaylist m3u8 = new M3U8ChildPlaylist(clientId, channelNo);
		m3u8.setFilename(m3u8file);
		m3u8.read();
		
		// Does the latest current video file exist in that m3u8? If not then need to add the discontinuity when a video file is added.
		boolean bNeedDiscontinuity = false;
		if (!ltTsInfo.isEmpty()) {		
			TsInfo tsInfoLatest = ltTsInfo.getLast();
			if (!m3u8.exists(tsInfoLatest) && tsInfoLatest.type == TsInfo.Type.VIDEO_FILE) {
				// Since the video file does not exist therefore add a discontinuity to this m3u8.
				bNeedDiscontinuity = true;
			}
		}

		// Get the new video files.
		LinkedList<TsInfo> ltAdds = compare(m3u8);
		for (TsInfo tsInfo : ltAdds) {
			if (tsInfo.getType() == TsInfo.Type.VIDEO_FILE) {
				// Add the discontinuity flag before add this video file.
				if (bNeedDiscontinuity) {
					ltTsInfo.add(TsInfo.newDiscontinuity());
					bNeedDiscontinuity = false;
				}

				ltTsInfo.add(tsInfo);
			}
		}
		
		// Update the program date time value.
		programDateTime = m3u8.programDateTime;
		
		// Remove old video files from the m3u8 file.
		while (countVideos() > 10) {
			mediaSequence++;
			ltTsInfo.removeFirst();
		}
		if (!ltTsInfo.isEmpty()) {
			TsInfo tsFirst = ltTsInfo.getFirst();
			if (tsFirst.type != TsInfo.Type.VIDEO_FILE)
				ltTsInfo.removeFirst();
		}
		
	}
	
	/** @return the number of videos segments found in this m3u8 file.
	 * */
	public int countVideos() {
		int count = 0;
		for (TsInfo ts : ltTsInfo) {
			if (ts.getType() == TsInfo.Type.VIDEO_FILE)
				count++;
		}
		return count;
	}
	
	public void add(TsInfo tsInfo) {
		ltTsInfo.add(tsInfo);
	
		// Get count of only video files.
		int count = 0;
		for (TsInfo ts : ltTsInfo) {
			if (ts.getType() == TsInfo.Type.VIDEO_FILE)
				count++;
		}
		
		// Remove old video file. If oldest is not a video file just continue until video file is removed.
		if (count > 10) {
			// Remove an extra one if the first one is not a Video file.
			if (ltTsInfo.getFirst().getType() != TsInfo.Type.VIDEO_FILE)
				ltTsInfo.removeFirst();			
			ltTsInfo.removeFirst();
		}
	}
	
	/** Loads in the m3u8 file (ie, the filename set in setFilename() function).
	 * */
	public void read() {
		targetDuration = 0;
		programDateTime = "";
		mediaSequence = 0;
		ltTsInfo = new LinkedList<TsInfo>();
		
		try {
			List<String> lines = Files.readAllLines(Paths.get(filename), Charset.defaultCharset());

			Iterator<String> itr = lines.iterator(); 
			while (itr.hasNext()) {
				String line = itr.next();

				// Get the value if it exists for this line.
				String value = "";
				int colonIndex = line.indexOf(':');
				if (colonIndex > -1)
					value = line.substring(colonIndex+1);
				
				if (line.startsWith("#EXTINF")) {
					int commaIndex = line.indexOf(',');					

					value = line.substring(colonIndex+1, commaIndex);
					float duration = Float.valueOf(value);
					
					if (itr.hasNext()) {
						String nextLine = itr.next();
						ltTsInfo.add(new TsInfo(nextLine, duration, TsInfo.Type.VIDEO_FILE));						
					}
					
				} else if (line.startsWith("#EXT-X-TARGETDURATION")) {
					targetDuration = Integer.valueOf(value);
					
				} else if (line.startsWith("#EXT-X-MEDIA-SEQUENCE")) {
					mediaSequence = Integer.valueOf(value);					
					
				} else if (line.startsWith("#EXT-X-PROGRAM-DATE-TIME")) {					
					programDateTime = line.substring(colonIndex+1); 
										
				} else if (line.startsWith("#EXT-X-ENDLIST")) {
					ltTsInfo.add(TsInfo.newEndList());
					
				} else if (line.startsWith("#EXT-X-DISCONTINUITY")) {
					ltTsInfo.add(TsInfo.newDiscontinuity());					
				
				} else if (line.startsWith("#EXT-X-VERSION")) {
					version = Integer.valueOf(value);					
					
				} else if (line.startsWith("#EXT-X-ALLOW-CACHE")) {
					allowCache = value;
				}
			}					
		} catch (Exception e) {
			LogSystem.getInstance().printError(clientId, channelNo, "Unable to read m3u8 file out: " + this, e);
		}
	}
	
	/** This will write out the .m3u8 file. It will write to a temporary file and then atomically move it to the correct location. 
	 * */
	public void write() {
		try {			
			// Get a temporary file.
	    	Path dir = Paths.get(filename).getParent();
			Path temp = Files.createTempFile(dir, "temp", ".m3u8");					
			BufferedWriter writer =  Files.newBufferedWriter( temp, Charset.defaultCharset(), StandardOpenOption.CREATE);

			// Write out to the temporary file.
			writer.write("#EXTM3U\n");
								
			if (version > 0)
				writer.write("#EXT-X-VERSION:" + version + "\n");
		
			writer.write("#EXT-X-MEDIA-SEQUENCE:" + mediaSequence + "\n");
			
			if (!"".equals(allowCache))
				writer.write("#EXT-X-ALLOW-CACHE:" + allowCache + "\n");
			
			if (!"".equals(programDateTime))
				writer.write("#EXT-X-PROGRAM-DATE-TIME:" + programDateTime + "\n");

			writer.write("#EXT-X-TARGETDURATION:" + targetDuration + "\n");

			for (TsInfo tsInfo : ltTsInfo) {
				tsInfo.write(writer);
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
	
	/** @return the latest TsInfo record.
	 * */
	public TsInfo getLatestTsInfo() {
		return ltTsInfo.getLast();
	}
	
	/** @return true if the tsInfoExists in this m3u8 file.
	 * */
	public boolean exists(TsInfo tsInfoExists) {
		for (TsInfo tsInfo : ltTsInfo) {
			if (tsInfo.equalTo(tsInfoExists))
				return true;
		}
		
		return false;
	}
	
	/** Compare the m3u8 to this one. It will take the latest one in this list and determine where
	 * at that is in the m3u8 and then return all of the TsInfo after that one. If it doesn't match
	 * any at all then all of them will end up being returned.
	 * */	
	public LinkedList<TsInfo> compare(M3U8ChildPlaylist m3u8) {
		LinkedList<TsInfo> ltCompared = new LinkedList<TsInfo>();		
		if (ltTsInfo.isEmpty())
			return ltCompared;
				
		TsInfo tsInfoEnd = this.ltTsInfo.getLast();
		
		boolean bFoundEnd = false;
		for (TsInfo tsInfo : m3u8.ltTsInfo) {
			if (bFoundEnd)
				ltCompared.add(tsInfo);
			else {
				if (tsInfoEnd.equalTo(tsInfo))
					bFoundEnd = true;
			}
		}		
		
		if (!bFoundEnd) {
			ltCompared.addAll(m3u8.ltTsInfo);
		}
		
		return ltCompared;
	}
	
	/** This will remove the directories for the filename.
	 * */
	public void removeDirectories() {
		for (TsInfo tsInfo : ltTsInfo) {
			tsInfo.removeDirectories();
		}
	}
	
	/** Duration set to 10 seconds.
	 * */
	public void setDurationTimeTo10Seconds() {
		targetDuration = 10;
	}
	
	public String toString() {
		return "{M3U8\n  filename: " + filename + "\n  targetDuration: " + targetDuration + "\n  programDateTime: " + programDateTime + "\n  mediaSequence: " + mediaSequence 
			+ "\n  ltTsInfo: " + ltTsInfo + "}";
	}
}
