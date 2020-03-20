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

import info.muspoe.test.neo4j.vo.RecordDaily;
import info.muspoe.test.neo4j.vo.RecordTime;
import info.muspoe.test.neo4j.service.Neo4jService;
import info.muspoe.test.neo4j.vo.NinjaValue;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.neo4j.driver.Record;
import org.neo4j.driver.Values;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class WuhanVirus extends Neo4jService {

    public static final int DAILY = 0;
    public static final int TIME_CONFIRMED = 1;
    public static final int TIME_DEATHS = 2;
    public static final int TIME_RECOVERED = 3;

    public static final String URL_BASE
            = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/";
    public static final String URL_DAILY
            = URL_BASE + "csse_covid_19_daily_reports/%s.csv";
    public static final String URL_TIME
            = URL_BASE + "csse_covid_19_time_series/time_series_19-covid-%s.csv";
    public static final String URL_NINJA
            = "https://corona.lmao.ninja/countries";

    public void reset_graph() {

        try (var session = driver.session()) {
            session.run("MATCH(n:Country),(m:Province) DETACH DELETE n,m;");
        }
    }

    public void test_file(String url) throws FileNotFoundException, Exception {

        new URL(url).openStream().close();
    }

    public void create_graph(int data_type, String... date) throws Exception {

        switch (data_type) {
            case TIME_CONFIRMED ->
                create_graph_time(TIME_CONFIRMED);
            case TIME_DEATHS ->
                create_graph_time(TIME_DEATHS);
            case TIME_RECOVERED ->
                create_graph_time(TIME_RECOVERED);
            case DAILY ->
                create_graph_daily(date[0]);
            default ->
                System.out.println("Invalid data type, please try again.");
        }
    }

    private void create_graph_daily(String date)
            throws FileNotFoundException, Exception {

        var url = String.format(URL_DAILY, date);
        System.out.println("Loading data from " + url);
        var params = Values.parameters("url", url);
        test_file(url);
        reset_graph();

        List<RecordDaily> values = runCypher(
                """
                LOAD CSV WITH HEADERS
                FROM $url AS line
                RETURN line;""", params, RecordDaily::new);
        System.out.println(values.size() + " records loaded.");

        values.stream().forEach(rd -> {
            if (rd.getProvince() == null) {
                var params0 = Values.parameters(
                        "name", rd.getCountry(),
                        "confirmed", rd.getConfirmed(),
                        "deaths", rd.getDeaths(),
                        "recovered", rd.getRecovered());
                runCypherSingle(
                        """
                        MERGE (n:Country{name:$name})
                        SET n.confirmed=$confirmed, n.deaths=$deaths, n.recovered=$recovered
                        RETURN n;""", params0);
                System.out.printf("Country %s created, with confirmed=%d, deaths=%d, recovered=%d.\n",
                        rd.getName(), rd.getConfirmed(), rd.getDeaths(), rd.getRecovered());
            } else {
                var params0 = Values.parameters(
                        "cname", rd.getCountry(),
                        "pname", rd.getProvince(),
                        "confirmed", rd.getConfirmed(),
                        "deaths", rd.getDeaths(),
                        "recovered", rd.getRecovered());
                var c = runCypherSingle(
                        """
                        MERGE (m:Country{name:$cname}) WITH m
                        MERGE (n:Province{name:$pname})-[:IS_PROVINCE_OF]->(m)
                        SET n.confirmed=$confirmed, n.deaths=$deaths, n.recovered=$recovered
                        RETURN m;""", params0).get("m");
                System.out.printf("Province %s created, with confirmed=%d, deaths=%d, recovered=%d.\n",
                        rd.getProvince(), rd.getConfirmed(), rd.getDeaths(), rd.getRecovered());
                var confirmed = rd.getConfirmed() + c.get("confirmed", 0);
                var deaths = rd.getDeaths() + c.get("deaths", 0);
                var recovered = rd.getRecovered() + c.get("recovered", 0);
                params0 = Values.parameters(
                        "name", rd.getCountry(),
                        "confirmed", confirmed,
                        "deaths", deaths,
                        "recovered", recovered);
                runCypherSingle(
                        """
                        MATCH (n:Country{name:$name})
                        SET n.confirmed=$confirmed, n.deaths=$deaths, n.recovered=$recovered
                        RETURN n;""", params0);
                System.out.printf("Country %s updated, with confirmed=%d, deaths=%d, recovered=%d.\n",
                        rd.getCountry(), confirmed, deaths, recovered);
            }
        });
    }

    private void create_graph_time(int type)
            throws FileNotFoundException, Exception {

        var url = String.format(URL_TIME, switch (type) {
            case TIME_DEATHS ->
                "Deaths";
            case TIME_RECOVERED ->
                "Recovered";
            default ->
                "Confirmed";
        });
        test_file(url);
        reset_graph();

        var params = Values.parameters("url", url);
        List<RecordTime> values = runCypher(
                """
                LOAD CSV WITH HEADERS
                FROM $url AS line
                RETURN line;""", params, RecordTime::new);
        System.out.println(values.size() + " records loaded.");

        var dates = values.get(0).getData().keySet();
        var countries = values.stream()
                .collect(Collectors.groupingBy(
                        rt -> rt.getCountry(),
                        Collectors.toList()));

        var i = new AtomicInteger();
        var country_data = countries.entrySet().stream()
                .collect(Collectors.toMap(
                        Entry::getKey,
                        entry -> dates.stream()
                                .collect(Collectors.toMap(
                                        date -> RecordTime.date_formatter.format(date),
                                        date -> entry.getValue().stream()
                                                .mapToInt(rt -> rt.getData().get(date))
                                                .sum()))));
        System.out.println(country_data.size() + " countries prepared to write.");

        i.set(0);
        var create = "CREATE " + country_data.entrySet().stream()
                .parallel()
                .map(entry -> String.format("(:Country{name:\"%s\"})", entry.getKey()))
                .collect(Collectors.joining(",")) + ";";

        runCypher(create);

        i.set(0);
        country_data.entrySet().stream()
                .parallel()
                .map(entry
                        -> String.format("MATCH (n:Country) WHERE n.name=\"%s\" SET %s",
                        entry.getKey(),
                        entry.getValue().entrySet().stream()
                                .map(e -> String.format("n.`%s`=%d", e.getKey(), e.getValue()))
                                .collect(Collectors.joining(","))) + ";")
                .forEach(this::runCypher);
    }

    public void list_by_time(String date) throws Exception {

        var result = runCypher("""
                               MATCH (n:Country) 
                               RETURN n;""",
                r -> Map.entry(
                        r.get("n").get("name", ""),
                        r.get("n").get(date, -1)));

        if (result.get(0).getValue() == -1) {
            Logger.getGlobal().severe("ERROR - Specified date not exists.");
        } else {
            var width = result.stream().mapToInt(r -> r.getKey().length()).max().getAsInt() + 4;
            System.out.println("Specified Date: " + date);
            System.out.printf("%" + width + "s%8s\n", "Country", "Number");
            System.out.printf("%" + width + "s%8s\n", "-------", "------");
            var n = new AtomicInteger(0);
            result.stream()
                    .sorted(Entry.comparingByValue(Comparator.reverseOrder()))
                    .forEach(e -> System.out.printf("%3d.%" + (width - 4) + "s%8d\n",
                    n.incrementAndGet(), e.getKey(), e.getValue()));
        }
    }

    public void list_confirmed(String country) throws Exception {

        WuhanVirus.this.list_confirmed("MATCH (n)-->(m:Country) WHERE m.name='" + country + "'", "Province");
    }

    public void list_confirmed() throws Exception {

        WuhanVirus.this.list_confirmed("MATCH (n:Country) ", "Country");

    }

    private void list_confirmed(String query_prefix, String title) {

        var query = query_prefix + """
                                   RETURN
                                        n.name AS name, 
                                        n.confirmed AS confirmed, 
                                        n.deaths AS deaths, 
                                   CASE n.confirmed
                                        WHEN 0 THEN 0.0 
                                        ELSE floor(toFloat(n.deaths) / n.confirmed * 1000) / 10 
                                   END AS rate;""";
        var result = runCypher(query, DeathRate::new);
        var width = result.stream().mapToInt(r -> r.name.length()).max().getAsInt() + 4;
        System.out.printf("%" + width + "s%11s%8s  %s\n", "Country", "Confirmed", "Deaths", "Death Rate(%)");
        System.out.printf("%" + width + "s%11s%8s  %s\n", "-------", "---------", "------", "-------------");
        var n = new AtomicInteger(0);
        result.stream().sorted()
                .forEach(r -> System.out.printf("%3d.%" + (width - 4) + "s%11d%8d%1d0.1f\n",
                n.incrementAndGet(), r.name, r.confirmed, r.deaths, r.death_rate));
    }

    public void list_ninja() {

        var params = Values.parameters("url", URL_NINJA);
        var query = """
                    WITH $url AS url
                    CALL apoc.load.json(url)
                    YIELD value RETURN value;""";
        var result = runCypher(query, params, NinjaValue::new);
        var width = result.stream().mapToInt(r -> r.getCountry().length()).max().getAsInt() + 4;
        System.out.printf("%" + width + "s%7s%10s%6s\n", "Country", "Cases", "Cases/Mil", "Death");
        System.out.printf("%" + width + "s%7s%10s%6s\n", "-------", "-----", "---------", "-----");
        var n = new AtomicInteger(0);
        result.stream()
                .sorted(Comparator.comparing(NinjaValue::getCasesPerOneMillion, Comparator.reverseOrder()))
                .forEach(r -> System.out.printf("%3d.%" + (width - 4) + "s%7d%10d%6d\n", n.incrementAndGet(),
                r.getCountry(), r.getCases(), r.getCasesPerOneMillion(), r.getDeaths()));
    }
}

class DeathRate implements Comparable<DeathRate> {

    String name;
    int confirmed, deaths;
    double death_rate;

    public DeathRate(Record r) {
        this.name = r.get("name", "");
        this.confirmed = r.get("confirmed", 0);
        this.deaths = r.get("deaths", 0);
        this.death_rate = r.get("rate", 0.0);
    }

    @Override
    public int compareTo(DeathRate o) {

        return Integer.compare(o.confirmed, this.confirmed);
    }
}
