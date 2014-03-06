delete from fileversion fv1
where fv1.version <= (
	select max(fv2.version)-?
	from fileversion fv2
	where fv1.filehistory_id=fv2.filehistory_id
)

