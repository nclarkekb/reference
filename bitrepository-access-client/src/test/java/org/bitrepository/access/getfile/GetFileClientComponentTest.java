/*
 * #%L
 * bitrepository-access-client
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
package org.bitrepository.access.getfile;

import java.io.File;
import java.math.BigInteger;
import java.net.URL;

import org.bitrepository.access.AccessComponentFactory;
import org.bitrepository.bitrepositoryelements.TimeMeasureTYPE;
import org.bitrepository.bitrepositorymessages.GetFileFinalResponse;
import org.bitrepository.bitrepositorymessages.GetFileProgressResponse;
import org.bitrepository.bitrepositorymessages.GetFileRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileResponse;
import org.bitrepository.clienttest.DefaultFixtureClientTest;
import org.bitrepository.common.bitrepositorycollection.MutableClientSettings;
import org.bitrepository.protocol.fileexchange.TestFileStore;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test class for the 'GetFileClient'.
 */
public class GetFileClientComponentTest extends DefaultFixtureClientTest {
    private GetFileClient getFileClient;
    private TestGetFileMessageFactory testMessageFactory;
    private TestFileStore pillar1FileStore;
    private TestFileStore pillar2FileStore;

    @BeforeMethod (alwaysRun=true)
    public void beforeMethodSetup() throws Exception {
        // In case of multiple types of GetFileClients, the concrete class should be configurable (perhaps specialize 
        // this test with an overload of this method)
        getFileClient = 
            new GetFileClientTestWrapper(AccessComponentFactory.getInstance().createGetFileClient(slaConfiguration), 
                    testEventManager);
        
        if (useMockupPillar()) {
            testMessageFactory = new TestGetFileMessageFactory(slaConfiguration.getId());
            pillar1FileStore = new TestFileStore("Pillar1");
            pillar2FileStore = new TestFileStore("Pillar2");
            // The following line is also relevant for non-mockup senarios, where the pillars needs to be initialized 
            // with content.
        }
        httpServer.clearFiles();
    }

    @Test(groups = {"regressiontest"})
    public void verifyGetFileClientFromFactory() throws Exception {
        Assert.assertTrue(AccessComponentFactory.getInstance().createGetFileClient(slaConfiguration) 
                instanceof SimpleGetFileClient, 
                "The default GetFileClient from the Access factory should be of the type '" + 
                SimpleGetFileClient.class.getName() + "'.");
    }

    @Test(groups = {"test-first"})
    public void identifyAndGetSinglePillar() throws Exception {
        addDescription("Tests whether a specific message is sent by the GetClient, when only a single pillar " +
        "participates");
        addStep("Set the number of pillars for this SLA to 1", "");
        ((MutableClientSettings)slaConfiguration).setNumberOfPillars(1);
        
        addStep("Request the fastest delivery of a file. A callback listener should be supplied.", 
                "A IdentifyPillarsForGetFileRequest will be sent to the pillar.");
        getFileClient.retrieveFastest(DEFAULT_FILE_ID);
        IdentifyPillarsForGetFileRequest receivedIdentifyRequestMessage = null;
        if (useMockupPillar()) {
            receivedIdentifyRequestMessage = slaTopic.waitForMessage(IdentifyPillarsForGetFileRequest.class);
            Assert.assertEquals(receivedIdentifyRequestMessage, 
                    testMessageFactory.createIdentifyPillarsForGetFileIDsRequest(receivedIdentifyRequestMessage));

            Assert.assertNotNull(receivedIdentifyRequestMessage, "No IdentifyPillarsForGetFileRequest received.");
            Assert.assertEquals(receivedIdentifyRequestMessage.getFileID(), DEFAULT_FILE_ID);
        }

        addStep("The pillar sends a response to the identify message.", 
        "The callback listener should notify of the response and the client should send a GetFileRequest message to " +
        "the pillar"); 

        //Todo Verify the result when the GetFileClient callback interface has been defined 
        GetFileRequest receivedGetFileRequest = null;
        if (useMockupPillar()) {
            IdentifyPillarsForGetFileResponse identifyResponse = testMessageFactory.createIdentifyPillarsForGetFileResponse(
                    receivedIdentifyRequestMessage, PILLAR1_ID, pillar1TopicId);
            messageBus.sendMessage(clientTopicId, identifyResponse);
            receivedGetFileRequest = pillar1Topic.waitForMessage(GetFileRequest.class);
            Assert.assertEquals(receivedGetFileRequest, 
                    testMessageFactory.createGetFileRequest(receivedGetFileRequest,PILLAR1_ID));
        }
        
        addStep("The pillar sends a getFile response to the GetClient.", 
        "The GetClient should notify about the response through the callback interface."); 
        if (useMockupPillar()) {
            GetFileProgressResponse getFileProgressResponse = testMessageFactory.createGetFileProgressResponse(
                    receivedGetFileRequest, PILLAR1_ID, pillar1TopicId);
            messageBus.sendMessage(clientTopicId, getFileProgressResponse);
        }
        // ToDo The listener call should be verified.
        
        addStep("The file is uploaded to the indicated url and the pillar sends a final response upload message", 
                "The GetFileClient notifies that the file is ready through the callback listener and the uploaded " +
                "file is present.");
        if (useMockupPillar()) {
            // ToDo Switch to use test uploader using the attributes supplied in the request, eg. 
            // testPillar.uploadFile(receivedGetFileRequest.getFileAddress(), receivedGetFileRequest.getFileID());
                      
            httpServer.uploadFile(pillar1FileStore.getInputstream(receivedGetFileRequest.getFileID()),
                    new URL(receivedGetFileRequest.getFileAddress()));

            GetFileFinalResponse completeMsg = testMessageFactory.createGetFileFinalResponse(
                    receivedGetFileRequest, PILLAR1_ID, pillar1TopicId);
            messageBus.sendMessage(clientTopicId, completeMsg);
        }
        // Todo Assert that the callback listener has received an 'uploadComplete' event.
        // How do we know the where the file is located. 
        File expectedUploadFile = pillar1FileStore.getFile(DEFAULT_FILE_ID);
        httpServer.assertFileEquals(expectedUploadFile, receivedGetFileRequest.getFileAddress());
    }

