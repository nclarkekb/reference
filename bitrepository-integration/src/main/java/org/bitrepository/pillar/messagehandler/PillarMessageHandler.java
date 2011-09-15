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

import org.bitrepository.collection.settings.standardsettings.Settings;
import org.bitrepository.pillar.ReferenceArchive;
import org.bitrepository.protocol.ProtocolConstants;
import org.bitrepository.protocol.messagebus.MessageBus;

/**
 * Abstract level for message handling. 
 */
public abstract class PillarMessageHandler<T> {

    /** The constant for the VERSION of the messages.*/
    protected final BigInteger VERSION = BigInteger.valueOf(ProtocolConstants.PROTOCOL_VERSION);
    /** The constant for the MIN_VERSION of the messages.*/
    protected final BigInteger MIN_VERSION = BigInteger.valueOf(ProtocolConstants.PROTOCOL_MIN_VERSION);
    
    /** The dispatcher for sending alarm messages.*/
    protected final AlarmDispatcher alarmDispatcher;
    /** The settings for this setup.*/
    protected final Settings settings;
    /** The messagebus for communication.*/ 
    protected final MessageBus messagebus;
    /** The reference archive.*/
    protected final ReferenceArchive archive;
    
    /**
     * Constructor. 
     * @param settings The settings for handling the message.
     * @param messageBus The bus for communication.
     * @param alarmDispatcher The dispatcher of alarms.
     * @param referenceArchive The archive for the data.
     */
    protected PillarMessageHandler(Settings settings, MessageBus messageBus, AlarmDispatcher alarmDispatcher, 
            ReferenceArchive referenceArchive) {
        this.settings = settings;
        this.messagebus = messageBus;
        this.alarmDispatcher = alarmDispatcher;
        this.archive = referenceArchive;
    }
    
    /** 
     * The method for handling the message connected to the specific handler.
     * @param message The message to handle.
     */
    abstract void handleMessage(T message);
    
    /**
     * Validates that it is the correct BitrepositoryCollectionId.
     * @param bitrepositoryCollectionId The collection id to validate.
     */
    protected void validateBitrepositoryCollectionId(String bitrepositoryCollectionId) {
        if(!bitrepositoryCollectionId.equals(settings.getBitRepositoryCollectionID())) {
            throw new IllegalArgumentException("The message had a wrong BitRepositoryIdCollection: "
                    + "Expected '" + settings.getBitRepositoryCollectionID() + "' but was '" 
                    + bitrepositoryCollectionId + "'.");
        }
    }

    /**
     * Validates that it is the correct pillar id.
     * @param pillarId The pillar id.
     */
    protected void validatePillarId(String pillarId) {
        if(!pillarId.equals(settings.getPillar().getPillarID())) {
            throw new IllegalArgumentException("The message had a wrong PillarId: "
                    + "Expected '" + settings.getPillar().getPillarID() + "' but was '" 
                    + pillarId + "'.");
        }
    }
}
