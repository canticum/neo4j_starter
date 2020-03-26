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

import info.muspoe.test.neo4j.vo.Metadata;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.logging.Logger;
import org.neo4j.driver.Query;
import org.neo4j.driver.Values;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public abstract class CSSEGISandData extends WuhanVirus {

    public static final String URL_BASE
            = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/";

    public static String[] LABELS = {"Country", "Province", "Metadata"};

    public static final DateTimeFormatter DAILY_REPORT_FORMATTER
            = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    static Metadata metadata;

    abstract void update_data();

    public void reset_graph() {

        reset_graph(LABELS);
    }

    public void reset_graph(String... labels) {
        Logger.getGlobal().info("reset_graph");

        var params = Values.parameters("labels", Arrays.asList(labels));
        var query = new Query("""
                        MATCH(n)
                        UNWIND labels(n) AS label
                        WITH n, label
                        WHERE label IN $labels
                        DETACH DELETE n;
                        """, params);
        run(query);
    }
}
