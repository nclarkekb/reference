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

import java.util.HashMap;
import java.util.Map;

import org.bitrepository.bitrepositoryelements.Alarm;
import org.bitrepository.bitrepositoryelements.AlarmCode;
import org.bitrepository.bitrepositorymessages.DeleteFileRequest;
import org.bitrepository.bitrepositorymessages.GetAuditTrailsRequest;
import org.bitrepository.bitrepositorymessages.GetChecksumsRequest;
import org.bitrepository.bitrepositorymessages.GetFileIDsRequest;
import org.bitrepository.bitrepositorymessages.GetFileRequest;
import org.bitrepository.bitrepositorymessages.GetStatusRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForDeleteFileRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetChecksumsRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileIDsRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForPutFileRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForReplaceFileRequest;
import org.bitrepository.bitrepositorymessages.PutFileRequest;
import org.bitrepository.bitrepositorymessages.ReplaceFileRequest;
import org.bitrepository.common.ArgumentValidator;
import org.bitrepository.common.settings.Settings;
import org.bitrepository.pillar.audit.MemorybasedAuditTrailManager;
import org.bitrepository.pillar.checksumpillar.cache.ChecksumCache;
import org.bitrepository.protocol.messagebus.AbstractMessageListener;
import org.bitrepository.protocol.messagebus.MessageBus;
import org.bitrepository.settings.collectionsettings.AlarmLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This instance handles the conversations for the checksum pillar.
 * It only responds to requests. It does not it self start conversations, though it might send Alarms when something 
 * is not right.
 * All other messages than requests are considered garbage.
 * Every message (even garbage) is currently put into the audit trails.
 */
public class ChecksumPillarMediator extends AbstractMessageListener {
    /** The log.*/
    private Logger log = LoggerFactory.getLogger(getClass());

    /** The settings.*/
    private final Settings settings;
    /** The messagebus. Package protected on purpose.*/
    private final MessageBus messagebus;
    /** The archive. Package protected on purpose.*/
    private final ChecksumCache cache;
    /** The handler of the audits. Package protected on purpose.*/
    private final MemorybasedAuditTrailManager audits;
    /** The dispatcher of alarms. Package protected on purpose.*/
    private final AlarmDispatcher alarmDispatcher;

    // THE MESSAGE HANDLERS!
    /** The map between the messagenames and their respective handlers.*/
    @SuppressWarnings("rawtypes")
    private Map<String, ChecksumPillarMessageHandler> handlers = new HashMap<String, ChecksumPillarMessageHandler>();

    /**
     * Constructor.
     * Sets the parameters of this mediator, and adds itself as a listener to the destinations.
     * 
     * @param messagebus The messagebus for this instance.
     * @param settings The settings for the reference pillar.
     * @param refArchive The archive for the reference pillar.
     * @param messageFactory The message factory.
     */
    public ChecksumPillarMediator(MessageBus messagebus, Settings settings, ChecksumCache refCache) {
        ArgumentValidator.checkNotNull(messagebus, "messageBus");
        ArgumentValidator.checkNotNull(settings, "settings");
        ArgumentValidator.checkNotNull(refCache, "ChecksumCache refCache");

        this.messagebus = messagebus;
        this.cache = refCache;
        this.settings = settings;
        this.audits = new MemorybasedAuditTrailManager();
        this.alarmDispatcher = new AlarmDispatcher(settings, messagebus);

        // Initialise the messagehandlers.
        initialiseHandlers();

        // add to both the general topic and the local queue.
        messagebus.addListener(settings.getCollectionDestination(), this);
        messagebus.addListener(settings.getReferenceSettings().getPillarSettings().getReceiverDestination(), this);
    }
    
