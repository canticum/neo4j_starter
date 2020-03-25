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

import org.neo4j.driver.Record;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class NovelCOVIDValue {

    String country;
    int recovered, cases, critical, active, casesPerOneMillion, deaths, todayCases, todayDeaths;

    public NovelCOVIDValue(Record record) {
        var r = record.get("value");
        this.country = r.get("country", "");
        this.recovered = r.get("recovered", 0);
        this.cases = r.get("cases", 0);
        this.critical = r.get("critical", 0);
        this.active = r.get("active", 0);
        this.casesPerOneMillion = r.get("casesPerOneMillion", 0);
        this.deaths = r.get("deaths", 0);
        this.todayCases = r.get("todayCases", 0);
        this.todayDeaths = r.get("todayDeaths", 0);
    }

    public String getCountry() {
        return country;
    }

    public int getRecovered() {
        return recovered;
    }

    public int getCases() {
        return cases;
    }

    public int getCritical() {
        return critical;
    }

    public int getActive() {
        return active;
    }

    public int getCasesPerOneMillion() {
        return casesPerOneMillion;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getTodayCases() {
        return todayCases;
    }

    public int getTodayDeaths() {
        return todayDeaths;
    }

    @Override
    public String toString() {
        return "{" + "country=" + country + ", recovered=" + recovered + ", cases=" + cases + ", critical=" + critical + ", active=" + active + ", casesPerOneMillion=" + casesPerOneMillion + ", deaths=" + deaths + ", todayCases=" + todayCases + ", todayDeaths=" + todayDeaths + '}';
    }
}
