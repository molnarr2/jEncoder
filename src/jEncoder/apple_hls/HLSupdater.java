package jEncoder.apple_hls;

import java.util.LinkedList;

import jEncoder.client.*;
import jEncoder.util.LogSystem;

public class HLSupdater {

    // The only instance of this class.
    private static HLSupdater instance = null;
    
    /** The only reason ClientInfo is saved is to have a way to concurrency work correctly. The ChannelInfo
     * has concurrency to keep from the end user updating the HLS too quickly.
     * */
    private static LinkedList<ClientInfo> ltClientInfo = new LinkedList<ClientInfo>();
    
    // [start] Methods: Constructor and getInstance()

    protected HLSupdater() {
    }
    
    /** @return the only instance of this class.
     */
    public static HLSupdater getInstance() {
        if(instance == null) {
            instance = new HLSupdater();
        }
        return instance;
    }

    // [end]    
    
    /** This will update all HLS archive videos for a given client and channel #.
     * @param clientInfoId is the ClientInfo.client_info_id.
     * @param channelNo is the ChannelInfo.channel_no.
     * */
    public void updateAllHLS(int clientInfoId, int channelNo, String jobIdNumber) {
    	LogSystem.getInstance().printInfo(clientInfoId, channelNo, "Job [" + jobIdNumber + "] Refreshing all HLS inout.");
    	
    	ClientInfo client = getClient(clientInfoId);
    	client.updateAllHLS(channelNo, jobIdNumber);
    }
    
    /** This will update a single video HLS archive.
     * @param clientInfoId is the ClientInfo.client_info_id.
     * @param channelNo is the ChannelInfo.channel_no.
     * @param filename is the name of the file to update it's HLS archive. ex: 1_1_20150505145533.mp4.
     * */
    public void updateSingleHLS(int clientInfoId, int channelNo, String filename, String jobIdNumber) {
    	LogSystem.getInstance().printInfo(clientInfoId, channelNo, "Job [" + jobIdNumber + "] Refreshing HLS for file:[" + filename + "]");
    	
    	ClientInfo client = getClient(clientInfoId);
    	client.updateSingleHLS(channelNo, filename, jobIdNumber);    	
    }
    
    /** @return the client's information.
     * */
    private ClientInfo getClient(int clientInfoId) {
    	for (ClientInfo client : ltClientInfo) {
    		if (client.isClientId(clientInfoId))
    			return client;
    	}
    	ClientInfo newClient = new ClientInfo(clientInfoId);
    	ltClientInfo.add(newClient);
    	return newClient;    	
    }
}
