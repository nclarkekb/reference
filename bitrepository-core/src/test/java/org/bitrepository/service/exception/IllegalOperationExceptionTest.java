package org.bitrepository.service.exception;

import org.bitrepository.bitrepositoryelements.ResponseCode;
import org.bitrepository.bitrepositoryelements.ResponseInfo;
import org.jaccept.structure.ExtendedTestCase;
import org.testng.Assert;
import org.testng.annotations.Test;

public class IllegalOperationExceptionTest extends ExtendedTestCase {
    
    @Test(groups = { "regressiontest" })
    public void testIdentifyContributor() throws Exception {
        addDescription("Test the instantiation of the exception");
        addStep("Setup", "");
        String errMsg = "TEST-ERROR";
        ResponseCode errCode = ResponseCode.FAILURE;
        ResponseInfo ri = new ResponseInfo();
        ri.setResponseText(errMsg);
        ri.setResponseCode(errCode);
        String causeMsg = "CAUSE-EXCEPTION";
        
        addStep("Try to throw such an exception", "Should be able to be caught and validated");
        try {
            throw new IllegalOperationException(ri);
        } catch(Exception e) {
            Assert.assertTrue(e instanceof IllegalOperationException);
            Assert.assertEquals(e.getMessage(), errMsg);
            Assert.assertEquals(((IllegalOperationException) e).getResponseInfo().getResponseCode(), errCode);
            Assert.assertEquals(((IllegalOperationException) e).getResponseInfo().getResponseText(), errMsg);
            Assert.assertNull(e.getCause());
        }
        
        addStep("Throw the exception with an embedded exception", "The embedded exception should be the same.");
        try {
            throw new IllegalOperationException(ri, new IllegalArgumentException(causeMsg));
        } catch(Exception e) {
            Assert.assertTrue(e instanceof IllegalOperationException);
            Assert.assertTrue(e instanceof RequestHandlerException);
            Assert.assertEquals(e.getMessage(), errMsg);
            Assert.assertEquals(((IllegalOperationException) e).getResponseInfo().getResponseCode(), errCode);
            Assert.assertEquals(((IllegalOperationException) e).getResponseInfo().getResponseText(), errMsg);
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof IllegalArgumentException);
            Assert.assertEquals(e.getCause().getMessage(), causeMsg);
            Assert.assertNotNull(((RequestHandlerException) e).toString());
        }
    }
}
