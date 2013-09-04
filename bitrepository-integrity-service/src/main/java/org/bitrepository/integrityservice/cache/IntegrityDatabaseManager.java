package org.bitrepository.integrityservice.cache;

import org.bitrepository.service.database.DatabaseManager;
import org.bitrepository.service.database.DatabaseMigrator;
import org.bitrepository.settings.referencesettings.DatabaseSpecifics;

public class IntegrityDatabaseManager extends DatabaseManager {

    private static final String INTEGRITY_SERVICE_DATABASE_SCHEMA = "sql/derby/integrityDBCreation.sql";
    private final DatabaseSpecifics databaseSpecifics;
        
    public IntegrityDatabaseManager(DatabaseSpecifics databaseSpecifics) {
        this.databaseSpecifics = databaseSpecifics;
    }
    
    @Override
    protected DatabaseSpecifics getDatabaseSpecifics() {
        return databaseSpecifics;
    }

    @Override
    protected DatabaseMigrator getMigrator() {
        return null;
    }

    @Override
    protected boolean needsMigration() {
        return false;
    }

    @Override
    protected String getDatabaseCreationScript() {
        return INTEGRITY_SERVICE_DATABASE_SCHEMA;
    }

}
