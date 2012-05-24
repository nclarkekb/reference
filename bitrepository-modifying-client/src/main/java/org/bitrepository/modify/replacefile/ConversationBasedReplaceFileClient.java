/*
 * #%L
 * Bitrepository Modifying Client
 * 
 * $Id: ConversationBasedDeleteFileClient.java 639 2011-12-15 10:24:45Z jolf $
 * $HeadURL: https://sbforge.org/svn/bitrepository/bitrepository-reference/trunk/bitrepository-modifying-client/src/main/java/org/bitrepository/modify/deletefile/ConversationBasedDeleteFileClient.java $
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
package org.bitrepository.modify.replacefile;

import java.net.URL;

import org.bitrepository.bitrepositoryelements.ChecksumDataForFileTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.client.AbstractClient;
import org.bitrepository.client.conversation.mediator.ConversationMediator;
import org.bitrepository.client.eventhandler.EventHandler;
import org.bitrepository.common.ArgumentValidator;
import org.bitrepository.common.settings.Settings;
import org.bitrepository.modify.replacefile.conversation.ReplaceFileConversationContext;
import org.bitrepository.modify.replacefile.conversation.SimpleReplaceFileConversation;
import org.bitrepository.modify.replacefile.pillarselector.AllPillarsSelectorForReplaceFile;
import org.bitrepository.modify.replacefile.pillarselector.SpecificPillarSelectorForReplaceFile;
import org.bitrepository.protocol.messagebus.MessageBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A conversation based implementation of the ReplaceFileClient.
 */
public class ConversationBasedReplaceFileClient extends AbstractClient implements ReplaceFileClient {
    /** The log for this class.*/
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Constructor.
     * @param messageBus The messagebus for communication.
     * @param settings The configurations and settings.
     */
    public ConversationBasedReplaceFileClient(MessageBus messageBus, ConversationMediator conversationMediator, 
            Settings settings, String clientID) {
        super(settings, conversationMediator, messageBus, clientID);
    }
    
    @Override
    public void replaceFile(String fileId, String pillarId, ChecksumDataForFileTYPE checksumForDeleteAtPillar, 
            ChecksumSpecTYPE checksumRequestedForDeletedFile, URL url, long sizeOfNewFile, 
            ChecksumDataForFileTYPE checksumForNewFileValidationAtPillar, ChecksumSpecTYPE checksumRequestsForNewFile, 
            EventHandler eventHandler, String auditTrailInformation) {
        ArgumentValidator.checkNotNullOrEmpty(fileId, "String fileId");
        ArgumentValidator.checkNotNullOrEmpty(pillarId, "String pillarId");
        ArgumentValidator.checkNotNull(checksumForDeleteAtPillar, "ChecksumDataForFileTYPE checksumForDeleteAtPillar");
        validateFileID(fileId);
        
        log.info("Requesting the replacement of the file '" + fileId + "' at the pillar '" + pillarId + "' from the "
                + "URL '" + url + "' and with the size '" + sizeOfNewFile + "', where the old file has the checksum '" 
                + checksumForDeleteAtPillar + "' and is requested the checksum '" + checksumRequestedForDeletedFile 
                + "', and the new file has the checksum '" + checksumForNewFileValidationAtPillar + "' and requesting "
                + "the checksum '" + checksumRequestsForNewFile + "'. With the audittrail '" + auditTrailInformation 
                + "'");
        ReplaceFileConversationContext context = new ReplaceFileConversationContext(fileId, 
                new SpecificPillarSelectorForReplaceFile(
                        settings.getCollectionSettings().getClientSettings().getPillarIDs(), pillarId), 
                sizeOfNewFile, url, checksumForDeleteAtPillar, checksumRequestedForDeletedFile, 
                checksumForNewFileValidationAtPillar, checksumRequestsForNewFile, settings, messageBus, 
                clientID, eventHandler, auditTrailInformation);
        SimpleReplaceFileConversation conversation = new SimpleReplaceFileConversation(context);
        startConversation(conversation);
    }
    
    @Override
    public void replaceFileAtAllPillars(String fileId, ChecksumDataForFileTYPE checksumForDeleteAtPillar, 
            ChecksumSpecTYPE checksumRequestedForDeletedFile, URL url, long sizeOfNewFile, 
            ChecksumDataForFileTYPE checksumForNewFileValidationAtPillar, ChecksumSpecTYPE checksumRequestsForNewFile, 
            EventHandler eventHandler, String auditTrailInformation) {
        ArgumentValidator.checkNotNullOrEmpty(fileId, "String fileId");
        ArgumentValidator.checkNotNull(checksumForDeleteAtPillar, "ChecksumDataForFileTYPE checksumForDeleteAtPillar");
        validateFileID(fileId);
        
        log.info("Requesting the replacement of the file '" + fileId + "' at every pillar from the URL '" + url 
                + "' and with the size '" + sizeOfNewFile + "', where the old file has the checksum '" 
                + checksumForDeleteAtPillar + "' and is requested the checksum '" + checksumRequestedForDeletedFile 
                + "', and the new file has the checksum '" + checksumForNewFileValidationAtPillar + "' and requesting "
                + "the checksum '" + checksumRequestsForNewFile + "'. With the audittrail '" + auditTrailInformation 
                + "'");
        ReplaceFileConversationContext context = new ReplaceFileConversationContext(fileId, 
                new AllPillarsSelectorForReplaceFile(
                        settings.getCollectionSettings().getClientSettings().getPillarIDs()), 
                sizeOfNewFile, url, checksumForDeleteAtPillar, checksumRequestedForDeletedFile, 
                checksumForNewFileValidationAtPillar, checksumRequestsForNewFile, settings, messageBus, 
                clientID, eventHandler, auditTrailInformation);
        SimpleReplaceFileConversation conversation = new SimpleReplaceFileConversation(context);
        startConversation(conversation);
    }
    
}
