-- Selects all dirty multichunks

select distinct mcf.multichunk_id
from multichunk_full mcf
where mcf.databaseversion_status='DIRTY'

minus

select distinct mcf.multichunk_id
from multichunk_full mcf
where mcf.databaseversion_status='MASTER'
