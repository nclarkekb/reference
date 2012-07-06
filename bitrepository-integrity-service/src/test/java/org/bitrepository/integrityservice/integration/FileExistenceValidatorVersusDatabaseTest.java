package org.bitrepository.integrityservice.integration;

import static org.bitrepository.integrityservice.cache.database.DatabaseConstants.CHECKSUM_TABLE;
import static org.bitrepository.integrityservice.cache.database.DatabaseConstants.FILES_TABLE;
import static org.bitrepository.integrityservice.cache.database.DatabaseConstants.FILE_INFO_TABLE;
import static org.bitrepository.integrityservice.cache.database.DatabaseConstants.PILLAR_TABLE;

import java.io.File;
import java.math.BigInteger;
import java.sql.Connection;
import java.util.Arrays;

import org.bitrepository.bitrepositoryelements.FileIDs;
import org.bitrepository.bitrepositoryelements.FileIDsData;
import org.bitrepository.bitrepositoryelements.FileIDsData.FileIDsDataItems;
import org.bitrepository.bitrepositoryelements.FileIDsDataItem;
import org.bitrepository.common.database.DatabaseUtils;
import org.bitrepository.common.settings.Settings;
import org.bitrepository.common.settings.TestSettingsProvider;
import org.bitrepository.common.utils.CalendarUtils;
import org.bitrepository.common.utils.DatabaseTestUtils;
import org.bitrepository.common.utils.FileUtils;
import org.bitrepository.integrityservice.cache.IntegrityDatabase;
import org.bitrepository.integrityservice.cache.IntegrityModel;
import org.bitrepository.integrityservice.checking.FileExistenceValidator;
import org.bitrepository.integrityservice.checking.reports.MissingFileReport;
import org.bitrepository.integrityservice.mocks.MockAuditManager;
import org.jaccept.structure.ExtendedTestCase;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class FileExistenceValidatorVersusDatabaseTest extends ExtendedTestCase {
    /** The settings for the tests. Should be instantiated in the setup.*/
    Settings settings;
    MockAuditManager auditManager;
    
    public static final String TEST_PILLAR_1 = "test-pillar-1";
    public static final String TEST_PILLAR_2 = "test-pillar-2";
    
    public static final String FILE_1 = "test-file-1";
    
    String DATABASE_NAME = "integritydb";
    String DATABASE_DIRECTORY = "test-database";
    String DATABASE_URL = "jdbc:derby:" + DATABASE_DIRECTORY + "/" + DATABASE_NAME;
    File dbDir = null;
    Connection dbCon;
    
    @BeforeMethod (alwaysRun = true)
    public void setup() throws Exception {
        settings = TestSettingsProvider.reloadSettings("IntegrityCheckingUnderTest");
        settings.getCollectionSettings().getClientSettings().getPillarIDs().clear();
        settings.getCollectionSettings().getClientSettings().getPillarIDs().add(TEST_PILLAR_1);
        settings.getCollectionSettings().getClientSettings().getPillarIDs().add(TEST_PILLAR_2);
        settings.getReferenceSettings().getIntegrityServiceSettings().setTimeBeforeMissingFileCheck(0L);
        auditManager = new MockAuditManager();

        File dbFile = new File("src/test/resources/integritydb.jar");
        Assert.assertTrue(dbFile.isFile(), "The database file should exist");
        
        dbDir = FileUtils.retrieveDirectory(DATABASE_DIRECTORY);
        FileUtils.retrieveSubDirectory(dbDir, DATABASE_NAME);
        
        dbCon = DatabaseTestUtils.takeDatabase(dbFile, DATABASE_NAME, dbDir);
        auditManager = new MockAuditManager();
        settings.getReferenceSettings().getIntegrityServiceSettings().setIntegrityDatabaseUrl(DATABASE_URL);
    }
    

    @AfterMethod (alwaysRun = true)
    public void clearDatabase() throws Exception {
        DatabaseUtils.executeStatement(dbCon, "DELETE FROM " + FILE_INFO_TABLE, new Object[0]);
        DatabaseUtils.executeStatement(dbCon, "DELETE FROM " + FILES_TABLE, new Object[0]);
        DatabaseUtils.executeStatement(dbCon, "DELETE FROM " + CHECKSUM_TABLE, new Object[0]);
        DatabaseUtils.executeStatement(dbCon, "DELETE FROM " + PILLAR_TABLE, new Object[0]);
    }

    @AfterClass (alwaysRun = true)
    public void cleanup() throws Exception {
        if(dbCon != null) {
            dbCon.close();
        }
        if(dbDir != null) {
            FileUtils.delete(dbDir);
        }
    }
    
    @Test(groups = {"regressiontest", "integritytest"})
    public void testNoData() {
        addDescription("Test the file existence validator without any data in the cache.");
        IntegrityModel cache = getIntegrityModel();
        FileExistenceValidator validator = new FileExistenceValidator(settings, cache, auditManager);
        
        addStep("Validate the file ids", "Should not have integrity issues.");
        MissingFileReport report = validator.generateReport(cache.getAllFileIDs());
        Assert.assertFalse(report.hasIntegrityIssues(), report.generateReport());
    }
    
    @Test(groups = {"regressiontest", "integritytest"})
    public void testSimilarData() {
        addDescription("Test the file existence validator when both pillars have similar data.");
        IntegrityModel cache = getIntegrityModel();
        FileExistenceValidator validator = new FileExistenceValidator(settings, cache, auditManager);
        
        addStep("Add data to the cache", "");
        FileIDsData fileidData = createFileIdData(FILE_1);
        cache.addFileIDs(fileidData, getAllFileIDs(), TEST_PILLAR_1);
        cache.addFileIDs(fileidData, getAllFileIDs(), TEST_PILLAR_2);
        
        addStep("Validate the file ids", "Should not have integrity issues.");
        Assert.assertFalse(validator.generateReport(cache.getAllFileIDs()).hasIntegrityIssues());
    }
    
    @Test(groups = {"regressiontest", "integritytest"})
    public void testMissingDataAtOnePillar() {
        addDescription("Test the file existence validator when the pillars data differ.");
        IntegrityModel cache = getIntegrityModel();
        FileExistenceValidator validator = new FileExistenceValidator(settings, cache, auditManager);
        
        addStep("Add data to the cache for only one pillar", "");
        FileIDsData fileidData = createFileIdData(FILE_1);
        cache.addFileIDs(fileidData, getAllFileIDs(), TEST_PILLAR_1);
        
        addStep("Validate the file ids", "Should be missing at pillar 2.");
        MissingFileReport report = validator.generateReport(cache.getAllFileIDs());
        Assert.assertTrue(report.hasIntegrityIssues());
        Assert.assertEquals(report.getDeleteableFiles().size(), 0);
        Assert.assertEquals(report.getMissingFiles().size(), 1);
        Assert.assertNotNull(report.getMissingFiles().get(FILE_1));
        Assert.assertEquals(report.getMissingFiles().get(FILE_1), Arrays.asList(TEST_PILLAR_2));
    }
    
    @Test(groups = {"regressiontest", "integritytest"})
    public void testDataSatToMissing() {
        addDescription("Test the file existence validator when the filestate is set to missing at one pillar.");
        IntegrityModel cache = getIntegrityModel();
        FileExistenceValidator validator = new FileExistenceValidator(settings, cache, auditManager);
        
        addStep("Add data to the cache for only one pillar", "");
        FileIDsData fileidData = createFileIdData(FILE_1);
        cache.addFileIDs(fileidData, getAllFileIDs(), TEST_PILLAR_1);
        cache.addFileIDs(fileidData, getAllFileIDs(), TEST_PILLAR_2);
        cache.setFileMissing(FILE_1, Arrays.asList(TEST_PILLAR_1));
        
        addStep("Validate the file ids", "Should be missing at pillar 1.");
        MissingFileReport report = validator.generateReport(cache.getAllFileIDs());
        Assert.assertTrue(report.hasIntegrityIssues());
        Assert.assertEquals(report.getDeleteableFiles().size(), 0);
        Assert.assertEquals(report.getMissingFiles().size(), 1);
        Assert.assertNotNull(report.getMissingFiles().get(FILE_1));
        Assert.assertEquals(report.getMissingFiles().get(FILE_1), Arrays.asList(TEST_PILLAR_1));
    }
    
    @Test(groups = {"regressiontest", "integritytest"})
    public void testDataMissingAtBothPillars() {
        addDescription("Test the file existence validator when the file is missing at both pillars.");
        IntegrityModel cache = getIntegrityModel();
        FileExistenceValidator validator = new FileExistenceValidator(settings, cache, auditManager);
        
        addStep("Add data to the cache for only one pillar", "");
        FileIDsData fileidData = createFileIdData(FILE_1);
        cache.addFileIDs(fileidData, getAllFileIDs(), TEST_PILLAR_1);
        cache.setFileMissing(FILE_1, Arrays.asList(TEST_PILLAR_1));
        
        addStep("Validate the file ids", "Should be missing at pillar 1.");
        MissingFileReport report = validator.generateReport(cache.getAllFileIDs());
        Assert.assertTrue(report.hasIntegrityIssues());
        Assert.assertEquals(report.getDeleteableFiles().size(), 1);
        Assert.assertEquals(report.getDeleteableFiles(), Arrays.asList(FILE_1));
        Assert.assertEquals(report.getMissingFiles().size(), 0);
    }
    
    private FileIDsData createFileIdData(String ... fileids) {
        FileIDsData res = new FileIDsData();
        FileIDsDataItems items = new FileIDsDataItems();
        for(String fileid : fileids) {
            FileIDsDataItem item = new FileIDsDataItem();
            item.setFileID(fileid);
            item.setFileSize(BigInteger.ONE);
            item.setLastModificationTime(CalendarUtils.getNow());
            items.getFileIDsDataItem().add(item);
        }
        res.setFileIDsDataItems(items);
        return res;
    }

    private IntegrityModel getIntegrityModel() {
//        return new TestIntegrityModel(settings.getCollectionSettings().getClientSettings().getPillarIDs());
        return new IntegrityDatabase(settings);

    }
    
    private FileIDs getAllFileIDs() {
        FileIDs res = new FileIDs();
        res.setAllFileIDs("true");
        return res;
    }

}
