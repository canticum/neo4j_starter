/*
 * Copyright 2020 Jonathan Chang, Chun-yien <ccy@musicapoetica.org>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.muspoe.test.neo4j;

import info.muspoe.test.neo4j.service.Neo4jService;
import java.util.ArrayList;
import java.util.List;
import org.neo4j.driver.Query;
import org.neo4j.driver.Values;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class SevenBridges extends Neo4jService {

    public static final int EULAR = 0;
    public static final int TODAY = 1;

    public SevenBridges(int port, String user, String password) {

        super("bolt://localhost:" + port, user, password);
    }

    public void reset_graph() {

        runTransaction(new Query("MATCH(n:Place) DETACH DELETE n;"));

    }

    public void create_graph(int n) {

        List<Query> queries = new ArrayList<>();

        queries.add(new Query(
                """
                CREATE (left:Place{name:"left"}),
                       (right:Place{name:"right"}),
                       (up:Place{name:"up"}),
                       (down:Place{name:"down"}),
                       (left)-[:Bridge]->(up),
                       (left)<-[:Bridge]-(up),
                       (left)-[:Bridge]->(down),
                       (left)<-[:Bridge]-(down),
                       (left)-[:Bridge]->(right),
                       (right)-[:Bridge]->(up),
                       (right)-[:Bridge]->(down);"""));

        var query_today = new Query(
                """
                MATCH (:Place{name:"left"})<-[r]-()
                DELETE r;""");

        if (n == TODAY) {
            queries.add(query_today);
        }

        runTransaction(queries);
    }

    public int path_count(String place, Integer num) {

        var params = Values.parameters("place", place, "num", num);
        return runCypherSingle("""
                        MATCH (p)
                        WHERE p.name=$place
                        CALL apoc.path.expand(p, "", "", $num, $num)
                        YIELD path
                        RETURN count(path) AS c;
                        """, params).get("c", 0);
    }

    public int countBridges() {

        return runCypherSingle("""
                        MATCH ()-[bridge:Bridge]->()
                        RETURN count(bridge) AS c;
                        """, null).get("c", 0);
    }
}
