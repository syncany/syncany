select mcc.chunk_checksum, mcc.multichunk_id 
from multichunk_chunk mcc 
join multichunk mc on mc.id=mcc.multichunk_id 
where mcc.chunk_checksum in ( unnest(?) )