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
import org.neo4j.driver.Record;

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

    public static void main(String[] args) throws Exception {

        try (var instance = new CSSEGISandData_TimeSeries()) {
            instance.reset_graph();
            instance.update_data();
        }
    }

    /**
     * Constructor.
     *
     * @throws Exception
     */
    public CSSEGISandData_TimeSeries() throws Exception {

        // 讀入遠端資料
        data = Arrays.stream(TYPES)
                .collect(Collectors.toMap(type -> type,
                        type -> run("LOAD CSV WITH HEADERS FROM $url AS line RETURN line;",
                                Values.parameters("url", URL.apply(type)),
                                Record_TimeSeries::new)));
        Logger.getGlobal().log(Level.INFO, "{0} records loaded.",
                data.values().stream().mapToInt(List::size).sum());

        metadata = run("MERGE (n:Metadata) RETURN n;", null, Metadata::new).get(0);
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
            var md = new Metadata(runSingle(
                    "MERGE (n:Metadata) RETURN n;", null));
            var country_number = runSingle(
                    "MATCH (n:Country) RETURN count(n) AS num;", null)
                    .get("num").asInt();
            return (!dates.equals(md.getTimeSeries_dates()) || country_number < 100);
        } catch (Exception ex) {
            return true;
        }
    }

    @Override
    public void update_data() {
        Logger.getGlobal().info("update_data");

        update_metadata();
        update_time_series_data();
    }

    private void update_metadata() {
        Logger.getGlobal().info("update_metadata");

        var params = Values.parameters(
                "timeSeries_confirmed_uri", URL.apply("confirmed"),
                "timeSeries_deaths_uri", URL.apply("deaths"),
                "timeSeries_dates", dates
        );
        metadata = run(cypher_metadata, params, Metadata::new).get(0);
    }

    List<Query> queries;

    public void update_time_series_data() {
        Logger.getGlobal().info("update_time_series_data");

        // 以國家為單位重整
        System.out.println("Reorganize data by country");
        var data_by_country = data.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(rec -> Map.entry(entry.getKey(), rec)))
                .map(entry -> Map.entry(entry.getValue().getCountry(), Map.entry(entry.getKey(), entry.getValue())))
                .collect(Collectors.toList());
        System.out.println("data_by_country.size()=" + data_by_country.size());

        queries = new ArrayList<>();

        //將讀入的國家或省份建入資料庫
        System.out.println("Build the read-in country or province into the database");
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
        runTx(queries);
        System.out.println("\n" + queries.size() + " queries executed.");

        // for debug
        check_countries_with_provinces();

        //讀入所有省份，預備加總
        System.out.println("Find all provinces and prepare to add up");
        var provinces = run(cypher_all_provinces, null,
                r -> Map.entry(r.get("country").asString(), r.get("province")));
        System.out.println(provinces.get(0) + "=" + provinces.get(0).getKey().length());
        //讀入國家節點上已有數據的，防止覆寫
        System.out.println("Find out the existing data on the national node and prevent overwriting");
        Map<String, Map<String, List<Integer>>> countries_with_data = new HashMap<>();
        run(cypher_country_with_data, null, this::recordEntry)
                .stream()
                .filter(entry -> !entry.getValue().get("confirmed").isEmpty())
                .forEach(entry -> countries_with_data.put(entry.getKey(), entry.getValue()));

        System.out.println("country_updated.size() = " + countries_with_data.size());

        //將省份數據加總入國家數據
        System.out.println("Add province data to national data");
        for (Entry<String, Value> entry : provinces) {
            var country = entry.getKey();
            var confirmed = entry.getValue().get("confirmed").asList(Value::asInt);
            var deaths = entry.getValue().get("deaths").asList(Value::asInt);
            var province = Map.of("confirmed", confirmed, "deaths", deaths);
            countries_with_data.merge(country, province, (o, n) -> Map.ofEntries(
                    sum("confirmed", o, n),
                    sum("deaths", o, n)));
        }
        System.out.println(provinces.size() + " provinces being processed.");
        System.out.println(countries_with_data.size() + " countries being processed.");

        queries = new ArrayList<>();

        //將加總後的國家數據寫入
        System.out.println("--------------------UPDATING COUNTRIES FROM THEIR PROVINCES...");
