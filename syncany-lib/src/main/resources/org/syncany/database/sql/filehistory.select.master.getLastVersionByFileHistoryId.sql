SELECT *
 FROM fileversion
 WHERE filehistory_id = ?
  AND version IN (
   SELECT MAX(fv.version) AS version
    FROM fileversion AS fv
    JOIN databaseversion AS dbv
     ON fv.databaseversion_id = dbv.id
    WHERE fv.filehistory_id = ?
     AND dbv.status = 'MASTER'
  );