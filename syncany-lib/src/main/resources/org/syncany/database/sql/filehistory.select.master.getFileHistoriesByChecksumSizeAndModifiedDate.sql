SELECT fv.filehistory_id as filehistory_id, MAX(fv.version) AS version
 FROM fileversion AS fv
 JOIN databaseversion AS dbv
  ON fv.databaseversion_id = dbv.id
 WHERE dbv.status='MASTER'
  AND fv.filecontent_checksum = ?
  AND fv.size = ?
  AND fv.lastmodified = ?
 GROUP BY fv.filehistory_id;