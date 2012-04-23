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
package org.bitrepository.pillar.referencepillar.messagehandler;

import org.bitrepository.bitrepositoryelements.FileAction;
import org.bitrepository.bitrepositoryelements.ResponseCode;
import org.bitrepository.bitrepositoryelements.ResponseInfo;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileResponse;
import org.bitrepository.common.ArgumentValidator;
import org.bitrepository.pillar.common.PillarContext;
import org.bitrepository.pillar.exceptions.IdentifyPillarsException;
import org.bitrepository.pillar.referencepillar.ReferenceArchive;
import org.bitrepository.protocol.utils.TimeMeasurementUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for handling the identification of this pillar for the purpose of performing the GetFile operation.
 */
public class IdentifyPillarsForGetFileRequestHandler extends ReferencePillarMessageHandler<IdentifyPillarsForGetFileRequest> {
    /** The log.*/
    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Constructor.
     * @param context The context of the message handler.
     * @param referenceArchive The archive for the data.
     */
    public IdentifyPillarsForGetFileRequestHandler(PillarContext context, ReferenceArchive referenceArchive) {
        super(context, referenceArchive);
    }
    
    /**
     * Handles the identification messages for the GetFile operation.
     * @param message The IdentifyPillarsForGetFileRequest message to handle.
     */
    public void handleMessage(IdentifyPillarsForGetFileRequest message) {
        ArgumentValidator.checkNotNull(message, "IdentifyPillarsForGetFileRequest message");

        try {
            validateBitrepositoryCollectionId(message.getCollectionID());
            checkThatFileIsAvailable(message);
            respondSuccesfullIdentification(message);
        } catch (IllegalArgumentException e) {
            getAlarmDispatcher().handleIllegalArgumentException(e);
        } catch (IdentifyPillarsException e) {
            log.warn("Unsuccessfull identification for the GetFile operation.", e);
            respondUnsuccessfulIdentification(message, e);
        } catch (RuntimeException e) {
            getAuditManager().addAuditEvent(message.getFileID(), message.getFrom(), "Failed identifying pillar.", 
                    message.getAuditTrailInformation(), FileAction.FAILURE);
            getAlarmDispatcher().handleRuntimeExceptions(e);
        }
    }
    
    /**
     * Validates that the requested file is within the archive. 
     * Otherwise an {@link IdentifyPillarsException} with the appropriate errorcode is thrown.
     * @param message The request for the identification for the GetFileRequest operation.
     */
    private void checkThatFileIsAvailable(IdentifyPillarsForGetFileRequest message) {
        if(!getArchive().hasFile(message.getFileID())) {
            ResponseInfo irInfo = new ResponseInfo();
            irInfo.setResponseCode(ResponseCode.FILE_NOT_FOUND_FAILURE);
            irInfo.setResponseText("The file '" + message.getFileID() 
                    + "' does not exist within the archive.");
            
            throw new IdentifyPillarsException(irInfo);
        }
    }
    
    /**
     * Method for making a successful response to the identification.
     * @param message The request message to respond to.
     */
    private void respondSuccesfullIdentification(IdentifyPillarsForGetFileRequest message) {
        // Create the response.
        IdentifyPillarsForGetFileResponse reply = createIdentifyPillarsForGetFileResponse(message);
        
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
     * Method for sending a bad response.
     * @param message The identification request to respond to.
     */
    private void respondUnsuccessfulIdentification(IdentifyPillarsForGetFileRequest message, 
            IdentifyPillarsException cause) {
        // Create the response.
        IdentifyPillarsForGetFileResponse reply = createIdentifyPillarsForGetFileResponse(message);
        
        reply.setTimeToDeliver(TimeMeasurementUtils.getMaximumTime());
        reply.setResponseInfo(cause.getResponseInfo());
        
        // Send resulting file.
        getMessageBus().sendMessage(reply);
    }
    
    /**
     * Creates a IdentifyPillarsForGetFileResponse based on a 
     * IdentifyPillarsForGetFileRequest. The following fields are not inserted:
     * <br/> - TimeToDeliver
     * <br/> - AuditTrailInformation
     * <br/> - IdentifyResponseInfo
     * 
     * @param msg The IdentifyPillarsForGetFileRequest to base the response on.
     * @return The response to the request.
     */
    private IdentifyPillarsForGetFileResponse createIdentifyPillarsForGetFileResponse(
            IdentifyPillarsForGetFileRequest msg) {
        IdentifyPillarsForGetFileResponse res 
                = new IdentifyPillarsForGetFileResponse();
        res.setMinVersion(MIN_VERSION);
        res.setVersion(VERSION);
        res.setCorrelationID(msg.getCorrelationID());
        res.setFileID(msg.getFileID());
        res.setFrom(getSettings().getReferenceSettings().getPillarSettings().getPillarID());
        res.setTo(msg.getReplyTo());
        res.setPillarID(getSettings().getReferenceSettings().getPillarSettings().getPillarID());
        res.setCollectionID(getSettings().getCollectionID());
        res.setReplyTo(getSettings().getReferenceSettings().getPillarSettings().getReceiverDestination());
        
        return res;
    }
}