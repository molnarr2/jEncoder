package jEncoder.media;

import jEncoder.Configuration;
import jEncoder.util.LogSystem;
import jEncoder.web.Web;

import java.util.LinkedList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/** There is always one thread dedicated to just archive video editing.
 * */
public class MediaEncoderEngine {
	/** Total number of threads that can work. Excludes the archive video editing thread. */
	private static int MAX_WORKERS;
		
	/** How much time this worker will sleep before checking if there is any work to consume. (1000 = 1 second). */
	private static final int SLEEP_TIME_FOR_WORKER = 1000 * 10;
	
	/** How much time to check the web site for new work.  (1000 = 1 second). */
	private static final int SLEEP_CHECK_FOR_NEW_WORK = 1000 * 60;

	/** The current job ticket #. (Just an unique number that each job is given to easier track in the logs so that one output of the parameters can be printed). */
	private static int jobTicketNumber = 0;	
	
	/** This is the list of work orders that need to be processed. */
	LinkedList<ProcessParameters> ltWorkOrders = new LinkedList<ProcessParameters>();
	
	/** This is the list of work orders that need to be processed. (Archive only) */
	LinkedList<ProcessParameters> ltArchiveWorkOrders = new LinkedList<ProcessParameters>();
	
    // The only instance of this class.
    private static MediaEncoderEngine instance = null;
    
    // [start] Methods: Constructor and getInstance()

    protected MediaEncoderEngine() {
    	// Number of workers comes from the server-settings.conf configuration file.
    	MAX_WORKERS = Configuration.getInstance().getEncoderWorkers();
    }
    
    /** @return the only instance of this class.
     */
    public static MediaEncoderEngine getInstance() {
        if(instance == null) {
            instance = new MediaEncoderEngine();
        }
        return instance;
    }

    // [end]        
	
	/** This will start up the engine and create the worker threads. They do not die.
	 * */
	public void startEngine() {
		
		// Start up the worker threads.
		for (int i=0; i < MAX_WORKERS; i++) {
			new Thread() {				
				public void run() {
					// Thread does not stop.
					while (true) {
						
						// Keep working until there is no more work.
						while (true) {				
							// Check for some work to do.
							ProcessParameters pp = getOrder();
							if (pp == null)
								break;
							
							// Work on the load.
							MediaProcess media = new MediaProcess(pp);
							media.process();
						}
						
						// Sleep for a time.
						try {
							sleep(SLEEP_TIME_FOR_WORKER);
						} catch (Exception e) {
							LogSystem.getInstance().printError("Unable to sleep.", e);
						}
					}
				}
			}.start();
		}
		
		// Start up the archive video thread worker.
		new Thread() {				
			public void run() {
				// Thread does not stop.
				while (true) {
					
					// Keep working until there is no more work.
					while (true) {				
						// Check for some work to do.
						ProcessParameters pp = getArchiveOnlyOrder();
						if (pp == null)
							break;
						
						// Work on the load.
						MediaProcess media = new MediaProcess(pp);
						media.process();
					}
					
					// Sleep for a time.
					try {
						sleep(SLEEP_TIME_FOR_WORKER);
					} catch (Exception e) {
						LogSystem.getInstance().printError("Unable to sleep.", e);
					}
				}
			}
		}.start();

		// Start up the thread that checks for new encoding jobs.
		new Thread() {				
			public void run() {
				// This is a never ending thread.
				// Now check every so often for encoding jobs from the web site.
				while (true) {
					
					// Check for encoding jobs and add them to the order list.
					checkForEncodingJobs();
										
					// Sleep for a while and then check for more work.
					try {
						sleep(SLEEP_CHECK_FOR_NEW_WORK);
					} catch (Exception e) {
						LogSystem.getInstance().printError("Unable to sleep.", e);
					}
				}
			}
		}.start();
		
	}
	
	/** This will check for encoding jobs to work on.
	 * */
	private synchronized void checkForEncodingJobs() {
		try {
			
			System.out.println("CHECKING FOR ENCODING JOBS");
			
			JSONObject obj = Web.getInstance().getEncodingJobs();
			if (obj == null)
				return;

			// Process the archive videos.
			JSONArray arr = (JSONArray)obj.get("archive");						
			int cnt = arr.size();
			for (int i=0; i < cnt; i++) {
				JSONObject aJob = (JSONObject)arr.get(i);
				ProcessParameters pp = ProcessParameters.newProcessParametersUsingJSON(aJob, getNextJobTicketNumber());
				LogSystem.getInstance().printInfo(pp.cid, pp.cno, "Job [" + pp.jobIdNumber + "] ARCHIVE Received [" + pp + "]");

				// Archive videos are clipped so use the the archive thread.
				if (pp.cmd == ProcessParameters.COMMAND.ARCHIVE)
					ltArchiveWorkOrders.add(pp);
				else				
					ltWorkOrders.add(pp);
			}
			
			// Process the InOut videos.
			arr = (JSONArray)obj.get("inout");
			cnt = arr.size();
			for (int i=0; i < cnt; i++) {
				JSONObject aJob = (JSONObject)arr.get(i);
				ProcessParameters pp = ProcessParameters.newProcessParametersUsingJSON(aJob, getNextJobTicketNumber());
				LogSystem.getInstance().printInfo(pp.cid, pp.cno, "Job [" + pp.jobIdNumber + "] INOUT Received [" + pp + "]");
				ltWorkOrders.add(pp);
			}

			// Process the download history.
			arr = (JSONArray)obj.get("download");						
			cnt = arr.size();
			for (int i=0; i < cnt; i++) {
				JSONObject aJob = (JSONObject)arr.get(i);
				ProcessParameters pp = ProcessParameters.newProcessParametersUsingJSON(aJob, getNextJobTicketNumber());
				LogSystem.getInstance().printInfo(pp.cid, pp.cno, "Job [" + pp.jobIdNumber + "] DOWNLOAD Received [" + pp + "]");

				// Download videos are not re-encoded so it is a quick process, put it in the archive work orders.
				ltArchiveWorkOrders.add(pp);
			}
			
		} catch (Exception e) {
			LogSystem.getInstance().printError("Check for Encoding job error.", e);
		}
		
	}
	
	/** @return a new order or null if none exist. These are archive only videos.
	 * */
	private synchronized ProcessParameters getArchiveOnlyOrder() {		
		if (ltArchiveWorkOrders.size() == 0)
			return null;
		return ltArchiveWorkOrders.pop();
	}
	
	/** @return a new order or null if none exist. These are recording or in/out videos.
	 * */
	private synchronized ProcessParameters getOrder() {
		if (ltWorkOrders.size() == 0)
			return null;
		return ltWorkOrders.pop();
	}

	/** @return an unique string ID in increment of the encoding job number.
	 * */
	private synchronized String getNextJobTicketNumber() {		
		jobTicketNumber++;
		return String.format("J%06d", jobTicketNumber); 
	}
}
