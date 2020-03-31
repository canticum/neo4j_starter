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
package info.muspoe.c1730.neo4j.vo;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.driver.Record;
import static info.muspoe.c1730.neo4j.wuhan.CSSEGISandData_TimeSeries.TIME_SERIES_FORMATTER;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class Record_TimeSeries extends Location {

    private Map<LocalDate, Integer> timeSeriesData;

    public Record_TimeSeries(Record record) {

        var r = record.get("line");
        this.province = r.get("Province/State", (String) null);
        this.country = r.get("Country/Region", (String) null);
        this.latitude = Double.valueOf(r.get("Lat", "0.0"));
        this.longitude = Double.valueOf(r.get("Long", "0.0"));
        this.timeSeriesData = new HashMap<>();
        r.keys().forEach(key -> {
            if (key.matches("^\\d{1,2}\\/\\d{1,2}\\/\\d{2}$")) {
                var num = Integer.valueOf(r.get(key, "0"));
                var date = LocalDate.parse(key, TIME_SERIES_FORMATTER);
                timeSeriesData.put(date, num);
            }
        });
//        System.out.println("record created: " + this.getName());
    }

    public Map<LocalDate, Integer> getTimeSeriesData() {

        return timeSeriesData;
    }

    @Override
    public String toString() {
     
        return "Record_TimeSeries{" + "timeSeriesData=" + timeSeriesData + '}';
    }
}
