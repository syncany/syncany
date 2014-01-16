select fv0.* 
from fileversion_master fv0
where fv0.filehistory_id=(
  select fv1.filehistory_id 
  from fileversion fv1
  where fv1.path=?
    and fv1.status<>? 
    and fv1.version=(
      select max(fv2.version)
      from fileversion fv2
      where fv1.filehistory_id=fv2.filehistory_id
    )
)
