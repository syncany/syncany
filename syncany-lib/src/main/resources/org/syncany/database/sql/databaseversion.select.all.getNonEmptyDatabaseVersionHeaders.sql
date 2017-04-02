select dbv.*
from databaseversion dbv
join fileversion fv on dbv.id=fv.databaseversion_id
group by dbv.id
order by dbv.id
