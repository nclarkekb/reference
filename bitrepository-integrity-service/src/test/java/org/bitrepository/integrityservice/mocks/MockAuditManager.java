/*
 * #%L
 * Bitrepository Integrity Service
 * %%
 * Copyright (C) 2010 - 2012 The State and University Library, The Royal Library and The State Archives, Denmark
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
package org.bitrepository.integrityservice.mocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.bitrepository.bitrepositoryelements.AuditTrailEvent;
import org.bitrepository.bitrepositoryelements.FileAction;
import org.bitrepository.service.audit.AuditTrailManager;

public class MockAuditManager implements AuditTrailManager {

    private int callsToAddAuditEvent = 0;
    @Override
    public void addAuditEvent(String fileId, String actor, String info, String auditTrail, FileAction operation) {
        callsToAddAuditEvent++;
    }
    public int getCallsToAddAuditEvent() {
        return callsToAddAuditEvent;
    }

    private int callsToGetAudits = 0;
    @Override
    public Collection<AuditTrailEvent> getAudits(String fileId, Long minSeqNumber, Long maxSeqNumber, Date minDate,
            Date maxDate) {
        callsToGetAudits++;
        return new ArrayList<AuditTrailEvent>();
    }
    public int getCallsToGetAudits() {
        return callsToGetAudits;
    }
}