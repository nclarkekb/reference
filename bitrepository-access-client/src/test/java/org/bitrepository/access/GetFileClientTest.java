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
package org.bitrepository.access;

import java.io.File;
import java.math.BigInteger;
import java.net.URL;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;

import org.bitrepository.bitrepositoryelements.TimeMeasureTYPE;
import org.bitrepository.bitrepositorymessages.GetFileComplete;
import org.bitrepository.bitrepositorymessages.GetFileRequest;
import org.bitrepository.bitrepositorymessages.GetFileResponse;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileReply;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileRequest;
import org.bitrepository.protocol.AbstractMessageListener;
import org.bitrepository.protocol.ProtocolComponentFactory;
import org.jaccept.structure.ExtendedTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test class for the 'GetFileClient'.
 */
public class GetFileClientTest extends ExtendedTestCase {
    
    private Logger log = LoggerFactory.getLogger(getClass());

    private static int WAITING_TIME_FOR_MESSAGE = 5000;
    
    @Test(groups = {"factory"})
    public void verifyGetFileClientFromFactory() throws Exception {
        GetFileClientExternalAPI client = AccessComponentFactory.getInstance().retrieveGetFileClient();
        Assert.assertTrue(client instanceof SimpleGetFileClient, "The default GetFileClient from the Access factory"
                + " should be of the type '" + SimpleGetFileClient.class.getName() + "'.");
    }

