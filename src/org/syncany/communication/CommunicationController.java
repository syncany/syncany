package org.syncany.communication;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

import org.syncany.Application;
import org.syncany.Constants;
import org.syncany.config.Profile;
import org.syncany.config.Settings;
import org.syncany.connection.plugins.Connection;
import org.syncany.connection.plugins.PluginInfo;
import org.syncany.connection.plugins.Plugins;
import org.syncany.db.CloneFile;
import org.syncany.db.Database;
import org.syncany.util.FileUtil;
import com.google.gson.*;

/**
 * Class for communication between GUI and daemon via JSON
 * 
 * @author Paul Steinhilber
 */
public class CommunicationController {
	
	public enum SyncanyStatus {
		// to inform the GUI about the daemons status
		inSync, updating, connecting, disconnected, error;
	}
	
	static class Message {
		private String messageType;
		private Data data;
		private Message(String messageType, Data data){
			this.messageType = messageType;
			this.data = data;
		}
		private Message(String messageType){
			this.messageType = messageType;
		}
	}
	
	static class ErrorMessage extends Message {
		private ErrorMessage(int errorNr, String errorMsg){
			super("error", new Data(errorNr, errorMsg));
		}
	}
	
	public static class Data {
		private int errorNr;
		private String comment;
		private String rootDir;
		private String appDir;
		private String cacheDir;
		private String userName;
		private String machineName;
		private String[] files;
		private String[] fileStatus;
		private SyncanyStatus status;
		private connectionSettings connection;
		private encryptionSettings encryption;
		private Data(int errorNr, String errorMsg){
			this.errorNr = errorNr;
			this.comment = errorMsg;
		}
		private Data(SyncanyStatus status) {
			this.status = status;
		}
		private Data(String[] files, String[] status) {
			this.fileStatus = status;
			this.files = files;
		}
		private Data(String comment){
			this.comment = comment;
		}
		public String getRootDir() {
			return rootDir;
		}
		public String getAppDir() {
			return appDir;
		}
		public String getCacheDir() {
			return cacheDir;
		}
		public connectionSettings getConnection() {
			return connection;
		}
	}
	
	public static class connectionSettings {
	    private String type;
	    private Map<String, String> settings;
		
	    public String getType() {
			return type;
		}
		public Map<String, String> getSettings() {
			return settings;
		}
	}
	static class encryptionSettings {
	    private String pass;
	}
	
	private static final Logger logger = Logger.getLogger(CommunicationController.class
			.getSimpleName());
	private static CommunicationController instance;
	private static Gson gson;
	private SyncanyStatus latestStatus = SyncanyStatus.connecting;
	private Application application;
	
	public static CommunicationController getInstance(){
		if (instance == null) {
			instance = new CommunicationController();
		}
		return instance;
	}
	
	private CommunicationController() {
		gson = new Gson();
	}
	
	/** called when receiving new message */
	public void processMessage(String json) {
		Message msg; 
		
		try {
			msg = gson.fromJson(json, Message.class);
			
			if (msg.messageType.equals("initialData")) {
				processInitialData(msg.data);
				return;
			} else if (msg.messageType.equals("requestFileStatus")) {
				// GUI requests filestatus -> answer with fileStatus
				processFileStatusRequests(msg.data); 
				return;
			} else if (msg.messageType.equals("shutDown")) {
				// GUI requests to shut down core
				sendAck();
				CommunicationSocket.getInstance().disconnect();
				application.doShutdown(); 
				return;
			} else if (msg.messageType.equals("echo")) {
				// return the received message
				sendMessage(msg);
				return;
			} else if (msg.messageType.equals("ack")) {
				// nothing to do here
				return;
			} else {
				// unknown message type
				logger.warning("Unknown Message Type: " + msg.messageType);
				sendMessage(new ErrorMessage(101, "Unknown Message Type: " + msg.messageType));
			}
		} catch(Exception e) {
			logger.severe("ERROR during message Processing: "+ e.getMessage());
		}
	}
	
	/** called to send a message */
	private void sendMessage(Message msg){
		String msgJson = gson.toJson(msg);
		CommunicationSocket.getInstance().send(msgJson);
	}
	
