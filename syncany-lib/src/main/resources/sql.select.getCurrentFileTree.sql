select * 
from fileversion fv
where 
	fv.status<>'DELETED'
	and fv.version=(
		select max(fv1.version) 
		from fileversion fv1 
		where fv.filehistory_id=fv1.filehistory_id
	)
