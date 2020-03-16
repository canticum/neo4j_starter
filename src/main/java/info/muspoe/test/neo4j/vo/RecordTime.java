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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.driver.Record;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class RecordTime extends Location {

    private Map<Date, Integer> data;

    public static final SimpleDateFormat date_formatter
            = new SimpleDateFormat("MM/dd/yy");

    public RecordTime(Record r) {
        
        var record = r.get("line");
        this.province = record.get("Province/State", (String) null);
        this.country = record.get("Country/Region", (String) null);
        this.latitude = Double.valueOf(record.get("Lat", "0.0"));
        this.longitude = Double.valueOf(record.get("Long", "0.0"));
        data = new HashMap<>();
        record.keys().forEach(key -> {
            if (key.matches("^\\d{1,2}\\/\\d{1,2}\\/\\d{2}$")) {
                try {
                    data.put(date_formatter.parse(key), Integer.valueOf(record.get(key, "0")));
                } catch (ParseException ex) {
                    Logger.getLogger(RecordTime.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        System.out.println("record created:" + this.getName());
    }

    public Map<Date, Integer> getData() {
        
        return data;
    }
}
