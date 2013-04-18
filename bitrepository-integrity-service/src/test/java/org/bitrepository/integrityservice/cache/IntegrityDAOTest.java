/*
 * #%L
 * Bitrepository Integrity Client
 * 
 * $Id$
 * $HeadURL$
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
package org.bitrepository.integrityservice.cache;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.bitrepository.bitrepositoryelements.ChecksumDataForChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.FileIDsData;
import org.bitrepository.bitrepositoryelements.FileIDsData.FileIDsDataItems;
import org.bitrepository.bitrepositoryelements.FileIDsDataItem;
import org.bitrepository.service.database.DBConnector;
import org.bitrepository.common.utils.Base16Utils;
import org.bitrepository.common.utils.CalendarUtils;
import org.bitrepository.common.utils.SettingsUtils;
import org.bitrepository.integrityservice.IntegrityDatabaseTestCase;
import org.bitrepository.integrityservice.cache.database.ChecksumState;
import org.bitrepository.integrityservice.cache.database.FileState;
import org.bitrepository.integrityservice.cache.database.IntegrityDAO;
import org.bitrepository.integrityservice.mocks.MockAuditManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class IntegrityDAOTest extends IntegrityDatabaseTestCase {
    /** The settings for the tests. Should be instantiated in the setup.*/
    MockAuditManager auditManager;
    String TEST_PILLAR_1 = "MY-TEST-PILLAR-1";
    String TEST_PILLAR_2 = "MY-TEST-PILLAR-2";
    String EXTRA_PILLAR = "MY-EXTRA-PILLAR";
    
    String TEST_FILE_ID = "TEST-FILE-ID";
    String TEST_CHECKSUM = "1234cccc4321";
    
    String TEST_COLLECTIONID;
    public static final String EXTRA_COLLECTION = "extra-collection";

    @BeforeMethod (alwaysRun = true)
    @Override
    public void setup() throws Exception {
        super.setup();
        TEST_COLLECTIONID = settings.getRepositorySettings().getCollections().getCollection().get(0).getID();
        auditManager = new MockAuditManager();
    }
    
    @Override 
    protected void customizeSettings() {
        org.bitrepository.settings.repositorysettings.Collection c0 = 
                settings.getRepositorySettings().getCollections().getCollection().get(0);
        c0.getPillarIDs().getPillarID().clear();
        c0.getPillarIDs().getPillarID().add(TEST_PILLAR_1);
        c0.getPillarIDs().getPillarID().add(TEST_PILLAR_2);
        settings.getRepositorySettings().getCollections().getCollection().clear();
        settings.getRepositorySettings().getCollections().getCollection().add(c0);
        
        org.bitrepository.settings.repositorysettings.Collection extraCollection = 
                new org.bitrepository.settings.repositorysettings.Collection();
        extraCollection.setID(EXTRA_COLLECTION);
        org.bitrepository.settings.repositorysettings.PillarIDs pids 
            = new org.bitrepository.settings.repositorysettings.PillarIDs();
        pids.getPillarID().add(TEST_PILLAR_1);
        pids.getPillarID().add(EXTRA_PILLAR);
        extraCollection.setPillarIDs(pids);
        settings.getRepositorySettings().getCollections().getCollection().add(extraCollection);
        
        
    }

    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void instantiationTest() throws Exception {
        addDescription("Testing the connection to the integrity database.");
        IntegrityDAO cache = createDAO();
        Assert.assertNotNull(cache);
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testPillarInkonsistencies() throws Exception {
        addDescription("Testing how the database handles inkonsistencies in the list of pillars.");
        addStep("Test when creating the database when only pillar 1 defined", "Is created and closes fine afterwards.");
        settings.getRepositorySettings().getCollections().getCollection().get(0).getPillarIDs().getPillarID().clear();
        settings.getRepositorySettings().getCollections().getCollection().get(0).getPillarIDs().getPillarID().add(TEST_PILLAR_1);
        IntegrityDAO cache = createDAO();
        cache.close();
        
        addStep("Testing when only the other pillar is defined.", "Throws an IllegalStateException");
        settings.getRepositorySettings().getCollections().getCollection().get(0).getPillarIDs().getPillarID().clear();
        settings.getRepositorySettings().getCollections().getCollection().get(0).getPillarIDs().getPillarID().add(TEST_PILLAR_2);
        try {
            cache = createDAO();
            Assert.fail("Should throw an IllegalStateException here!");
        } catch (IllegalStateException e) {
            // Expected.
        }
        
        addStep("Testing when both the other pillars are defined.", "Throws an IllegalStateException");
        settings.getRepositorySettings().getCollections().getCollection().get(0).getPillarIDs().getPillarID().clear();
        settings.getRepositorySettings().getCollections().getCollection().get(0).getPillarIDs().getPillarID().add(TEST_PILLAR_1);
        settings.getRepositorySettings().getCollections().getCollection().get(0).getPillarIDs().getPillarID().add(TEST_PILLAR_2);
        try {
            cache = createDAO();
            Assert.fail("Should throw an IllegalStateException here!");
        } catch (IllegalStateException e) {
            // Expected.
        }
    }

    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void reinitialiseDatabaseTest() throws Exception {
        addDescription("Testing the connection to the integrity database.");
        addStep("Setup manually.", "Should be created.");
        DBConnector connector = new DBConnector(
                settings.getReferenceSettings().getIntegrityServiceSettings().getIntegrityDatabase());
                        
        IntegrityDAO cache = new IntegrityDAO(connector, settings.getRepositorySettings().getCollections());
        Assert.assertNotNull(cache);

        addStep("Close the connection and create another one.", "Should not fail");
        connector.getConnection().close();

        synchronized(this) {
            wait(100);
        }
        
        DBConnector reconnector = new DBConnector(
                settings.getReferenceSettings().getIntegrityServiceSettings().getIntegrityDatabase());
        cache = new IntegrityDAO(reconnector, settings.getRepositorySettings().getCollections());
    }

    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void initialStateExtractionTest() throws Exception {
        addDescription("Tests the initial state of the IntegrityModel. Should not contain any data.");
        IntegrityDAO cache = createDAO();
        
        addStep("Test the 'findFilesWithOldChecksum'", "Should deliver an empty collection");
        Collection<String> oldChecksums = cache.findFilesWithOldChecksum(new Date(0), TEST_PILLAR_1, TEST_COLLECTIONID);
        Assert.assertNotNull(oldChecksums);
        Assert.assertEquals(oldChecksums.size(), 0);
        
        addStep("Test the 'findMissingChecksums'", "Should deliver an empty collection");
        Collection<String> missingChecksums = cache.findMissingChecksums(TEST_COLLECTIONID);
        Assert.assertNotNull(missingChecksums);
        Assert.assertEquals(missingChecksums.size(), 0);
        
        addStep("Test the 'findMissingFiles'", "Should deliver an empty collection");
        Collection<String> missingFiles = cache.findMissingFiles(TEST_COLLECTIONID);
        Assert.assertNotNull(missingFiles);
        Assert.assertEquals(missingFiles.size(), 0);
        
        addStep("Test the 'getAllFileIDs'", "Should deliver an empty collection");
        Collection<String> allFileIDs = cache.getAllFileIDs(TEST_COLLECTIONID);
        Assert.assertNotNull(allFileIDs);
        Assert.assertEquals(allFileIDs.size(), 0);
        
        addStep("Test the 'getFileInfosForFile'", "Should deliver an empty collection");
        Collection<FileInfo> fileInfos = cache.getFileInfosForFile(TEST_FILE_ID, TEST_COLLECTIONID);
        Assert.assertNotNull(fileInfos);
        Assert.assertEquals(fileInfos.size(), 0);
        
        addStep("Test the 'getNumberOfMissingFilesForAPillar'", "Should be zero for both pillars.");
        Assert.assertEquals(cache.getNumberOfChecksumErrorsForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 0);
        Assert.assertEquals(cache.getNumberOfChecksumErrorsForAPillar(TEST_PILLAR_2, TEST_COLLECTIONID), 0);
        
        addStep("Test the 'getNumberOfExistingFilesForAPillar'", "Should be zero for both pillars.");
        Assert.assertEquals(cache.getNumberOfExistingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 0);
        Assert.assertEquals(cache.getNumberOfExistingFilesForAPillar(TEST_PILLAR_2, TEST_COLLECTIONID), 0);
        
        addStep("Test the 'getNumberOfMissingFilesForAPillar'", "Should be zero for both pillars.");
        Assert.assertEquals(cache.getNumberOfMissingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 0);
        Assert.assertEquals(cache.getNumberOfMissingFilesForAPillar(TEST_PILLAR_2, TEST_COLLECTIONID), 0);
        
        addStep("Test the 'getPillarsMissingFile'", "Should deliver an empty collection");
        Collection<String> pillarsMissingFile = cache.getMissingAtPillars(TEST_FILE_ID, TEST_COLLECTIONID);
        Assert.assertNotNull(pillarsMissingFile);
        Assert.assertEquals(pillarsMissingFile.size(), 0);
        
        addStep("Test that the database knows the extra collection", "should deliver an empty collection and no errors");
        Collection<String> extraCollectionFileIDs = cache.getAllFileIDs(EXTRA_COLLECTION);
        Assert.assertNotNull(extraCollectionFileIDs);
        Assert.assertEquals(extraCollectionFileIDs.size(), 0);
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testIngestOfFileIDsData() throws Exception {
        addDescription("Tests the ingesting of file ids data");
        IntegrityDAO cache = createDAO();
        
        addStep("Create data", "Should be ingested into the database");
        FileIDsData data1 = getFileIDsData(TEST_FILE_ID);
        cache.updateFileIDs(data1, TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.updateFileIDs(data1, TEST_PILLAR_2, TEST_COLLECTIONID);
        
        addStep("Extract the data", "Should be identical to the ingested data");
        Collection<FileInfo> fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, TEST_COLLECTIONID);
        Assert.assertNotNull(fileinfos);
        for(FileInfo fi : fileinfos) {
            Assert.assertEquals(fi.getFileId(), TEST_FILE_ID);
            Assert.assertNull(fi.getChecksum());
            Assert.assertEquals(fi.getChecksumState(), ChecksumState.UNKNOWN);
            Assert.assertEquals(fi.getFileState(), FileState.EXISTING);
            Assert.assertEquals(fi.getDateForLastChecksumCheck(), CalendarUtils.getEpoch());
            Assert.assertEquals(fi.getDateForLastFileIDCheck(), data1.getFileIDsDataItems().getFileIDsDataItem().get(0).getLastModificationTime());
            Assert.assertEquals(fi.getFileSize(), new Long(data1.getFileIDsDataItems().getFileIDsDataItem().get(0).getFileSize().longValue()));
        }
        
        addStep("Check that the extra collection is untouched by the ingest", "should deliver an empty collection and no errors");
        Collection<String> extraCollectionFileIDs = cache.getAllFileIDs(EXTRA_COLLECTION);
        Assert.assertNotNull(extraCollectionFileIDs);
        Assert.assertEquals(extraCollectionFileIDs.size(), 0);
    }

    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testIngestOfChecksumsData() throws Exception {
        addDescription("Tests the ingesting of checksums data");
        IntegrityDAO cache = createDAO();
        
        addStep("Create data", "Should be ingested into the database");
        List<ChecksumDataForChecksumSpecTYPE> csData = getChecksumResults(TEST_FILE_ID, TEST_CHECKSUM);
        cache.updateChecksumData(csData, TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.updateChecksumData(csData, TEST_PILLAR_2, TEST_COLLECTIONID);
        
        addStep("Extract the data", "Should be identical to the ingested data");
        Collection<FileInfo> fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, TEST_COLLECTIONID);
        Assert.assertNotNull(fileinfos);
        for(FileInfo fi : fileinfos) {
            Assert.assertEquals(fi.getFileId(), TEST_FILE_ID);
            Assert.assertEquals(fi.getChecksum(), TEST_CHECKSUM);
            Assert.assertEquals(fi.getChecksumState(), ChecksumState.UNKNOWN);
            Assert.assertEquals(fi.getFileState(), FileState.EXISTING);
            Assert.assertEquals(fi.getDateForLastChecksumCheck(), csData.get(0).getCalculationTimestamp());
            Assert.assertEquals(fi.getDateForLastFileIDCheck(), CalendarUtils.getEpoch());
        }
        
        addStep("Check that the extra collection is untouched by the ingest", "should deliver an empty collection and no errors");
        Collection<String> extraCollectionFileIDs = cache.getAllFileIDs(EXTRA_COLLECTION);
        Assert.assertNotNull(extraCollectionFileIDs);
        Assert.assertEquals(extraCollectionFileIDs.size(), 0);
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testDeletingEntry() throws Exception {
        addDescription("Tests the deletion of an FileID entry from a collection. " +
        		"Checks that it does not effect another collection with a fileID equal to the deleted");
        IntegrityDAO cache = createDAO();
        Collection<FileInfo> fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, TEST_COLLECTIONID);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 0);

        addStep("Create data", "Should be ingested into the database");
        FileIDsData data1 = getFileIDsData(TEST_FILE_ID);
        cache.updateFileIDs(data1, TEST_PILLAR_1, TEST_COLLECTIONID);       
        cache.updateFileIDs(data1, TEST_PILLAR_2, TEST_COLLECTIONID);
        
        cache.updateFileIDs(data1, TEST_PILLAR_1, EXTRA_COLLECTION);
        cache.updateFileIDs(data1, EXTRA_PILLAR, EXTRA_COLLECTION);
        
        addStep("Ensure that the data is present", "the data is present");
        fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, TEST_COLLECTIONID);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 2);
        fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, EXTRA_COLLECTION);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 2);
        
        addStep("Delete the entry", "No fileinfos should be extracted from collection: " + TEST_COLLECTIONID + ".");
        cache.removeFileId(TEST_FILE_ID, TEST_COLLECTIONID);
        fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, TEST_COLLECTIONID);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 0);
        
        addStep("Check that the data in the extra collection is still present", "the data is present");
        fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, EXTRA_COLLECTION);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 2);
    }

    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testDeletingNonExistingEntry() throws Exception {
        addDescription("Tests the deletion of an nonexisting FileID entry.");
        IntegrityDAO cache = createDAO();
        
        String nonexistingFileEntry = "NON-EXISTING-FILE-ENTRY" + new Date().getTime();

        addStep("Create data", "Should be ingested into the database");
        FileIDsData data1 = getFileIDsData(TEST_FILE_ID);
        cache.updateFileIDs(data1, TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.updateFileIDs(data1, TEST_PILLAR_2, TEST_COLLECTIONID);
        
        Assert.assertEquals(cache.getAllFileIDs(TEST_COLLECTIONID).size(), 1);
        Collection<FileInfo> fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, TEST_COLLECTIONID);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 2);
        
        addStep("Delete a nonexisting entry", "Should not change the state of the database.");
        cache.removeFileId(nonexistingFileEntry, TEST_COLLECTIONID);
        Assert.assertEquals(cache.getAllFileIDs(TEST_COLLECTIONID).size(), 1);
        fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, TEST_COLLECTIONID);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 2);
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testSettingStateToMissing() throws Exception {
        addDescription("Tests the ability to set an file to missing at a given pillar.");
        IntegrityDAO cache = createDAO();

        addStep("Create data", "Should be ingested into the database");
        FileIDsData data1 = getFileIDsData(TEST_FILE_ID);
        cache.updateFileIDs(data1, TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.updateFileIDs(data1, TEST_PILLAR_2, TEST_COLLECTIONID);
        cache.updateFileIDs(data1, TEST_PILLAR_1, EXTRA_COLLECTION);
        cache.updateFileIDs(data1, EXTRA_PILLAR, EXTRA_COLLECTION);
        
        Collection<FileInfo> fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, TEST_COLLECTIONID);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 2);
        for(FileInfo fi : fileinfos) {
            Assert.assertEquals(fi.getFileState(), FileState.EXISTING);
        }
        
        fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, EXTRA_COLLECTION);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 2);
        for(FileInfo fi : fileinfos) {
            Assert.assertEquals(fi.getFileState(), FileState.EXISTING);
        }
        
        addStep("Set the file to missing", "Should change state.");
        cache.setFileMissing(TEST_FILE_ID, TEST_PILLAR_1, TEST_COLLECTIONID);
        fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, TEST_COLLECTIONID);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 2);
        for(FileInfo fi : fileinfos) {
            if(fi.getPillarId().equals(TEST_PILLAR_2)) {
                Assert.assertEquals(fi.getFileState(), FileState.EXISTING);
            } else {
                Assert.assertEquals(fi.getFileState(), FileState.MISSING);
            }
        }
        
        addStep("Check that the changes in the first collection does not effect the extra collection", "The collection is not effected");
        fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, EXTRA_COLLECTION);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 2);
        for(FileInfo fi : fileinfos) {
            Assert.assertEquals(fi.getFileState(), FileState.EXISTING);
        }
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testSettingChecksumStateToError() throws Exception {
        addDescription("Tests the ability to set the checksum state to error for a given pillar.");
        IntegrityDAO cache = createDAO();

        addStep("Create data", "Should be ingested into the database");
        List<ChecksumDataForChecksumSpecTYPE> csData = getChecksumResults(TEST_FILE_ID, TEST_CHECKSUM); 
        cache.updateChecksumData(csData, TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.updateChecksumData(csData, TEST_PILLAR_2, TEST_COLLECTIONID);
        cache.updateChecksumData(csData, TEST_PILLAR_1, EXTRA_COLLECTION);
        cache.updateChecksumData(csData, EXTRA_PILLAR, EXTRA_COLLECTION);
        
        Collection<FileInfo> fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, TEST_COLLECTIONID);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 2);
        for(FileInfo fi : fileinfos) {
            Assert.assertEquals(fi.getChecksumState(), ChecksumState.UNKNOWN);
        }
        
        fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, EXTRA_COLLECTION);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 2);
        for(FileInfo fi : fileinfos) {
            Assert.assertEquals(fi.getChecksumState(), ChecksumState.UNKNOWN);
        }
        
        addStep("Set the checksum to error on " + TEST_PILLAR_1 + " for collection " + TEST_COLLECTIONID, "Should change state.");
        cache.setChecksumError(TEST_FILE_ID, TEST_PILLAR_1, TEST_COLLECTIONID);
        fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, TEST_COLLECTIONID);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 2);
        for(FileInfo fi : fileinfos) {
            if(fi.getPillarId().equals(TEST_PILLAR_2)) {
                Assert.assertEquals(fi.getChecksumState(), ChecksumState.UNKNOWN);
            } else {
                Assert.assertEquals(fi.getChecksumState(), ChecksumState.ERROR);
            }
        }
        
        addStep("Check that the changes does not effect the extra collection", "The extra collection is unchanged.");
        fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, EXTRA_COLLECTION);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 2);
        for(FileInfo fi : fileinfos) {
            Assert.assertEquals(fi.getChecksumState(), ChecksumState.UNKNOWN);
        }
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testSettingChecksumStateToValid() throws Exception {
        addDescription("Tests the ability to set the checksum stat to valid for a given pillar.");
        IntegrityDAO cache = createDAO();

        addStep("Create data", "Should be ingested into the database");
        List<ChecksumDataForChecksumSpecTYPE> csData = getChecksumResults(TEST_FILE_ID, TEST_CHECKSUM); 
        cache.updateChecksumData(csData, TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.updateChecksumData(csData, TEST_PILLAR_2, TEST_COLLECTIONID);
        cache.updateChecksumData(csData, TEST_PILLAR_1, EXTRA_COLLECTION);
        cache.updateChecksumData(csData, EXTRA_PILLAR, EXTRA_COLLECTION);
        
        Collection<FileInfo> fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, TEST_COLLECTIONID);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 2);
        for(FileInfo fi : fileinfos) {
            Assert.assertEquals(fi.getChecksumState(), ChecksumState.UNKNOWN);
        }
        
        fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, EXTRA_COLLECTION);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 2);
        for(FileInfo fi : fileinfos) {
            Assert.assertEquals(fi.getChecksumState(), ChecksumState.UNKNOWN);
        }
        
        addStep("Set the file to missing", "Should change state.");
        cache.setChecksumValid(TEST_FILE_ID, TEST_PILLAR_1, TEST_COLLECTIONID);
        fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, TEST_COLLECTIONID);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 2);
        for(FileInfo fi : fileinfos) {
            if(fi.getPillarId().equals(TEST_PILLAR_2)) {
                Assert.assertEquals(fi.getChecksumState(), ChecksumState.UNKNOWN);
            } else {
                Assert.assertEquals(fi.getChecksumState(), ChecksumState.VALID);
            }
        }
        
        addStep("Check that the changes does not effect the extra collection", "The extra collection is unchanged");
        fileinfos = cache.getFileInfosForFile(TEST_FILE_ID, EXTRA_COLLECTION);
        Assert.assertNotNull(fileinfos);
        Assert.assertEquals(fileinfos.size(), 2);
        for(FileInfo fi : fileinfos) {
            Assert.assertEquals(fi.getChecksumState(), ChecksumState.UNKNOWN);
        }
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testInconsistentChecksum() throws Exception {
        addDescription("Testing the localization of inconsistent checksums");
        IntegrityDAO cache = createDAO();
        
        String checksum1_1 = "11";
        String checksum1_2 = "12";
        String checksum3 = "33";
        String checksum2_1 = "21";
        String checksum2_2 = "22";
        
        String BAD_FILE_ID_1 = "BAD-FILE-1";
        String BAD_FILE_ID_2 = "BAD-FILE-2";
        String GOOD_FILE_ID = "GOOD-FILE";

        addStep("Update the database with 2 inconsistent files and one consistent file.", 
                "Ingesting the data into the database");
        List<ChecksumDataForChecksumSpecTYPE> csData1_1 = getChecksumResults(BAD_FILE_ID_1, checksum1_1);
        List<ChecksumDataForChecksumSpecTYPE> csData1_2 = getChecksumResults(BAD_FILE_ID_2, checksum1_2);
        List<ChecksumDataForChecksumSpecTYPE> csData1_3 = getChecksumResults(GOOD_FILE_ID, checksum3);
        cache.updateChecksumData(csData1_1, TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.updateChecksumData(csData1_2, TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.updateChecksumData(csData1_3, TEST_PILLAR_1, TEST_COLLECTIONID);
        List<ChecksumDataForChecksumSpecTYPE> csData2_1 = getChecksumResults(BAD_FILE_ID_1, checksum2_1);
        List<ChecksumDataForChecksumSpecTYPE> csData2_2 = getChecksumResults(BAD_FILE_ID_2, checksum2_2);
        List<ChecksumDataForChecksumSpecTYPE> csData2_3 = getChecksumResults(GOOD_FILE_ID, checksum3);
        cache.updateChecksumData(csData2_1, TEST_PILLAR_2, TEST_COLLECTIONID);
        cache.updateChecksumData(csData2_2, TEST_PILLAR_2, TEST_COLLECTIONID);
        cache.updateChecksumData(csData2_3, TEST_PILLAR_2, TEST_COLLECTIONID);

        addStep("Find the files with inconsistent checksums", "Bad file 1 and 2");
        List<String> filesWithChecksumError = cache.findFilesWithInconsistentChecksums(new Date(), TEST_COLLECTIONID);
        Assert.assertEquals(filesWithChecksumError, Arrays.asList(BAD_FILE_ID_1, BAD_FILE_ID_2));
        
        addStep("Set the files with consistent checksums to valid.", 
                "Only the good file changes its state to valid.");
        cache.setFilesWithConsistentChecksumsToValid(TEST_COLLECTIONID);
        for(FileInfo fi : cache.getFileInfosForFile(BAD_FILE_ID_1, TEST_COLLECTIONID)) {
            Assert.assertEquals(fi.getChecksumState(), ChecksumState.UNKNOWN);
        }
        for(FileInfo fi : cache.getFileInfosForFile(BAD_FILE_ID_2, TEST_COLLECTIONID)) {
            Assert.assertEquals(fi.getChecksumState(), ChecksumState.UNKNOWN);
        }
        for(FileInfo fi : cache.getFileInfosForFile(GOOD_FILE_ID, TEST_COLLECTIONID)) {
            Assert.assertEquals(fi.getChecksumState(), ChecksumState.VALID);
        }
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testTooNewChecksum() throws Exception {
        addDescription("Testing that too new files are found with checksum error.");
        IntegrityDAO cache = createDAO();
        
        String checksum1_1 = "11";
        String checksum1_2 = "12";
        String checksum2_1 = "21";
        String checksum2_2 = "22";
        
        String BAD_FILE_ID_1 = "BAD-FILE-1";
        String BAD_FILE_ID_2 = "BAD-FILE-2";

        addStep("Update the database with 2 inconsistent files", "Ingesting the data into the database");
        List<ChecksumDataForChecksumSpecTYPE> csData1_1 = getChecksumResults(BAD_FILE_ID_1, checksum1_1);
        List<ChecksumDataForChecksumSpecTYPE> csData1_2 = getChecksumResults(BAD_FILE_ID_2, checksum1_2);
        cache.updateChecksumData(csData1_1, TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.updateChecksumData(csData1_2, TEST_PILLAR_1, TEST_COLLECTIONID);
        List<ChecksumDataForChecksumSpecTYPE> csData2_1 = getChecksumResults(BAD_FILE_ID_1, checksum2_1);
        List<ChecksumDataForChecksumSpecTYPE> csData2_2 = getChecksumResults(BAD_FILE_ID_2, checksum2_2);
        cache.updateChecksumData(csData2_1, TEST_PILLAR_2, TEST_COLLECTIONID);
        cache.updateChecksumData(csData2_2, TEST_PILLAR_2, TEST_COLLECTIONID);

        addStep("Find the files with inconsistent checksums, but with an older date than 'epoch'", 
                "No such files expected.");
        List<String> filesWithChecksumError = cache.findFilesWithInconsistentChecksums(new Date(0), TEST_COLLECTIONID);
        Assert.assertEquals(filesWithChecksumError, Arrays.asList());
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testNoChecksums() throws Exception {
        addDescription("Testing the checksum validation, when no checksums exists.");
        IntegrityDAO cache = createDAO();
        
        addStep("Update the database with 2 inconsistent files and one consistent file.", 
                "Ingesting the data into the database");
        FileIDsData data1 = getFileIDsData(TEST_FILE_ID);
        cache.updateFileIDs(data1, TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.updateFileIDs(data1, TEST_PILLAR_2, TEST_COLLECTIONID);

        addStep("Finding the files with inconsistent checksums", "No checksum thus no errors");
        List<String> filesWithChecksumError = cache.findFilesWithInconsistentChecksums(new Date(), TEST_COLLECTIONID);
        Assert.assertEquals(filesWithChecksumError, Arrays.asList());
    }

    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testMissingChecksum() throws Exception {
        addDescription("Testing the checksum validation, when only one pillar has a checksum for a file.");
        IntegrityDAO cache = createDAO();
        
        addStep("Update the database with 2 inconsistent files and one consistent file.", 
                "Ingesting the data into the database");
        cache.updateFileIDs(getFileIDsData(TEST_FILE_ID), TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.updateChecksumData(getChecksumResults(TEST_FILE_ID, TEST_CHECKSUM), TEST_PILLAR_2, TEST_COLLECTIONID);

        addStep("Finding the files with inconsistent checksums", "No checksum thus no errors");
        List<String> filesWithChecksumError = cache.findFilesWithInconsistentChecksums(new Date(), TEST_COLLECTIONID);      
        Assert.assertEquals(filesWithChecksumError, Arrays.asList());
    }

    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testSettingFileStateToUnknown() throws Exception {
        addDescription("Tests setting all the filestates to unknown.");
        IntegrityDAO cache = createDAO();
        
        addStep("Update the 2 file ids", "Ingesting the data into the database");
        cache.updateFileIDs(getFileIDsData(TEST_FILE_ID, TEST_FILE_ID+"1"), TEST_PILLAR_1, TEST_COLLECTIONID);
        Assert.assertEquals(cache.getNumberOfExistingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 2);
        Assert.assertEquals(cache.getNumberOfMissingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 0);

        cache.updateFileIDs(getFileIDsData(TEST_FILE_ID, TEST_FILE_ID+"1"), TEST_PILLAR_1, EXTRA_COLLECTION);
        Assert.assertEquals(cache.getNumberOfExistingFilesForAPillar(TEST_PILLAR_1, EXTRA_COLLECTION), 2);
        Assert.assertEquals(cache.getNumberOfMissingFilesForAPillar(TEST_PILLAR_1, EXTRA_COLLECTION), 0);
        
        addStep("Set the file state of all files to unknown.", "Neither any missing nor existing files for the pillar");
        cache.setAllFileStatesToUnknown(TEST_COLLECTIONID);
        Assert.assertEquals(cache.getNumberOfExistingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 0);
        Assert.assertEquals(cache.getNumberOfMissingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 0);
        
        addStep("Check that the changes in collection '" + TEST_COLLECTIONID + "' does not effect collection '" + EXTRA_COLLECTION +"'.", 
                "The collection is uneffected");
        Assert.assertEquals(cache.getNumberOfExistingFilesForAPillar(TEST_PILLAR_1, EXTRA_COLLECTION), 2);
        Assert.assertEquals(cache.getNumberOfMissingFilesForAPillar(TEST_PILLAR_1, EXTRA_COLLECTION), 0);
    }

    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testSettingUnknownFilesToMissing() throws Exception {
        addDescription("Tests setting the unknown files to missing.");
        IntegrityDAO cache = createDAO();
        
        addStep("Update the 2 file ids", "Ingesting the data into the database");
        cache.updateFileIDs(getFileIDsData(TEST_FILE_ID, TEST_FILE_ID+"1"), TEST_PILLAR_1, TEST_COLLECTIONID);
        Assert.assertEquals(cache.getNumberOfExistingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 2);
        Assert.assertEquals(cache.getNumberOfMissingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 0);
        cache.updateFileIDs(getFileIDsData(TEST_FILE_ID, TEST_FILE_ID+"1"), TEST_PILLAR_1, EXTRA_COLLECTION);
        Assert.assertEquals(cache.getNumberOfExistingFilesForAPillar(TEST_PILLAR_1, EXTRA_COLLECTION), 2);
        Assert.assertEquals(cache.getNumberOfMissingFilesForAPillar(TEST_PILLAR_1, EXTRA_COLLECTION), 0);        
        
        addStep("Set the file state of all files to unknown.", "Neither any missing nor existing files for the pillar");
        cache.setAllFileStatesToUnknown(TEST_COLLECTIONID);
        Assert.assertEquals(cache.getNumberOfExistingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 0);
        Assert.assertEquals(cache.getNumberOfMissingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 0);
        
        addStep("Ensure that the changes to one collection does not influence the other", "The extra collection is uneffected");
        Assert.assertEquals(cache.getNumberOfExistingFilesForAPillar(TEST_PILLAR_1, EXTRA_COLLECTION), 2);
        Assert.assertEquals(cache.getNumberOfMissingFilesForAPillar(TEST_PILLAR_1, EXTRA_COLLECTION), 0);
        
        addStep("Set the unknown files to missing.", "Both files is missing.");
        cache.setOldUnknownFilesToMissing(new Date(), TEST_COLLECTIONID);
        Assert.assertEquals(cache.getNumberOfExistingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 0);
        Assert.assertEquals(cache.getNumberOfMissingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 2);
        
        addStep("Ensure that the changes to one collection does not influence the other", "The extra collection is uneffected");
        Assert.assertEquals(cache.getNumberOfExistingFilesForAPillar(TEST_PILLAR_1, EXTRA_COLLECTION), 2);
        Assert.assertEquals(cache.getNumberOfMissingFilesForAPillar(TEST_PILLAR_1, EXTRA_COLLECTION), 0);
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testSettingNewUnknownFileToMissing() throws Exception {
        addDescription("Tests that only unknown files older than the time-stamp is set to missing.");
        IntegrityDAO cache = createDAO();
        String file2 = TEST_FILE_ID + "2";
        
        addStep("Update with two files, one at the time, and record a timestamp in between", 
                "Ingesting the data into the database");
        cache.updateFileIDs(getFileIDsData(TEST_FILE_ID), TEST_PILLAR_1, TEST_COLLECTIONID);

        Date betweenFiles = new Date();
        cache.updateFileIDs(getFileIDsData(file2), TEST_PILLAR_1, TEST_COLLECTIONID);
        Assert.assertEquals(cache.getNumberOfExistingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 2);
        Assert.assertEquals(cache.getNumberOfMissingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 0);
        
        addStep("Set the file state of all files to unknown.", "Neither any missing nor existing files for the pillar");
        cache.setAllFileStatesToUnknown(TEST_COLLECTIONID);
        Assert.assertEquals(cache.getNumberOfExistingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 0);
        Assert.assertEquals(cache.getNumberOfMissingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 0);        
        
        addStep("Set the unknown files older than the timestamp to missing.", 
                "Only the oldest file should be marked as missing.");
        cache.setOldUnknownFilesToMissing(betweenFiles, TEST_COLLECTIONID);
        Assert.assertEquals(cache.getNumberOfExistingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 0);
        Assert.assertEquals(cache.getNumberOfMissingFilesForAPillar(TEST_PILLAR_1, TEST_COLLECTIONID), 1);        
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testExtractingAllKnownFilesForPillars() throws Exception {
        addDescription("Tests that known files can be extracted for specific pillars.");
        IntegrityDAO cache = createDAO();
        String file2 = TEST_FILE_ID + "-2";
        String file3 = TEST_FILE_ID + "-3";
        
        addStep("Insert two files into database for a pillar", "Ingesting the data into the database");
        cache.updateFileIDs(getFileIDsData(TEST_FILE_ID, file2), TEST_PILLAR_1, TEST_COLLECTIONID);
        addStep("Insert a file to the extra collection for the common pillar", "Data is ingested into the database");
        cache.updateFileIDs(getFileIDsData(file3), TEST_PILLAR_1, EXTRA_COLLECTION);
        
        addStep("Extract all the existing file ids for the pillar for collection '" + TEST_COLLECTIONID + "'", "Both file ids is found.");
        Collection<String> fileIds = cache.getFilesOnPillar(TEST_PILLAR_1, 0, Long.MAX_VALUE, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.size() == 2, "Number of files: " + fileIds.size());
        Assert.assertTrue(fileIds.contains(TEST_FILE_ID));
        Assert.assertTrue(fileIds.contains(file2));
        Assert.assertFalse(fileIds.contains(file3));

        addStep("Extract the single fileID for the extra collection", "Only the one file id exists");
        fileIds = cache.getFilesOnPillar(TEST_PILLAR_1, 0, Long.MAX_VALUE, EXTRA_COLLECTION);
        Assert.assertTrue(fileIds.size() == 1, "Number of files: " + fileIds.size());
        Assert.assertTrue(fileIds.contains(file3));
        Assert.assertFalse(fileIds.contains(file2));
        Assert.assertFalse(fileIds.contains(TEST_FILE_ID));
                
        addStep("Extract all the existing file ids for another pillar", "No files are found.");
        fileIds = cache.getFilesOnPillar(TEST_PILLAR_2, 0, Long.MAX_VALUE, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.isEmpty());
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testExtractingAllKnownFilesForPillarsLimits() throws Exception {
        addDescription("Tests the limits for extracting files for specific pillars.");
        IntegrityDAO cache = createDAO();
        String file2 = TEST_FILE_ID + "-2";
        
        addStep("Insert two files into database for a pillar", "Ingesting the data into the database");
        cache.updateFileIDs(getFileIDsData(TEST_FILE_ID, file2), TEST_PILLAR_1, TEST_COLLECTIONID);
        
        addStep("Extract with a maximum of 0", "No files");
        Collection<String> fileIds = cache.getFilesOnPillar(TEST_PILLAR_1, 0, 0, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.isEmpty());

        addStep("Extract with a maximum of 1", "The first file.");
        fileIds = cache.getFilesOnPillar(TEST_PILLAR_1, 0, 1, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.size() == 1);
        Assert.assertTrue(fileIds.contains(TEST_FILE_ID));
        
        addStep("Extract with a minimum of 1 and maximum of infinite", "The last file.");
        fileIds = cache.getFilesOnPillar(TEST_PILLAR_1, 1, Long.MAX_VALUE, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.size() == 1);
        Assert.assertTrue(fileIds.contains(file2));

        addStep("Extract with a minimum of 1 and maximum of 0", "No files.");
        fileIds = cache.getFilesOnPillar(TEST_PILLAR_1, 1, 0, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.isEmpty());
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testExtractingAllKnownFilesForPillarsIgnoresMissingFiles() throws Exception {
        addDescription("Tests that only existing files are extracted for specific pillars.");
        IntegrityDAO cache = createDAO();
        String file2 = TEST_FILE_ID + "-2";
        
        addStep("Insert two files into database for a pillar, and mark one as missing", 
                "Ingesting the data into the database");
        cache.updateFileIDs(getFileIDsData(TEST_FILE_ID, file2), TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.setFileMissing(file2, TEST_PILLAR_1, TEST_COLLECTIONID);
        
        addStep("Extract all the existing file ids for the pillar", "Only one file id is found.");
        Collection<String> fileIds = cache.getFilesOnPillar(TEST_PILLAR_1, 0, Long.MAX_VALUE, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.size() == 1, "Number of files: " + fileIds.size());
        Assert.assertTrue(fileIds.contains(TEST_FILE_ID));
        Assert.assertFalse(fileIds.contains(file2));
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testExtractingAllMissingFilesForPillars() throws Exception {
        addDescription("Tests that missing files can be extracted for specific pillars.");
        IntegrityDAO cache = createDAO();
        String file2 = TEST_FILE_ID + "-2";
        String file3 = TEST_FILE_ID + "-3";
        
        addStep("Insert two files into database for a pillar and mark them as missing", 
                "Ingesting the data into the database");
        cache.updateFileIDs(getFileIDsData(TEST_FILE_ID, file2), TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.setFileMissing(TEST_FILE_ID, TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.setFileMissing(file2, TEST_PILLAR_1, TEST_COLLECTIONID);
        
        addStep("Add a missing file to the extra collection", "the data is ingested");
        cache.updateFileIDs(getFileIDsData(file3), TEST_PILLAR_1, EXTRA_COLLECTION);
        cache.setFileMissing(file3, TEST_PILLAR_1, EXTRA_COLLECTION);
        
        addStep("Extract all the missing file ids for the pillar", "Both file ids is found.");
        Collection<String> fileIds = cache.getMissingFilesOnPillar(TEST_PILLAR_1, 0, Long.MAX_VALUE, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.size() == 2, "Number of files: " + fileIds.size());
        Assert.assertTrue(fileIds.contains(TEST_FILE_ID));
        Assert.assertTrue(fileIds.contains(file2));
        Assert.assertFalse(fileIds.contains(file3));
        
        addStep("Ensure extract the one missing file from the extra collection", "The file exists");
        fileIds = cache.getMissingFilesOnPillar(TEST_PILLAR_1, 0, Long.MAX_VALUE, EXTRA_COLLECTION);
        Assert.assertTrue(fileIds.size() == 1, "Number of files: " + fileIds.size());
        Assert.assertFalse(fileIds.contains(TEST_FILE_ID));
        Assert.assertFalse(fileIds.contains(file2));
        Assert.assertTrue(fileIds.contains(file3));

        addStep("Extract all the missing file ids for another pillar", "No files are found.");
        fileIds = cache.getMissingFilesOnPillar(TEST_PILLAR_2, 0, Long.MAX_VALUE, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.isEmpty());
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testExtractingAllMissingFilesForPillarsLimits() throws Exception {
        addDescription("Tests the limits for extracting missing files for specific pillars.");
        IntegrityDAO cache = createDAO();
        String file2 = TEST_FILE_ID + "-2";
        
        addStep("Insert two files into database for a pillar and set them to missing", 
                "Ingesting the data into the database");
        cache.updateFileIDs(getFileIDsData(TEST_FILE_ID, file2), TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.setFileMissing(TEST_FILE_ID, TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.setFileMissing(file2, TEST_PILLAR_1, TEST_COLLECTIONID);
        
        addStep("Extract with a maximum of 0", "No files");
        Collection<String> fileIds = cache.getMissingFilesOnPillar(TEST_PILLAR_1, 0, 0, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.isEmpty());

        addStep("Extract with a maximum of 1", "The first file.");
        fileIds = cache.getMissingFilesOnPillar(TEST_PILLAR_1, 0, 1, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.size() == 1);
        Assert.assertTrue(fileIds.contains(TEST_FILE_ID));
        
        addStep("Extract with a minimum of 1 and maximum of infinite", "The last file.");
        fileIds = cache.getMissingFilesOnPillar(TEST_PILLAR_1, 1, Long.MAX_VALUE, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.size() == 1);
        Assert.assertTrue(fileIds.contains(file2));

        addStep("Extract with a minimum of 1 and maximum of 0", "No files.");
        fileIds = cache.getMissingFilesOnPillar(TEST_PILLAR_1, 1, 0, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.isEmpty());
    }

    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testExtractingMissingFilesForPillarsIgnoresExistingFiles() throws Exception {
        addDescription("Tests that only missing files are extracted for specific pillars.");
        IntegrityDAO cache = createDAO();
        String file2 = TEST_FILE_ID + "-2";
        
        addStep("Insert two files into database for a pillar, and mark one as missing", 
                "Ingesting the data into the database");
        cache.updateFileIDs(getFileIDsData(TEST_FILE_ID, file2), TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.setFileMissing(file2, TEST_PILLAR_1, TEST_COLLECTIONID);
        
        addStep("Extract all the missing file ids for the pillar", "Only one file id is found.");
        Collection<String> fileIds = cache.getMissingFilesOnPillar(TEST_PILLAR_1, 0, Long.MAX_VALUE, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.size() == 1, "Number of files: " + fileIds.size());
        Assert.assertFalse(fileIds.contains(TEST_FILE_ID));
        Assert.assertTrue(fileIds.contains(file2));
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testExtractingFilesWithChecksumErrorForPillars() throws Exception {
        addDescription("Tests that files with checksum error can be extracted for specific pillars.");
        IntegrityDAO cache = createDAO();
        String file2 = TEST_FILE_ID + "-2";
        String file3 = TEST_FILE_ID + "-3";
        
        addStep("Insert two files into database for a pillar and mark them as having checksum error", 
                "Ingesting the data into the database");
        cache.updateFileIDs(getFileIDsData(TEST_FILE_ID, file2), TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.setChecksumError(TEST_FILE_ID, TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.setChecksumError(file2, TEST_PILLAR_1, TEST_COLLECTIONID);
        
        addStep("Insert one file for the extra collection into the database, and mark is as having a checksumerror", 
                "The data is ingested");
        cache.updateFileIDs(getFileIDsData(file3), TEST_PILLAR_1, EXTRA_COLLECTION);
        cache.setChecksumError(file3, TEST_PILLAR_1, EXTRA_COLLECTION);
                
        addStep("Extract all the files with checksum error for the pillar", "Both file ids is found.");
        Collection<String> fileIds = cache.getFilesWithChecksumErrorsOnPillar(TEST_PILLAR_1, 0, Long.MAX_VALUE, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.size() == 2, "Number of files: " + fileIds.size());
        Assert.assertTrue(fileIds.contains(TEST_FILE_ID));
        Assert.assertTrue(fileIds.contains(file2));
        Assert.assertFalse(fileIds.contains(file3));

        addStep("Extrat all files with checksum errors for the pillar in the extra collection", "Only one file is found");
        fileIds = cache.getFilesWithChecksumErrorsOnPillar(TEST_PILLAR_1, 0, Long.MAX_VALUE, EXTRA_COLLECTION);
        Assert.assertTrue(fileIds.size() == 1, "Number of files: " + fileIds.size());
        Assert.assertFalse(fileIds.contains(TEST_FILE_ID));
        Assert.assertFalse(fileIds.contains(file2));
        Assert.assertTrue(fileIds.contains(file3));
        
        addStep("Extract all the files with checksum error for another pillar", "No files are found.");
        fileIds = cache.getFilesWithChecksumErrorsOnPillar(TEST_PILLAR_2, 0, Long.MAX_VALUE, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.isEmpty());
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testExtractingFilesWithChecksumErrorForPillarsLimits() throws Exception {
        addDescription("Tests the limits for extracting files with checksum error for specific pillars.");
        IntegrityDAO cache = createDAO();
        String file2 = TEST_FILE_ID + "-2";
        
        addStep("Insert two files into database for a pillar and mark them as having checksum error", 
                "Ingesting the data into the database");
        cache.updateFileIDs(getFileIDsData(TEST_FILE_ID, file2), TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.setChecksumError(TEST_FILE_ID, TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.setChecksumError(file2, TEST_PILLAR_1, TEST_COLLECTIONID);
        
        addStep("Extract with a maximum of 0", "No files");
        Collection<String> fileIds = cache.getFilesWithChecksumErrorsOnPillar(TEST_PILLAR_1, 0, 0, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.isEmpty());

        addStep("Extract with a maximum of 1", "The first file.");
        fileIds = cache.getFilesWithChecksumErrorsOnPillar(TEST_PILLAR_1, 0, 1, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.size() == 1);
        Assert.assertTrue(fileIds.contains(TEST_FILE_ID));
        
        addStep("Extract with a minimum of 1 and maximum of infinite", "The last file.");
        fileIds = cache.getFilesWithChecksumErrorsOnPillar(TEST_PILLAR_1, 1, Long.MAX_VALUE, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.size() == 1);
        Assert.assertTrue(fileIds.contains(file2));

        addStep("Extract with a minimum of 1 and maximum of 0", "No files.");
        fileIds = cache.getFilesWithChecksumErrorsOnPillar(TEST_PILLAR_1, 1, 0, TEST_COLLECTIONID);
        Assert.assertTrue(fileIds.isEmpty());
    }
    
    @Test(groups = {"regressiontest", "databasetest", "integritytest"})
    public void testExtractCollectionFileSize() throws Exception {
        addDescription("Tests that the accumulated size of the collection can be extracted");
        IntegrityDAO cache = createDAO();
        
        addStep("Insert test data into database", "Data is ingested");
        String file2 = TEST_FILE_ID + "-2";
        String file3 = TEST_FILE_ID + "-3";
        Long size1 = new Long(100);
        Long size2 = new Long(200);
        Long size3 = new Long(300);
        FileIDsData data1 = makeFileIDsDataWithGivenFileSize(TEST_FILE_ID, size1);
        FileIDsData data2 = makeFileIDsDataWithGivenFileSize(file2, size2);
        FileIDsData data3 = makeFileIDsDataWithGivenFileSize(file3, size3);
        cache.updateFileIDs(data1, TEST_PILLAR_1, TEST_COLLECTIONID);
        cache.updateFileIDs(data2, TEST_PILLAR_1, TEST_COLLECTIONID);      
        cache.updateFileIDs(data2, TEST_PILLAR_2, TEST_COLLECTIONID);
        cache.updateFileIDs(data3, TEST_PILLAR_2, TEST_COLLECTIONID);
        
        addStep("Check that the data has been properly ingested into the database", "The data has been ingested");
        List<String> pillar1Files = cache.getFilesOnPillar(TEST_PILLAR_1, 0, Long.MAX_VALUE, TEST_COLLECTIONID);
        Assert.assertEquals(pillar1Files.size(), 2);
        Assert.assertTrue(pillar1Files.contains(TEST_FILE_ID));
        Assert.assertTrue(pillar1Files.contains(file2));
        Assert.assertFalse(pillar1Files.contains(file3));
        
        List<String> pillar2Files = cache.getFilesOnPillar(TEST_PILLAR_2, 0, Long.MAX_VALUE, TEST_COLLECTIONID);
        Assert.assertEquals(pillar2Files.size(), 2);
        Assert.assertFalse(pillar2Files.contains(TEST_FILE_ID));
        Assert.assertTrue(pillar2Files.contains(file2));
        Assert.assertTrue(pillar2Files.contains(file3));
        
        List<String> collectionFiles = cache.getAllFileIDs(TEST_COLLECTIONID);
        Assert.assertEquals(collectionFiles.size(), 3);
        Assert.assertTrue(collectionFiles.contains(TEST_FILE_ID));
        Assert.assertTrue(collectionFiles.contains(file2));
        Assert.assertTrue(collectionFiles.contains(file3));
        
        Long pillar1Size = size1 + size2;
        Long pillar2Size = size2 + size3;
        Long collectionSize = size1 + size2 + size3;
        
        addStep("Check the reported size of the first pillar in the collection", "The reported size matches the precalculated");
        Assert.assertEquals(cache.getCollectionFileSizeAtPillar(TEST_COLLECTIONID, TEST_PILLAR_1), pillar1Size);
        addStep("Check the reported size of the second pillar in the collection", "The reported size matches the precalculated");
        Assert.assertEquals(cache.getCollectionFileSizeAtPillar(TEST_COLLECTIONID, TEST_PILLAR_2), pillar2Size);
        addStep("Check the reported size of the whole collection", "The reported size matches the precalculated");
        Assert.assertEquals(cache.getCollectionFileSize(TEST_COLLECTIONID), collectionSize);
        
        
    }
    
    private FileIDsData makeFileIDsDataWithGivenFileSize(String fileID, Long size) {
        FileIDsData res = new FileIDsData();
        FileIDsDataItems items = new FileIDsDataItems();
        FileIDsDataItem dataItem = new FileIDsDataItem();
        dataItem.setFileID(fileID);
        dataItem.setFileSize(BigInteger.valueOf(size));
        dataItem.setLastModificationTime(CalendarUtils.getNow());
        items.getFileIDsDataItem().add(dataItem);
        res.setFileIDsDataItems(items);
        return res;
    }
    
    private List<ChecksumDataForChecksumSpecTYPE> getChecksumResults(String fileId, String checksum) {
        List<ChecksumDataForChecksumSpecTYPE> res = new ArrayList<ChecksumDataForChecksumSpecTYPE>();
        
        ChecksumDataForChecksumSpecTYPE csData = new ChecksumDataForChecksumSpecTYPE();
        csData.setChecksumValue(Base16Utils.encodeBase16(checksum));
        csData.setCalculationTimestamp(CalendarUtils.getNow());
        csData.setFileID(fileId);
        res.add(csData);
        return res;
    }
    
    private FileIDsData getFileIDsData(String... fileIds) {
        FileIDsData res = new FileIDsData();
        FileIDsDataItems items = new FileIDsDataItems();
        
        for(String fileId : fileIds) {
            FileIDsDataItem dataItem = new FileIDsDataItem();
            dataItem.setFileID(fileId);
            dataItem.setFileSize(BigInteger.valueOf(items.getFileIDsDataItem().size() + 1));
            dataItem.setLastModificationTime(CalendarUtils.getNow());
            items.getFileIDsDataItem().add(dataItem);
        } 
        
        res.setFileIDsDataItems(items);
        return res;
    }
    
    private IntegrityDAO createDAO() {
        return new IntegrityDAO(new DBConnector(
                settings.getReferenceSettings().getIntegrityServiceSettings().getIntegrityDatabase()),
                settings.getRepositorySettings().getCollections());
    }
}
