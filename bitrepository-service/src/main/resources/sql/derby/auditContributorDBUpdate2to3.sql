---
-- #%L
-- Bitrepository Audit Trail Service
-- %%
-- Copyright (C) 2010 - 2012 The State and University Library, The Royal Library and The State Archives, Denmark
-- %%
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Lesser General Public License as 
-- published by the Free Software Foundation, either version 2.1 of the 
-- License, or (at your option) any later version.
-- 
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Lesser Public License for more details.
-- 
-- You should have received a copy of the GNU General Lesser Public 
-- License along with this program.  If not, see
-- <http://www.gnu.org/licenses/lgpl-2.1.html>.
-- #L%
---

connect 'jdbc:derby:auditcontributerdb';

-- Change the 'information' column from varchar(255) into CLOB
ALTER TABLE audittrail ADD COLUMN NEW_information CLOB;
UPDATE audittrail SET NEW_information=information;
ALTER TABLE audittrail DROP COLUMN information;
RENAME COLUMN audittrail.NEW_information TO information;

-- Change the 'audit' column from varchar(255) into CLOB
ALTER TABLE audittrail ADD COLUMN NEW_audit CLOB;
UPDATE audittrail SET NEW_audit = audit;
ALTER TABLE audittrail DROP COLUMN audit;
RENAME COLUMN audittrail.NEW_audit TO audit;

-- Set the table versions
insert into tableversions ( tablename, version ) values ( 'auditcontributordb', 3);
update tableversions set version = 3 where tablename = 'audit';
