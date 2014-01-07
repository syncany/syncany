-- Tables

CREATE CACHED TABLE chunk (
  checksum varchar(40) NOT NULL,
  size bigint NOT NULL,
  PRIMARY KEY (checksum)
);

CREATE CACHED TABLE databaseversion (
  id int NOT NULL IDENTITY,
  status varchar(45) NOT NULL,
  localtime datetime NOT NULL,
  client varchar(45) NOT NULL,
  vectorclock_serialized varchar(1024) NOT NULL,
  UNIQUE (vectorclock_serialized)
);

CREATE CACHED TABLE databaseversion_vectorclock (
  databaseversion_id int NOT NULL,
  client varchar(45) NOT NULL,
  logicaltime int NOT NULL,
  PRIMARY KEY (databaseversion_id, client),
  FOREIGN KEY (databaseversion_id) REFERENCES databaseversion (id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE CACHED TABLE filecontent (
  checksum varchar(40) NOT NULL,
  size bigint NOT NULL,
  CONSTRAINT pk_checksum PRIMARY KEY (checksum)
);

CREATE CACHED TABLE filecontent_chunk (
  filecontent_checksum varchar(40) NOT NULL,
  chunk_checksum varchar(40) NOT NULL,
  num int NOT NULL,
  PRIMARY KEY (filecontent_checksum, chunk_checksum, num),
  FOREIGN KEY (filecontent_checksum) REFERENCES filecontent (checksum) ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY (chunk_checksum) REFERENCES chunk (checksum) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE CACHED TABLE filehistory (
  id varchar(40) NOT NULL,
  databaseversion_id int NOT NULL,
  PRIMARY KEY (id, databaseversion_id),
  FOREIGN KEY (databaseversion_id) REFERENCES databaseversion (id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE CACHED TABLE fileversion (
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
  PRIMARY KEY (filehistory_id, version),
  FOREIGN KEY (filehistory_id, databaseversion_id) REFERENCES filehistory (id, databaseversion_id) ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY (filecontent_checksum) REFERENCES filecontent (checksum) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE CACHED TABLE multichunk (
  id varchar(40) NOT NULL,
  PRIMARY KEY (id)
);

CREATE CACHED TABLE multichunk_chunk (
  multichunk_id varchar(40) NOT NULL,
  chunk_checksum varchar(40) NOT NULL,
  PRIMARY KEY (multichunk_id, chunk_checksum),
  FOREIGN KEY (multichunk_id) REFERENCES multichunk (id) ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY (chunk_checksum) REFERENCES chunk (checksum) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE CACHED TABLE known_databases (
  id int NOT NULL IDENTITY,
  database_name varchar(255) NOT NULL,
  UNIQUE (database_name)
);


-- Non-primary indices                              

CREATE INDEX idx_databaseversion_status ON databaseversion (status);
CREATE INDEX idx_databaseversion_vectorclock_serialized ON databaseversion (vectorclock_serialized);
CREATE INDEX idx_fileversion_path ON fileversion (path);
CREATE INDEX idx_fileversion_status ON fileversion (status);
CREATE INDEX idx_fileversion_filecontent_checksum ON fileversion (filecontent_checksum);


-- Views

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
