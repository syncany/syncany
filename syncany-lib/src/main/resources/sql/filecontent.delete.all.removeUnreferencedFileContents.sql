-- Remove file contents that are not used by any file versions anymore

delete from filecontent
where checksum not in (
	select distinct filecontent_checksum
	from fileversion
)
