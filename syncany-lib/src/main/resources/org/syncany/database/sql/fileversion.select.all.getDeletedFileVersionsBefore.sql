-- Selects the largest version of all deleted file histories,
-- if this deletion was before a certain point in time.

select *
from fileversion
where (filehistory_id, version) in (
	select filehistory_id, max(version)
	from fileversion
	where status='DELETED'
	group by filehistory_id
)
and updated<?
