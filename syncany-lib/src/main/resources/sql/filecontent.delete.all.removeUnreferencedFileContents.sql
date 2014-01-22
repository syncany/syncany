-- Remove file contents that are not used by any file versions anymore

delete from filecontent
where checksum in (
	select distinct checksum
	from filecontent

		minus
	
	select distinct filecontent_checksum
	from fileversion
)
