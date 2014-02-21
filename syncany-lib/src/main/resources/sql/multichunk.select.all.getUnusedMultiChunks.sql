-- Select all multichunks that are completely unused, assuming that only the
-- last X file versions will be kept.

-- Strategy
--  1. Select all multichunks 
--  2. Subtract multichunks that are still needed

select id as multichunk_id
from multichunk
where id not in (
	select distinct mcc.multichunk_id
	from fileversion fv1
	join filecontent_chunk fcc on fv1.filecontent_checksum=fcc.filecontent_checksum
	join multichunk_chunk mcc on fcc.chunk_checksum=mcc.chunk_checksum
	where fv1.version > (
		select max(fv2.version)-?
		from fileversion fv2
		where fv1.filehistory_id=fv2.filehistory_id
	)
)
