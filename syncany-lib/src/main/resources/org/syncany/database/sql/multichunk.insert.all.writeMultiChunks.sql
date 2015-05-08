merge into multichunk as multichunk_target
using (values(?)) as multichunk_ref(id)
on (multichunk_target.id = multichunk_ref.id)
when not matched then insert (id, databaseversion_id, size) values (multichunk_ref.id, ?, ?)
