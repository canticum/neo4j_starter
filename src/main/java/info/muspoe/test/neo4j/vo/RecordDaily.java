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

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.driver.Record;

public class RecordDaily extends Location {

    private int recovered, deaths, confirmed;
    private Date lastUpdate;

    public static final SimpleDateFormat date_formatter
            = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //Format: 2020-03-14T10:13:09

    public RecordDaily(Record r) {

        try {
            var record = r.get("line");
            this.province = record.get("Province/State", (String) null);
            this.country = record.get("Country/Region", (String) null);
            this.lastUpdate = date_formatter.parse(
                    record.get("Last Update", "").replace("T", " "));
            this.confirmed = Integer.valueOf(record.get("Confirmed", "0"));
            this.deaths = Integer.valueOf(record.get("Deaths", "0"));
            this.recovered = Integer.valueOf(record.get("Recovered", "0"));
            this.latitude = Double.valueOf(record.get("Latitude", "0.0"));
            this.longitude = Double.valueOf(record.get("Longitude", "0.0"));
            System.out.println("record created:" + this.getName());
        } catch (ParseException ex) {
            Logger.getLogger(RecordDaily.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getRecovered() {
        return recovered;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getConfirmed() {
        return confirmed;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }
}
