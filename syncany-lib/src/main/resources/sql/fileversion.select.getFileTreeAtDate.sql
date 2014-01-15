-- Determine the file tree (as on file system) at a certain date

-- Query:
--  1. Select ALL file versions before the date (only MASTER)
--  2. Substract all file versions that contain an entry marked DELETED
--     a) Select deleted file history IDs
--     b) Select all file versions with this ID

select *
from fileversion_full
where 
	databaseversion_status='MASTER'
	and databaseversion_localtime<=?
	
minus 

select *
from fileversion_full
where filehistory_id in (
	select filehistory_id
	from fileversion_full
	where 
		databaseversion_status='MASTER'
		and status='DELETED'
		and databaseversion_localtime<=?
)
