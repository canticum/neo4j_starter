// $place=Place to start, $num=number of bridges traversed
// APOC plugin required
MATCH (p) WHERE p.name=$place
CALL apoc.path.expand(p, "", "", $num, $num)
YIELD path
RETURN count(path) AS c;
