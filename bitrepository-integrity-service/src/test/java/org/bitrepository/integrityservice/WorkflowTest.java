package org.bitrepository.integrityservice;

import java.util.Date;

import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.common.settings.Settings;
import org.bitrepository.common.settings.TestSettingsProvider;
import org.bitrepository.integrityservice.cache.IntegrityModel;
import org.bitrepository.integrityservice.mocks.MockChecker;
import org.bitrepository.integrityservice.mocks.MockCollector;
import org.bitrepository.integrityservice.mocks.MockIntegrityModel;
import org.bitrepository.integrityservice.mocks.MockWorkflow;
import org.bitrepository.integrityservice.workflow.scheduler.CollectAllChecksumsWorkflow;
import org.bitrepository.integrityservice.workflow.scheduler.CollectAllFileIDsWorkflow;
import org.bitrepository.integrityservice.workflow.scheduler.CollectObsoleteChecksumsWorkflow;
import org.bitrepository.integrityservice.workflow.scheduler.IntegrityValidatorWorkflow;
import org.jaccept.structure.ExtendedTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test that triggering works.
 */
public class WorkflowTest extends ExtendedTestCase {
    Settings settings;
    
    @BeforeClass (alwaysRun = true)
    public void setup() {
        settings = TestSettingsProvider.reloadSettings();
    }
    
    @Test(groups = {"regressiontest"})
    public void testIntervalWorkflow() {
        addDescription("Test the basic functionallity of the workflows.");
        addStep("Setup variables and workflow.", "No errors");
        String name = "mock-1";
        long interval = 3600000L; 
        MockWorkflow workflow = new MockWorkflow(interval, name);
        
        addStep("Validate the interface to the workflow.", "Should contain the given variables and none of the functions should have been called.");
        Assert.assertEquals(workflow.getNextRunCount(), 0, "Did not expect any calls for NextRun yet.");
        Assert.assertEquals(workflow.getWorkflowCalled(), 0, "Did not expect any calls for trigger yet.");
        Assert.assertEquals(workflow.timeBetweenRuns(), interval);
        Assert.assertEquals(workflow.getName(), name);
        
        addStep("Retrieve the date for next run and then trigger it.", 
                "Should be counted and the workflow should get a new date for the next run.");
        Date nextRun = workflow.getNextRun();
        workflow.trigger();
        Assert.assertEquals(workflow.getNextRunCount(), 1, "Should have called for NextRun once");
        Assert.assertEquals(workflow.getWorkflowCalled(), 1, "Should have triggered the workflow once");
        
        
        Assert.assertTrue( nextRun != workflow.getNextRun(), "The dates should differ");
        Assert.assertTrue(nextRun.getTime() < workflow.getNextRun().getTime(), 
                "The initial date for 'NextRun' should be prior to the current.");
    }
    
    @Test(groups = {"regressiontest"})
    public void testCollectAllChecksumWorkflow() {
        addDescription("Testing that the CollecAllChecksumWorkflow calls the 'getChecksums' function on the collector.");
        addStep("Setup variables", "No errors.");
        MockCollector collector = new MockCollector();
        String workflowName = "CollectAllChecksumWorkflowTest";
        ChecksumSpecTYPE csType = new ChecksumSpecTYPE();
        long interval = 25000L;
        CollectAllChecksumsWorkflow workflow = new CollectAllChecksumsWorkflow(interval, workflowName, 
                csType, settings, collector, null);
        
        addStep("Validate initial position.", "Should be no calls for any method yet.");
        Assert.assertEquals(collector.getNumberOfCallsForGetChecksums(), 0, "No calls for GetChecksum shuld have been made");
        Assert.assertEquals(collector.getNumberOfCallsForGetFileIDs(), 0, "No calls for GetFileIDs shuld have been made");
        
        addStep("Trigger the workflow", "Should make a call for GetChecksums, but none for GetFileIDs");
        workflow.trigger();
        Assert.assertEquals(collector.getNumberOfCallsForGetChecksums(), 1, "One call for GetChecksums should have been made");
        Assert.assertEquals(collector.getNumberOfCallsForGetFileIDs(), 0, "No calls for GetFileIDs shuld have been made");

        addStep("Trigger the workflow 4 times more", "Should have made 5 call for GetChecksums, but none for GetFileIDs");
        workflow.trigger();
        workflow.trigger();
        workflow.trigger();
        workflow.trigger();
        Assert.assertEquals(collector.getNumberOfCallsForGetChecksums(), 5, "5 calls for GetChecksums should have been made");
        Assert.assertEquals(collector.getNumberOfCallsForGetFileIDs(), 0, "No calls for GetFileIDs shuld have been made");
    }
    
