select *
from fileversion 
where 
	status<>?
	and path like ?
	and filehistory_id like ?	
	and substr_count(path, '/')>=?
	and substr_count(path, '/')<=?		
	and type in (unnest(?))			
	and (filehistory_id, version) in (
		select filehistory_id, max(version)
		from fileversion_master
		where updated<=?
		group by filehistory_id				
	)
	
