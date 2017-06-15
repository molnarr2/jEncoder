package jEncoder.apple_hls;

import java.io.BufferedWriter;
import java.io.File;

public class TsInfo {
	/** What type is this TSInfo record. */
	public enum Type {VIDEO_FILE, DISCONTINUITY, ENDLIST};
	
	/** The filename for this TS file. */
	String filename;
	/** The duration in seconds of the TS file. */
	float duration;
	/** What type this is. */
	Type type;
	
	public TsInfo(String filename, float duration, Type type) {
		this.filename = filename;
		this.duration = duration;
		this.type = type;
	}
	
	public static TsInfo newDiscontinuity() {
		return new TsInfo("", 0, Type.DISCONTINUITY);
	}
	
	public static TsInfo newEndList() {
		return new TsInfo("", 0, Type.ENDLIST);
	}
	
	public String getFilename() {
		return this.filename;		
	}
	
	public float getDuration() {
		return this.duration;
	}
	
	public Type getType() {
		return type;
	}
	
	/** @return true if this is equal to the tsInfo ie same type, filename, and duration.
	 * */
	public boolean equalTo(TsInfo tsInfo) {
		if (this.type != tsInfo.type)
			return false;
		if (this.duration != tsInfo.duration)
			return false;
		return this.filename.equals(tsInfo.filename);
	}
	
	public void write(BufferedWriter bw) throws Exception {
		if (type == Type.VIDEO_FILE) {
			String sDuration = String.format("%.5f", duration);
			bw.write("#EXTINF:" + sDuration + ",\n");
			bw.write(filename + "\n");
		} else if (type == Type.DISCONTINUITY) {
			bw.write("#EXT-X-DISCONTINUITY\n");
		} else if (type == Type.ENDLIST) {
			bw.write("#EXT-X-ENDLIST\n");			
		}
	}	

	public String toString() {
		return "{TsInfo filename: " + filename + " duration: " + duration + " type: " + type + "}";
	}

	public void updatePath(String path) {
		filename = path + filename;
	}
	
	/** This will remove the directories for the filename.
	 * */
	public void removeDirectories() {
		File f = new File(filename);
		filename = f.getName();
	}
}