//        for (var entry : countries_with_data.entrySet()) {
//            var country = entry.getKey();
//            var confirmed = entry.getValue().get("confirmed");
//            var deaths = entry.getValue().get("deaths");
//            var params = Values.parameters(
//                    "country", country, "confirmed", confirmed, "deaths", deaths);
//            System.out.println(params);
//            var result = run(cypher_update_country, params, this::recordEntry);
//            System.out.println(result);
//        }
        countries_with_data.entrySet().stream()
                .map(entry
                        -> Values.parameters(
                        "country", entry.getKey(),
                        "confirmed", entry.getValue().get("confirmed"),
                        "deaths", entry.getValue().get("deaths")))
                .map(params -> new Query(cypher_update_country, params))
                .peek(System.out::println)
                .forEach(queries::add);
        runTx(queries);
        System.out.println(queries.size() + " queries executed.");
        // 結束值檢查
        check_countries_with_provinces();
    }

    Entry<String, Map<String, List<Integer>>> recordEntry(Record r) {

        return Map.entry(r.get("name").asString(),
                Map.of("confirmed", r.get("confirmed", List.of(), Value::asInt),
                        "deaths", r.get("deaths", List.of(), Value::asInt)));
    }

    <K> Entry<K, List<Integer>> sum(K key, Map<K, List<Integer>> m1, Map<K, List<Integer>> m2) {

        return Map.entry(
                key,
                IntStream.range(0, m1.get(key).size())
                        .map(i -> m1.get(key).get(i) + m2.get(key).get(i))
                        .boxed()
                        .collect(Collectors.toList()));
    }

    private void check_countries_with_provinces() {
        System.out.println("--------------------check_countries_with_provinces");

        var query = "MATCH(n:Country)--() RETURN DISTINCT n;";
        var list = run(query, null, (Record rec) -> Map.entry(
                rec.get("n").get("name").asString(),
                rec.get("n").get("confirmed").isNull() + "," + rec.get("n").get("deaths").isNull()));
        list.forEach(System.out::println);
    }

    public void list_by_country(String country) {

        var params = Values.parameters("country", country);
        var result = run(
                """
                MATCH(n:Country{name:$country})
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
        var result = run("MATCH (n:Country) RETURN n;", null,
                r -> Map.entry(
                        r.get("n").get("name").asString(),
                        List.of(
                                r.get("n").get("confirmed", List.of(), Value::asInt).get(index),
                                r.get("n").get("deaths", List.of(), Value::asInt).get(index)))
        );

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

    static final String cypher_metadata
            = """
              MERGE (n:Metadata)
              SET n.timeSeries_confirmed_uri=$timeSeries_confirmed_uri,
                  n.timeSeries_deaths_uri=$timeSeries_deaths_uri,
                  n.timeSeries_dates=$timeSeries_dates
              RETURN n;""";
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
              CALL apoc.create.setProperty(m,$type,$values) YIELD node
              RETURN n;""";
    static final String cypher_all_provinces
            = """
              MATCH (n:Province)-->(m)              
              RETURN n AS province, m.name AS country;""";
    static final String cypher_country_with_data
            = """
              MATCH (n:Country)<--()              
              RETURN n.name AS name, n.confirmed AS confirmed, n.deaths AS deaths;""";
    static final String cypher_update_country
            = """
              MATCH(n:Country{name:$country})
              SET n.confirmed=$confirmed, n.deaths=$deaths
              RETURN n.name AS name, n.confirmed AS confirmed, n.deaths AS deaths;""";
}
