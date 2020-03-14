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

import java.util.Properties;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Values;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class SevenBridges implements AutoCloseable {

    public static final int EULAR = 0;
    public static final int TODAY = 1;

    private Driver driver;

    public SevenBridges() {
        
        try {
            Properties pros = new Properties();
            pros.load(SevenBridges.class.getResourceAsStream("/neo4j.properties"));
            var uri = pros.getProperty("uri");
            var user = pros.getProperty("user");
            var password = pros.getProperty("password");
            driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
            driver.verifyConnectivity();
        } catch (Exception ex) {
            System.out.println("*******************************");
            System.out.println(" Neo4j initialization failed.");
            System.out.println("*******************************");
            driver.close();
            System.exit(0);
        }
    }

    public void reset_graph() {
        
        try ( var session = driver.session()) {
            session.run("MATCH(n:Place) DETACH DELETE n;");
        }
    }

    public void create_graph(int n) {

        var query = """
                     CREATE
                        (left:Place{name:"left"}),
                        (right:Place{name:"right"}),
                        (up:Place{name:"up"}),
                        (down:Place{name:"down"}),
                        (left)-[:Bridge]->(up),
                        (left)-[:Bridge]->(down),
                        (left)-[:Bridge]->(right),
                        (right)-[:Bridge]->(up),
                        (right)-[:Bridge]->(down)"""
                + (n == TODAY ? ";"
                        : """
                            ,
                            (left)<-[:Bridge]-(up),
                            (left)<-[:Bridge]-(down);
                          """);

        try ( var session = driver.session()) {
            session.run(query);
        }
    }

    public int path_count(String place, Integer num) {

        var params = Values.parameters("place", place, "num", num);
        try ( var session = driver.session()) {
            return session.run("""
                        MATCH (p)
                        WHERE p.name=$place
                        CALL apoc.path.expand(p, "", "", $num, $num)
                        YIELD path
                        RETURN count(path) AS c;
                        """, params)
                    .single()
                    .get("c", 0);
        }
    }

    public int countBridges() {
        
        try ( var session = driver.session()) {
            return session.run("""
                        MATCH ()-[bridge:Bridge]->()
                        RETURN count(bridge) AS c;
                        """)
                    .single()
                    .get("c", 0);
        }
    }

    @Override
    public void close() throws Exception {

        driver.close();
    }
}
