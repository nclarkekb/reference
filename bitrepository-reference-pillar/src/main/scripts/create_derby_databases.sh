#!/bin/bash

###
# #%L
# Bitrepository Reference Pillar
# %%
# Copyright (C) 2010 - 2012 The State and University Library, The Royal Library and The State Archives, Denmark
# %%
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as
# published by the Free Software Foundation, either version 2.1 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Lesser Public License for more details.
#
# You should have received a copy of the GNU General Lesser Public
# License along with this program.  If not, see
# <http://www.gnu.org/licenses/lgpl-2.1.html>.
# #L%
###

CONFDIR="conf"
LOGBACK="-Dlogback.configurationFile=$CONFDIR/logback.xml" #configuration directory
JAVA="$JAVA_HOME/bin/java"
CLASSPATH="-classpath ./:lib/*"
CHECKSUM_DB_SCRIPT="sql/derby/checksumDBCreation.sql";
AUDIT_CONTRIBUTOR_DB_SCRIPT="sql/derby/auditContributorDBCreation.sql";

cd $(dirname $(perl -e "use Cwd 'abs_path';print abs_path('$0');"))/..
echo "Running pillar database creation from $PWD dir"
#Check availability of crucial system components
[ -x "$JAVA" ] || exit

$JAVA $CLASSPATH org.bitrepository.pillar.common.ChecksumDatabaseCreator $CONFDIR $CHECKSUM_DB_SCRIPT </dev/null
>checksum_database_creation.out 2>&1 &
$JAVA $CLASSPATH org.bitrepository.pillar.common.PillarAuditTrailDatabaseCreator $CONFDIR $AUDIT_CONTRIBUTOR_DB_SCRIPT </dev/null>auditcontributor_database_creation.out 2>&1 &
