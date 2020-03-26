MATCH path=(a)-[*5]-()
WHERE a.name='left'
RETURN count(path) AS c;
