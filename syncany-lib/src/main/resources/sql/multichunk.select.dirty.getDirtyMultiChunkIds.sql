-- Selects all dirty multichunks

select distinct mc.id multichunk_id
from multichunk mc
join databaseversion dbv on mc.databaseversion_id=dbv.id
where dbv.status='DIRTY'
