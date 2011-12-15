package org.bitrepository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.text.SimpleDateFormat;

import org.bitrepository.access.AccessComponentFactory;
import org.bitrepository.access.getchecksums.GetChecksumsClient;
import org.bitrepository.access.getfile.GetFileClient;
import org.bitrepository.access.getfileids.GetFileIDsClient;
import org.bitrepository.bitrepositoryelements.ChecksumDataForChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumDataForFileTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.FileIDs;
import org.bitrepository.common.settings.Settings;
import org.bitrepository.modify.ModifyComponentFactory;
import org.bitrepository.modify.deletefile.DeleteFileClient;
import org.bitrepository.modify.putfile.PutFileClient;
import org.bitrepository.protocol.eventhandler.EventHandler;
import org.bitrepository.protocol.exceptions.OperationFailedException;
import org.bitrepository.settings.collectionsettings.CollectionSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicClient {
    private PutFileClient putClient;
    private GetFileClient getClient;
    private GetChecksumsClient getChecksumClient;
    private GetFileIDsClient getFileIDsClient;
    private DeleteFileClient deleteFileClient;
    private EventHandler eventHandler;
    private String logFile;
    private Settings settings;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private ArrayBlockingQueue<String> shortLog;

    
    public BasicClient(Settings settings, String logFile) {
        log.debug("---- Basic client instanciated ----");
        this.logFile = logFile;
        changeLogFiles();
        shortLog = new ArrayBlockingQueue<String>(50);
        eventHandler = new BasicEventHandler(logFile, shortLog);
        this.settings = settings;
        putClient = ModifyComponentFactory.getInstance().retrievePutClient(settings);
        getClient = AccessComponentFactory.getInstance().createGetFileClient(settings);
        getChecksumClient = AccessComponentFactory.getInstance().createGetChecksumsClient(settings);
        getFileIDsClient = AccessComponentFactory.getInstance().createGetFileIDsClient(settings);
        deleteFileClient = ModifyComponentFactory.getInstance().retrieveDeleteFileClient(settings);
    }
    
    public void shutdown() {
        putClient.shutdown();
        getClient.shutdown();
        getChecksumClient.shutdown();
        getFileIDsClient.shutdown();
    }
    
    public String putFile(String fileID, long fileSize, String URLStr) {
        URL url;
        try {
            url = new URL(URLStr);
            putClient.putFileWithId(url, fileID, fileSize, eventHandler);
            return "Placing '" + fileID + "' in Bitrepository :)";
        } catch (MalformedURLException e) {
            return "The string: '" + URLStr + "' is not a valid URL!";
        }
    }
    
    public String getFile(String fileID, String URLStr) {
        URL url;
        try {
            url = new URL(URLStr);
            getClient.getFileFromFastestPillar(fileID, url, eventHandler);
            return "Fetching '" + fileID + "' from Bitrepository :)";
        } catch (MalformedURLException e) {
            return "The string: '" + URLStr + "' is not a valid URL!";
        }
    }
    
    public String getLog() {
        File logfile = new File(logFile);
        try {
            FileReader fr = new FileReader(logfile);
            BufferedReader br = new BufferedReader(fr);
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = br.readLine()) != null) {
                result.append(line + "\n");
            }
            return result.toString();
        } catch (FileNotFoundException e) {
            return "Unable find log file... '" + logfile.getAbsolutePath() + "'";
        } catch (IOException e) {
            return "Unable to read log... '" + logfile.getAbsolutePath() + "'";
        }
    }
    
    public String getHtmlLog() {
        File logfile = new File(logFile);
        try {
            FileReader fr = new FileReader(logfile);
            BufferedReader br = new BufferedReader(fr);
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = br.readLine()) != null) {
                result.append(line + "<br>");
            }
            return result.toString();
        } catch (FileNotFoundException e) {
            return "Unable find log file... '" + logfile.getAbsolutePath() + "'";
        } catch (IOException e) {
            return "Unable to read log... '" + logfile.getAbsolutePath() + "'";
        }
    }
    
    public String getShortHtmlLog() {
    	StringBuilder sb = new StringBuilder();
    	List<String> entries = new ArrayList<String>();
    	for(String entry : shortLog) {
    		entries.add(entry);
    	}
    	Collections.reverse(entries);
    	for(String entry : entries) {
    		sb.append(entry + "<br>");
    	}
    	
    	return sb.toString();
    }
    
    public String getSettingsSummary() {
        StringBuilder sb = new StringBuilder();
        CollectionSettings collectionSettings = settings.getCollectionSettings();
        sb.append("CollectionID: <i>" + collectionSettings.getCollectionID() + "</i><br>");
        sb.append("Pillar(s) in configuration: <br> <i>");
        List<String> pillarIDs = collectionSettings.getClientSettings().getPillarIDs(); 
        for(String pillarID : pillarIDs) {
        	sb.append("&nbsp;&nbsp;&nbsp; " + pillarID + "<br>");
        }
        sb.append("</i>");
        sb.append("Messagebus URL: <br> &nbsp;&nbsp;&nbsp; <i>"); 
        sb.append(collectionSettings.getProtocolSettings().getMessageBusConfiguration().getURL() + "</i><br>");
        return sb.toString();
    }
    
    public Map<String, Map<String, String>> getChecksums(String fileIDsText, String checksumType, String salt) {
    	ChecksumSpecTYPE checksumSpecItem = new ChecksumSpecTYPE();
    	if(salt != null || !salt.equals("")) {
    		checksumSpecItem.setChecksumSalt(salt);	
    	}
    	checksumSpecItem.setChecksumType(checksumType);
    	FileIDs fileIDs = new FileIDs();
    	fileIDs.setFileID(fileIDsText);

    	GetChecksumsResults results = new GetChecksumsResults();
    	GetChecksumsEventHandler handler = new GetChecksumsEventHandler(results, eventHandler);
    	
    	try {
            getChecksumClient.getChecksums(settings.getCollectionSettings().getClientSettings().getPillarIDs(),
                    fileIDs, checksumSpecItem, null, handler, "Arf arf, deliver those checksums");
        } catch (OperationFailedException e1) {
            // Jonas this should not throw exceptions... bleh!
        }
    	
        try {
            while(!results.isDone() && !results.hasFailed()) {
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            // Uhm, we got aborted, should return error..
        }
    	
    	return results.getResults();
    }
    
    public GetFileIDsResults getFileIDs(String fileIDsText, boolean allFileIDs) {
    	GetFileIDsResults results = new GetFileIDsResults(
    			settings.getCollectionSettings().getClientSettings().getPillarIDs());
    	GetFileIDsEventHandler handler = new GetFileIDsEventHandler(results, eventHandler);
    	FileIDs fileIDs = new FileIDs();
    	
    	if(allFileIDs) {
    		fileIDs.setAllFileIDs(allFileIDs);
    	} else {
    	    fileIDs.setFileID(fileIDsText);
    	}
    	try {
    	    getFileIDsClient.getFileIDs(settings.getCollectionSettings().getClientSettings().getPillarIDs(),
    	            fileIDs, null, handler, "Deliver my fileIDs garrh");

    	    while(!results.isDone() && !results.hasFailed()) {
    	        Thread.sleep(500);
    	    }
	    } catch (OperationFailedException e) {
	            //This should not happen Jonas!
		} catch (InterruptedException e) {
			// Uhm, we got aborted, should return error..
		}
		
    	return results;
    }
    
    public String deleteFile(String fileID, String pillarID, String deleteChecksum, String deleteChecksumType, 
            String deleteChecksumSalt, String approveChecksumType, String approveChecksumSalt) {
        if(fileID == null) {
            return "Missing fileID!";
        }
        if(pillarID == null || !settings.getCollectionSettings().getClientSettings().getPillarIDs().contains(pillarID)) {
            return "Missing or unknown pillarID!";
        }
        if(deleteChecksum == null || deleteChecksum.equals("")) {
            return "Checksum for pillar check is missing";
        }
        if(deleteChecksumType == null || deleteChecksumType.equals("")) {
            return "Checksum type for pillar check is invalid";
        }
        ChecksumDataForFileTYPE verifyingChecksum = new ChecksumDataForFileTYPE();
        verifyingChecksum.setChecksumValue(deleteChecksum);
        ChecksumSpecTYPE deleteChecksumSpec = new ChecksumSpecTYPE();
        if(deleteChecksumSalt == null || !deleteChecksumSalt.equals("")) {
            deleteChecksumSalt = null;
        }
        deleteChecksumSpec.setChecksumSalt(deleteChecksumSalt);
        deleteChecksumSpec.setChecksumType(deleteChecksumType);
        Date now = new Date();
        verifyingChecksum.setCalculationTimestamp(XMLGregorianCalendarConverter.asXMLGregorianCalendar(now));
        verifyingChecksum.setChecksumSpec(deleteChecksumSpec);
        ChecksumSpecTYPE requestedChecksumSpec = null;
        log.info("----- Got DeleteFileRequest with approveChecksumtype = " + approveChecksumType);
        if(approveChecksumType != null && !approveChecksumType.equals("disabled")) {
            requestedChecksumSpec = new ChecksumSpecTYPE();
            requestedChecksumSpec.setChecksumType(approveChecksumType);
            requestedChecksumSpec.setChecksumSalt(approveChecksumSalt);
        }
        
        try {
        deleteFileClient.deleteFile(fileID, pillarID, verifyingChecksum, requestedChecksumSpec, 
                eventHandler, "Kick that file");
        } catch (OperationFailedException e) {
            //This should not happen Jonas!
        }
        return "Deleting file";
    }
    
    private void changeLogFiles() {
        File oldLogFile = new File(logFile);
        String date = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String newName = logFile + "-" + date;
        System.out.println("Moving old log file to: " + newName);
        oldLogFile.renameTo(new File(newName));
    }
    
}
