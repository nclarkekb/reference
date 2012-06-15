/*
 * #%L
 * Bitrepository Audit Trail Service
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
package org.bitrepository.audittrails.preserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.bitrepository.audittrails.store.AuditTrailStore;
import org.bitrepository.client.eventhandler.EventHandler;
import org.bitrepository.common.ArgumentValidator;
import org.bitrepository.common.settings.Settings;
import org.bitrepository.common.utils.FileUtils;
import org.bitrepository.modify.putfile.PutFileClient;
import org.bitrepository.protocol.CoordinationLayerException;
import org.bitrepository.protocol.FileExchange;
import org.bitrepository.protocol.ProtocolComponentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the preservation of audit trails to the local collection.
 * 
 * 
 */
public class LocalAuditTrailPreserver implements AuditTrailPreserver {
    /** The log.*/
    private Logger log = LoggerFactory.getLogger(getClass());
    /** The audit trails store, where the audit trails can be extracted.*/
    private final AuditTrailStore store;
    /** The put file client for sending the resulting files. */
    private final PutFileClient client;

    /** The interval between checking whether it should perform the preservation of audit trails. */
    private final Long checkInterval;
    /** The maximum time between committing audit trails.*/
    private final Long timeLimit;
    
    /** The timer for scheduling the preservation of audit trails.*/
    private Timer timer;
    /** The timertask for preserving the audit trails.*/
    private AuditPreservationTimerTask auditTask = null;
    /** The Audit trail packer for packing and compressing the audit trails.*/
    private final AuditPacker auditPacker;
    
    /**
     * Constructor.
     * @param settings The settings for the audit trail service.
     * @param store The storage of the audit trails, which should be preserved.
     * @param client The PutFileClient for putting the audit trail packages to the collection.
     */
    public LocalAuditTrailPreserver(Settings settings, AuditTrailStore store, PutFileClient client) {
        ArgumentValidator.checkNotNull(settings, "Settings settings");
        ArgumentValidator.checkNotNull(store, "AuditTrailStore store");
        ArgumentValidator.checkNotNull(client, "PutFileClient client");
        
        this.store = store;
        this.client = client;
        this.checkInterval = settings.getReferenceSettings().getAuditTrailServiceSettings()
                .getTimerTaskCheckInterval();
        this.timeLimit = settings.getReferenceSettings().getAuditTrailServiceSettings()
                .getAuditTrailPreservationInterval();
        this.auditPacker = new AuditPacker(store, settings);
    }
    
    @Override
    public void start() {
        if(timer != null) {
            log.debug("Cancelling old timer.");
            timer.cancel();
        }
        
        log.info("Instantiating the preservation of workflows.");
        timer = new Timer();
        auditTask = new AuditPreservationTimerTask(timeLimit);
        timer.scheduleAtFixedRate(auditTask, checkInterval, checkInterval);
    }

    @Override
    public void close() {
        if(timer != null) {
            timer.cancel();
        }
    }

    @Override
    public void preserveAuditTrailsNow() {
        if(auditTask == null) {
            log.info("preserving the audit trails ");
        } else {
            auditTask.resetTime();
        }
        performAuditTrailPreservation();
    }
    
    /**
     * Performs the audit trails preservation.
     * Uses the AuditPacker to pack the audit trails in a file, then uploads the file to the default file-server, and
     * finally use the PutFileClient to ingest the package into the collection.
     * When the 'put' has completed the Store is updated with the newest preservation sequence numbers for the 
     * contributors.
     */
    private synchronized void performAuditTrailPreservation() {
        try {
            File auditPackage = auditPacker.createNewPackage();
            URL url = uploadFile(auditPackage);
            log.info("Uploaded the file '" + auditPackage + "' to '" + url.toExternalForm() + "'");
            
            EventHandler eventHandler = new AuditPreservationEventHandler(auditPacker.getSequenceNumbersReached(), 
                    store);
            client.putFile(url, auditPackage.getName(), auditPackage.length(), null, null, eventHandler, 
                    "Preservation of audit trails from the AuditTrail service.");

            log.debug("Cleanup of the uploaded audit trail package.");
            FileUtils.delete(auditPackage);
        } catch (IOException e) {
            throw new CoordinationLayerException("Cannot perform the preservation of audit trails.", e);
        }
    }
    
    /**
     * Uploads the file to a server.
     * @param file The file to upload.
     * @return The URL to the file.
     * @throws IOException If any issues occur with uploading the file.
     */
    @SuppressWarnings("deprecation")
    private URL uploadFile(File file) throws IOException {
        FileExchange exchange = ProtocolComponentFactory.getInstance().getFileExchange();
        return exchange.uploadToServer(new FileInputStream(file), file.getName());
    }
    
    /**
     * Timer task for keeping track of the automated collecting of audit trails.
     */
    private class AuditPreservationTimerTask extends TimerTask {
        /** The interval between running this timer task.*/
        private final long interval;
        /** The date for the next run.*/
        private Date nextRun;
        
        /**
         * Constructor.
         * @param interval The interval between running this timer task.
         */
        private AuditPreservationTimerTask(long interval) {
            this.interval = interval;
            resetTime();
        }
        
        /**
         * Resets the date for next run.
         */
        private void resetTime() {
            nextRun = new Date(System.currentTimeMillis() + interval);
        }
        
        @Override
        public void run() {
            if(nextRun.getTime() < System.currentTimeMillis()) {
                log.debug("Time to preserve the audit trails.");
                resetTime();
                performAuditTrailPreservation();
            }
        }
    }
}