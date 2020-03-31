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
package info.muspoe.c1730.neo4j.wuhan;

import info.muspoe.c1730.neo4j.WuhanVirus;
import info.muspoe.c1730.neo4j.vo.DeathRate;
import info.muspoe.c1730.neo4j.vo.Metadata;
import info.muspoe.c1730.neo4j.vo.Record_DailyReport;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.driver.Values;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class CSSEGISandData_DailyReport extends CSSEGISandData {

    public static final String URL_DAILY_REPORT
            = URL_BASE + "csse_covid_19_daily_reports/%s.csv";
    public static final Function<String, String> URL = (date)
            -> String.format(URL_DAILY_REPORT, date);
    LocalDate dailyReport_updated_date;

    public CSSEGISandData_DailyReport() throws Exception {

        this.dailyReport_updated_date = LocalDate.now();
        do {
            this.dailyReport_updated_date = this.dailyReport_updated_date.minusDays(1);
        } while (!WuhanVirus.test(URL.apply(
                dailyReport_updated_date.format(DAILY_REPORT_FORMATTER))));
    }

    public boolean updateRequired() {
        Logger.getGlobal().info("updateRequired");

        try {
            metadata = new Metadata(runSingle("MATCH (n:Metadata) RETURN n;", null));
            var country_number = runSingle("MATCH (n:Country) RETURN count(n) AS num;", null)
                    .get("num").asInt();
            return (this.dailyReport_updated_date.isAfter(metadata.getDailyReport_updated_date()) || country_number < 100);
        } catch (Exception ex) {
            return true;
        }

    }

    @Override
    public void update_data() {
        Logger.getGlobal().info("update_data");

        try {
            reset_graph();
            update_metadata();
            update_daily_report_data(dailyReport_updated_date.format(DAILY_REPORT_FORMATTER));
        } catch (Exception ex) {
            Logger.getLogger(CSSEGISandData_DailyReport.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void update_metadata() {
        Logger.getGlobal().info("update_metadata");

        var daily_report_updated_date = this.dailyReport_updated_date.format(DAILY_REPORT_FORMATTER);
        var params = Values.parameters(
                "daily_report_uri", URL.apply(daily_report_updated_date),
                "daily_report_updated_date", daily_report_updated_date
        );
        metadata = run("""
                  MERGE (n:Metadata) 
                  SET n.daily_report_uri=$daily_report_uri,
                      n.daily_report_updated_date=$daily_report_updated_date
                  RETURN n;""", params, Metadata::new).get(0);
    }

    @Deprecated
    public void update_daily_report_data(String date) throws Exception {
        Logger.getGlobal().info("update_daily_report_data");

        var url = URL.apply(date);

        Logger.getGlobal().log(Level.INFO, "Loading data from {0}", url);
        var params = Values.parameters("url", url);
        List<Record_DailyReport> values = run("""
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
                runSingle(
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
                var c = runSingle(cypher, params).get("n");
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
                runSingle(
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
        List<DeathRate> result = run(query, null, DeathRate::new);
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
