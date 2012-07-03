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
package org.bitrepository.integrityservice.workflow.step;

import org.bitrepository.integrityservice.alerter.IntegrityAlerter;
import org.bitrepository.integrityservice.checking.IntegrityChecker;
import org.bitrepository.integrityservice.checking.reports.IntegrityReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A workflow step for finding obsolete checksums.
 * Sends an alarm if any checksums are too old.
 */
public class FindObsoleteChecksumsStep implements WorkflowStep {
    /** The log.*/
    private Logger log = LoggerFactory.getLogger(getClass());
    /** Checker for performing the integrity checks.*/
    private final IntegrityChecker checker;
    /** The dispatcher of alarms.*/
    private final IntegrityAlerter dispatcher;
    /** The timeout for the obsolete */
    private final Long obsoleteTimeout;
    
    /**
     * Constructor.
     * @param store The storage for the integrity data.
     * @param obsoleteTimeout The interval for a checksum timestamp to timeout and become obsolete.
     */
    public FindObsoleteChecksumsStep(IntegrityChecker checker, IntegrityAlerter alarmDispatcher, long obsoleteTimeout) {
        this.checker = checker;
        this.obsoleteTimeout = obsoleteTimeout;
        this.dispatcher = alarmDispatcher;
    }
    
    @Override
    public String getName() {
        return "Finding obsolete checksums";
    }

    /**
     * Goes through all the file ids in the database and extract their respective fileinfos.
     * Then it goes through all the file infos to validate that the timestamp for the checksum calculation.
     */
    @Override
    public synchronized void performStep() {
        IntegrityReport report = checker.checkObsoleteChecksums(obsoleteTimeout);
        
        if(report.hasIntegrityIssues()) {
            log.debug("No checksum are obsolete at any pillar.");
        } else {
            dispatcher.integrityFailed(report);
        }
    }
}
