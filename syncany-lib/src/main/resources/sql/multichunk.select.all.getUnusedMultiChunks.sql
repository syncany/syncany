select distinct mcc1.multichunk_id id, mc.size
from multichunk_chunk mcc1
join multichunk mc on mcc1.multichunk_id=mc.id
where multichunk_id not in (
	select distinct mcc.multichunk_id
	from fileversion fv
	join filecontent_chunk fcc on fv.filecontent_checksum=fcc.filecontent_checksum
	join multichunk_chunk mcc on fcc.chunk_checksum=mcc.chunk_checksum
)
