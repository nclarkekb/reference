/*
 * #%L
 * Bitrepository Reference Pillar
 * 
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2010 - 2012 The State and University Library, The Royal Library and The State Archives, Denmark
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.bitrepository.pillar.checksumpillar.messagehandler;

import java.io.IOException;
import java.net.URL;

import org.bitrepository.bitrepositoryelements.ChecksumDataForFileTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.ResponseCode;
import org.bitrepository.bitrepositoryelements.ResponseInfo;
import org.bitrepository.bitrepositorymessages.ReplaceFileFinalResponse;
import org.bitrepository.bitrepositorymessages.ReplaceFileProgressResponse;
import org.bitrepository.bitrepositorymessages.ReplaceFileRequest;
import org.bitrepository.common.ArgumentValidator;
import org.bitrepository.common.utils.CalendarUtils;
import org.bitrepository.pillar.checksumpillar.cache.ChecksumStore;
import org.bitrepository.pillar.common.PillarContext;
import org.bitrepository.pillar.exceptions.InvalidMessageException;
import org.bitrepository.protocol.CoordinationLayerException;
import org.bitrepository.protocol.FileExchange;
import org.bitrepository.protocol.ProtocolComponentFactory;
import org.bitrepository.protocol.utils.Base16Utils;
import org.bitrepository.protocol.utils.ChecksumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for handling the ReplaceFile operation.
 * In the positive scenario: <ul> 
 * <li> It starts by validating the message </li> 
 * <li> Afterwards it sends a positive progress to tell that the new file is being downloaded </li>
 * <li> Then the new file is downloaded to 'tempDir' </li>
 * <li> A progress message is then send to tell that the new file is replacing the old one.</li>
 * <li> Then it performs the replacement of the file by deleting the old one (moving it from 'fileDir' to 'retainDir') 
 * and then moves the new file from 'tempDir' to 'fileDir'</li>
 * <li> Finally the complete message is send.</li>
 * </ul>
 * If anything goes wrong a FinalResponse is send telling about the failure.
 */
public class ReplaceFileRequestHandler extends ChecksumPillarMessageHandler<ReplaceFileRequest> {
    /** The log.*/
    private Logger log = LoggerFactory.getLogger(getClass());
    
    /**
     * Constructor.
     * @param context The context of the message handler.
     * @param refCache The cache for the checksum data.
     */
    public ReplaceFileRequestHandler(PillarContext context, ChecksumStore refCache) {
        super(context, refCache);
    }

    @Override
    public void handleMessage(ReplaceFileRequest message) {
        ArgumentValidator.checkNotNull(message, "ReplaceFileRequest message");

        try {
            validateMessage(message);
            sendProgressMessageDownloadNewFile(message);
            String newChecksum = downloadTheNewFile(message);
            sendProgressMessageDeleteOldFile(message);
            ChecksumDataForFileTYPE requestedOldChecksum = calculateChecksumOnOldFile(message);
            replaceTheEntry(message, newChecksum);
            ChecksumDataForFileTYPE requestedNewChecksum = calculateChecksumOnNewFile(message);
            sendFinalResponse(message, requestedOldChecksum, requestedNewChecksum);
        } catch (InvalidMessageException e) {
            sendFailedResponse(message, e.getResponseInfo());
        } catch (IllegalArgumentException e) {
            log.warn("Caught IllegalArgumentException. Message '" + message + "'.", e);
            getAlarmDispatcher().handleIllegalArgumentException(e);
        } catch (RuntimeException e) {
            log.warn("Internal RunTimeException caught. Sending response for 'error at my end'.", e);
            ResponseInfo fri = new ResponseInfo();
            fri.setResponseCode(ResponseCode.FAILURE);
            fri.setResponseText("Error: " + e.getMessage());
            sendFailedResponse(message, fri);
        }
    }

