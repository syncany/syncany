delete from fileversion
where filehistory_id in (
	select fv.filehistory_id
	from fileversion_master_maxversion fvmax
	join fileversion_master fv on fvmax.filehistory_id=fv.filehistory_id and fvmax.version=fv.version 
	where fv.status='DELETED'
)
