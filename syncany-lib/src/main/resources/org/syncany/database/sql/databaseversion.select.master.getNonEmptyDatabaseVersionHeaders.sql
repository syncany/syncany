select dbv.*
from databaseversion dbv
join fileversion fv on dbv.id=fv.databaseversion_id
order by dbv.id
