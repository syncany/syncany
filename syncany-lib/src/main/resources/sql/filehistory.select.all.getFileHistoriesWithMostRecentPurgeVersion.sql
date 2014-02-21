-- Selects the most recent purge version per file history. All versions lower 
-- than the selected file version will be purged

select filehistory_id, max(version)-? as most_recent_purge_version
from fileversion
group by filehistory_id
