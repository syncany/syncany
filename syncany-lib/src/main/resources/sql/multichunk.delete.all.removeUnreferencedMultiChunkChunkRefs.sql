delete from multichunk_chunk
where multichunk_id not in (
	select distinct mcc.multichunk_id
	from fileversion fv1
	join filecontent_chunk fcc on fv1.filecontent_checksum=fcc.filecontent_checksum
	join multichunk_chunk mcc on fcc.chunk_checksum=mcc.chunk_checksum
)
