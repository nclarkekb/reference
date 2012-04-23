/*
 * #%L
 * bitrepository-access-client
 * *
 * $Id: PutFileRequestHandler.java 687 2012-01-09 12:56:47Z ktc $
 * $HeadURL: https://sbforge.org/svn/bitrepository/bitrepository-reference/trunk/bitrepository-reference-pillar/src/main/java/org/bitrepository/pillar/messagehandler/PutFileRequestHandler.java $
 * %%
 * Copyright (C) 2010 - 2011 The State and University Library, The Royal Library and The State Archives, Denmark
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
import org.bitrepository.bitrepositoryelements.ResponseCode;
import org.bitrepository.bitrepositoryelements.ResponseInfo;
import org.bitrepository.bitrepositorymessages.PutFileFinalResponse;
import org.bitrepository.bitrepositorymessages.PutFileProgressResponse;
import org.bitrepository.bitrepositorymessages.PutFileRequest;
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
 * Class for performing the PutFile operation.
 * TODO handle error scenarios.
 */
public class PutFileRequestHandler extends ChecksumPillarMessageHandler<PutFileRequest> {
    /** The log.*/
    private Logger log = LoggerFactory.getLogger(getClass());
    
    /**
     * Constructor.
     * @param context The context of the message handler.
     * @param refCache The cache for the checksum data.
     */
    public PutFileRequestHandler(PillarContext context, ChecksumStore refCache) {
        super(context, refCache);
    }
    
