-- Select file history identifier, file version and corresponding database version for all non-deleted file versions with the given path.
-- 
-- Strategy:
-- - Select file history identifier and max. file version from `fileversion` (fvmax)
-- - Join rest of the table (fv)
-- - Join database version table (dbv)
-- - Rule out deleted versions (where)
-- 
-- Please note: It is important that the initial inner select (fvmax) does not rule out deleted versions, as this has caused trouble in the past
--
-- Note that we only select the newest version from every filehistory, since we use this query to
-- find filehistories to build on, so older versions are not needed. In addition, we only consider
-- versions in MASTER, since we can't build on DIRTY databases.
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