    /**
     * Method for validating the content of the message.
     * @param message The message requesting the operation, which should be validated.
     * @return Whether it was valid.
     */
    protected void validateMessage(ReplaceFileRequest message) {
        // Validate the message.
        validateBitrepositoryCollectionId(message.getCollectionID());
        validatePillarId(message.getPillarID());
        validateChecksumSpec(message.getChecksumRequestForExistingFile());
        validateChecksumSpec(message.getChecksumRequestForNewFile());
        if(message.getChecksumDataForExistingFile() != null) {
            validateChecksumSpec(message.getChecksumDataForExistingFile().getChecksumSpec());
        }
        if(message.getChecksumDataForNewFile() != null) {
            validateChecksumSpec(message.getChecksumDataForNewFile().getChecksumSpec());
        }
        

        // Validate, that we have the requested file.
        if(!getCache().hasFile(message.getFileID())) {
            ResponseInfo responseInfo = new ResponseInfo();
            responseInfo.setResponseCode(ResponseCode.FILE_NOT_FOUND_FAILURE);
            responseInfo.setResponseText("The file '" + message.getFileID() + "' has been requested, but we do "
                    + "not have that file!");
            throw new InvalidMessageException(responseInfo);
        }
        
        // validate that a checksum for the old file has been given.
        ChecksumDataForFileTYPE checksumData = message.getChecksumDataForExistingFile();
        ChecksumSpecTYPE checksumType = checksumData.getChecksumSpec();
        // TODO add a check for a given setting is set to true.
        if(checksumType == null) {
            ResponseInfo responseInfo = new ResponseInfo();
            responseInfo.setResponseCode(ResponseCode.FAILURE);
            responseInfo.setResponseText("A checksum for replacing a file is required!");
            throw new InvalidMessageException(responseInfo);
        }
        
        // calculate and validate the checksum of the file.
        String checksum = getCache().getChecksum(message.getFileID());
        String requestedChecksum = Base16Utils.decodeBase16(checksumData.getChecksumValue());
        if(!checksum.equals(requestedChecksum)) {
            // Log the different checksums, but do not send the right checksum back!
            log.info("Failed to handle replace operation on file '" + message.getFileID() + "' since the request had "
                    + "the checksum '" + requestedChecksum + "' where our local file has the value '" + checksum 
                    + "'. Sending alarm and respond failure.");
            String errMsg = "Requested to replace the file '" + message.getFileID() + "' with checksum '"
                    + requestedChecksum + "', but our file had a different checksum.";
            getAlarmDispatcher().sendInvalidChecksumAlarm(message.getFileID(), errMsg);
            
            ResponseInfo responseInfo = new ResponseInfo();
            responseInfo.setResponseCode(ResponseCode.EXISTING_FILE_CHECKSUM_FAILURE);
            responseInfo.setResponseText(errMsg);
            throw new InvalidMessageException(responseInfo);
        }
    }
    
    /**
     * Sends a progress message to tell that the replacement is happening.
     * @param message The request to base the progress response upon.
     */
    private void sendProgressMessageDownloadNewFile(ReplaceFileRequest message) {       
        ReplaceFileProgressResponse response = createProgressResponse(message);
        String responseText = "Progress: downloading the new file from: '" + message.getFileAddress() + "'";
        log.debug(responseText);
        
        ResponseInfo responseInfo = new ResponseInfo();
        responseInfo.setResponseCode(ResponseCode.OPERATION_ACCEPTED_PROGRESS);
        responseInfo.setResponseText(responseText);
        
        response.setResponseInfo(responseInfo);
        getMessageBus().sendMessage(response);
    }
    
