package jEncoder.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author molnarr2
 */
public class LogSystem {
    
	/** The path to the log file without the extension. */
    public static final String LOG_ERROR_FILE_PATH = "/opt/log/gomedia/jEncoder.";

    // The only instance of this class.
    private static LogSystem instance = null;
    
    // This is where the logging of this class will go to.
    private BufferedWriter bwLog = null;
    // The date the logging system is using.
    private String bwLogDate = null;
    
    protected LogSystem() {
        // Exists only to defeat instantiation.
    }
    
    /** @return the only instance of this class.
     */
    public static LogSystem getInstance() {
        if(instance == null) {
            instance = new LogSystem();
        }
        return instance;
    }
    
    /** This will print an "INFO:" line. 
     * @param line should not have \n, that will be added. Only the information should be in the line.
     */
    public synchronized void printInfo(int clientId, int channelNo, String line) {
        beginFileWrite();
       
        // Write out to the file.
        try {
        	String message = "CID" + clientId + "-" + channelNo + "-T" + this.currentTime() + "  INFO: " + line + System.lineSeparator();
        	System.out.print(message);
            bwLog.write(message);
            bwLog.flush();
        } catch (IOException e) {
            ;
        }
    }

    /** This will print an "ERROR:" line. 
     * @param line should not have \n, that will be added. Only the error should be in the line.
     */
    public synchronized void printError(int clientId, int channelNo, String line, Exception e) {
        beginFileWrite();
       
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String exceptionAsString = sw.toString();    		
        
        // Write out to the file.
        try {
        	String message = "CID" + clientId + "-" + channelNo + "-T" + this.currentTime() + "  ERROR: " + line + System.lineSeparator() + "    EXCEPTION: " + exceptionAsString + System.lineSeparator();
        	System.out.print(message);
            bwLog.write(message);
            bwLog.flush();
        } catch (IOException ioe) {
            ;
        }        
    }
    
    /** This will print an "ERROR:" line. 
     * @param line should not have \n, that will be added. Only the error should be in the line.
     */
    public synchronized void printError(int clientId, int channelNo, String line, Throwable e) {
        beginFileWrite();
       
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String exceptionAsString = sw.toString();    		
        
        // Write out to the file.
        try {
        	String message = "CID" + clientId + "-" + channelNo + "-T" + this.currentTime() + "  ERROR: " + line + System.lineSeparator() + "    EXCEPTION: " + exceptionAsString + System.lineSeparator();
        	System.out.print(message);
            bwLog.write(message);
            bwLog.flush();
        } catch (IOException ioe) {
            ;
        }        
    }
    
    /** This will print an "ERROR:" line. 
     * @param line should not have \n, that will be added. Only the error should be in the line.
     */
    public synchronized void printError(int clientId, int channelNo, String line) {
        beginFileWrite();
        
        // Write out to the file.
        try {
        	String message = "CID" + clientId + "-" + channelNo + "-T" + this.currentTime() + "  ERROR: " + line + System.lineSeparator() + System.lineSeparator();
        	System.out.print(message);
            bwLog.write(message);
            bwLog.flush();
        } catch (IOException ioe) {
            ;
        }        
    }
    
    /** This will print an "ERROR:" line. 
     * @param line should not have \n, that will be added. Only the error should be in the line.
     */
    public synchronized void printError(String line, Exception e) {
        beginFileWrite();
       
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String exceptionAsString = sw.toString();    		
        
        // Write out to the file.
        try {
        	String message = this.currentTime() + "T  ERROR: " + line + System.lineSeparator() + "    EXCEPTION: " + exceptionAsString + System.lineSeparator();
        	System.out.print(message);
            bwLog.write(message);
            bwLog.flush();
        } catch (IOException ioe) {
            ;
        }        
    }
    
    /** This will print an "ERROR:" line. 
     * @param line should not have \n, that will be added. Only the error should be in the line.
     */
    public synchronized void printError(String line) {
        beginFileWrite();
        
        // Write out to the file.
        try {
        	String message = this.currentTime() + "T  ERROR: " + line + System.lineSeparator();
        	System.out.print(message);
            bwLog.write(message);
            bwLog.flush();
        } catch (IOException ioe) {
            ;
        }        
    }
    
    /** This will make sure the correct file is open and ready to be written to.
     * If the file is old, ie, from yesterday than close that file and start up a
     * new one.
     */
    private void beginFileWrite() {
        try {

            // Since the log file is already open, see if a new one needs to be opened.
            if (this.bwLog != null) {
                // The current date matches the date of the day therefore don't create another one.
                String currentDate = this.currentDate();
                if (currentDate.equals(this.bwLogDate)) {
                    return;
                }
                
                this.bwLog.close();
                this.bwLog = null;
            }

            // Create a new log file.
            String currentDate = this.currentDate();
            String fileName = LogSystem.LOG_ERROR_FILE_PATH + currentDate;
            File file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }                                    

            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            this.bwLog = new BufferedWriter(fw);
            this.bwLogDate = currentDate;
        
        } catch (IOException e) {
            this.bwLog = null;
        } 
    }
    
    /** @return the current date.
     */
    private String currentDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
        Date date = new Date();
        return dateFormat.format(date);
    }
    
    /** @return the current time. Example: HH:MM:SST
     */
    private String currentTime() {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);        
    }
}