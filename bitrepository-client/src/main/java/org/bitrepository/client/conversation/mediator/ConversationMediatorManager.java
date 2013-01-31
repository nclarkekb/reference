/*
 * #%L
 * Bitrepository Protocol
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
package org.bitrepository.client.conversation.mediator;

import java.util.HashMap;
import java.util.Map;

import org.bitrepository.common.settings.Settings;
import org.bitrepository.protocol.security.SecurityManager;

/** 
 * Get your <code>ConversationMediator</code> here.
 */
public class ConversationMediatorManager {
    /** Map of the loaded mediators */
    private static final Map<String,ConversationMediator> mediatorMap = new HashMap<String,ConversationMediator>();
    
    /**
     * Private constructor. To prevent instantiation of this utility class.
     */
    private ConversationMediatorManager() { }
    
    /**
     * Will return a the <code>ConversationMediator</code> for the collection indicated in the settings. 
     * If the mediator does doesn't exist, it will be created.
     * @param settings The settings to use for this mediator.
     * @param securityManager The securityManager to use for this conversation mediator.
     * @return The settings for the indicated CollectionID.
     */
    public static synchronized ConversationMediator getConversationMediator(Settings settings, SecurityManager securityManager) {
        String collectionID = settings.getCollectionID();
        if (!mediatorMap.containsKey(collectionID)) {
            ConversationMediator mediator = new CollectionBasedConversationMediator(settings, securityManager);
            mediatorMap.put(collectionID, mediator);
        }
        return mediatorMap.get(collectionID);
    }


    /**
     * Can be used to inject a custom messageBus for a mediator collectionID.
     * @param collectionID
     * @param mediator The custom instance of the mediator.
     */
    public static void injectCustomConversationMediator(String collectionID, ConversationMediator mediator) {
        mediatorMap.put(collectionID, mediator);
    }
}