    /**
     * Downloading the new file to replace the old one. 
     * Also validates against the given checksum. Will log a warning, if no checksum for validation is in the request.
     * @param message The request containing the location of the file and the checksum of it.
     */
    @SuppressWarnings("deprecation")
    private String downloadTheNewFile(ReplaceFileRequest message) {
        log.debug("Retrieving the data to be stored from URL: '" + message.getFileAddress() + "'");
        FileExchange fe = ProtocolComponentFactory.getInstance().getFileExchange();

        String checksum = null;
        try {
            checksum = ChecksumUtils.generateChecksum(fe.downloadFromServer(new URL(message.getFileAddress())), 
                    getChecksumType());
        } catch (IOException e) {
            throw new CoordinationLayerException("Could not download the file '" + message.getFileID() 
                    + "' from the url '" + message.getFileAddress() + "'.", e);
        }
        
        ChecksumDataForFileTYPE csType = message.getChecksumDataForNewFile();
        if(csType != null) {
            String requestedChecksum = Base16Utils.decodeBase16(csType.getChecksumValue());
            if(!checksum.equals(requestedChecksum)) {
                log.error("Expected checksums '" + requestedChecksum + "' but the checksum was '" 
                        + checksum + "' for the file '" + message.getFileID() + "'");
                ResponseInfo responseInfo = new ResponseInfo();
                responseInfo.setResponseCode(ResponseCode.NEW_FILE_CHECKSUM_FAILURE);
                responseInfo.setResponseText("Wrong checksum! Expected: [" + requestedChecksum 
                        + "], but calculated: [" + checksum + "]");
                throw new InvalidMessageException(responseInfo);
            }
        } else {
            // TODO is such a checksum required?
            log.warn("No checksum for validating the new file.");
        }
        
        return checksum;
    }
    
    /**
     * Replaces the old entry with the new one.
     * @param message The request for replacing the entry.
     * @param newChecksum The new checksum to replace the old one with.
     */
    private void replaceTheEntry(ReplaceFileRequest message, String newChecksum) {
        String oldChecksum = Base16Utils.decodeBase16(message.getChecksumDataForExistingFile().getChecksumValue());
        getCache().replaceEntry(message.getFileID(), oldChecksum, newChecksum);
    }

    /**
     * Sends a progress message to tell that the replacement is happening.
     * @param message The request to base the progerss response upon.
     */
    private void sendProgressMessageDeleteOldFile(ReplaceFileRequest message) {       
        ReplaceFileProgressResponse response = createProgressResponse(message);
        String responseText = "Progress: deleting the old file '" + message.getFileID() + "' and replacing it with '"
                + message.getFileAddress() + "'";
        log.debug(responseText);
        
        ResponseInfo responseInfo = new ResponseInfo();
        responseInfo.setResponseCode(ResponseCode.OPERATION_ACCEPTED_PROGRESS);
        responseInfo.setResponseText(responseText);
        
        response.setResponseInfo(responseInfo);
        getMessageBus().sendMessage(response);
    }
    
    /**
     * Calculates the checksum for the old file.
     * ONLY USE BEFORE THE FILE HAS BEEN REMOVED FROM THE ARCHIVE!
     * @param message The message containing the request for the checksum to be calculated, along with the 
     * specification about which algorithm and salt to use.
     * @return The checksum data for the old file in the archive.
     * If no checksum specification for the 'old' file has been defined, then an null is returned.
     */
    private ChecksumDataForFileTYPE calculateChecksumOnOldFile(ReplaceFileRequest message) {
        ChecksumSpecTYPE csType = message.getChecksumRequestForExistingFile();
        if(csType != null) {
            return calculatedChecksumForFile(csType, message.getFileID());
        }
        
        return null;
    }

    /**
     * Calculates the checksum for the new file.
     * ONLY USE WHEN THE FILE HAS BEEN MOVED TO THE ARCHIVE!
     * @param message The message containing the request for the checksum to be calculated, along with the 
     * specification about which algorithm and salt to use.
     * @return The checksum data for the new file in the archive.
     * If no checksum specification for the 'old' file has been defined, then an null is returned.
     */
    private ChecksumDataForFileTYPE calculateChecksumOnNewFile(ReplaceFileRequest message) {
        ChecksumSpecTYPE csType = message.getChecksumRequestForNewFile();
        if(csType != null) {
            return calculatedChecksumForFile(csType, message.getFileID());
        }
        
        return null;
    }

    /**
     * Calculates the specified checksum on the file in the archive with the given file id.
     * @param checksumType The specification about which type of checksum to calculate.
     * @param fileId The id of the file to calculate the checksum upon. 
     * @return The checksum for the given file.
     */
    private ChecksumDataForFileTYPE calculatedChecksumForFile(ChecksumSpecTYPE checksumType, String fileId) {
        ChecksumDataForFileTYPE res = new ChecksumDataForFileTYPE();
        
        String checksum = getCache().getChecksum(fileId);
        
        res.setChecksumSpec(checksumType);
        res.setCalculationTimestamp(CalendarUtils.getNow());
        res.setChecksumValue(Base16Utils.encodeBase16(checksum));
        
        return res;
    }
    
