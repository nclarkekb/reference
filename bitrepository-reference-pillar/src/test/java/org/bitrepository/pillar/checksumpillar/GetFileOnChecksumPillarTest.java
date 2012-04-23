/*
 * #%L
 * Bitrepository Reference Pillar
 * 
 * $Id: PutFileOnReferencePillarTest.java 589 2011-12-01 15:34:42Z jolf $
 * $HeadURL: https://sbforge.org/svn/bitrepository/bitrepository-reference/trunk/bitrepository-reference-pillar/src/test/java/org/bitrepository/pillar/PutFileOnReferencePillarTest.java $
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
package org.bitrepository.pillar.checksumpillar;

import java.io.File;
import java.util.Date;

import org.bitrepository.bitrepositoryelements.ChecksumType;
import org.bitrepository.bitrepositoryelements.FileIDs;
import org.bitrepository.bitrepositoryelements.FilePart;
import org.bitrepository.bitrepositoryelements.ResponseCode;
import org.bitrepository.bitrepositorymessages.GetFileFinalResponse;
import org.bitrepository.bitrepositorymessages.GetFileRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileResponse;
import org.bitrepository.common.utils.FileUtils;
import org.bitrepository.pillar.DefaultFixturePillarTest;
import org.bitrepository.pillar.MockAlarmDispatcher;
import org.bitrepository.pillar.MockAuditManager;
import org.bitrepository.pillar.checksumpillar.messagehandler.ChecksumPillarMediator;
import org.bitrepository.pillar.common.PillarContext;
import org.bitrepository.pillar.messagefactories.GetFileMessageFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests the PutFile functionality on the ReferencePillar.
 */
public class GetFileOnChecksumPillarTest extends DefaultFixturePillarTest {
    GetFileMessageFactory msgFactory;
    
    MemoryCache cache;
    ChecksumPillarMediator mediator;
    MockAlarmDispatcher alarmDispatcher;
    MockAuditManager audits;
    
    @BeforeMethod (alwaysRun=true)
    public void initialiseDeleteFileTests() throws Exception {
        msgFactory = new GetFileMessageFactory(settings);
        File dir = new File(settings.getReferenceSettings().getPillarSettings().getFileDir());
        if(dir.exists()) {
            FileUtils.delete(dir);
        }
        
        addStep("Initialize the pillar.", "Should not be a problem.");
        cache = new MemoryCache();
        audits = new MockAuditManager();
        alarmDispatcher = new MockAlarmDispatcher(settings, messageBus);
        PillarContext context = new PillarContext(settings, messageBus, alarmDispatcher, audits);
        mediator = new ChecksumPillarMediator(context, cache);
    }    
    @AfterMethod (alwaysRun=true) 
    public void closeArchive() {
        if(cache != null) {
            cache.cleanUp();
        }
        if(mediator != null) {
            mediator.close();
        }
    }
    
    @Test( groups = {"regressiontest", "pillartest"})
    public void checksumPillarGetFileIdentification() throws Exception {
        addDescription("Tests that the ChecksumPillar rejects a GetFile identification.");
        addStep("Setting up the variables for the test.", "Should be instantiated.");
        String FILE_ID = DEFAULT_FILE_ID + new Date().getTime();
        String auditTrail = "GET-FILE-TEST";
        String pillarId = settings.getReferenceSettings().getPillarSettings().getPillarID();
        settings.getReferenceSettings().getPillarSettings().setChecksumPillarChecksumSpecificationType(
                ChecksumType.MD5.toString());
        FileIDs fileids = new FileIDs();
        fileids.setFileID(FILE_ID);
        
        addStep("Create and send the identify request message.", 
                "Should be received and handled by the checksum pillar.");
        IdentifyPillarsForGetFileRequest identifyRequest = msgFactory.createIdentifyPillarsForGetFileRequest(
                auditTrail, FILE_ID, FROM, clientDestinationId);
        if(useEmbeddedPillar()) {
            mediator.onMessage(identifyRequest);
        } else {
            messageBus.sendMessage(identifyRequest);
        }
        
        addStep("Retrieve and validate the response from the checksum pillar.", 
                "The checksum pillar should make a response.");
        IdentifyPillarsForGetFileResponse receivedIdentifyResponse = clientTopic.waitForMessage(
                IdentifyPillarsForGetFileResponse.class);
        Assert.assertEquals(receivedIdentifyResponse, 
                msgFactory.createIdentifyPillarsForGetFileResponse(
                        identifyRequest.getCorrelationID(),
                        FILE_ID, 
                        pillarId,
                        receivedIdentifyResponse.getReplyTo(),
                        receivedIdentifyResponse.getResponseInfo(),
                        receivedIdentifyResponse.getTimeToDeliver(),
                        receivedIdentifyResponse.getTo()));
        Assert.assertEquals(receivedIdentifyResponse.getResponseInfo().getResponseCode(), 
                ResponseCode.REQUEST_NOT_UNDERSTOOD_FAILURE);      
    }
    
    @Test( groups = {"regressiontest", "pillartest"})
    public void checksumPillarGetFileOperation() throws Exception {
        addDescription("Tests that the ChecksumPillar rejects a GetFile operation.");
        addStep("Setting up the variables for the test.", "Should be instantiated.");
        String FILE_ADDRESS = "http://sandkasse-01.kb.dk/dav/test.txt";
        FilePart filePart = null;
        String FILE_ID = DEFAULT_FILE_ID + new Date().getTime();
        String auditTrail = "GET-FILE-TEST";
        String pillarId = settings.getReferenceSettings().getPillarSettings().getPillarID();
        settings.getReferenceSettings().getPillarSettings().setChecksumPillarChecksumSpecificationType(
                ChecksumType.MD5.toString());
        FileIDs fileids = new FileIDs();
        fileids.setFileID(FILE_ID);
        
        addStep("Create and send the GetFile request message.", 
                "Should be received and handled by the pillar.");
        GetFileRequest getRequest = msgFactory.createGetFileRequest(auditTrail, msgFactory.getNewCorrelationID(), 
                FILE_ADDRESS, FILE_ID, filePart, FROM, pillarId, clientDestinationId, pillarDestinationId);
        if(useEmbeddedPillar()) {
            mediator.onMessage(getRequest);
        } else {
            messageBus.sendMessage(getRequest);
        }
        
        addStep("Retrieve and validate the final response from the checksum pillar.", 
                "The checksum pillar should reject the operation.");
        GetFileFinalResponse receivedFinalResponse = clientTopic.waitForMessage(
                GetFileFinalResponse.class);
        Assert.assertEquals(receivedFinalResponse, 
                msgFactory.createGetFileFinalResponse(getRequest.getCorrelationID(), 
                        receivedFinalResponse.getFileAddress(), FILE_ID, filePart, pillarId, pillarDestinationId, 
                        receivedFinalResponse.getResponseInfo(), clientDestinationId));
        Assert.assertEquals(receivedFinalResponse.getResponseInfo().getResponseCode(), 
                ResponseCode.REQUEST_NOT_UNDERSTOOD_FAILURE);      
    }
}