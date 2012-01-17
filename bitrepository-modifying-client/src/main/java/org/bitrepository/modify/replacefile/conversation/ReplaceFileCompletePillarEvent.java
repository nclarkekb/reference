/*
 * #%L
 * Bitrepository Access
 * 
 * $Id: ChecksumsCompletePillarEvent.java 548 2011-11-21 16:43:43Z jolf $
 * $HeadURL: https://sbforge.org/svn/bitrepository/bitrepository-reference/trunk/bitrepository-access-client/src/main/java/org/bitrepository/access/getchecksums/conversation/ChecksumsCompletePillarEvent.java $
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
package org.bitrepository.modify.replacefile.conversation;

import org.bitrepository.bitrepositoryelements.ChecksumDataForFileTYPE;
import org.bitrepository.protocol.eventhandler.PillarOperationEvent;

/**
* Contains the result of a ReplaceFile request sent to a single pillar.
*/
public class ReplaceFileCompletePillarEvent extends PillarOperationEvent {
    /** @see #getChecksums(). */
    private final ChecksumDataForFileTYPE result;
    
    /**
     * @param result The result returned by the pillar.
     * @param pillarID The pillar which generated the result
     * @param info Additional information.
     */
    public ReplaceFileCompletePillarEvent(ChecksumDataForFileTYPE result, String pillarID, String info) {
        super(OperationEventType.COMPONENT_COMPLETE, info, pillarID);
        this.result = result;
    }

    /** 
     * @return The checksum result from a single pillar. 
     */
    public ChecksumDataForFileTYPE getChecksums() {
        return result;
    }
}