    @Test(groups = {"test first"})
    public void identifyAndGetForSimpleGetFileClient() throws Exception {
        addDescription("Tests whether a specific message is sent by the GetClient");
        String dataId = "dataId1";
        String slaId = "THE-SLA";
        String pillarId = "THE-ONLY-PILLAR";
        SimpleGetFileClient gc = new SimpleGetFileClient();
        TestMessageListener listener = new TestMessageListener();
        ProtocolComponentFactory.getInstance().getMessageBus().addListener(gc.getQueue(), listener);
        
        File oldFile = new File(gc.getFileDir(), dataId);
        if(oldFile.exists()) {
            Assert.assertTrue(oldFile.delete(), "The previously downloaded "
                    + "file should be deleted.");
        }
        
        addStep("Request the fastest delivery of file " + dataId, 
                "The GetClient should send a IdentifyPillarsForGetFileRequest "
                + "message.");
        gc.retrieveFastest(dataId, slaId, 1);

        synchronized(this) {
            try {
                wait(WAITING_TIME_FOR_MESSAGE);
            } catch (Exception e) {
                // print, but ignore!
                e.printStackTrace();
            }
        }
        
        addStep("Ensure that the IdentifyPillarsForGetFileRequest message has "
                + "been caught.", "It should be valid.");
        Assert.assertNotNull(listener.getMessage(), "The message must not be null.");
        Assert.assertEquals(listener.getMessageClass().getName(), 
                IdentifyPillarsForGetFileRequest.class.getName(), 
                "The message should be of the type '" 
                + IdentifyPillarsForGetFileRequest.class.getName() + "'");
        IdentifyPillarsForGetFileRequest identifyMessage = (IdentifyPillarsForGetFileRequest) listener.getMessage();
        Assert.assertEquals(identifyMessage.getFileID(), dataId);

        addStep("Sending a reply for the message.", "Should be handled by the "
                + "GetClient.");
        TimeMeasureTYPE time = new TimeMeasureTYPE();
        time.setMiliSec(BigInteger.valueOf(1000));

        IdentifyPillarsForGetFileReply reply = new IdentifyPillarsForGetFileReply();
        reply.setCorrelationID(identifyMessage.getCorrelationID());
        reply.setFileID(identifyMessage.getFileID());
        reply.setMinVersion((short) 1);
        reply.setPillarID(pillarId);
        // reply.setReplyTo(value)
        reply.setSlaID(identifyMessage.getSlaID());
        reply.setTimeToDeliver(time);
        reply.setVersion((short) 1);

        ProtocolComponentFactory.getInstance().getMessageBus().sendMessage(
                gc.getQueue(), reply);
        
        synchronized(this) {
            try {
                wait(WAITING_TIME_FOR_MESSAGE);
            } catch (Exception e) {
                // print, but ignore!
                e.printStackTrace();
            }
        }
        
        addStep("Verifies whether the GetClient sends a request for the file.", 
                "Should be a GetFileRequest for the pillar.");
        Assert.assertEquals(listener.getMessageClass(), GetFileRequest.class,
                "The last message should be a GetFileRequest");
        // The test fails here (2011-03-16). The TestMessageListener does not
        // receive the message...

        GetFileRequest getMessage = (GetFileRequest) listener.getMessage();
        Assert.assertEquals(getMessage.getFileID(), dataId);
        Assert.assertEquals(getMessage.getPillarID(), pillarId);
        
        addStep("Upload a file to the given destination, send a complete to "
                + "the GetClient.", "The GetClient should download the file."); 
        GetFileResponse getReply = new GetFileResponse();
        getReply.setMinVersion((short) 1);
        getReply.setVersion((short) 1);
        getReply.setPillarID(pillarId);
        getReply.setFileID(dataId);

        ProtocolComponentFactory.getInstance().getMessageBus().sendMessage(
                gc.getQueue(), getReply);
        
        synchronized(this) {
            try {
                wait(WAITING_TIME_FOR_MESSAGE);
            } catch (Exception e) {
                // print, but ignore!
                e.printStackTrace();
            }
        }
        
        // TODO read from log, that it has been caught by the GetClientServer.
        Assert.assertEquals(listener.getMessageClass().getName(), 
                getReply.getClass().getName());

        addStep("Uploading the file to the default HTTPServer.", 
                "Should be allowed.");
        File uploadFile = new File("src/test/resources/test.txt");
        URL url = ProtocolComponentFactory.getInstance().getFileExchange().uploadToServer(uploadFile);
        
        addStep("Send a complete upload message with the URL", "Should be "
                + "caugth by the GetClient.");
        GetFileComplete completeMsg = new GetFileComplete();
        completeMsg.setCompleteCode("Complete code");
        completeMsg.setCompleteText("Complete text");
        completeMsg.setCorrelationID(identifyMessage.getCorrelationID());
        completeMsg.setFileAddress(url.toExternalForm());
        completeMsg.setSlaID(slaId);
        completeMsg.setFileID(dataId);
        completeMsg.setMinVersion((short) 1);
        completeMsg.setVersion((short) 1);
        completeMsg.setPillarID(pillarId);

        ProtocolComponentFactory.getInstance().getMessageBus().sendMessage(
                gc.getQueue(), completeMsg);
        
        synchronized(this) {
            try {
                wait(WAITING_TIME_FOR_MESSAGE);
            } catch (Exception e) {
                // print, but ignore!
                e.printStackTrace();
            }
        }
        
        addStep("Verify that the file is downloaded in by the GetClient and "
                + "placed within the GetClient's fileDir.", "Should be fine!");
        File outputFile = new File(gc.getFileDir(), dataId);
        Assert.assertTrue(outputFile.isFile());
    }
    