    @Test(groups = {"regressiontest"})
    public void chooseFastestPillarGetFileClient() throws Exception {
        addDescription("Set the GetClient to retrieve a file as fast as "
                + "possible, where it has to choose between to pillars with "
                + "different times. The messages should be delivered at the "
                + "same time.");
        addStep("Defining the test variables.", "Nothing should be able to go wrong here!");
        String fileId = "fileId";
        String fastPillar = "THE-FAST-PILLAR";
        String slowPillar = "THE-SLOW-PILLAR";
        ((MutableClientSettings)slaConfiguration).setNumberOfPillars(2);

        addStep("Defining the variables for the GetFileClient and defining them in the configuration", 
        "It should be possible to change the values of the configurations.");

        addStep("Make the GetClient ask for fastest pillar.", "It should send message to identify which pillars.");

        getFileClient.retrieveFastest(fileId);

        IdentifyPillarsForGetFileRequest identifyRequestMessage = 
            slaTopic.waitForMessage(IdentifyPillarsForGetFileRequest.class);
        TimeMeasureTYPE fastTime = new TimeMeasureTYPE();
        fastTime.setTimeMeasureUnit("milliseconds");
        fastTime.setTimeMeasureValue(BigInteger.valueOf(10L));
        TimeMeasureTYPE slowTime = new TimeMeasureTYPE();
        slowTime.setTimeMeasureValue(BigInteger.valueOf(20000L));
        slowTime.setTimeMeasureUnit("hours");

        IdentifyPillarsForGetFileResponse fastReply = new IdentifyPillarsForGetFileResponse();
        fastReply.setCorrelationID(identifyRequestMessage.getCorrelationID());
        fastReply.setFileID(identifyRequestMessage.getFileID());
        fastReply.setMinVersion(BigInteger.valueOf(1L));
        fastReply.setVersion(BigInteger.valueOf(1L));
        fastReply.setBitrepositoryContextID(identifyRequestMessage.getBitrepositoryContextID());
        fastReply.setTimeToDeliver(fastTime);
        fastReply.setPillarID(fastPillar);  
        fastReply.setReplyTo(pillar1TopicId);
        messageBus.sendMessage(identifyRequestMessage.getReplyTo(), fastReply);

        fastReply.setReplyTo(identifyRequestMessage.getReplyTo());
        IdentifyPillarsForGetFileResponse slowReply = new IdentifyPillarsForGetFileResponse();
        slowReply.setCorrelationID(identifyRequestMessage.getCorrelationID());
        slowReply.setFileID(identifyRequestMessage.getFileID());
        slowReply.setMinVersion(BigInteger.valueOf(1L));
        slowReply.setVersion(BigInteger.valueOf(1L));
        slowReply.setBitrepositoryContextID(identifyRequestMessage.getBitrepositoryContextID());
        slowReply.setTimeToDeliver(slowTime);
        slowReply.setPillarID(slowPillar);  
        slowReply.setReplyTo(pillar2TopicId);
        messageBus.sendMessage(identifyRequestMessage.getReplyTo(), slowReply);

        addStep("Verify that it has chosen the fast pillar.", "");
        GetFileRequest getFileRequestMessage = pillar1Topic.waitForMessage(GetFileRequest.class);
        Assert.assertNotNull(getFileRequestMessage, "No GetFileResponse received.");
        Assert.assertEquals(getFileRequestMessage.getFileID(), identifyRequestMessage.getFileID());
        Assert.assertEquals(getFileRequestMessage.getPillarID(), fastPillar);     
    }
}
