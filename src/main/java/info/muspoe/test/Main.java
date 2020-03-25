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
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class Main {

    private static final int PORT = 11006;

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {

        var time = (args.length > 0 && args[0].trim().toUpperCase().equals("TODAY"))
                ? SevenBridges.TODAY : SevenBridges.EULAR;
        var port = (args.length > 1)
                ? Integer.parseInt(args[1].trim()) : PORT;

        System.out.println("***************************");
        System.out.println(" Königsberg Bridge Problem ");
        System.out.println("***************************");

        System.out.println("Time = "
                + (time == SevenBridges.EULAR ? "EULAR" : "TODAY"));

        try (var sb = new SevenBridges(port, "neo4j", "123456")) {

            sb.reset_graph();
            sb.create_graph(time);
            var bridge_num = sb.countBridges();
            System.out.println("Bridge number = " + bridge_num);

            IntStream.rangeClosed(1, bridge_num).mapToObj(
                    n -> Map.entry(n,
                            Stream.of("left", "up", "down", "right")
                                    .map(p -> Map.entry(p, sb.path_count(p, n)))
                                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue)))
            ).forEach(entry -> System.out.printf("* %d bridges, start from:\n%s", entry.getKey(),
                    entry.getValue().entrySet().stream()
                            .map(e -> String.format("\t%-6s =%4d%n", e.getKey(), e.getValue()))
                            .collect(Collectors.joining())
            ));
        }
    }
}
