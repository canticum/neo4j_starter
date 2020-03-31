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

import org.neo4j.driver.Record;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class DeathRate implements Comparable<DeathRate> {

    String name;
    int confirmed, deaths;
    double death_rate;

    public DeathRate(Record r) {

        this.name = r.get("name").asString();
        this.confirmed = r.get("confirmed").asInt();
        this.deaths = r.get("deaths").asInt();
        this.death_rate = (this.confirmed > 0) ? 100.0 * this.deaths / this.confirmed : 0.0;
    }

    public String getName() {
        return name;
    }

    public int getConfirmed() {
        return confirmed;
    }

    public int getDeaths() {
        return deaths;
    }

    public double getDeath_rate() {
        return death_rate;
    }

    @Override
    public int compareTo(DeathRate o) {

        return Integer.compare(o.confirmed, this.confirmed);
    }
}
