select client, max(filenumber) filenumber
from known_databases
group by client, filenumber