    @Test(groups = {"test first"})
    public void chooseFastestPillarSimpleGetFileClient() throws Exception {
        addDescription("Set the GetClient to retrieve a file as fast as "
                + "possible, where it has to choose between to pillars with "
                + "different times. The messages should be delivered at the "
                + "same time.");
        String dataId = "dataId2";
        String slaId = "THE-SLA";
        String fastPillar = "THE-FAST-PILLAR";
        String slowPillar = "THE-SLOW-PILLAR";
        SimpleGetFileClient gc = new SimpleGetFileClient();
        TestMessageListener listener = new TestMessageListener();
        ProtocolComponentFactory.getInstance().getMessageBus().addListener(gc.getQueue(), listener);
        
        addStep("Make the GetClient ask for fastest pillar.", "");
        gc.retrieveFastest(dataId, slaId, 2);
        
        synchronized(this) {
            try {
                wait(WAITING_TIME_FOR_MESSAGE);
            } catch (Exception e) {
                // print, but ignore!
                e.printStackTrace();
            }
        }
        
        Assert.assertNotNull(listener.getMessageClass());
        Assert.assertEquals(listener.getMessageClass().getName(), 
                IdentifyPillarsForGetFileRequest.class.getName());
        IdentifyPillarsForGetFileRequest request = (IdentifyPillarsForGetFileRequest) listener.getMessage();
        Assert.assertEquals(request.getFileID(), dataId);
        Assert.assertEquals(request.getSlaID(), slaId);
        TimeMeasureTYPE fastTime = new TimeMeasureTYPE();
        fastTime.setMiliSec(BigInteger.valueOf(10));
        TimeMeasureTYPE slowTime = new TimeMeasureTYPE();
        slowTime.setMiliSec(BigInteger.valueOf(10000));
        
        IdentifyPillarsForGetFileReply fastReply = new IdentifyPillarsForGetFileReply();
        fastReply.setCorrelationID(request.getCorrelationID());
        fastReply.setFileID(dataId);
        fastReply.setMinVersion((short) 1);
        fastReply.setVersion((short) 1);
        fastReply.setSlaID(request.getSlaID());
        fastReply.setTimeToDeliver(fastTime);
        fastReply.setPillarID(fastPillar);
        IdentifyPillarsForGetFileReply slowReply = new IdentifyPillarsForGetFileReply();
        slowReply.setCorrelationID(request.getCorrelationID());
        slowReply.setFileID(dataId);
        slowReply.setMinVersion((short) 1);
        slowReply.setVersion((short) 1);
        slowReply.setSlaID(request.getSlaID());
        slowReply.setTimeToDeliver(slowTime);
        slowReply.setPillarID(slowPillar);
        
        ProtocolComponentFactory.getInstance().getMessageBus().sendMessage(
                gc.getQueue(), fastReply);
        
        synchronized(this) {
            try {
                wait(WAITING_TIME_FOR_MESSAGE);
            } catch (Exception e) {
                // print, but ignore!
                e.printStackTrace();
            }
        }
        ProtocolComponentFactory.getInstance().getMessageBus().sendMessage(
                gc.getQueue(), slowReply);
        synchronized(this) {
            try {
                wait(WAITING_TIME_FOR_MESSAGE);
            } catch (Exception e) {
                // print, but ignore!
                e.printStackTrace();
            }
        }
        
        addStep("Verify that it has chosen the fast pillar.", "");
        Assert.assertEquals(listener.getMessageClass().getName(), 
                GetFileRequest.class.getName());
        GetFileRequest getRequest = (GetFileRequest) listener.getMessage();
        Assert.assertEquals(getRequest.getFileID(), dataId);
        Assert.assertEquals(getRequest.getPillarID(), fastPillar);
        
    }
    
    /**
     * Test message listener
     */
    @SuppressWarnings("rawtypes")
    protected class TestMessageListener extends AbstractMessageListener
            implements ExceptionListener {
        private Object lastMessage;

        @Override
        public void onMessage(GetFileRequest message) {
            onMessage((Object) message);
        }

        @Override
        public void onMessage(GetFileResponse message) {
            onMessage((Object) message);
        }

        @Override
        public void onMessage(IdentifyPillarsForGetFileRequest message) {
            onMessage((Object) message);
        }

        public void onMessage(Object msg) {
            try {
                lastMessage = msg;
                log.debug("TestMessageListener onMessage: " + msg.getClass());
            } catch (Exception e) {
                Assert.fail("Should not throw an exception: ", e);
            }
        }

        @Override
        public void onException(JMSException e) {
            e.printStackTrace();
        }
        public Object getMessage() {
            return lastMessage;
        }
        public Class getMessageClass() {
            return lastMessage.getClass();
        }
    }
}