/*
 * #%L
 * Bitmagasin modify client
 * 
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2010 The State and University Library, The Royal Library and The State Archives, Denmark
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
package org.bitrepository.modify.putfile;

import java.net.URL;
import java.util.List;
import org.bitrepository.bitrepositoryelements.ChecksumDataForFileTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.client.eventhandler.BlockingEventHandler;
import org.bitrepository.client.eventhandler.ContributorEvent;
import org.bitrepository.client.eventhandler.EventHandler;
import org.bitrepository.client.eventhandler.OperationEvent;

/**
 * Wrappes a <code>PutFileClient</code> to provide a blocking client. The client will block until the PutFileOperation
 * has finished.
 */
public class BlockingPutFileClient {
    private final PutFileClient client;

    public BlockingPutFileClient(PutFileClient client) {
        this.client = client;
    }
    /**
     * Method for performing a blocking puFIle operation.
     *
     * @param url The URL where the file to be put is located.
     * @param fileId The id of the file.
     * @param sizeOfFile The number of bytes the file requires.
     * @param checksumForValidationAtPillar The checksum for validating at pillar side.
     * @param checksumRequestsForValidation The checksum for validating at client side.
     * @param eventHandler The EventHandler for the operation.
     * @param auditTrailInformation The audit trail information.
     */
    public List<ContributorEvent> putFile(
            URL url,
            String fileId,
            long sizeOfFile,
            ChecksumDataForFileTYPE checksumForValidationAtPillar,
            ChecksumSpecTYPE checksumRequestsForValidation,
            EventHandler eventHandler,
            String auditTrailInformation)
            throws Exception {
        BlockingEventHandler blocker = new BlockingEventHandler(eventHandler);
        client.putFile(url, fileId, sizeOfFile, checksumForValidationAtPillar, checksumRequestsForValidation,
                blocker, auditTrailInformation);
        OperationEvent finishEvent = blocker.awaitFinished();
        if(finishEvent.getType() == OperationEvent.OperationEventType.COMPLETE) {
            return blocker.getResults();
        } else if (finishEvent.getType() == OperationEvent.OperationEventType.FAILED) {
            throw new NegativeArraySizeException(finishEvent.getInfo());
        } else throw new RuntimeException("Received unexpected event type" + finishEvent);
    }
}