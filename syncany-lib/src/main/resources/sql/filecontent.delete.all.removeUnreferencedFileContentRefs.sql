-- Remove file content / chunk references for file contents
-- that are not used by any file versions anymore

delete from filecontent_chunk
where filecontent_checksum not in (
	select distinct filecontent_checksum
	from fileversion
)
