-- Selects the largest version of all deleted file histores

select *
from fileversion
where (filehistory_id, version) in (
	select filehistory_id, max(version)
	from fileversion
	where status='DELETED'
	and updated<?
	group by filehistory_id
)
