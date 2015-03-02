-- Selects all versions that should be purged before a certain time
-- It is not deleted if and only if it is the last version of a file.

select *
from fileversion
where (filehistory_id, version) not in (
	select filehistory_id, max(version)
	from fileversion
	group by filehistory_id
)
and updated < ?
