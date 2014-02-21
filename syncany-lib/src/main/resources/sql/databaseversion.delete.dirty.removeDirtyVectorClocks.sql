delete from databaseversion_vectorclock
where databaseversion_id in (select id from databaseversion where status='DIRTY')
