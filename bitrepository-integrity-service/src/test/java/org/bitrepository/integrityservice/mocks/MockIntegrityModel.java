/*
 * #%L
 * Bitrepository Integrity Service
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
package org.bitrepository.integrityservice.mocks;

import java.util.Collection;
import java.util.List;

import org.bitrepository.bitrepositoryelements.ChecksumDataForChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.FileIDsData;
import org.bitrepository.integrityservice.cache.FileInfo;
import org.bitrepository.integrityservice.cache.IntegrityModel;

/**
 * Wrapper of an integrity model and count how many times each method it used.
 */
public class MockIntegrityModel implements IntegrityModel {
    
    private final IntegrityModel integrityModel;
    
    /**
     * Constructor.
     */
    public MockIntegrityModel(IntegrityModel integrityModel) {
        this.integrityModel = integrityModel;
    }

    private int callsForAddfileIDs = 0;
    @Override
    public void addFileIDs(FileIDsData data, String pillarId) {
        callsForAddfileIDs++;
        integrityModel.addFileIDs(data, pillarId);
    }
    public int getCallsForAddFileIDs() {
        return callsForAddfileIDs;        
    }

    private int callsForAddChecksums = 0;
    @Override
    public void addChecksums(List<ChecksumDataForChecksumSpecTYPE> data, ChecksumSpecTYPE checksumType, String pillarId) {
        callsForAddChecksums++;
        integrityModel.addChecksums(data, checksumType, pillarId);
    }
    public int getCallsForAddChecksums() {
        return callsForAddChecksums;
    }

    private int callsForGetFileInfos = 0;
    @Override
    public Collection<FileInfo> getFileInfos(String fileId) {
        callsForGetFileInfos++;
        return integrityModel.getFileInfos(fileId);
    }
    public int getCallsForGetFileInfos() {
        return callsForGetFileInfos;
    }

    private int callsForGetAllFileIDs = 0;
    @Override
    public Collection<String> getAllFileIDs() {
        callsForGetAllFileIDs++;
        return integrityModel.getAllFileIDs();
    }
    public int getCallsForGetAllFileIDs() {
        return callsForGetAllFileIDs;
    }

    private int callsForGetNumberOfFiles = 0;
    @Override
    public long getNumberOfFiles(String pillarId) {
        callsForGetNumberOfFiles++;
        return integrityModel.getNumberOfFiles(pillarId);
    }
    public int getCallsForGetNumberOfFiles() {
        return callsForGetNumberOfFiles;
    }

    private int callsForGetNumberOfMissingFiles = 0;
    @Override
    public long getNumberOfMissingFiles(String pillarId) {
        callsForGetNumberOfMissingFiles++;
        return integrityModel.getNumberOfMissingFiles(pillarId);
    }
    public int getCallsForGetNumberOfMissingFiles() {
        return callsForGetNumberOfMissingFiles;
    }

    private int callsForGetNumberOfChecksumErrors = 0;
    @Override
    public long getNumberOfChecksumErrors(String pillarId) {
        callsForGetNumberOfChecksumErrors++;
        return integrityModel.getNumberOfChecksumErrors(pillarId);
    }
    public int getCallsForGetNumberOfChecksumErrors() {
        return callsForGetNumberOfChecksumErrors;
    }

    private int callsForSetFileMissing = 0;
    @Override
    public void setFileMissing(String fileId, Collection<String> pillarIds) {
        callsForSetFileMissing++;
        integrityModel.setFileMissing(fileId, pillarIds);
    }
    public int getCallsForSetFileMissing() {
        return callsForSetFileMissing;
    }

    private int callsForSetChecksumError = 0;
    @Override
    public void setChecksumError(String fileId, Collection<String> pillarIds) {
        callsForSetChecksumError++;
        integrityModel.setChecksumError(fileId, pillarIds);
    }
    public int getCallsForSetChecksumError() {
        return callsForSetChecksumError;
    }

    private int callsForSetChecksumAgreement = 0;
    @Override
    public void setChecksumAgreement(String fileId, Collection<String> pillarIds) {
        callsForSetChecksumAgreement++;
        integrityModel.setChecksumAgreement(fileId, pillarIds);
    }
    public int getCallsForSetChecksumAgreement() {
        return callsForSetChecksumAgreement;
    }
}