	//-------------------- -------------------- -------------------- --------------------
	//	Process Received Messages
	//-------------------- -------------------- -------------------- --------------------
	
	/** use this when starting from file */
	public void processInitialData(String json) throws Exception {   
	    Data data = gson.fromJson(json, Data.class);
	    processInitialData(data);
	}
	
	public void processInitialData(Data data) throws Exception {   
	  	Profile profile; 
    	Connection conn = null;
    	File rootFolder;
    	
    	// create the needed directories if not already present
    	FileUtil.mkdirsVia(new File (data.appDir));
    	FileUtil.mkdirsVia(new File (data.cacheDir));
    	FileUtil.mkdirsVia(new File (data.rootDir));
    	

    	/* ATTENTION: Keep this order in mind 
		 * Create and configure Settings first,
		 * then create Profile */
    	
    	// SETTING EVERYTHING IN SETTINGS-CLASS
    	Settings.createInstance(
    			new File(data.appDir),		// AppDbDir
    			new File(data.cacheDir),	// AppCacheDir
    			data.userName,				// UserName
    			data.machineName			// MachineName
    	);

    	profile = Profile.getInstance();

    	// set encryption password & salt
    	profile.getRepository().getEncryption().setPassword(data.encryption.pass);
    	// TODO: What to use as salt?
    	profile.getRepository().getEncryption().setSalt("SALT"); 
    	
    	profile.getRepository().setChunkSize(Constants.DEFAULT_CHUNK_SIZE); 

    	// Load the required plugin
    	PluginInfo plugin = Plugins.get(data.connection.type);
    	
    	if (plugin == null) {
    		sendMessage(new ErrorMessage(102, "Plugin not supported: " + data.connection.type));
    		updateStatus(SyncanyStatus.error);
    		
    		logger.severe("PLUGIN NOT SUPPORTED " + data.connection.type);
    		throw new Exception("Plugin not supported: " + data.connection.type);
    	}
    	
    	// initialize connection
    	conn = plugin.createConnection();
    	conn.init(data.connection.settings);
 
    	profile.getRepository().setConnection(conn);
    	rootFolder = new File(data.rootDir);
    	profile.setRoot(rootFolder); 
    	
        // Start app!
        try {
        	application = new Application();
            application.start();
        }
        catch (final Exception e) {
        	e.printStackTrace();
        }
	}
	
	public void processFileStatusRequests(Data data){
		String[] files = data.files;
		String[] fileStatus = new String[files.length];
		
		for(int i=0; i<files.length; i++) {
			// use CloneFile.SyncStatus to describe fileStatus
			try {
				CloneFile cf = Database.getInstance().getFileOrFolder(new File(files[i]));
				fileStatus[i] = cf.getSyncStatus().toString();
			} catch(Exception e) {
				// getting status for file i throws exception
				fileStatus[i] = "NOTFOUND";
			}
		}
		
		Message msg = new Message("fileStatus", new Data(files, fileStatus));
		sendMessage(msg);
	}
	
	//-------------------- -------------------- -------------------- --------------------
	//	Create messages to send
	//-------------------- -------------------- -------------------- --------------------
	
	/** request the initial data */
	public void requestInitialization() {	
		sendMessage(new Message("initializeMe"));
	}
	
	public void sendAck() {	
		sendMessage(new Message("ack"));
	}
	
	/** send deamons status to GUI if it changes */
	public void updateStatus(SyncanyStatus status) {
		// only update the status if it has changed
		if(status != latestStatus) {
			logger.info("send status to GUI: " + status.toString());
			
			latestStatus = status;
			sendMessage(new Message("syncanyStatus", new Data(status)));
		}
	}
	
	//-------------------- -------------------- -------------------- --------------------
	//	Needed for Testing
	//-------------------- -------------------- -------------------- --------------------
	/** only needed for testhelperclass atm */
	public Data getInitialData(String json) throws Exception {   
	    Data data = gson.fromJson(json, Data.class);
	    return data;
	}
	
	public String createMessage(String comment) {
		Message msg = new Message("echo", new Data(comment));
		return gson.toJson(msg);
	}
	
}
