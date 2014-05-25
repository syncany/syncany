-- Remove file content / chunk references for file contents
-- that are not used by any file versions anymore

-- Without the "where .. is not null" clause, the subquery is 
-- always empty if a filecontent is null (= folder/zero-byte)

delete from filecontent_chunk
where filecontent_checksum not in (
	select distinct filecontent_checksum
	from fileversion
	where filecontent_checksum is not null	
)
