merge into multichunk_muddy as multichunk_muddy_target
using (values(?)) as multichunk_muddy_ref(id)
on (multichunk_muddy_target.id = multichunk_muddy_ref.id)
when not matched then insert (id, machine_name, machine_version) values (multichunk_muddy_ref.id, ?, ?)
