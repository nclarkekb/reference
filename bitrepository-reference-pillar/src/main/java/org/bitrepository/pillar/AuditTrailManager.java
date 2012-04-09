/*
 * #%L
 * Bitmagasin integrationstest
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
package org.bitrepository.pillar;

import java.util.Collection;

import org.bitrepository.bitrepositoryelements.AuditTrailEvent;
import org.bitrepository.bitrepositoryelements.FileAction;

/**
 * The interface for the audit trail handlers.
 */
public interface AuditTrailManager {
    /**
     * Adds an audit trail event to the manager.
     * @param fileId The id of the file, where the operation has been performed.
     * @param actor The name of the actor.
     * @param info Information about the reason for the audit trail to be logged.
     * @param auditTrail The string for the audit trail information from the message performing the operation.
     * @param operation The performed operation.
     */
    void addAuditEvent(String fileId, String actor, String info, String auditTrail, FileAction operation);
    
    /**
     * Method for extracting all the audit trails.
     * @param fileId [OPTIONAL] The id of the file to request audits for.
     * @param sequenceNumber [OPTIONAL] The lowest sequence number
     * @return The all audit trails.
     */
    Collection<AuditTrailEvent> getAudits(String fileId, Long sequenceNumber);
}
