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
package org.bitrepository.pillar.messagehandler;

import java.math.BigInteger;

import org.bitrepository.bitrepositoryelements.IdentifyResponseCodePositiveType;
import org.bitrepository.bitrepositoryelements.IdentifyResponseInfo;
import org.bitrepository.bitrepositoryelements.TimeMeasureTYPE;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForPutFileRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForPutFileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for handling the identification of this pillar for the purpose of performing the PutFile operation.
 * TODO handle error scenarios.
 */
public class PutFileIdentificationMessageHandler extends PillarMessageHandler<IdentifyPillarsForPutFileRequest> {
    /** The log.*/
    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Constructor.
     * @param mediator The mediator for this pillar.
     */
    public PutFileIdentificationMessageHandler(PillarMediator mediator) {
        super(mediator);
    }
    
    /**
     * Handles the identification messages for the PutFile operation.
     * TODO perhaps synchronisation?
     * @param message The IdentifyPillarsForPutFileRequest message to handle.
     */
    public void handleMessage(IdentifyPillarsForPutFileRequest message) {
        try {
            // validate message
            validateBitrepositoryCollectionId(message.getBitRepositoryCollectionID());

            log.info("Creating reply for '" + message + "'");
            IdentifyPillarsForPutFileResponse reply 
                    = mediator.msgFactory.createIdentifyPillarsForPutFileResponse(message);

            // Needs to filled in: AuditTrailInformation, PillarChecksumSpec, ReplyTo, TimeToDeliver
            reply.setReplyTo(mediator.settings.getProtocol().getLocalDestination());
            reply.setTimeToDeliver(mediator.settings.getPillar().getTimeToDeliver());
            reply.setAuditTrailInformation(null);
            reply.setPillarChecksumSpec(null); // NOT A CHECKSUM PILLAR
            
            IdentifyResponseInfo irInfo = new IdentifyResponseInfo();
            irInfo.setIdentifyResponseCode(IdentifyResponseCodePositiveType.IDENTIFICATION_POSITIVE.value().toString());
            irInfo.setIdentifyResponseText("Operation acknowledged and accepted.");
            reply.setIdentifyResponseInfo(irInfo);

            log.info("Sending IdentifyPillarsForPutfileResponse: " + reply);
            mediator.messagebus.sendMessage(reply);
        } catch (IllegalArgumentException e) {
            log.warn("Caught IllegalArgumentException", e);
            alarmIllegalArgument(e);
        }
    }
}
