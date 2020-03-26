MATCH ()-[bridge:Bridge]->()
RETURN count(bridge) AS c;
