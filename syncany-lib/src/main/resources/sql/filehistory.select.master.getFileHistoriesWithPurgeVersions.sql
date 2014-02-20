-- Selects n-x fileversions (= all but n), where n is the total number of 
-- file versions and x is the number of file versions to keep

select * 
from fileversion fv1
where version < (
	select max(fv2.version) - ?
	from fileversion fv2
	where fv2.filehistory_id=fv1.filehistory_id
)
order by fv1.filehistory_id, fv1.version
