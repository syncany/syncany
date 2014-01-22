-- Remove file content / chunk references for file contents
-- that are not used by any file versions anymore

delete from filecontent_chunk
where filecontent_checksum in (
	select distinct checksum
	from filecontent

		minus
	
	select distinct filecontent_checksum
	from fileversion
)