    /**
     * Handles the identification messages for the PutFile operation.
     * @param message The IdentifyPillarsForPutFileRequest message to handle.
     */
    public void handleMessage(PutFileRequest message) {
        ArgumentValidator.checkNotNull(message, "PutFileRequest message");
        
        try {
            validateMessage(message);
            tellAboutProgress(message);
            retrieveFile(message);
            sendFinalResponse(message);
        } catch (InvalidMessageException e) {
            sendFailedResponse(message, e.getResponseInfo());
        } catch (IllegalArgumentException e) {
            log.warn("Caught IllegalArgumentException. Possible intruder -> Sending alarm! ", e);
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
     * Validates the message.
     * @param message The message to validate.
     */
    private void validateMessage(PutFileRequest message) {
        // validate message
        validateBitrepositoryCollectionId(message.getCollectionID());
        validatePillarId(message.getPillarID());
        validateChecksumSpec(message.getChecksumDataForNewFile().getChecksumSpec());
        validateChecksumSpec(message.getChecksumRequestForNewFile());
        
        // verify, that we already have the file
        if(getCache().hasFile(message.getFileID())) {
            log.warn("Cannot perform put for a file, '" + message.getFileID() 
                    + "', which we already have within the archive");
            // Then tell the mediator, that we failed.
            ResponseInfo fri = new ResponseInfo();
            fri.setResponseCode(ResponseCode.DUPLICATE_FILE_FAILURE);
            fri.setResponseText("File is already within archive.");
            throw new InvalidMessageException(fri);
        }
    }
    
    /**
     * Method for sending a progress response.
     * @param message The message to base the response upon.
     */
    private void tellAboutProgress(PutFileRequest message) {
        log.info("Respond that we are starting to retrieve the file.");
        PutFileProgressResponse pResponse = createPutFileProgressResponse(message);
        
        // Needs to fill in: AuditTrailInformation, PillarChecksumSpec, ProgressResponseInfo
        pResponse.setPillarChecksumSpec(null);
        ResponseInfo prInfo = new ResponseInfo();
        prInfo.setResponseCode(ResponseCode.OPERATION_ACCEPTED_PROGRESS);
        prInfo.setResponseText("Started to receive date.");  
        pResponse.setResponseInfo(prInfo);
        
        log.info("Sending ProgressResponseInfo: " + prInfo);
        getMessageBus().sendMessage(pResponse);
    }
    
    /**
     * Retrieves the actual data, validates it and stores it.
     * @param message The request to for the file to put.
     */
    @SuppressWarnings("deprecation")
    private void retrieveFile(PutFileRequest message) {
        log.debug("Retrieving the data to be stored from URL: '" + message.getFileAddress() + "'");
        FileExchange fe = ProtocolComponentFactory.getInstance().getFileExchange();
        
        String calculatedChecksum = null;
        try {
            calculatedChecksum = ChecksumUtils.generateChecksum(fe.downloadFromServer(new URL(message.getFileAddress())),
                    getChecksumType());
        } catch (IOException e) {
            throw new CoordinationLayerException("Could not download the file '" + message.getFileID() 
                    + "' from the url '" + message.getFileAddress() + "'.", e);
        }
        
        if(message.getChecksumDataForNewFile() != null) {
            ChecksumDataForFileTYPE csType = message.getChecksumDataForNewFile();
            String givenChecksum = Base16Utils.decodeBase16(csType.getChecksumValue());
            if(!calculatedChecksum.equals(givenChecksum)) {
                log.error("Expected checksums '" + givenChecksum + "' but the checksum was '" 
                        + calculatedChecksum + "'.");
                throw new IllegalStateException("Wrong checksum! Expected: [" + givenChecksum 
                + "], but calculated: [" + calculatedChecksum + "]");
            }
        } else {
            // TODO is such a checksum required?
            log.warn("No checksums for validating the retrieved file.");
        }
        
        getCache().putEntry(message.getFileID(), calculatedChecksum);
    }
    
    /**
     * Method for sending the final response for the requested put operation.
     * @param message The message requesting the put operation.
     */
    private void sendFinalResponse(PutFileRequest message) {
        
        PutFileFinalResponse fResponse = createPutFileFinalResponse(message);
        
        // insert: AuditTrailInformation, ChecksumsDataForNewFile, FinalResponseInfo, PillarChecksumSpec
        ResponseInfo frInfo = new ResponseInfo();
        frInfo.setResponseCode(ResponseCode.OPERATION_COMPLETED);
        frInfo.setResponseText("The put has be finished.");
        fResponse.setResponseInfo(frInfo);
        fResponse.setPillarChecksumSpec(null); // NOT A CHECKSUM PILLAR
        
        ChecksumDataForFileTYPE checksumForValidation = new ChecksumDataForFileTYPE();
        
        if(message.getChecksumRequestForNewFile() != null) {
            checksumForValidation.setChecksumValue(getCache().getChecksum(message.getFileID()).getBytes());
            checksumForValidation.setCalculationTimestamp(CalendarUtils.getNow());
            checksumForValidation.setChecksumSpec(message.getChecksumRequestForNewFile());
            log.info("Requested checksum calculated: " + checksumForValidation);
        } else {
            // TODO is such a request required?
            log.info("No checksum validation requested.");
            checksumForValidation = null;
        }
        
        fResponse.setChecksumDataForNewFile(checksumForValidation);
        
        // Finish by sending final response.
        log.info("Sending PutFileFinalResponse: " + fResponse);
        getMessageBus().sendMessage(fResponse);
    }
    
    /**
     * Method for sending a response telling, that the operation has failed.
     * @param message The message requesting the put operation.
     * @param frInfo The information about why it has failed.
     */
    private void sendFailedResponse(PutFileRequest message, ResponseInfo frInfo) {
        // send final response telling, that the file already exists!
        PutFileFinalResponse fResponse = createPutFileFinalResponse(message);
        fResponse.setResponseInfo(frInfo);
        getMessageBus().sendMessage(fResponse);
    }
    
    /**
     * Creates a PutFileProgressResponse based on a PutFileRequest. Missing the 
     * following fields:
     * <br/> - AuditTrailInformation
     * <br/> - PillarChecksumSpec
     * <br/> - ProgressResponseInfo
     * 
     * @param msg The PutFileRequest to base the progress response on.
     * @return The PutFileProgressResponse based on the request.
     */
    private PutFileProgressResponse createPutFileProgressResponse(PutFileRequest msg) {
        PutFileProgressResponse res = new PutFileProgressResponse();
        res.setMinVersion(MIN_VERSION);
        res.setVersion(VERSION);
        res.setCorrelationID(msg.getCorrelationID());
        res.setFileAddress(msg.getFileAddress());
        res.setFileID(msg.getFileID());
        res.setFrom(getSettings().getReferenceSettings().getPillarSettings().getPillarID());
        res.setTo(msg.getReplyTo());
        res.setPillarID(getSettings().getReferenceSettings().getPillarSettings().getPillarID());
        res.setCollectionID(getSettings().getCollectionID());
        res.setReplyTo(getSettings().getReferenceSettings().getPillarSettings().getReceiverDestination());
        res.setPillarChecksumSpec(getChecksumType());
        
        return res;
    }
    
    /**
     * Creates a PutFileFinalResponse based on a PutFileRequest. Missing the
     * following fields:
     * <br/> - AuditTrailInformation
     * <br/> - ChecksumsDataForNewFile
     * <br/> - FinalResponseInfo
     * <br/> - PillarChecksumSpec
     * 
     * @param msg The PutFileRequest to base the final response message on.
     * @return The PutFileFinalResponse message based on the request.
     */
    private PutFileFinalResponse createPutFileFinalResponse(PutFileRequest msg) {
        PutFileFinalResponse res = new PutFileFinalResponse();
        res.setMinVersion(MIN_VERSION);
        res.setVersion(VERSION);
        res.setCorrelationID(msg.getCorrelationID());
        res.setFileAddress(msg.getFileAddress());
        res.setFileID(msg.getFileID());
        res.setFrom(getSettings().getReferenceSettings().getPillarSettings().getPillarID());
        res.setTo(msg.getReplyTo());
        res.setPillarID(getSettings().getReferenceSettings().getPillarSettings().getPillarID());
        res.setCollectionID(getSettings().getCollectionID());
        res.setReplyTo(getSettings().getReferenceSettings().getPillarSettings().getReceiverDestination());
        res.setPillarChecksumSpec(getChecksumType());
        
        return res;
    }
}