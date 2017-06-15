package jEncoder.media;

import jEncoder.util.LogSystem;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

public class Restorecon {
    public static final String RESTORECON_PATH = "/sbin/restorecon";

    // Client information.
    int clientId;
    int channelNo;
    
    /** This is the input directory to the HLS without the slash. */
    public String inputDirectoryPath;

    /** The job ID number. */
    public String jobIdNumber;

	public Restorecon(int clientId, int channelNo, String inputDirectoryPath, String jobIdNumber) {
    	this.clientId = clientId;
    	this.channelNo = channelNo;
    	this.inputDirectoryPath = inputDirectoryPath;
    	this.jobIdNumber = jobIdNumber;
    }

	public void run() {
    	CommandLine cmdLine = new CommandLine(RESTORECON_PATH);
    	cmdLine.addArgument("-r");
    	cmdLine.addArgument(inputDirectoryPath + "*");

    	DefaultExecutor executor = new DefaultExecutor();
//    	int exitValues[] = {0,1};
//    	executor.setExitValues(exitValues);

    	try {
    		executor.execute(cmdLine);
    	} catch (Exception e) {
			LogSystem.getInstance().printError(clientId, channelNo, formatMessageForLog("Output file does not exist."), e);
    	}
	}

	private String formatMessageForLog(String message) {
		return "Job [" + jobIdNumber + "] " + message; 
	}	
}
