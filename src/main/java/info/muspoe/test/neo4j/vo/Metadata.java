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

import static info.muspoe.test.neo4j.wuhan.CSSEGISandDataReader.DAILY_REPORT_FORMATTER;
import java.time.LocalDate;
import java.util.Map;
import java.util.logging.Logger;
import org.neo4j.driver.Record;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class Metadata {

    String daily_report_uri, timeseries_confirmed_uri, timeseries_deaths_uri;
    LocalDate updated_date;

    public Metadata(Record record) {
        Logger.getGlobal().info("Metadata");

        var r = record.get("n");
        this.daily_report_uri = r.get("daily_report_uri", "");
        this.timeseries_confirmed_uri = r.get("timeseries_confirmed_uri", "");
        this.timeseries_deaths_uri = r.get("timeseries_deaths_uri", "");
        var updated = r.get("updated_date", "");
        if (!updated.isEmpty()) {
            this.updated_date = LocalDate.parse(updated, DAILY_REPORT_FORMATTER);
        }
    }

    public Map<String, String> getTimeseries_uris() {
        return Map.of(
                "confirmed", this.timeseries_confirmed_uri,
                "deaths", this.timeseries_deaths_uri
        );
    }

    public String getDaily_report_uri() {
        return daily_report_uri;
    }

    public String getTimeseries_confirmed_uri() {
        return timeseries_confirmed_uri;
    }

    public String getTimeseries_deaths_uri() {
        return timeseries_deaths_uri;
    }

    public LocalDate getUpdated_date() {
        return updated_date;
    }

    public void setUpdated_date(LocalDate updated_date) {
        this.updated_date = updated_date;
    }

    @Override
    public String toString() {
        return "Metadata{" + "daily_report_uri=" + daily_report_uri
                + ", timeseries_confirmed_uri=" + timeseries_confirmed_uri
                + ", timeseries_deaths_uri=" + timeseries_deaths_uri
                + ", updated_date=" + updated_date + '}';
    }

}
