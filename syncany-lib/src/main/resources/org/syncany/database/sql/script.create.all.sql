-- Tables

CREATE CACHED TABLE IF NOT EXISTS databaseversion (
  id int NOT NULL IDENTITY,
  status varchar(45) NOT NULL,
  localtime datetime NOT NULL,
  client varchar(45) NOT NULL,
  vectorclock_serialized varchar(1024) NOT NULL,
  UNIQUE (vectorclock_serialized)
);

CREATE CACHED TABLE IF NOT EXISTS  chunk (
  checksum varchar(40) NOT NULL,
  databaseversion_id int NOT NULL,
  size bigint NOT NULL,
  PRIMARY KEY (checksum),
  FOREIGN KEY (databaseversion_id) REFERENCES databaseversion (id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE CACHED TABLE IF NOT EXISTS  databaseversion_vectorclock (
  databaseversion_id int NOT NULL,
  client varchar(45) NOT NULL,
  logicaltime int NOT NULL,
  PRIMARY KEY (databaseversion_id, client),
  FOREIGN KEY (databaseversion_id) REFERENCES databaseversion (id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE CACHED TABLE IF NOT EXISTS  filecontent (
  checksum varchar(40) NOT NULL,
  databaseversion_id int NOT NULL,
  size bigint NOT NULL,
  PRIMARY KEY (checksum),
  FOREIGN KEY (databaseversion_id) REFERENCES databaseversion (id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE CACHED TABLE IF NOT EXISTS  filecontent_chunk (
  filecontent_checksum varchar(40) NOT NULL,
  chunk_checksum varchar(40) NOT NULL,
  num int NOT NULL,
  PRIMARY KEY (filecontent_checksum, chunk_checksum, num),
  FOREIGN KEY (filecontent_checksum) REFERENCES filecontent (checksum) ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY (chunk_checksum) REFERENCES chunk (checksum) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE CACHED TABLE IF NOT EXISTS  filehistory (
  id varchar(40) NOT NULL,
  databaseversion_id int NOT NULL,
  PRIMARY KEY (id, databaseversion_id),
  FOREIGN KEY (databaseversion_id) REFERENCES databaseversion (id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE CACHED TABLE IF NOT EXISTS  fileversion (
  filehistory_id varchar(40) NOT NULL,
  version int NOT NULL,
  databaseversion_id int NOT NULL,
  path varchar(1024) NOT NULL,
  type varchar(45) NOT NULL,
  status varchar(45) NOT NULL,
  size bigint NOT NULL,
  lastmodified datetime NOT NULL,
  linktarget varchar(1024),
  filecontent_checksum varchar(40) DEFAULT NULL,
  updated datetime NOT NULL,
  posixperms varchar(45) DEFAULT NULL,
  dosattrs varchar(45) DEFAULT NULL,
  PRIMARY KEY (filehistory_id, version, databaseversion_id),
  FOREIGN KEY (filehistory_id, databaseversion_id) REFERENCES filehistory (id, databaseversion_id) ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY (filecontent_checksum) REFERENCES filecontent (checksum) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE CACHED TABLE IF NOT EXISTS  multichunk (
  id varchar(40) NOT NULL,
  databaseversion_id int NOT NULL,
  size bigint NOT NULL,  
  PRIMARY KEY (id),
  FOREIGN KEY (databaseversion_id) REFERENCES databaseversion (id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE CACHED TABLE IF NOT EXISTS  multichunk_chunk (
  multichunk_id varchar(40) NOT NULL,
  chunk_checksum varchar(40) NOT NULL,
  PRIMARY KEY (multichunk_id, chunk_checksum),
  FOREIGN KEY (multichunk_id) REFERENCES multichunk (id) ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY (chunk_checksum) REFERENCES chunk (checksum) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE CACHED TABLE IF NOT EXISTS  multichunk_muddy (
  id varchar(40) NOT NULL,
  machine_name varchar(255) NOT NULL,
  machine_version int NOT NULL,
  PRIMARY KEY (id)
);

CREATE CACHED TABLE IF NOT EXISTS  known_databases (
  id int NOT NULL IDENTITY,
  client varchar(45) NOT NULL,
  filenumber int NOT NULL,
  UNIQUE (client, filenumber)
);

CREATE CACHED TABLE IF NOT EXISTS  general_settings (
  key varchar(255) NOT NULL,
  value varchar(255) NOT NULL,
  PRIMARY KEY (key)
);

-- Non-primary indices                              

CREATE INDEX idx_databaseversion_status ON databaseversion (status);
CREATE INDEX idx_databaseversion_vectorclock_serialized ON databaseversion (vectorclock_serialized);
CREATE INDEX idx_fileversion_path ON fileversion (path);
CREATE INDEX idx_fileversion_status ON fileversion (status);
CREATE INDEX idx_fileversion_filecontent_checksum ON fileversion (filecontent_checksum);


-- Views

CREATE VIEW databaseversion_master AS
  SELECT dbv.*, vc.logicaltime as client_version
  FROM databaseversion dbv
  JOIN databaseversion_vectorclock vc on dbv.id=vc.databaseversion_id and dbv.client=vc.client
  WHERE dbv.status='MASTER';

CREATE VIEW fileversion_master AS
  SELECT fv0.* 
  FROM fileversion fv0
  JOIN databaseversion dbv 
    ON fv0.databaseversion_id=dbv.id 
       AND dbv.status='MASTER';   
       
CREATE VIEW fileversion_master_maxversion AS
  SELECT DISTINCT filehistory_id, MAX(version) version
  FROM fileversion_master
  GROUP BY filehistory_id;     
  
CREATE VIEW fileversion_master_last AS
  SELECT fv.* 
  FROM fileversion_master_maxversion fvmax
  JOIN fileversion_master fv 
    ON fvmax.filehistory_id=fv.filehistory_id 
       AND fvmax.version=fv.version 
  WHERE fv.status<>'DELETED';    
  
  
-- Full Views   

create view filehistory_full as
	select 
		dbv.status as databaseversion_status, 
		dbv.localtime as databaseversion_localtime, 
		dbv.client as databaseversion_client, 	
		dbv.vectorclock_serialized as databaseversion_vectorclock_serialized, 	
		fh.*
	from databaseversion dbv
	join filehistory fh on dbv.id=fh.databaseversion_id;
	
create view fileversion_full as
	select 		
		fhf.databaseversion_status, 
		fhf.databaseversion_localtime, 
		fhf.databaseversion_client, 	
		fhf.databaseversion_vectorclock_serialized, 	
		fv.*
	from filehistory_full fhf
	join fileversion fv on fhf.id=fv.filehistory_id and fhf.databaseversion_id=fv.databaseversion_id;	
	

-- Functions

--!DELIMITER=end;

create function substr_count(haystack varchar(255), needle varchar(255))
returns integer
begin atomic
	declare strCount integer;
	declare lastIndex integer;

	set strCount = 0;
	set lastIndex = 1;

	while lastIndex <> 0 do
		set lastIndex = locate(needle, haystack, lastIndex);

		if lastIndex <> 0 then
			set strCount = strCount + 1;
			set lastIndex = lastIndex + length(needle);
		end if;
	end while;

	return strCount;
end;
