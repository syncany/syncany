merge into general_settings as general_settings_target
using (values (?, ?)) as general_settings_ref(key, value)
on (general_settings_target.key = general_settings_ref.key)
when not matched then insert (key, value) values (general_settings_ref.key, general_settings_ref.value)
when matched then update set general_settings_target.value=general_settings_ref.value
