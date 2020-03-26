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
package info.muspoe.test.neo4j.service;

import info.muspoe.test.neo4j.SevenBridges;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class Neo4jService implements AutoCloseable {

    protected Driver driver;

    public Neo4jService() {

        try {
            Properties pros = new Properties();
            pros.load(SevenBridges.class.getResourceAsStream("/neo4j.properties"));
            var uri = pros.getProperty("uri");
            var user = pros.getProperty("user");
            var password = pros.getProperty("password");
            connect(uri, user, password);
        } catch (IOException ex) {
            System.out.println("******************************");
            System.out.println(" Neo4j initialization failed. ");
            System.out.println("******************************");
            System.exit(0);
        }
    }

    public Neo4jService(String uri, String user, String password) {

        connect(uri, user, password);
    }

    private void connect(String uri, String user, String password) {

        try {
            driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
            driver.verifyConnectivity();
        } catch (Exception ex) {
            System.out.println("******************************");
            System.out.println(" Neo4j initialization failed. ");
            System.out.println("******************************");
            System.out.println(ex.getMessage());
            driver.close();
            System.exit(0);
        }
    }

    public synchronized void runTx(List<Query> queries) {

        try (var session = driver.session();
             var tx = session.beginTransaction()) {
            queries.forEach(tx::run);
//            queries.stream().map(tx::run).map(Result::list).forEach(consumer::accept);
            tx.commit();
        }
    }

    public synchronized void run(Query query) {

        try (var session = driver.session();
             var tx = session.beginTransaction()) {
            tx.run(query);
            tx.commit();
        }
    }

    public synchronized <T> List<T> run(
            String query,
            Value parameters,
            Function<Record, T> mapFunction) {

        try (var session = driver.session()) {
            return session.run(query, parameters).list(mapFunction);
        }
    }

    public synchronized Record runSingle(
            String query,
            Value parameters) {

        try (var session = driver.session()) {
            return session.run(query, parameters).single();
        }
    }

    public synchronized void removeNodes(List<String> labels) {

        var params = Values.parameters("labels", labels);
        var query = new Query(
                """
                MATCH(n)
                UNWIND labels(n) AS label
                WITH label, n
                WHERE label IN $labels                
                DETACH DELETE n;""", params);
        Neo4jService.this.run(query);
    }

    @Override
    public void close() throws Exception {

        driver.close();
    }
}
