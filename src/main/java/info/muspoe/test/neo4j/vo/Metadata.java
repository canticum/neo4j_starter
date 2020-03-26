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
package info.muspoe.test.neo4j.vo;

import static info.muspoe.test.neo4j.wuhan.CSSEGISandData_TimeSeries.DAILY_REPORT_FORMATTER;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class Metadata {

    String dailyReport_uri, timeSeries_confirmed_uri, timeSeries_deaths_uri;
    LocalDate dailyReport_updated_date;
    List<String> timeSeries_dates;

    public Metadata(Record record) {

        var r = record.get("n");
        this.dailyReport_uri = r.get("dailyReport_uri", "");
        this.timeSeries_confirmed_uri = r.get("timeSeries_confirmed_uri", "");
        this.timeSeries_deaths_uri = r.get("timeSeries_deaths_uri", "");
        this.timeSeries_dates = r.get("timeSeries_dates", List.of(), Value::asString);
        var dru_date = r.get("dailyReport_updated_date", "");
        if (!dru_date.isEmpty()) {
            this.dailyReport_updated_date = LocalDate.parse(dru_date, DAILY_REPORT_FORMATTER);
        }
    }

    public Map<String, String> getTimeseries_uris() {
        return Map.of(
                "confirmed", this.timeSeries_confirmed_uri,
                "deaths", this.timeSeries_deaths_uri
        );
    }

    public String getDaily_report_uri() {
        return dailyReport_uri;
    }

    public String getTimeSeries_confirmed_uri() {
        return timeSeries_confirmed_uri;
    }

    public String getTimeSeries_deaths_uri() {
        return timeSeries_deaths_uri;
    }

    public List<String> getTimeSeries_dates() {
        return timeSeries_dates;
    }

    public LocalDate getDailyReport_updated_date() {
        return dailyReport_updated_date;
    }

    public void setDailyReport_updated_date(LocalDate dailyReport_updated_date) {
        this.dailyReport_updated_date = dailyReport_updated_date;
    }

    @Override
    public String toString() {
        return "Metadata{" + "dailyReport_uri=" + dailyReport_uri
                + ", timeSeries_confirmed_uri=" + timeSeries_confirmed_uri
                + ", timeSeries_deaths_uri=" + timeSeries_deaths_uri
                + ", dailyReport_updated_date=" + dailyReport_updated_date
                + ", timeSeries_dates=" + timeSeries_dates
                + '}';
    }

}
