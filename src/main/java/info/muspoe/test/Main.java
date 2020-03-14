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
package info.muspoe.test;

import info.muspoe.test.neo4j.SevenBridges;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        var time = (args.length > 0 && args[0].trim().toUpperCase().equals("TODAY"))
                ? SevenBridges.TODAY : SevenBridges.EULAR;

        try ( var sb = new SevenBridges()) {
            System.out.println("Time=" + (time == SevenBridges.EULAR ? "EULAR" : "TODAY"));
            sb.reset_graph();
            sb.create_graph(time);
            var num = sb.countBridges();
            IntStream.rangeClosed(1, num)
                    .mapToObj(
                            n -> String.format("* %d bridges, start from:\n%s", n,
                                    Stream.of("left", "up", "down", "right")
                                            .map(p -> String.format("\t%-6s =%4d%n", p.toUpperCase(), sb.path_count(p, n)))
                                            .collect(Collectors.joining())))
                    .forEach(System.out::println);
        }
    }
}
