select dbvm.*
from databaseversion_master dbvm
order by dbvm.id desc
limit ? offset ?
