package jEncoder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import jEncoder.util.LogSystem;

public class Configuration {
	private static Configuration instance = null;
	
	private final static String CONFIGURATION_FILEPATH = "/opt/config/server-settings.conf";
	
	private String webServerName = "";
	
	private String webServerRootPath = "";
	
	private String evostreamServerName = "";
	
	/** Number of encoder workers. Default is 2.*/
	private int encoderWorkers = 2;
	
	protected Configuration() {
	}
	
	public static Configuration getInstance() {
		if (instance == null) {
			instance = new Configuration();
			instance.loadConfigurationFile();
		}
		return instance;
	}
    
	private void loadConfigurationFile() {
		// Load in the configuration file fields.
		try {
			File file = new File (CONFIGURATION_FILEPATH);
			Reader ir = new InputStreamReader(new FileInputStream(file));
			BufferedReader in = new BufferedReader(ir);
			String line;
			while ((line = in.readLine()) != null) {
				// Skip the comment lines.
				if (line.indexOf('#') != -1)
					continue;
				
				
				String columns[] = line.split("=");
				if (columns.length >= 2) {
					String key = columns[0];
					String value = columns[1];
					
					if ("WEB_SERVER_NAME".equals(key))
						webServerName = value;
					else if ("EVOSTREAM_SERVER_NAME".equals(key))
						evostreamServerName = value;					
					else if ("WEB_SERVER_ROOT_PATH".equals(key)) {
						if (value == null)
							webServerRootPath = "";
						else 
							webServerRootPath = value;
					} else if ("JENCODER_WORKERS".equals(key)) {
						encoderWorkers = Integer.parseInt(value);
					}
				}				
			}
			in.close();
			
		} catch (Exception e) {
	    	LogSystem.getInstance().printError("IpAddressExclude.loadConfigurationFile() Unable to process the configuration file: " + CONFIGURATION_FILEPATH);
		}
	}
	
	public int getEncoderWorkers() {
		return encoderWorkers;
	}
	
	public String getEvostreamServerName() {
		return evostreamServerName;
	}
	
	public String getWebServerName() {
		return webServerName;
	}
	
	public String getWebServerRootPath() {
		return webServerRootPath;
	}
}