    /**
     * Method for instantiating the handlers.
     */
    private void initialiseHandlers() {
        this.handlers.put(IdentifyPillarsForGetFileRequest.class.getName(), 
                new IdentifyPillarsForGetFileRequestHandler(settings, messagebus, alarmDispatcher, cache));
        this.handlers.put(GetFileRequest.class.getName(), 
                new GetFileRequestHandler(settings, messagebus, alarmDispatcher, cache));
        this.handlers.put(IdentifyPillarsForGetFileIDsRequest.class.getName(), 
                new IdentifyPillarsForGetFileIDsRequestHandler(settings, messagebus, alarmDispatcher, cache));
        this.handlers.put(GetFileIDsRequest.class.getName(), 
                new GetFileIDsRequestHandler(settings, messagebus, alarmDispatcher, cache));
        this.handlers.put(IdentifyPillarsForGetChecksumsRequest.class.getName(), 
                new IdentifyPillarsForGetChecksumsRequestHandler(settings, messagebus, alarmDispatcher, cache));
        this.handlers.put(GetChecksumsRequest.class.getName(), 
                new GetChecksumsRequestHandler(settings, messagebus, alarmDispatcher, cache));
        
        this.handlers.put(IdentifyPillarsForPutFileRequest.class.getName(), 
                new IdentifyPillarsForPutFileRequestHandler(settings, messagebus, alarmDispatcher, cache));
        this.handlers.put(PutFileRequest.class.getName(), 
                new PutFileRequestHandler(settings, messagebus, alarmDispatcher, cache));
        this.handlers.put(IdentifyPillarsForDeleteFileRequest.class.getName(), 
                new IdentifyPillarsForDeleteFileRequestHandler(settings, messagebus, alarmDispatcher, cache));
        this.handlers.put(DeleteFileRequest.class.getName(), 
                new DeleteFileRequestHandler(settings, messagebus, alarmDispatcher, cache));
        this.handlers.put(IdentifyPillarsForReplaceFileRequest.class.getName(), 
                new IdentifyPillarsForReplaceFileRequestHandler(settings, messagebus, alarmDispatcher, cache));
        this.handlers.put(ReplaceFileRequest.class.getName(), 
                new ReplaceFileRequestHandler(settings, messagebus, alarmDispatcher, cache));
    }
    
    /**
     * Method for sending an alarm when a received message does not have a handler.
     * 
     * @param message The message which does not have a handler.
     */
    private void noHandlerAlarm(Object message) {
        String msg = "Cannot handle message of type '" + message.getClass().getCanonicalName() + "'";
        log.warn(msg + ": " + message.toString());
        
        // create a descriptor.
        Alarm ad = new Alarm();
        ad.setAlarmCode(AlarmCode.FAILED_OPERATION);
        ad.setAlarmText(msg);
        
        alarmDispatcher.sendAlarm(ad);
    }
    
