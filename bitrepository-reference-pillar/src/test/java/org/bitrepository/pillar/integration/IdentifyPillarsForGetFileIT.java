package org.bitrepository.pillar.integration;

import org.bitrepository.bitrepositoryelements.ResponseCode;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileRequest;
import org.bitrepository.bitrepositorymessages.IdentifyPillarsForGetFileResponse;
import org.bitrepository.pillar.messagefactories.GetFileMessageFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;

public class IdentifyPillarsForGetFileIT extends PillarIntegrationTest {
    protected GetFileMessageFactory msgFactory;


    @BeforeMethod(alwaysRun=true)
    public void initialiseReferenceTest() throws Exception {
        msgFactory = new GetFileMessageFactory(componentSettings);
    }

    //@Test ( groups = {"pillar-integration-test"})
    public void goodCaseIdentificationIT() {
        addDescription("Tests the general IdentifyPillarsForGetFile functionality of the pillar for the successful scenario.");

        addStep("Create and send the identify request message.",
                "Should be received and handled by the pillar.");
        IdentifyPillarsForGetFileRequest identifyRequest = msgFactory.createIdentifyPillarsForGetFileRequest(
                "", TestFileHelper.DEFAULT_FILE_ID, getPillarID(), clientDestinationId);
        messageBus.sendMessage(identifyRequest);

        addStep("Retrieve and validate the response getPillarID() the pillar.",
                "The pillar should make a response.");
        IdentifyPillarsForGetFileResponse receivedIdentifyResponse = clientTopic.waitForMessage(
                IdentifyPillarsForGetFileResponse.class);
        Assert.assertEquals(receivedIdentifyResponse.getCorrelationID(), identifyRequest.getCollectionID());
        Assert.assertEquals(receivedIdentifyResponse.getCorrelationID(), identifyRequest.getCorrelationID());
        Assert.assertEquals(receivedIdentifyResponse.getFrom(), getPillarID());
        Assert.assertEquals(receivedIdentifyResponse.getFileID(), TestFileHelper.DEFAULT_FILE_ID);
        Assert.assertEquals(receivedIdentifyResponse.getPillarID(), getPillarID());
        Assert.assertEquals(receivedIdentifyResponse.getResponseInfo().getResponseCode(),
                ResponseCode.IDENTIFICATION_POSITIVE);
        Assert.assertEquals(receivedIdentifyResponse.getReplyTo(), componentSettings.getReceiverDestinationID());
        Assert.assertEquals(receivedIdentifyResponse.getTo(), identifyRequest.getReplyTo());
    }
}
