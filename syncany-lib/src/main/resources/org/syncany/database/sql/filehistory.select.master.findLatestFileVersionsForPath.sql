SELECT fvmax.version AS version,
  fvmax.filehistory_id AS filehistory_id,
  dbv.vectorclock_serialized AS vectorclock_serialized
 FROM (
  SELECT fv.filehistory_id as filehistory_id, MAX(fv.version) AS version
   FROM fileversion AS fv
   JOIN databaseversion AS dbv
    ON fv.databaseversion_id = dbv.id
   WHERE dbv.status='MASTER'
    AND fv.path = ?
   GROUP BY fv.filehistory_id
 ) AS fvmax
 JOIN fileversion AS fv
  ON fvmax.filehistory_id = fv.filehistory_id
   AND fvmax.version = fv.version
 JOIN databaseversion AS dbv
  ON fv.databaseversion_id = dbv.id
 WHERE fv.status <> 'DELETED';