    @Override
    protected void reportUnsupported(Object message) {
        audits.addMessageReceivedAudit("Received unsupported: " + message.getClass());
        if(AlarmLevel.WARNING.equals(settings.getCollectionSettings().getPillarSettings().getAlarmLevel())) {
            noHandlerAlarm(message);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(DeleteFileRequest message) {
        log.info("Received: " + message);
        audits.addMessageReceivedAudit("Received: " + message.getClass() + " : " + message.getAuditTrailInformation());

        ChecksumPillarMessageHandler<DeleteFileRequest> handler = handlers.get(message.getClass().getName());
        if(handler != null) {
            handler.handleMessage(message);
        } else {
            noHandlerAlarm(message);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(GetAuditTrailsRequest message) {
        log.info("Received: " + message);
        audits.addMessageReceivedAudit("Received: " + message.getClass() + " : " + message.getAuditTrailInformation());

        ChecksumPillarMessageHandler<GetAuditTrailsRequest> handler = handlers.get(message.getClass().getName());
        if(handler != null) {
            handler.handleMessage(message);
        } else {
            noHandlerAlarm(message);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(GetChecksumsRequest message) {
        log.info("Received: " + message);
        audits.addMessageReceivedAudit("Received: " + message.getClass() + " : " + message.getAuditTrailInformation());

        ChecksumPillarMessageHandler<GetChecksumsRequest> handler = handlers.get(message.getClass().getName());
        if(handler != null) {
            handler.handleMessage(message);
        } else {
            noHandlerAlarm(message);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(GetFileIDsRequest message) {
        log.info("Received: " + message);
        audits.addMessageReceivedAudit("Received: " + message.getClass() + " : " + message.getAuditTrailInformation());

        ChecksumPillarMessageHandler<GetFileIDsRequest> handler = handlers.get(message.getClass().getName());
        if(handler != null) {
            handler.handleMessage(message);
        } else {
            noHandlerAlarm(message);
        }    
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(GetFileRequest message) {
        log.info("Received: " + message);
        audits.addMessageReceivedAudit("Received: " + message.getClass() + " : " + message.getAuditTrailInformation());

        ChecksumPillarMessageHandler<GetFileRequest> handler = handlers.get(message.getClass().getName());
        if(handler != null) {
            handler.handleMessage(message);
        } else {
            noHandlerAlarm(message);
        }    
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(GetStatusRequest message) {
        log.info("Received: " + message);
        audits.addMessageReceivedAudit("Received: " + message.getClass() + " : " + message.getAuditTrailInformation());

        ChecksumPillarMessageHandler<GetStatusRequest> handler = handlers.get(message.getClass().getName());
        if(handler != null) {
            handler.handleMessage(message);
        } else {
            noHandlerAlarm(message);
        }    
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(IdentifyPillarsForDeleteFileRequest message) {
        log.info("Received: " + message);
        audits.addMessageReceivedAudit("Received: " + message.getClass() + " : " + message.getAuditTrailInformation());

        ChecksumPillarMessageHandler<IdentifyPillarsForDeleteFileRequest> handler = handlers.get(message.getClass().getName());
        if(handler != null) {
            handler.handleMessage(message);
        } else {
            noHandlerAlarm(message);
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(IdentifyPillarsForGetChecksumsRequest message) {
        log.info("Received: " + message);
        audits.addMessageReceivedAudit("Received: " + message.getClass() + " : " + message.getAuditTrailInformation());

        ChecksumPillarMessageHandler<IdentifyPillarsForGetChecksumsRequest> handler 
                = handlers.get(message.getClass().getName());
        if(handler != null) {
            handler.handleMessage(message);
        } else {
            noHandlerAlarm(message.getClass());
        }    
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(IdentifyPillarsForGetFileIDsRequest message) {
        log.info("Received: " + message);
        audits.addMessageReceivedAudit("Received: " + message.getClass() + " : " + message.getAuditTrailInformation());

        ChecksumPillarMessageHandler<IdentifyPillarsForGetFileIDsRequest> handler = handlers.get(message.getClass().getName());
        if(handler != null) {
            handler.handleMessage(message);
        } else {
            noHandlerAlarm(message.getClass());
        }    
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(IdentifyPillarsForGetFileRequest message) {
        log.info("Received: " + message);
        audits.addMessageReceivedAudit("Received: " + message.getClass() + " : " + message);

        ChecksumPillarMessageHandler<IdentifyPillarsForGetFileRequest> handler = handlers.get(message.getClass().getName());
        if(handler != null) {
            handler.handleMessage(message);
        } else {
            noHandlerAlarm(message.getClass());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(IdentifyPillarsForPutFileRequest message) {
        log.info("Received: " + message);
        audits.addMessageReceivedAudit("Received: " + message.getClass() + " : " + message.getAuditTrailInformation());

        ChecksumPillarMessageHandler<IdentifyPillarsForPutFileRequest> handler = handlers.get(message.getClass().getName());
        if(handler != null) {
            handler.handleMessage(message);
        } else {
            noHandlerAlarm(message.getClass());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(IdentifyPillarsForReplaceFileRequest message) {
        log.info("Received: " + message);
        audits.addMessageReceivedAudit("Received: " + message.getClass() + " : " + message.getAuditTrailInformation());

        ChecksumPillarMessageHandler<IdentifyPillarsForReplaceFileRequest> handler = handlers.get(message.getClass().getName());
        if(handler != null) {
            handler.handleMessage(message);
        } else {
            noHandlerAlarm(message.getClass());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(PutFileRequest message) {
        log.info("Received: " + message);
        audits.addMessageReceivedAudit("Received: " + message.getClass() + " : " + message.getAuditTrailInformation());

        ChecksumPillarMessageHandler<PutFileRequest> handler = handlers.get(message.getClass().getName());
        if(handler != null) {
            handler.handleMessage(message);
        } else {
            noHandlerAlarm(message.getClass());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMessage(ReplaceFileRequest message) {
        log.info("Received: " + message);
        audits.addMessageReceivedAudit("Received: " + message.getClass() + " : " + message.getAuditTrailInformation());

        ChecksumPillarMessageHandler<ReplaceFileRequest> handler = handlers.get(message.getClass().getName());
        if(handler != null) {
            handler.handleMessage(message);
        } else {
            noHandlerAlarm(message.getClass());
        }
    }

    /**
    * Closes the mediator by removing all the message handlers.
    */
    @SuppressWarnings("rawtypes")
    public void close() {
        handlers.clear();
        handlers = new HashMap<String, ChecksumPillarMessageHandler>(); 
        // removes to both the general topic and the local queue.
        messagebus.removeListener(settings.getCollectionDestination(), this);
        messagebus.removeListener(settings.getReferenceSettings().getPillarSettings().getReceiverDestination(), this);
    }
}
