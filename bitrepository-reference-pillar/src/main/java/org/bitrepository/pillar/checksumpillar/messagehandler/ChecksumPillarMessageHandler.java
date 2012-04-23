/*
 * #%L
 * bitrepository-reference-pillar
 * *
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2010 - 2011 The State and University Library, The Royal Library and The State Archives, Denmark
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
package org.bitrepository.pillar.checksumpillar.messagehandler;

import org.bitrepository.bitrepositoryelements.ChecksumSpecTYPE;
import org.bitrepository.bitrepositoryelements.ChecksumType;
import org.bitrepository.bitrepositoryelements.ResponseCode;
import org.bitrepository.bitrepositoryelements.ResponseInfo;
import org.bitrepository.common.ArgumentValidator;
import org.bitrepository.pillar.checksumpillar.cache.ChecksumStore;
import org.bitrepository.pillar.common.PillarContext;
import org.bitrepository.pillar.common.PillarMessageHandler;
import org.bitrepository.pillar.exceptions.InvalidMessageException;
import org.bitrepository.protocol.utils.Base16Utils;

/**
 * Abstract level for message handling. 
 */
public abstract class ChecksumPillarMessageHandler<T> extends PillarMessageHandler<T> {
    /** The reference checksum cache.*/
    private final ChecksumStore cache;
    /** The specifications for the checksum type of this ChecksumPillar. */
    private ChecksumSpecTYPE checksumType;
    
    /**
     * Constructor. 
     * @param context The context of the message handler.
     * @param refCache The cache for the checksum data.
     */
    protected ChecksumPillarMessageHandler(PillarContext context, ChecksumStore refCache) {
        super(context);
        ArgumentValidator.checkNotNull(refCache, "ChecksumCache refCache");

        this.cache = refCache;
        this.checksumType = new ChecksumSpecTYPE();
        checksumType.setChecksumType(ChecksumType.fromValue(
                getSettings().getReferenceSettings().getPillarSettings().getChecksumPillarChecksumSpecificationType()));
        String salt = getSettings().getReferenceSettings().getPillarSettings().getChecksumPillarChecksumSpecificationSalt();
        if(salt != null) {
            checksumType.setChecksumSalt(Base16Utils.encodeBase16(salt));
        }
    }
    /**
     * @return The checksumType for this message handler.
     */
    protected ChecksumSpecTYPE getChecksumType() {
        return checksumType;
    }

    /**
     * @return The cache for this message handler.
     */
    protected ChecksumStore getCache() {
        return cache;
    }
    
    /**
     * Validates the checksum specification.
     * A null as checksum argument is ignored.
     * @param csSpec The checksum specification to validate. 
     */
    protected void validateChecksumSpec(ChecksumSpecTYPE csSpec) {
        if(csSpec == null) {
            return;
        }
        
        if(!(checksumType.equals(csSpec))) {
            ResponseInfo ri = new ResponseInfo();
            ri.setResponseCode(ResponseCode.REQUEST_NOT_UNDERSTOOD_FAILURE);
            ri.setResponseText("Cannot handle the checksum specification '" + csSpec + "'."
                    + "This checksum pillar can only handle '" + checksumType + "'");
            throw new InvalidMessageException(ri);
        }        
    }
}