    /**
     * Sends the final response for a successful finish of the operation.
     * @param message The message to base the response upon.
     * @param requestedOldChecksum [OPTIONAL] The requested checksum for the old file.
     * @param requestedNewChecksum [OPTIONAL] The requested checksum for the new file.
     */
    private void sendFinalResponse(ReplaceFileRequest message, ChecksumDataForFileTYPE requestedOldChecksum, 
            ChecksumDataForFileTYPE requestedNewChecksum) {
        ReplaceFileFinalResponse response = createFinalResponse(message);
        
        ResponseInfo ri = new ResponseInfo();
        ri.setResponseCode(ResponseCode.OPERATION_COMPLETED);
        ri.setResponseText("Successfully replaced the file '" + message.getFileID() + "' as requested!");
        response.setResponseInfo(ri);
        
        response.setChecksumDataForNewFile(requestedNewChecksum);
        response.setChecksumDataForExistingFile(requestedOldChecksum);
        
        getMessageBus().sendMessage(response);
    }
    
    /**
     * Sends the final response for a failed operation.
     * @param message The message to base the response upon.
     * @param ri The reason it has failed.
     */
    private void sendFailedResponse(ReplaceFileRequest message, ResponseInfo ri) {
        ReplaceFileFinalResponse response = createFinalResponse(message);
        response.setResponseInfo(ri);
        
        getMessageBus().sendMessage(response);
    }
    
    /**
     * Creates the generic ReplaceFileFinalResponse based on the request message.
     * Missing fields:
     * <br/> ResponseInfo
     * <br/> PillarChecksumSpec
     * @param message The ReplaceFileRequest to base the response upon.
     * @return The ReplaceFileFinalResponse based on the request.
     */
    private ReplaceFileProgressResponse createProgressResponse(ReplaceFileRequest message) {
        ReplaceFileProgressResponse res = new ReplaceFileProgressResponse();
        res.setCollectionID(getSettings().getCollectionID());
        res.setCorrelationID(message.getCorrelationID());
        res.setFileAddress(message.getFileAddress());
        res.setFileID(message.getFileID());
        res.setFrom(getSettings().getReferenceSettings().getPillarSettings().getPillarID());
        res.setMinVersion(MIN_VERSION);
        res.setPillarID(getSettings().getReferenceSettings().getPillarSettings().getPillarID());
        res.setReplyTo(getSettings().getReferenceSettings().getPillarSettings().getReceiverDestination());
        res.setTo(message.getReplyTo());
        res.setVersion(VERSION);
        res.setPillarChecksumSpec(getChecksumType());

        return res;
    }
    
    /**
     * Creates the generic ReplaceFileFinalResponse based on the request message.
     * Missing fields:
     * <br/> ResponseInfo
     * <br/> ChecksumDataForFile
     * <br/> PillarChecksumSpec
     * @param message The ReplaceFileRequest to base the response upon.
     * @return The ReplaceFileFinalResponse based on the request.
     */
    private ReplaceFileFinalResponse createFinalResponse(ReplaceFileRequest message) {
        ReplaceFileFinalResponse res = new ReplaceFileFinalResponse();
        res.setCollectionID(getSettings().getCollectionID());
        res.setCorrelationID(message.getCorrelationID());
        res.setFileAddress(message.getFileAddress());
        res.setFileID(message.getFileID());
        res.setFrom(getSettings().getReferenceSettings().getPillarSettings().getPillarID());
        res.setMinVersion(MIN_VERSION);
        res.setPillarID(getSettings().getReferenceSettings().getPillarSettings().getPillarID());
        res.setReplyTo(getSettings().getReferenceSettings().getPillarSettings().getReceiverDestination());
        res.setTo(message.getReplyTo());
        res.setVersion(VERSION);
        res.setPillarChecksumSpec(getChecksumType());

        return res;
    }
}