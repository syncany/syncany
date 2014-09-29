merge into multichunk_chunk as multichunk_chunk_target
using (values(?, ?)) as multichunk_chunk_ref(multichunk_id, chunk_checksum)
on (
	multichunk_chunk_target.multichunk_id = multichunk_chunk_ref.multichunk_id
	and multichunk_chunk_target.chunk_checksum = multichunk_chunk_ref.chunk_checksum
)
when not matched then insert (multichunk_id, chunk_checksum) values (multichunk_chunk_ref.multichunk_id, multichunk_chunk_ref.chunk_checksum)
