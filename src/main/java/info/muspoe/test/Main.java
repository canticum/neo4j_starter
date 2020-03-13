
package info.muspoe.test;

import info.muspoe.test.neo4j.SevenBridges;
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

        try (var sb = new SevenBridges()) {
            IntStream.rangeClosed(1, 7)
                    .peek(i -> System.out.printf("* Bridges: %d, start from:\n", i))
                    .forEach(i
                            -> Stream.of("left", "up", "down", "right")
                            .peek(p -> System.out.print("\t" + p.toUpperCase() + "\t= "))
                            .map(p -> sb.count(p, i))
                            .forEach(System.out::println));
        }
    }
}
