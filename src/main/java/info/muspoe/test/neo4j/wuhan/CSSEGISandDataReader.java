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
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import info.muspoe.test.neo4j.ex.SpecifiedDateNotExistsException;
import info.muspoe.test.neo4j.vo.*;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class CSSEGISandDataReader extends WuhanVirus {

    public static final int DAILY_REPORT = 0;
    public static final int TIME_SERIES_CONFIRMED = 1;
    public static final int TIME_SERIES_DEATHS = 2; //SERIES_
    public static final int TIME_SERIES_RECOVERED = 3; //SERIES_

    public static final String URL_BASE
            = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/";
    public static final String URL_DAILY_REPORT
            = URL_BASE + "csse_covid_19_daily_reports/%s.csv";
    public static final String URL_TIME_SERIES
            = URL_BASE + "csse_covid_19_time_series/time_series_covid19_%s_global.csv";

    public static final DateTimeFormatter DAILY_REPORT_FORMATTER
            = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    public static final DateTimeFormatter TIME_SERIES_FORMATTER
            = DateTimeFormatter.ofPattern("M/d/yy");

    LocalDate date;
    Metadata metadata;

    public CSSEGISandDataReader() throws Exception {

        this.date = LocalDate.now();
        do {
            this.date = this.date.minusDays(1);
        } while (!WuhanVirus.test_file(String.format(URL_DAILY_REPORT, date.format(DAILY_REPORT_FORMATTER))));
        if (updateRequired()) {
            update_data();
        }
    }

    private boolean updateRequired() {
        Logger.getGlobal().info("updateRequired");

        try {
            this.metadata = runCypher("MATCH (n:Metadata) RETURN n;", null, Metadata::new).get(0);
            var country_number = runCypherSingle("MATCH (n:Country) RETURN count(n) AS num;", null)
                    .get("num").asInt();
            return (this.date.isAfter(this.metadata.getUpdated_date()) || country_number < 100);
        } catch (Exception ex) {
            return true;
        }
    }

    private void update_data() throws Exception {
        Logger.getGlobal().info("update_data");

        reset_graph();
        update_metadata();
        update_time_series_data();
//        update_daily_report_data(date.format(DAILY_REPORT_FORMATTER));
    }

    private void update_metadata() {
        Logger.getGlobal().info("update_metadata");

        var date_string = this.date.format(DAILY_REPORT_FORMATTER);
        var params = Values.parameters(
                "daily_report_uri", String.format(URL_DAILY_REPORT, date_string),
                "timeseries_confirmed_uri", String.format(URL_TIME_SERIES, "confirmed"),
                "timeseries_deaths_uri", String.format(URL_TIME_SERIES, "deaths"),
                "updated_date", date_string
        );
        this.metadata = runCypher("""
                  MERGE (n:Metadata) 
                  SET n.daily_report_uri=$daily_report_uri,
                      n.timeseries_confirmed_uri=$timeseries_confirmed_uri,
                      n.timeseries_deaths_uri=$timeseries_deaths_uri,
                      n.updated_date=$updated_date
                  RETURN n;""", params, Metadata::new).get(0);
    }

    public void update_time_series_data() {
        Logger.getGlobal().info("update_time_series_data");

        // fetch time series data and store in Record_TimeSeries list.
        var data = Stream.of("confirmed", "deaths")
                .collect(Collectors.toMap(
                        type -> type,
                        type -> runCypher("LOAD CSV WITH HEADERS FROM $url AS line RETURN line;",
                                Values.parameters("url", this.metadata.getTimeseries_uris().get(type)),
                                Record_TimeSeries::new)));
        Logger.getGlobal().log(Level.INFO, "{0} records loaded.",
                data.values().stream().mapToInt(List::size).sum());

        // get date labels
        var dates = data.get("confirmed").get(0).getTimeSeriesData().entrySet().stream()
                .sorted(Entry.comparingByKey())
                .map(Entry::getKey)
                .map(d -> d.format(TIME_SERIES_FORMATTER))
                .collect(Collectors.toList());
        var params = Values.parameters("dates", dates);
        runCypher("MATCH (n:Metadata) SET n.time_series_dates=$dates;", params, null);

        // sum-up country data        
        var data_by_country = data.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(rec -> Map.entry(entry.getKey(), rec)))
                .map(entry -> Map.entry(entry.getValue().getCountry(), Map.entry(entry.getKey(), entry.getValue())))
                .collect(Collectors.toList());

        for (var entry : data_by_country) {
            var country = entry.getKey();
            var province = entry.getValue().getValue().getProvince();
            var type = entry.getValue().getKey().toLowerCase();
            var time_series_data = entry.getValue()
                    .getValue().getTimeSeriesData().entrySet().stream()
                    .sorted(Entry.comparingByKey())
                    .map(Entry::getValue)
                    .collect(Collectors.toList());
            params = Values.parameters(
                    "country", country,
                    "province", province,
                    "type", type,
                    "values", time_series_data);
            if (province == null) {
                // Country
                var cypher = """
                             MERGE (n:Country{name:$country}) WITH n
                             CALL apoc.create.setProperty(n,$type,$values)
                             YIELD node
                             RETURN node;""";
                runCypherSingle(cypher, params);
                Logger.getGlobal().log(Level.INFO, "Country {0} updated.", country);
            } else {
                // Province  
                var cypher = """
                             MERGE (n:Country{name:$country})
                             CREATE (m:Province{name:$province})-[:IS_PROVINCE_OF]->(n)
                             WITH n,m
                             CALL apoc.create.setProperty(m,$type,$values) 
                             YIELD node
                             RETURN n;""";
                var r = runCypherSingle(cypher, params)
                        .get("n").get(type, List.of(), Value::asInt);
                Logger.getGlobal().log(Level.INFO, "Province {0} created.", province);

                var updated_time_series_data = (r.isEmpty())
                        ? time_series_data
                        : IntStream.range(0, time_series_data.size())
                                .map(i -> time_series_data.get(i) + r.get(i))
                                .boxed().collect(Collectors.toList());
                params = Values.parameters(
                        "country", country,
                        "type", type,
                        "values", updated_time_series_data);
                cypher = """
                         MATCH (n:Country{name:$country}) 
                         CALL apoc.create.setProperty(n,$type,$values) 
                         YIELD node
                         RETURN node;""";
                runCypherSingle(cypher, params);
                Logger.getGlobal().log(Level.INFO, "Country {0} updated.", country);
            }
        }
    }

    public void list_by_time(String date) throws Exception {

        var index = runCypherSingle("""
                                    MATCH(n:Metadata)
                                    RETURN n.time_series_dates AS dates;""", null)
                .get("dates").asList(Value::asString).stream()
                .map(label -> LocalDate.parse(label, TIME_SERIES_FORMATTER))
                .collect(Collectors.toList())
                .indexOf(LocalDate.parse(date, TIME_SERIES_FORMATTER));
        if (index == -1) {
            throw new SpecifiedDateNotExistsException(date);
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

    @Deprecated
    public void update_daily_report_data(String date) throws Exception {
        Logger.getGlobal().info("update_daily_report_data");

        var url = String.format(URL_DAILY_REPORT, date);

        Logger.getGlobal().log(Level.INFO, "Loading data from {0}", url);
        var params = Values.parameters("url", url);
        List<Record_DailyReport> values = runCypher("""
                LOAD CSV WITH HEADERS
                FROM $url AS line
                RETURN line;""", params, Record_DailyReport::new);
        Logger.getGlobal().log(Level.INFO, "{0} records loaded.", values.size());

        for (var record : values) {
            params = Values.parameters(
                    "cname", record.getCountry(),
                    "pname", record.getProvince(),
                    "confirmed", record.getConfirmed(),
                    "deaths", record.getDeaths(),
                    "recovered", record.getRecovered());
            if (record.getProvince() == null) {
                // Country
                runCypherSingle(
                        """
                        MERGE (n:Country{name:$cname})
                        SET n.dailyreport_confirmed=$confirmed, 
                            n.dailyreport_deaths=$deaths, 
                            n.dailyreport_recovered=$recovered
                        RETURN n;""", params);
                Logger.getGlobal().log(Level.INFO,
                        "Country {0} created, with confirmed={1}, deaths={2}, recovered={3}.",
                        new Object[]{record.getName(), record.getConfirmed(), record.getDeaths(), record.getRecovered()});
            } else {
                // Province
                var cypher = """
                        MERGE (n:Country{name:$cname})
                        CREATE (m:Province{name:$pname})-[:IS_PROVINCE_OF]->(n)
                        SET m.dailyreport_confirmed=$confirmed, 
                            m.dailyreport_recovered=$recovered,
                            m.dailyreport_deaths=$deaths
                        RETURN n;""";
                var c = runCypherSingle(cypher, params).get("n");
                Logger.getGlobal().log(Level.INFO,
                        "Province {0} created, with confirmed={1}, deaths={2}, recovered={3}.",
                        new Object[]{record.getProvince(), record.getConfirmed(), record.getDeaths(), record.getRecovered()});
                var confirmed = record.getConfirmed() + c.get("confirmed", 0);
                var deaths = record.getDeaths() + c.get("deaths", 0);
                var recovered = record.getRecovered() + c.get("recovered", 0);
                params = Values.parameters(
                        "name", record.getCountry(),
                        "confirmed", confirmed,
                        "deaths", deaths,
                        "recovered", recovered);
                runCypherSingle(
                        """
                        MATCH (n:Country{name:$name})
                        SET n.dailyreport_confirmed=$confirmed, 
                            n.dailyreport_deaths=$deaths,
                            n.dailyreport_recovered=$recovered
                        RETURN n;""", params);
                Logger.getGlobal().log(Level.INFO,
                        "Country {0} updated, with confirmed={1}, deaths={2}, recovered={3}.",
                        new Object[]{record.getCountry(), confirmed, deaths, recovered});
            }
        }
    }

    @Deprecated
    public void list_confirmed(String country) throws Exception {
        Logger.getGlobal().info("list_confirmed(country)");

        var prefix = String.format("MATCH (n)-->(:Country{name:'%s'})", country);
        this.list_confirmed(prefix, "Province");
    }

    @Deprecated
    public void list_confirmed() throws Exception {
        Logger.getGlobal().info("list_confirmed()");

        var prefix = "MATCH (n:Country)";
        this.list_confirmed(prefix, "Country");

    }

    @Deprecated
    private void list_confirmed(String query_prefix, String title) {
        Logger.getGlobal().info("list_confirmed");

        var query = query_prefix + """
                                   RETURN
                                        n.name AS name, 
                                        n.confirmed AS confirmed, 
                                        n.deaths AS deaths;""";
        System.out.println(query);
        List<DeathRate> result = runCypher(query, null, DeathRate::new);
        var nw = result.stream().mapToInt(r -> r.getName().length()).max().getAsInt() + 4;
        var tw = title.length();
        System.out.printf("%" + nw + "s%11s%8s  %s\n", title,
                "Confirmed", "Deaths", "Death Rate");
        System.out.printf("%" + nw + "s%11s%8s  %s\n", "-".repeat(tw),
                "---------", "------", "----------");
        var n = new AtomicInteger(0);
        result.stream().sorted()
                .forEach(r -> System.out.printf("%3d.%" + (nw - 4) + "s%11d%8d%7.1f%%\n",
                n.incrementAndGet(), r.getName(), r.getConfirmed(), r.getDeaths(), r.getDeath_rate()));
    }
}
