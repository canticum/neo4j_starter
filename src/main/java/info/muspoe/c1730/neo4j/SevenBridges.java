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
package info.muspoe.c1730.neo4j;

import info.muspoe.c1730.neo4j.service.Neo4jService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.neo4j.driver.Query;
import org.neo4j.driver.Values;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
@SpringBootApplication
@RestController
public class SevenBridges extends Neo4jService {

    public static final int EULER = 0;
    public static final int TODAY = 1;
    private static String uri;
    private static String user;
    private static String password;
    static final String header = """
                                 <pre>
                                 ***************************
                                  Königsberg Bridge Problem
                                 ***************************
                                 """;

    static {
        try {
            Properties pros = new Properties();
            pros.load(SevenBridges.class.getResourceAsStream("/neo4j.properties"));
            uri = pros.getProperty("uri.local");
            user = pros.getProperty("user.local");
            password = pros.getProperty("password.local");
        } catch (IOException ex) {
            System.out.println("******************************");
            System.out.println(" Neo4j initialization failed. ");
            System.out.println("******************************");
            System.exit(0);
        }
    }

    public SevenBridges() {
    }

    public SevenBridges(String uri, String user, String password) {

        super(uri, user, password);
    }

    @GetMapping("/sb")
    public String hello(
            @RequestParam(value = "time", defaultValue = "euler") String t) {

        var time = switch (t.trim().toLowerCase()) {
            case "today" ->
                SevenBridges.TODAY;
            default ->
                SevenBridges.EULER;
        };

        StringBuilder result = new StringBuilder(header + "Time = "
                + (time == SevenBridges.EULER ? "EULER" : "TODAY")
                + "\n");

        try (var sb = new SevenBridges(uri, user, password)) {

            sb.reset_graph();
            sb.create_graph(time);
            var bridge_num = sb.countBridges();
            result.append("Bridge number = ").append(bridge_num).append("\n");

            IntStream.rangeClosed(1, bridge_num).mapToObj(
                    n -> Map.entry(n,
                            Stream.of("left", "up", "down", "right")
                                    .map(p -> Map.entry(p, sb.path_count(p, n)))
                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            ).forEach(entry -> result.append(String.format("* %d bridges, start from:\n%s", entry.getKey(),
                    entry.getValue().entrySet().stream()
                            .map(e -> String.format("\t%-6s =%4d%n", e.getKey(), e.getValue()))
                            .collect(Collectors.joining())
            )));
        } catch (Exception ex) {
            result.append(
                    Arrays.stream(ex.getStackTrace())
                            .map(StackTraceElement::toString)
                            .collect(Collectors.joining("\n"))
            );
        }
        return result.toString();
    }

    public void reset_graph() {

        removeNodes(List.of("Place"));
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

        runTx(queries);
    }

    public int path_count(String place, Integer num) {

        var params = Values.parameters("place", place, "num", num);
        return runSingle(
                """
                MATCH (p)
                WHERE p.name=$place
                CALL apoc.path.expand(p, "", "", $num, $num)
                YIELD path
                RETURN count(path) AS c;
                """, params).get("c", 0);
    }

    public int countBridges() {

        return runSingle(
                """
                MATCH ()-[bridge:Bridge]->()
                RETURN count(bridge) AS c;
                """, null).get("c", 0);
    }
}
