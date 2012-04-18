/*
 * #%L
 * bitrepository-access-client
 * *
 * $Id$
 * $HeadURL$
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

import java.util.ArrayList;
import java.util.List;

import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.FileIDs;
import org.bitrepository.bitrepositoryelements.ResponseCode;
import org.bitrepository.bitrepositoryelements.ResponseInfo;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetChecksumsRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetChecksumsResponse;
import org.bitrepository.common.ArgumentValidator;
import org.bitrepository.pillar.checksumpillar.cache.ChecksumStore;
import org.bitrepository.pillar.common.PillarContext;
import org.bitrepository.pillar.exceptions.IdentifyPillarsException;
import org.bitrepository.protocol.utils.TimeMeasurementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for handling the identification of this pillar for the purpose of performing the GetChecksums operation.
 */
public class IdentifyPillarsForGetChecksumsRequestHandler 
        extends ChecksumPillarMessageHandler<IdentifyPillarsForGetChecksumsRequest> {
    /** The log.*/
    private Logger log = LoggerFactory.getLogger(getClass());
    
    /**
     * Constructor.
     * @param context The context of the message handler.
     * @param refCache The cache for the checksum data.
     */
    public IdentifyPillarsForGetChecksumsRequestHandler(PillarContext context, ChecksumStore refCache) {
        super(context, refCache);
    }

    /**
     * Handles the identification messages for the GetChecksums operation.
     * @param message The IdentifyPillarsForGetChecksumsRequest message to handle.
     */
    public void handleMessage(IdentifyPillarsForGetChecksumsRequest message) {
        ArgumentValidator.checkNotNull(message, "IdentifyPillarsForGetChecksumsRequest message");

        try {
            validateMessage(message);
            checkThatAllRequestedFilesAreAvailable(message);
            checkThatTheChecksumFunctionIsAvailable(message);
            respondSuccesfullIdentification(message);
        } catch (IllegalArgumentException e) {
            getAlarmDispatcher().handleIllegalArgumentException(e);
        } catch (IdentifyPillarsException e) {
            log.warn("Unsuccessfull identification for the GetChecksums operation.", e);
            respondUnsuccessfulIdentification(message, e);
        } catch (RuntimeException e) {
            getAlarmDispatcher().handleRuntimeExceptions(e);
        }
    }
    
    /**
     * Validates the identification message.
     * @param message The identify message to validate.
     */
    private void validateMessage(IdentifyPillarsForGetChecksumsRequest message) {
        validateBitrepositoryCollectionId(message.getCollectionID());
        validateChecksumSpec(message.getChecksumRequestForExistingFile());        
    }
    
    /**
     * Validates that all the requested files in the filelist are present. 
     * Otherwise an {@link IdentifyPillarsException} with the appropriate errorcode is thrown.
     * @param message The message containing the list files. An empty filelist is expected 
     * when "AllFiles" or the parameter option is used.
     */
    public void checkThatAllRequestedFilesAreAvailable(IdentifyPillarsForGetChecksumsRequest message) {
        FileIDs fileids = message.getFileIDs();
        if(fileids == null) {
            log.debug("No fileids are defined in the identification request ('" + message.getCorrelationID() + "').");
            return;
        }
        
        List<String> missingFiles = new ArrayList<String>();
        String fileID = fileids.getFileID();
        if(fileID != null && !fileID.isEmpty() && !getCache().hasFile(fileID)) {
            missingFiles.add(fileID);
        }
        
        // Throw exception if any files are missing.
        if(!missingFiles.isEmpty()) {
            ResponseInfo irInfo = new ResponseInfo();
            irInfo.setResponseCode(ResponseCode.FILE_NOT_FOUND_FAILURE);
            irInfo.setResponseText(missingFiles.size() + " missing files: '" + missingFiles + "'");
            
            throw new IdentifyPillarsException(irInfo);
        }
    }
    
    /**
     * Validates that it is possible to instantiate the requested checksum algorithm.
     * Otherwise an {@link IdentifyPillarsException} with the appropriate errorcode is thrown.
     * @param message The message with the checksum algorithm to validate.
     */
    public void checkThatTheChecksumFunctionIsAvailable(IdentifyPillarsForGetChecksumsRequest message) {
        ChecksumSpecTYPE checksumSpec = message.getChecksumRequestForExistingFile();
        
        // validate that this non-mandatory field has been filled out.
        if(checksumSpec == null || checksumSpec.getChecksumType() == null) {
            log.debug("No checksumSpec in the identification. Thus no reason to expect, that we cannot handle it.");
            return;
        }
    }
    
    /**
     * Method for making a successful response to the identification.
     * @param message The request message to respond to.
     */
    private void respondSuccesfullIdentification(IdentifyPillarsForGetChecksumsRequest message) {
        // Create the response.
        IdentifyPillarsForGetChecksumsResponse reply = createIdentifyPillarsForGetChecksumsResponse(message);
        
        // set the missing variables in the reply:
        // TimeToDeliver, AuditTrailInformation, IdentifyResponseInfo
        reply.setTimeToDeliver(TimeMeasurementUtils.getTimeMeasurementFromMiliseconds(
                getSettings().getReferenceSettings().getPillarSettings().getTimeToStartDeliver()));
        
        ResponseInfo irInfo = new ResponseInfo();
        irInfo.setResponseCode(ResponseCode.IDENTIFICATION_POSITIVE);
        irInfo.setResponseText(RESPONSE_FOR_POSITIVE_IDENTIFICATION);
        reply.setResponseInfo(irInfo);
        
        // Send resulting file.
        getMessageBus().sendMessage(reply);
    }
    
    /**
     * Sends a bad response with the given cause.
     * @param message The identification request to respond to.
     * @param cause The cause of the bad identification (e.g. which files are missing).
     */
    private void respondUnsuccessfulIdentification(IdentifyPillarsForGetChecksumsRequest message, 
            IdentifyPillarsException cause) {
        IdentifyPillarsForGetChecksumsResponse reply = createIdentifyPillarsForGetChecksumsResponse(message);
        
        reply.setTimeToDeliver(TimeMeasurementUtils.getMaximumTime());
        reply.setResponseInfo(cause.getResponseInfo());
        
        getMessageBus().sendMessage(reply);
    }
    
    /**
     * Creates a IdentifyPillarsForGetChecksumsResponse based on a 
     * IdentifyPillarsForGetFileRequest. The following fields are not inserted:
     * <br/> - TimeToDeliver
     * <br/> - AuditTrailInformation
     * <br/> - IdentifyResponseInfo
     * <br/> - PillarChecksumSpec
     * 
     * @param msg The IdentifyPillarsForGetFileRequest to base the response on.
     * @return The response to the request.
     */
    private IdentifyPillarsForGetChecksumsResponse createIdentifyPillarsForGetChecksumsResponse(
            IdentifyPillarsForGetChecksumsRequest msg) {
        IdentifyPillarsForGetChecksumsResponse res 
                = new IdentifyPillarsForGetChecksumsResponse();
        res.setMinVersion(MIN_VERSION);
        res.setVersion(VERSION);
        res.setCorrelationID(msg.getCorrelationID());
        res.setFileIDs(msg.getFileIDs());
        res.setFrom(getSettings().getReferenceSettings().getPillarSettings().getPillarID());
        res.setTo(msg.getReplyTo());
        res.setChecksumRequestForExistingFile(msg.getChecksumRequestForExistingFile());
        res.setPillarID(getSettings().getReferenceSettings().getPillarSettings().getPillarID());
        res.setCollectionID(getSettings().getCollectionID());
        res.setReplyTo(getSettings().getReferenceSettings().getPillarSettings().getReceiverDestination());
        res.setPillarChecksumSpec(getChecksumType());
        
        return res;
    }
}
