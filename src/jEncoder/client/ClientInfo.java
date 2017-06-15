package jEncoder.client;

import java.util.LinkedList;

public class ClientInfo {
    
	/** ClientInfo.client_info_id. */
	private int clientInfoId;
	
    /** The only reason ChannelInfo is saved is to have a way to concurrency work correctly. The ChannelInfo
     * has concurrency to keep from the end user updating the HLS too quickly.
     * */
    private LinkedList<ChannelInfo> ltChannelInfo = new LinkedList<ChannelInfo>();
    
    // [start] Methods: Constructor and getInstance()

    public ClientInfo(int clientInfoId) {
    	this.clientInfoId = clientInfoId;
    }
    
    // [end]    
    
    /** This will update all HLS archive videos for a given channel #.
     * @param channelNo is the ChannelInfo.channel_no.
     * */
    public void updateAllHLS(int channelNo, String jobIdNumber) {
    	ChannelInfo channel = getChannel(channelNo);
    	channel.updateAllHLS(jobIdNumber);
    }
    
    /** This will update a single video HLS archive.
     * @param channelNo is the ChannelInfo.channel_no.
     * @param filename is the name of the file to update it's HLS archive. ex: 1_1_20150505145533.mp4.
     * */
    public void updateSingleHLS(int channelNo, String filename, String jobIdNumber) {
    	ChannelInfo channel = getChannel(channelNo);
    	channel.updateSingleHLS(filename, jobIdNumber);
    }
    
    /** @return the channel's information.
     * */
    private ChannelInfo getChannel(int channelNo) {
    	for (ChannelInfo channel : ltChannelInfo) {
    		if (channel.isChannelNo(channelNo))
    			return channel;
    	}
    	ChannelInfo newChannel = new ChannelInfo(clientInfoId, channelNo);
    	ltChannelInfo.add(newChannel);
    	return newChannel;    	
    }
    
    /** @return true if clientInfoId is equal to this clientInfoId.
     * */
    public boolean isClientId(int clientInfoId) {
    	 return this.clientInfoId == clientInfoId;
    }

}
