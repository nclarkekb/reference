/*
 * #%L
 * Bitrepository Reference Pillar
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
package org.bitrepository.pillar;

import org.bitrepository.bitrepositoryelements.ResponseCode;
import org.bitrepository.bitrepositorymessages.IdentifyContributorsForGetStatusRequest;
import org.bitrepository.bitrepositorymessages.IdentifyContributorsForGetStatusResponse;
import org.bitrepository.bitrepositorymessages.MessageResponse;
import org.bitrepository.pillar.checksumpillar.messagehandler.ChecksumPillarMediator;
import org.bitrepository.pillar.common.PillarContext;
import org.bitrepository.pillar.common.PillarMediator;
import org.bitrepository.service.contributor.ContributorContext;
import org.bitrepository.service.contributor.handler.RequestHandler;
import org.bitrepository.service.exception.RequestHandlerException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MediatorTest extends DefaultFixturePillarTest {
    ChecksumPillarMediator mediator;
    MockAlarmDispatcher alarmDispatcher;
    MockAuditManager audits;
    PillarContext context;
    
    @BeforeMethod (alwaysRun=true)
    public void initialiseTest() throws Exception {
        audits = new MockAuditManager();
        ContributorContext contributorContext = new ContributorContext(messageBus, componentSettings);
        alarmDispatcher = new MockAlarmDispatcher(contributorContext);
        context = new PillarContext(componentSettings, messageBus, alarmDispatcher, audits);
    }
    
    @Test( groups = {"regressiontest", "pillartest"})
    public void testMediatorRuntimeExceptionHandling() throws Exception {
        addDescription("Tests the handling of a runtime exception");
        addStep("Setup create and start the mediator.", "");
        
        TestMediator mediator = new TestMediator(context);
        try {
            mediator.start();
            
            Assert.assertEquals(alarmDispatcher.getCallsForSendAlarm(), 0);
            
            addStep("Send a request to the mediator.", "Should be caught.");
            IdentifyContributorsForGetStatusRequest request = new IdentifyContributorsForGetStatusRequest();
            request.setAuditTrailInformation("audit");
            request.setCollectionID(componentSettings.getCollectionID());
            request.setCorrelationID(UUID.randomUUID().toString());
            request.setFrom(getPillarID());
            request.setMinVersion(BigInteger.valueOf(1L));
            request.setReplyTo(clientDestinationId);
            request.setTo(componentSettings.getCollectionDestination());
            request.setVersion(BigInteger.valueOf(1L));
            messageBus.sendMessage(request);
            
            MessageResponse response = clientTopic.waitForMessage(IdentifyContributorsForGetStatusResponse.class);
            Assert.assertEquals(response.getResponseInfo().getResponseCode(), ResponseCode.FAILURE);
            Assert.assertEquals(alarmDispatcher.getCallsForSendAlarm(), 1);
        } finally {
            mediator.close();
        }
    }
    
    @Test( groups = {"regressiontest", "pillartest"})
    public void testMediatorInvalidCollectionID() throws Exception {
        addDescription("Tests the handling of an invalid collection id");
        addStep("Setup create and start the mediator.", "");
        String wrongCollectionID = "wrongCollectionID";
        
        TestMediator mediator = new TestMediator(context);
        try {
            mediator.testCollectionID(wrongCollectionID);
            Assert.fail("Should throw an " + IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Override
    protected String getComponentID() {
        return "MediatorUnderTest";
    }

    private class TestMediator extends PillarMediator {

        public TestMediator(PillarContext context) {
            super(context);
        }
        
        public void testCollectionID(String collectionID) {
            validateBitrepositoryCollectionId(collectionID);
        }

        @SuppressWarnings("rawtypes")
        @Override
        protected RequestHandler[] createListOfHandlers() {
            List<RequestHandler> handlers = new ArrayList<RequestHandler>();
            handlers.add(new ErroneousRequestHandler());
            return handlers.toArray(new RequestHandler[handlers.size()]);
        }
    }
    
    private class ErroneousRequestHandler implements RequestHandler<IdentifyContributorsForGetStatusRequest> {

        @Override
        public Class<IdentifyContributorsForGetStatusRequest> getRequestClass() {
            return IdentifyContributorsForGetStatusRequest.class;
        }

        @Override
        public void processRequest(IdentifyContributorsForGetStatusRequest request) throws RequestHandlerException {
            throw new RuntimeException("I am supposed to throw a RuntimeException");
        }

        @Override
        public IdentifyContributorsForGetStatusResponse generateFailedResponse(IdentifyContributorsForGetStatusRequest request) {
            IdentifyContributorsForGetStatusResponse res = new IdentifyContributorsForGetStatusResponse();
            res.setCollectionID(request.getCollectionID());
            res.setContributor(request.getTo());
            res.setCorrelationID(request.getCorrelationID());
            res.setFrom(request.getTo());
            res.setMinVersion(request.getMinVersion());
            res.setReplyTo(request.getTo());
            res.setTo(request.getReplyTo());
            res.setVersion(request.getVersion());
            return res;
        }
    }
}