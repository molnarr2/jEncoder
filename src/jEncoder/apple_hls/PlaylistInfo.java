package jEncoder.apple_hls;

import java.io.BufferedWriter;

public class PlaylistInfo {
	/** The header information. 
	 *  Example: #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=125952 */
	String header;
	
	/** The stream name. Example: livestreamLOW. */
	String streamName;
	
	/** The child playlist .m3u8 location. */
	String childPlaylist;
	
	public PlaylistInfo(String header, String streamName) {
		this.header = header;
		this.streamName = streamName;
	}
	
	/** @param bandwidth in bites. Ie, 500000 for 500k.
	 * @param name is the label for the end user to see, ie, "HD".
	 * @param childPlaylist is full path to the stream relative to the MasterPlaylist. ie, HD/HD.m3u8.
	 * */
	public PlaylistInfo(int bandwidth, String name, String childPlaylist) {
		this.header = "#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=" + bandwidth + ",NAME=\"" + name + "\"";
		this.childPlaylist = childPlaylist;
	}
	
	/** This will write out the PlaylistInfo using the childPlaylist information.
	 * */
	public void writeChildPlaylist(BufferedWriter bw) throws Exception {
		bw.write(header + "\n");
		bw.write(childPlaylist + "\n");
	}
	
	/** This will write out the PlaylistInfo as updated.
	 * */
	public void writeUpdated(BufferedWriter bw) throws Exception {
		bw.write(header + "\n");
		bw.write(streamName + "/updated.m3u8\n");
	}	
	
	/** This will write out the PlaylistInfo as inout.
	 * */
	public void writeInout(BufferedWriter bw) throws Exception {
		bw.write(header + "\n");
		bw.write(streamName + "/inout.m3u8\n");
	}	
	
	/** Is this stream64?
	 * @return true if this is a stream64 stream type.
	 * */
	public boolean isStream64() {
		if (streamName.startsWith("stream64"))
			return true;
		return false;
	}
}
