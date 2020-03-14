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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Values;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class COVID_19 implements AutoCloseable {

    private Driver driver;

    public static String url = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_19-covid-Confirmed.csv";

    public COVID_19() {

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
            session.run("MATCH(n:Country) DETACH DELETE n;");
        }
    }

    public void create_graph(String url) {

        var params = Values.parameters("url", url);
        try ( var session = driver.session()) {
//            var tx = session.beginTransaction();
            var values = session.run(
                    """
                    LOAD CSV WITH HEADERS
                    FROM $url AS line
                    RETURN line;
                    """, params).list(r -> r.get("line").asMap());
            System.out.println(values.size() + " records loaded.");

            var dates = values.get(0).keySet().stream()
                    .filter(key -> key.matches("^\\d{1,2}\\/\\d{1,2}\\/\\d{2}$"))
                    .sorted(COVID_19::compareByDate)
                    .collect(Collectors.toList());
            var countries = values.stream()
                    .collect(Collectors.groupingBy(
                            line -> line.get("Country/Region").toString(),
                            Collectors.toList()));
            System.out.println(countries.size() + " countries loaded.");

            var i = new AtomicInteger();
            var data = countries.entrySet().stream()
                    .peek(entry -> System.out.println(i.incrementAndGet() + ": " + entry.getKey()))
                    .collect(Collectors.toMap(
                            Entry::getKey,
                            entry -> dates.stream()
                                    .collect(Collectors.toMap(
                                            date -> date,
                                            date -> entry.getValue().stream()
                                                    .mapToInt(v -> Integer.valueOf(v.get(date).toString()))
                                                    .sum()))));
            System.out.println(data.size() + " countries prepared to write.");

            i.set(0);
            var create = "CREATE " + data.entrySet().stream()
                    .peek(entry -> System.out.println(i.incrementAndGet() + ": " + entry.getKey()))
                    .map(entry -> String.format("(:Country{name:\"%s\"})", entry.getKey()))
                    .collect(Collectors.joining(",")) + ";";
            System.out.println(create);
            session.run(create);
            
            i.set(0);
            data.entrySet().stream()
                    .peek(entry -> System.out.println(i.incrementAndGet() + ": " + entry.getKey()))
                    .map(entry
                            -> String.format("MATCH (n:Country) WHERE n.name=\"%s\" SET %s",
                            entry.getKey(),
                            entry.getValue().entrySet().stream()
                                    .map(e -> String.format("n.`%s`=%d", e.getKey(), e.getValue()))
                                    .collect(Collectors.joining(","))) + ";")
                    .peek(System.out::println)
                    .forEach(session::run);
        }
    }

    public static int compareByDate(String date1, String date2) {

        return toDate(date2).compareTo(toDate(date1));
    }

    public static Date toDate(String date) {

        try {
            return new SimpleDateFormat("MM/dd/yyyy").parse(date);
        } catch (ParseException ex) {
            return null;
        }
    }

    @Override
    public void close() throws Exception {

        driver.close();

    }
}

class Country {

    String name;
    Map<String, Integer> data;

    public Country(String name, Map<String, Integer> data) {

        this.name = name;
        this.data = data;
    }
}
