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
package org.bitrepository.access.getfile.selectors;

import org.bitrepository.bitrepositoryelements.TimeMeasureTYPE;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileResponse;
import org.bitrepository.protocol.flow.UnexpectedResponseException;
import org.bitrepository.protocol.pillarselector.PillarSelector;

/**
 * Used to select a specific pillar and find the topic for this pillar. The selection is implemented by sending a 
 * <code>IdentifyPillarsForGetFileRequest</code> and processing the responses.
 *
 */
public abstract class PillarSelectorForGetFile extends PillarSelector {
    private TimeMeasureTYPE timeToDeliver;
    
    /**
     * Delegates to super constructor.
     */
    protected PillarSelectorForGetFile(String[] pillarsWhichShouldRespond) {
        super(pillarsWhichShouldRespond);
    }

    /**
     * Each time this method is called the selector will check the response to see whether the selected pillar should 
     * be changed.
     * @param response The new response from a pillar.
     * @throws UnexpectedResponseException The selector was unable to process the response. The selector will still be 
     * able to continue, but the supplied response is ignored.
     */
    public abstract void processResponse(IdentifyPillarsForGetFileResponse response) throws UnexpectedResponseException;
    
    /**
     * The returned timeToDeliver for the selected pillar. May be null if no pillar has been selected.
     */
    public TimeMeasureTYPE getTimeToDeliver() {
        return timeToDeliver;
    }

    /**
     * The GetFile specific select operation.
     */
    protected final void selectPillar(String pillarID, String pillarTopic, TimeMeasureTYPE timeToDeliver) {
        super.selectPillar(pillarID, pillarTopic);
        this.timeToDeliver = timeToDeliver;
    }
    
}
