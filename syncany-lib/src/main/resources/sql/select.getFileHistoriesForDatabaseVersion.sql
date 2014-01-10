select fvf.*
from fileversion_full fvf
where fvf.databaseversion_vectorclock_serialized=?
