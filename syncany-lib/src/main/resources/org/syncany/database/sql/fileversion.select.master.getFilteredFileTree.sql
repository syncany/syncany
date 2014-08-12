select *
from fileversion 
where 
	status<>'DELETED'
	and path like ?
	and substr_count(path, '/')>=?
	and substr_count(path, '/')<=?		
	and type in (unnest(?))			
	and (filehistory_id, version) in (
		select filehistory_id, max(version)
		from fileversion_master
		where updated<=?
		group by filehistory_id				
	)
	