    @Test(groups = {"regressiontest"})
    public void testCollectAllFileIDsWorkflow() {
        addDescription("Testing that the CollecAllFileIDsWorkflow calls the 'getFileIDs' function on the collector.");
        addStep("Setup variables", "No errors.");
        MockCollector collector = new MockCollector();
        String workflowName = "CollectAllFileIDsWorkflowTest";
        long interval = 25000L;
        CollectAllFileIDsWorkflow workflow = new CollectAllFileIDsWorkflow(interval, workflowName, settings, collector, null);
        
        addStep("Validate initial position.", "Should be no calls for any method yet.");
        Assert.assertEquals(collector.getNumberOfCallsForGetChecksums(), 0, "No calls for GetChecksum shuld have been made");
        Assert.assertEquals(collector.getNumberOfCallsForGetFileIDs(), 0, "No calls for GetFileIDs shuld have been made");
        
        addStep("Trigger the workflow", "Should make a call for GetFileIDs, but none for GetChecksums");
        workflow.trigger();
        Assert.assertEquals(collector.getNumberOfCallsForGetChecksums(), 0, "No calls for GetChecksum expected.");
        Assert.assertEquals(collector.getNumberOfCallsForGetFileIDs(), 1, "One call for GetFileIDs expected.");

        addStep("Trigger the workflow 4 times more", "Should have made 5 call for GetFileIDs, but none for GetChecksums");
        workflow.trigger();
        workflow.trigger();
        workflow.trigger();
        workflow.trigger();
        Assert.assertEquals(collector.getNumberOfCallsForGetChecksums(), 0, "No calls for GetChecksums expected");
        Assert.assertEquals(collector.getNumberOfCallsForGetFileIDs(), 5, "5 calls for GetFileIDs expected");
    }

    @Test(groups = {"regressiontest"})
    public void testIntegrityValidatorWorkflow() {
        addDescription("Tests the IntegrityValidatorWorkflow");
        addStep("Setup the variables.", "No errors.");
        long interval = 3600000;
        String name = "IntegrityValidatorWorkflowTest";
        MockChecker checker = new MockChecker();
        IntegrityValidatorWorkflow workflow = new IntegrityValidatorWorkflow(interval, name, checker);
        
        addStep("Validate the initial position.", "No call for the checker yet.");
        Assert.assertEquals(checker.getCallsForCheckChecksums(), 0, "No calls for CheckChecksums expected");
        Assert.assertEquals(checker.getCallsForCheckFileIDs(), 0, "No calls for CheckFileIDs expected");
        
        addStep("Trigger the workflow", "One call both for the CheckChecksums and ChecksFileIDs expected");
        workflow.trigger();
        Assert.assertEquals(checker.getCallsForCheckChecksums(), 1, "One call for CheckChecksums expected");
        Assert.assertEquals(checker.getCallsForCheckFileIDs(), 1, "One call for CheckFileIDs expected");

        addStep("Trigger the workflow four more times.", "The CheckChecksums and ChecksFileIDs should have been called 5 times.");
        workflow.trigger();
        workflow.trigger();
        workflow.trigger();
        workflow.trigger();
        Assert.assertEquals(checker.getCallsForCheckChecksums(), 5, "Five calls for CheckChecksums expected");
        Assert.assertEquals(checker.getCallsForCheckFileIDs(), 5, "Five calls for CheckFileIDs expected");
    }

    @Test(groups = {"regressiontest"})
    public void testCollectObsoleteChecksumsWorkflow() {
        addDescription("Tests the CollectObsoleteChecksumWorkflow");
        addStep("Setup the varibles.", "No errors");
        long interval = 1L;
        String name = "CollectObsoleteChecksumsWorkflowTest";
        long obsoleteInterval = 1000000000L;
        MockCollector collector = new MockCollector();
        IntegrityModel cache = new TestIntegrityModel();
        MockIntegrityModel model = new MockIntegrityModel(cache);
        CollectObsoleteChecksumsWorkflow workflow = new CollectObsoleteChecksumsWorkflow(interval, name, 
                obsoleteInterval, new ChecksumSpecTYPE(), collector, model, null);
        
        addStep("Validate the initial position", "No calls for either collector or cache");
        // TODO
        addStep("Trigger the workflow", "Should call the cache.");
        // TODO
        addStep("Add one file with a new checksum to the cache. Then trigger the workflow.", 
                "Should only call the cache.");
        // TODO
        addStep("Add another file, but with an old checksum to the cache. Then trigger the workflow.",
                "Should call both the cache and the collector.");
        // TODO
    }    

    @Test(groups = "specificationonly")
    void testTriggerOnOldData() throws Exception {
        addDescription("Test that triggering on old data works");
        addStep("Set up the system", "No errors");
        // TODO
        addStep("Test that the trigger triggers when data is old", "Trigger responds with true");
        // TODO
        addStep("Test that event is to collect information", "Trigger calls information collection methods");
        // TODO
    }

    @Test(groups = "specificationonly")
    void testTriggerOfRandomSet() throws Exception {
        addDescription("Test that a trigger that should generate collection on random files does so.");
        addStep("Set up a trigger that generates collection of checksums on three random files", "No errors");
        // TODO
        addStep("Pull the trigger 10 times", "Trigger generates collection event for three random files");
        // TODO
    }
}
