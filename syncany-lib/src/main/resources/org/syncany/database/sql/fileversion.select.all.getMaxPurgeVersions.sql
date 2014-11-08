-- Selects the most recent purge version per file history. All versions lower 
-- than the selected file version will be purged

select *
from fileversion
where (filehistory_id, version) in (
	select filehistory_id, max(version)-? 
	from fileversion
	group by filehistory_id
	having max(version)-? > 0
)
