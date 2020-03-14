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

        var time = switch (args.length == 0 ? "EULAR" : args[0]) {
            case "TODAY" ->
                SevenBridges.TODAY;
            default ->
                SevenBridges.EULAR;
        };

        try (var sb = new SevenBridges()) {
            System.out.println("Time=" + (time == SevenBridges.EULAR ? "EULAR" : "TODAY"));
            sb.reset_graph();
            sb.create_graph(time);
            var n = sb.numberOfBridges();
            IntStream.rangeClosed(1, n)
                    .peek(i -> System.out.printf("* Bridges: %d, start from:\n", i))
                    .forEach(i
                            -> Stream.of("left", "up", "down", "right")
                            .peek(p -> System.out.print("\t" + p.toUpperCase() + "\t= "))
                            .map(p -> sb.path_count(p, i))
                            .forEach(System.out::println));
        }
    }
}
