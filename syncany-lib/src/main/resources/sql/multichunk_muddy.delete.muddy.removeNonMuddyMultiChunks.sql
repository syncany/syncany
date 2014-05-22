-- TODO [medium] This is not a final solution for #132

-- It does not take muddy multichunks into account that have not been 
-- referenced by a new database version. 

delete from multichunk_muddy
where id in (select id from multichunk)
