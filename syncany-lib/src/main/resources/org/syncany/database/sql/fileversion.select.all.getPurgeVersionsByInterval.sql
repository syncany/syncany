-- Selects all versions that should be purged given an datetimeformat to truncate
-- to and a beginning and ending timestamp

select *
from fileversion
where (filehistory_id, version) not in (
	select filehistory_id, max(version)
	from fileversion
	group by filehistory_id, trunc(updated, ?)
)
and updated > ?
and updated < ?
