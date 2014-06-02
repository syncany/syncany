-- To be called with setMaxRows(1) !

select *
from databaseversion 
where status='MASTER'
order by id desc
