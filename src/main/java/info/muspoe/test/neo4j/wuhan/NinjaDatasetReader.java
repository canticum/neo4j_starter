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

import info.muspoe.test.neo4j.ex.SpecifiedDateNotExistsException;
import info.muspoe.test.neo4j.vo.NinjaValue;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.neo4j.driver.Values;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class NinjaDatasetReader extends WuhanVirus {

    public static final String URL_NINJA
            = "https://corona.lmao.ninja/countries";

    public void list_ninja() {

        var params = Values.parameters("url", URL_NINJA);
        var query = """
                    WITH $url AS url
                    CALL apoc.load.json(url)
                    YIELD value RETURN value;""";
        var result = runCypher(query, params, NinjaValue::new);
        var width = result.stream().mapToInt(r -> r.getCountry().length()).max().getAsInt() + 4;
        System.out.printf("%" + width + "s%7s%10s%6s\n", "Country", "Cases", "Cases/Mil", "Death");
        System.out.printf("%" + width + "s%7s%10s%6s\n", "-------", "-----", "---------", "-----");
        var n = new AtomicInteger(0);
        result.stream()
                .sorted(Comparator.comparing(NinjaValue::getCasesPerOneMillion, Comparator.reverseOrder()))
                .forEach(r -> System.out.printf("%3d.%" + (width - 4) + "s%7d%10d%6d\n", n.incrementAndGet(),
                r.getCountry(), r.getCases(), r.getCasesPerOneMillion(), r.getDeaths()));
    }
}
