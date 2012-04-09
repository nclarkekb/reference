package org.bitrepository.access.getstatus;

import java.util.Date;

import org.bitrepository.bitrepositoryelements.ResultingStatus;
import org.bitrepository.bitrepositoryelements.StatusInfo;
import org.bitrepository.bitrepositoryelements.StatusCode;
import org.bitrepository.bitrepositorymessages.GetStatusFinalResponse;
import org.bitrepository.bitrepositorymessages.GetStatusProgressResponse;
import org.bitrepository.bitrepositorymessages.GetStatusRequest;
import org.bitrepository.bitrepositorymessages.IdentifyContributorsForGetStatusRequest;
import org.bitrepository.bitrepositorymessages.IdentifyContributorsForGetStatusResponse;
import org.bitrepository.bitrepositorymessages.Message;
import org.bitrepository.common.utils.CalendarUtils;
import org.bitrepository.protocol.TestMessageFactory;

public class TestGetStatusMessageFactory extends TestMessageFactory {

    protected final String collectionID;
    
    public TestGetStatusMessageFactory(String collectionID) {
        this.collectionID = collectionID;
    }
    
    public IdentifyContributorsForGetStatusResponse createIdentifyContributorsForGetStatusResponse(
            IdentifyContributorsForGetStatusRequest request) {
        IdentifyContributorsForGetStatusResponse response = createIdentifyContributorsForGetStatusResponse();
        response.setCorrelationID(request.getCorrelationID());
        response.setTo(request.getReplyTo());
        response.setReplyTo("ME");
        response.setResponseInfo(IDENTIFY_INFO_DEFAULT);
        response.setTimeToDeliver(TIME_TO_DELIVER_DEFAULT);
        
        return response;
    }
    
    public IdentifyContributorsForGetStatusResponse createIdentifyContributorsForGetStatusResponse() {
        IdentifyContributorsForGetStatusResponse response = new IdentifyContributorsForGetStatusResponse();
        setMessageDetails(response);
                
        return response;
    }
    
    public GetStatusProgressResponse createGetStatusProgressResponse() {
        GetStatusProgressResponse response = new GetStatusProgressResponse();
        setMessageDetails(response);
        
        return response;
    }
    
    public GetStatusFinalResponse createGetStatusFinalResponse(GetStatusRequest request) {
        GetStatusFinalResponse response = createGetStatusFinalResponse();
        response.setCorrelationID(request.getCorrelationID());
        response.setTo(request.getReplyTo());
        response.setReplyTo("ME");
        response.setContributor(request.getContributor());
        response.setResponseInfo(FINAL_INFO_DEFAULT);
        ResultingStatus status = new ResultingStatus();
        status.setStatusTimestamp(CalendarUtils.getXmlGregorianCalendar(new Date()));
        StatusInfo info = new StatusInfo();
        info.setStatusCode(StatusCode.OK);
        info.setStatusText("All is good");
        status.setStatusInfo(info);
        response.setResultingStatus(status);
        return response;
    }
    
    public GetStatusFinalResponse createGetStatusFinalResponse() {
        GetStatusFinalResponse response = new GetStatusFinalResponse();
        setMessageDetails(response);
        
        return response;
    } 
    
    private void setMessageDetails(Message msg) {
        msg.setCollectionID(collectionID);
        msg.setVersion(VERSION_DEFAULT);
        msg.setMinVersion(VERSION_DEFAULT);
    }
}
