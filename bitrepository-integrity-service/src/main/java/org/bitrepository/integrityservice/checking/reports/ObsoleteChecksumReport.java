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
package org.bitrepository.integrityservice.checking.reports;

import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * The report for a obsolete checksum check.
 */
public class ObsoleteChecksumReport implements IntegrityReport {
    /** The mapping between a file id and its respective obsolete checksums data.*/
    private final Map<String, ObsoleteChecksumData> obsoleteChecksum = new HashMap<String, ObsoleteChecksumData>();
    
    /**
     * Report obsolete checksum at a given pillar.
     * @param fileId The id of the file, which has an obsolete checksum.
     * @param pillarId The id of the pillar, where the checksum is obsolete.
     * @param date The time stamp for the latest checksum calculation for the given pillar.
     */
    public void reportMissingChecksum(String fileId, String pillarId, XMLGregorianCalendar date) {
        if(obsoleteChecksum.containsKey(fileId)) {
            obsoleteChecksum.get(fileId).addPillar(pillarId, date);
        } else {
            ObsoleteChecksumData ocd = new ObsoleteChecksumData(fileId);
            ocd.addPillar(pillarId, date);
            obsoleteChecksum.put(fileId, ocd);
        }
    }
    
    @Override
    public boolean hasIntegrityIssues() {
        return !obsoleteChecksum.isEmpty();
    }
    
    @Override
    public String generateReport() {
        if(!hasIntegrityIssues()) {
            return "No missing checksums. \n";
        }
        
        StringBuilder res = new StringBuilder();
        res.append("Files missing their checksum and at which pillars the checksum is missing: \n");
        for(ObsoleteChecksumData oc : obsoleteChecksum.values()) {
            res.append(oc.fileId + " : " + oc.pillarDates + "\n");
        }
        return res.toString();
    }
    
    /**
     * @return The mapping between the file ids with obsolete checksums and the obsolete checksum data.
     */
    public Map<String, ObsoleteChecksumData> getObsoleteChecksum() {
        return obsoleteChecksum;
    }
    
    /**
     * Container for the information about the obsolete pillars for a single file. 
     */
    public class ObsoleteChecksumData {
        /** The id of the file where the checksum is missing.*/
        final String fileId;
        /** The list of id for the pillars where the checksum of the file is missing. */
        final Map<String, XMLGregorianCalendar> pillarDates;
        
        /**
         * Constructor.
         * @param fileId The id of the file where the checksum is obsolete.
         */
        public ObsoleteChecksumData(String fileId) {
            this.fileId = fileId;
            this.pillarDates = new HashMap<String, XMLGregorianCalendar>();
        }
        
        /**
         * 
         * @param pillarId
         * @param date
         */
        public void addPillar(String pillarId, XMLGregorianCalendar date) {
            pillarDates.put(pillarId, date);
        }
        
        /**
         * @return The mapping between the pillars where the file has an old checksum and the date for the last 
         * checksum calculation.
         */
        public Map<String, XMLGregorianCalendar> getPillarDates() {
            return pillarDates;
        }
    }
}
