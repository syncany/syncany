-- Delete all chnks that are completely unused, assuming that only the
-- last X file versions will be kept.

-- Strategy
--  1. Select all chunks 
--  2. Subtract chunks that are still needed

delete from chunk
where checksum not in (
	select distinct mcc.chunk_checksum
	from fileversion fv1
	join filecontent_chunk fcc on fv1.filecontent_checksum=fcc.filecontent_checksum
	join multichunk_chunk mcc on fcc.chunk_checksum=mcc.chunk_checksum
	where fv1.version > (
		select max(fv2.version)-?
		from fileversion fv2
		where fv1.filehistory_id=fv2.filehistory_id
	)
)
