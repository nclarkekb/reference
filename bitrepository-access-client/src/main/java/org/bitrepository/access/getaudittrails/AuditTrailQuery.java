/*
 * #%L
 * Bitrepository Access
 * 
 * $Id$
 * $HeadURL$
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
package org.bitrepository.access.getaudittrails;

/**
 * Encapsulates the information need to communicate with a Bit Repository component over the message bus.
 */
public class AuditTrailQuery {
    private final String componentID;
    private final Integer minSequenceNumber;
    private final Integer maxSequenceNumber;
    private final Integer maxNumberOfResults;

    /**
     *
     * @param componentID
     */
    public AuditTrailQuery(String componentID, Integer maxNumberOfResults) {
        this(componentID, maxNumberOfResults, null, null);
    }

    /**
     *
     * Queries for all Audit Trails with sequence number larger than minSequenceNumber.
     * @param minSequenceNumber
     * @param componentID
     */
    public AuditTrailQuery(String componentID, Integer maxNumberOfResults, Integer minSequenceNumber) {
        this(componentID, maxNumberOfResults, minSequenceNumber, null);
    }
    /**
     * Queries for all Audit Trails with sequence number between minSequenceNumber and maxSequenceNumber.
     * @param minSequenceNumber Only return audit trail event with sequence number higher than <code>minSequenceNumber</code>.
     * @param maxSequenceNumber Only return audit trail event with sequence number lower than <code>maxSequenceNumber</code>.
     * param maxNumberOfResults If set will limit the number of results returned. If the result set is limited, only
     * the lowest sequence numbers are returned
     * @param componentID The ID of the component to query.
     */
    public AuditTrailQuery(String componentID, Integer maxNumberOfResults, Integer minSequenceNumber, Integer maxSequenceNumber) {
        super();
        this.componentID = componentID;
        this.maxNumberOfResults = maxNumberOfResults;
        if (minSequenceNumber != null && maxSequenceNumber != null && minSequenceNumber > maxSequenceNumber)
            throw new IllegalArgumentException(
                "minSequenceNumber=" + minSequenceNumber + " can not be greater than " +
                "maxSequenceNumber=" + maxSequenceNumber);
        this.minSequenceNumber = minSequenceNumber;
        this.maxSequenceNumber = maxSequenceNumber;
    }
    
    /**
     * @return The componentID for the component.
     */
    public String getComponentID() {
        return componentID;
    }

    public Integer getMinSequenceNumber() {
        return minSequenceNumber;
    }

    public Integer getMaxSequenceNumber() {
        return maxSequenceNumber;
    }

    public Integer getMaxNumberOfResults() {
        return maxNumberOfResults;
    }

    @Override
    public String toString() {
        return "AuditTrailQuery{" +
                "componentID='" + componentID + '\'' +
                ", minSequenceNumber=" + minSequenceNumber +
                ", maxSequenceNumber=" + maxSequenceNumber +
                ", maxNumberOfResults=" + maxNumberOfResults +
                '}';
    }
}
