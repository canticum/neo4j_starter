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

import info.muspoe.test.neo4j.service.Neo4jService;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class WuhanVirus extends Neo4jService {

    public void reset_graph() {

        try (var session = driver.session()) {
            session.run("""
                        MATCH(n)
                        UNWIND labels(n) AS label
                        WITH n, label
                        WHERE label IN ['Country','Province','Metadata']
                        DETACH DELETE n;
                        """);
        }
    }

    public static boolean test_file(String url) throws Exception {

        Logger.getGlobal().log(Level.INFO, "Testing data from {0}", url);
        try {
            new URL(url).openStream().close();
        } catch (FileNotFoundException ex) {
            return false;
        }
        return true;
    }
}
