/*
 * #%L
 * Bitrepository Access
 * 
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
package org.bitrepository.access.getchecksums.conversation;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.bitrepository.bitrepositoryelements.FinalResponseCodePositiveType;
import org.bitrepository.bitrepositoryelements.FinalResponseInfo;
import org.bitrepository.bitrepositoryelements.ResultingChecksums;
import org.bitrepository.bitrepositorymessages.GetChecksumsFinalResponse;
import org.bitrepository.bitrepositorymessages.GetChecksumsProgressResponse;
import org.bitrepository.bitrepositorymessages.GetChecksumsRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetChecksumsResponse;
import org.bitrepository.protocol.ProtocolConstants;
import org.bitrepository.protocol.conversation.FlowController;
import org.bitrepository.protocol.eventhandler.DefaultEvent;
import org.bitrepository.protocol.eventhandler.OperationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Models the behavior of a GetChecksums conversation during the operation phase. That is, it begins with the 
 * sending of <code>GetChecksumsRequest</code> messages and finishes with on the reception of the 
 * <code>GetChecksumsFinalResponse</code> messages from the responding pillars.
 * 
 * Note that this is only used by the GetChecksumsConversation in the same package, therefore the visibility is package 
 * protected.
 */
public class GettingChecksums extends GetChecksumsState {

    /** The log for this class. */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** The mapping between the pillars and their destinations.*/
    private final Map<String, String> pillarDestinations;
    /** The pillars, which has not yet answered.*/
    private Set<String> outstandingPillars;

    /** 
     * The timer for the getFileTimeout. It is run as a daemon thread, eg. it will not prevent the application from 
     * exiting */
    final Timer timer = new Timer(true);
    /** The timer task for timeout of getFile in this conversation. */
    final TimerTask getChecksumsTimeoutTask = new GetChecksumsTimerTask();

    /** The results of the checksums calculations. A map between the pillar and its calculated checksums.*/
    private Map<String, ResultingChecksums> results = new HashMap<String, ResultingChecksums>();

    /**
     * Constructor.
     * @param conversation The conversation where this state belongs.
     */
    public GettingChecksums(SimpleGetChecksumsConversation conversation) {
        super(conversation);
        pillarDestinations = conversation.selector.getPillarDestinations();
        outstandingPillars = pillarDestinations.keySet();
    }

    /**
     * Method for initiating this part of the conversation. Sending the GetChecksumsRequest.
     */
    public void start() {
        GetChecksumsRequest getChecksumsRequest = new GetChecksumsRequest();
        getChecksumsRequest.setBitRepositoryCollectionID(
                conversation.settings.getCollectionID());
        getChecksumsRequest.setCorrelationID(conversation.getConversationID());
        getChecksumsRequest.setFileIDs(conversation.fileIDs);
        getChecksumsRequest.setReplyTo(conversation.settings.getReferenceSettings().getClientSettings().getReceiverDestination());
        getChecksumsRequest.setFileChecksumSpec(conversation.checksumSpecifications);
        getChecksumsRequest.setMinVersion(BigInteger.valueOf(ProtocolConstants.PROTOCOL_MIN_VERSION));
        getChecksumsRequest.setVersion(BigInteger.valueOf(ProtocolConstants.PROTOCOL_VERSION));
        getChecksumsRequest.setAuditTrailInformation(conversation.auditTrailInformation);
        
        // Sending one request to each of the identified pillars.
        for(Entry<String, String> pillarDestination : pillarDestinations.entrySet()) {
            getChecksumsRequest.setPillarID(pillarDestination.getKey());
            getChecksumsRequest.setTo(pillarDestination.getValue());

            if(conversation.uploadUrl != null) {
                // making the URL: 'baseUrl'-'pillarId'
                getChecksumsRequest.setResultAddress(conversation.uploadUrl.toExternalForm() + "-" 
                        + pillarDestination.getKey());
            }

            conversation.messageSender.sendMessage(getChecksumsRequest); 
            monitor.requestSent("GetChecksumRequest sent to: " + pillarDestination.getKey(), 
                    pillarDestination.getKey());
        }

        timer.schedule(getChecksumsTimeoutTask,
                conversation.settings.getCollectionSettings().getClientSettings().getOperationTimeout().longValue());
    }

    @Override
    public void onMessage(IdentifyPillarsForGetChecksumsResponse response) {
        log.warn("(ConversationID: " + conversation.getConversationID() 
                + ") Received IdentifyPillarsForGetChecksumsResponse from " + response.getPillarID() 
                + " after the GetChecksumsRequest has been sent.");
    }

    @Override
    public void onMessage(GetChecksumsProgressResponse response) {
        monitor.progress(new DefaultEvent(OperationEvent.OperationEventType.Progress, 
                "Received progress response for retrieval of checksums " + response.getFileIDs() + ": response"));
    }

    @Override
    public void onMessage(GetChecksumsFinalResponse response) {
        log.info("(ConversationID: " + conversation.getConversationID() + ") "
                + "Received GetChecksumsFinalResponse from " + response.getPillarID() + ": \n{}", response);

        // Remove pillar from outstanding, if it has not yet replied.
        if(!outstandingPillars.contains(response.getPillarID())) {
            monitor.warning("(ConversationID: " + conversation.getConversationID() + ") "
                    + "Received unexpected final response from " + response.getPillarID() 
                    + ". Perhaps received previously.");
            log.warn("(ConversationID: " + conversation.getConversationID() + ") "
                    + "Received unexpected final response from " + response.getPillarID() 
                    + ". Perhaps received previously.");
        } else {
            outstandingPillars.remove(response.getPillarID());
        }

        if(validateFinalResponse(response.getFinalResponseInfo())) {
            monitor.progress(new DefaultEvent(OperationEvent.OperationEventType.PartiallyComplete, 
                                response.getFinalResponseInfo().toString()));
            // If calculations in message, then put them into the results map.
            if(response.getResultingChecksums() != null) {
                results.put(response.getPillarID(), response.getResultingChecksums());
            }
        } else {
            monitor.warning("Received bad FinalResponse from pillar: " + response.getFinalResponseInfo());
        }

        if(outstandingPillars.isEmpty()) {
            monitor.complete(new DefaultEvent(OperationEvent.OperationEventType.Complete, 
                                "All pillars have delivered their checksums."));
            conversation.getFlowController().unblock();
            conversation.setResults(results);
            conversation.conversationState = new GetChecksumsFinished(conversation);
        }
    }

    /**
     * Method for validating the FinalResponseInfo.
     * @param frInfo The FinalResponseInfo to be validated.
     * @return Whether the FinalRepsonseInfo tells that the operation has been a success or a failure.
     */
    private boolean validateFinalResponse(FinalResponseInfo frInfo) {
        try {
            if(FinalResponseCodePositiveType.SUCCESS.value().intValue() == new Integer(frInfo.getFinalResponseCode())) {
                return true;
            }
        } catch(NumberFormatException e) {
            log.warn("Could not handle FinalResponseInfo: " + frInfo, e);
        }
        return false;
    }

    /**
     * Method for handling a timeout for this operation.
     */
    protected void handleTimeout() {
        if (!conversation.hasEnded()) { 
            conversation.failConversation("No GetFileFinalResponse received before timeout");
        }
    }

    /**
     * The timer task class for the outstanding get file request. When the time is reached the conversation should be
     * marked as ended.
     */
    private class GetChecksumsTimerTask extends TimerTask {
        @Override
        public void run() {
            handleTimeout();
        }
    }
}
