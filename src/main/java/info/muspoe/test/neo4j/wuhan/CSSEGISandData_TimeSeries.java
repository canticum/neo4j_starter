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
package info.muspoe.test.neo4j.wuhan;

import info.muspoe.test.neo4j.ex.SpecifiedCountryNotExistsException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import info.muspoe.test.neo4j.ex.SpecifiedDateNotExistsException;
import info.muspoe.test.neo4j.vo.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.neo4j.driver.Query;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class CSSEGISandData_TimeSeries extends CSSEGISandData {

    public static final String URL_TIME_SERIES
            = URL_BASE + "csse_covid_19_time_series/time_series_covid19_%s_global.csv";

    public static final Function<String, String> URL = (type) -> {
        var url = String.format(URL_TIME_SERIES, type);
        System.out.println(url);
        return url;
    };

    public static final DateTimeFormatter TIME_SERIES_FORMATTER
            = DateTimeFormatter.ofPattern("M/d/yy");

    public static final String[] TYPES = {"confirmed", "deaths"};

    Map<String, List<Record_TimeSeries>> data;
    private List<String> dates;

    /**
     * Constructor.
     *
     * @throws Exception
     */
    public CSSEGISandData_TimeSeries() throws Exception {

        // 讀入遠端資料
        data = Arrays.stream(TYPES)
                .collect(Collectors.toMap(type -> type,
                        type -> runCypher("LOAD CSV WITH HEADERS FROM $url AS line RETURN line;",
                                Values.parameters("url", URL.apply(type)),
                                Record_TimeSeries::new)));
        Logger.getGlobal().log(Level.INFO, "{0} records loaded.",
                data.values().stream().mapToInt(List::size).sum());

        metadata = runCypher("""
                                 MERGE (n:Metadata)
                                 RETURN n;""", null, Metadata::new).get(0);
        dates = metadata.getTimeSeries_dates();
    }

    public boolean updateRequired() {
        Logger.getGlobal().info("updateRequired");

        // 更新日期標籤
        dates = data.get("confirmed").get(0).getTimeSeriesData().entrySet().stream()
                .sorted(Entry.comparingByKey())
                .map(Entry::getKey)
                .map(d -> d.format(TIME_SERIES_FORMATTER))
                .collect(Collectors.toList());
        try {
            var md = new Metadata(runCypherSingle(
                    "MERGE (n:Metadata) RETURN n;", null));
            var country_number = runCypherSingle(
                    "MATCH (n:Country) RETURN count(n) AS num;", null)
                    .get("num").asInt();
            return (!dates.equals(md.getTimeSeries_dates()) || country_number < 100);
        } catch (Exception ex) {
            return true;
        }
    }

    public void update_data() throws Exception {
        Logger.getGlobal().info("update_data");

        update_metadata();
        update_time_series_data();
    }

    static final String cypher_metadata
            = """
              MERGE (n:Metadata)
              SET n.timeSeries_confirmed_uri=$timeSeries_confirmed_uri,
                  n.timeSeries_deaths_uri=$timeSeries_deaths_uri,
                  n.timeSeries_dates=$timeSeries_dates
              RETURN n;""";

    private void update_metadata() {
        Logger.getGlobal().info("update_metadata");

        var params = Values.parameters(
                "timeSeries_confirmed_uri", URL.apply("confirmed"),
                "timeSeries_deaths_uri", URL.apply("deaths"),
                "timeSeries_dates", dates
        );
        metadata = runCypher(cypher_metadata, params, Metadata::new).get(0);
    }

    static final String cypher_country
            = """
              MERGE (n:Country{name:$country})
              WITH n
              CALL apoc.create.setProperty(n,$type,$values)
              YIELD node RETURN n;""";
    static final String cypher_province
            = """
              MERGE (n:Country{name:$country})
              MERGE (m:Province{name:$province})-[:IS_PROVINCE_OF]->(n)
              WITH n,m
              CALL apoc.create.setProperty(m,$type,$values) 
              YIELD node RETURN n;""";
    static final String cypher_all_provinces
            = """
              MATCH (n:Province)-->(m)              
              RETURN n AS province, m.name AS country;""";
    static final String cypher_update_country
            = """
              MATCH (n:Country{name:$country})
              SET n.confirmed=$confirmed, n.deaths=$deaths;""";

    public void update_time_series_data() {
        Logger.getGlobal().info("update_time_series_data");

        // 以國家為單位重整
        var data_by_country = data.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(rec -> Map.entry(entry.getKey(), rec)))
                .map(entry -> Map.entry(entry.getValue().getCountry(), Map.entry(entry.getKey(), entry.getValue())))
                .collect(Collectors.toList());
        System.out.println("data_by_country.size()=" + data_by_country.size());

        List<Query> queries = new ArrayList<>();

        for (var entry : data_by_country) {
            var country = entry.getKey();
            var province = entry.getValue().getValue().getProvince();
//            System.out.println(country + ": " + province);
            var type = entry.getValue().getKey().toLowerCase();
            var time_series_data = entry.getValue()
                    .getValue().getTimeSeriesData().entrySet().stream()
                    .sorted(Entry.comparingByKey())
                    .map(Entry::getValue)
                    .collect(Collectors.toList());
            var params = Values.parameters(
                    "country", country,
                    "province", province,
                    "type", type,
                    "values", time_series_data);
            if (province == null) {
                // Country
                queries.add(new Query(cypher_country, params));
            } else {
                // Province  
                queries.add(new Query(cypher_province, params));
            }
        }
        runTransaction(queries);
        var provinces = runCypher(cypher_all_provinces, null,
                r -> Map.entry(r.get("country").asString(), r.get("province")));
        Map<String, Map<String, List<Integer>>> country_updated = new HashMap<>();
        for (Entry<String, Value> entry : provinces) {
            var country = entry.getKey();
            var confirmed = entry.getValue().get("confirmed").asList(Value::asInt);
            var deaths = entry.getValue().get("deaths").asList(Value::asInt);
            var map = Map.of("confirmed", confirmed, "deaths", deaths);
            country_updated.merge(country, map, (o, n) -> Map.of(
                    "confirmed", sum(o.get("confirmed"), n.get("confirmed")),
                    "deaths", sum(o.get("deaths"), n.get("deaths"))));
        }
        queries.clear();
        country_updated.entrySet().forEach(entry -> {
            var params = Values.parameters(
                    "country", entry.getKey(),
                    "confirmed", entry.getValue().get("confirmed"),
                    "deaths", entry.getValue().get("deaths"));
            queries.add(new Query(cypher_update_country, params));
        });
        runTransaction(queries);
    }

    List<Integer> sum(List<Integer> l1, List<Integer> l2) {

        return IntStream.range(0, l1.size())
                .map(i -> l1.get(i) + l2.get(i))
                .boxed()
                .collect(Collectors.toList());
    }

    public void list_by_country(String country) {

        var params = Values.parameters("country", country);
        var result = runCypher(
                """
                MATCH(n:Country)
                WHERE n.name=$country 
                RETURN n;""", params, r -> r.get("n"));
        if (result.isEmpty()) {
            throw new SpecifiedCountryNotExistsException(country);
        }
//        System.out.println("Specified country: " + country);
        System.out.printf("%10s%12s%12s\n", "Date", "Confirmed", "Deaths");
        System.out.printf("%10s%12s%12s\n", "----", "---------", "------");
        IntStream.range(0, dates.size())
                .mapToObj(i -> String.format("%10s%12d%12d",
                dates.get(i),
                result.get(0).get("confirmed").asList(Value::asInt).get(i),
                result.get(0).get("deaths").asList(Value::asInt).get(i)))
                .forEach(System.out::println);
    }

    public void list_by_time(String date) throws Exception {

        var index = dates.stream()
                .map(label -> LocalDate.parse(label, TIME_SERIES_FORMATTER))
                .collect(Collectors.toList())
                .indexOf(LocalDate.parse(date, TIME_SERIES_FORMATTER));
        if (index == -1) {
            throw new SpecifiedDateNotExistsException(String.format(
                    "SpecifiedDateNotExistsException - Specified: %s,\nCurrent: %s",
                    date, metadata.getTimeSeries_dates()));
        }
        var result = runCypher("MATCH (n:Country) RETURN n;", null,
                r -> Map.entry(
                        r.get("n").get("name", ""),
                        List.of(
                                r.get("n").get("confirmed", List.of(), Value::asInt).get(index),
                                r.get("n").get("deaths", List.of(), Value::asInt).get(index))));

        var width = result.stream().mapToInt(r -> r.getKey().length()).max().getAsInt() + 4;
        System.out.println("Specified Date: " + date);
        System.out.printf("%" + width + "s%12s%12s\n", "Country", "Confirmed", "Deaths");
        System.out.printf("%" + width + "s%12s%12s\n", "-------", "---------", "------");
        var n = new AtomicInteger(0);
        result.stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().get(0), Comparator.reverseOrder()))
                .forEach(e -> System.out.printf("%3d.%" + (width - 4) + "s%12s%12s\n",
                n.incrementAndGet(), e.getKey(), e.getValue().get(0), e.getValue().get(1)));
    }

}
