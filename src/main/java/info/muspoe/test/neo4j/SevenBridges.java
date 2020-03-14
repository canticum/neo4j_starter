package info.muspoe.test.neo4j;

import java.util.Properties;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class SevenBridges implements AutoCloseable {

    public static final int EULAR = 0;
    public static final int TODAY = 1;

    private Driver driver;

    public SevenBridges() {
        try {
            Properties pros = new Properties();
            pros.load(SevenBridges.class.getResourceAsStream("/neo4j.properties"));
            var uri = pros.getProperty("uri");
            var user = pros.getProperty("user");
            var password = pros.getProperty("password");
            driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
            driver.verifyConnectivity();
        } catch (Exception ex) {
            System.out.println("*******************************");
            System.out.println(" Neo4j initialization failed.");
            System.out.println("*******************************");
            driver.close();
            System.exit(0);
        }
    }

    public void reset_graph() {
        try (var session = driver.session()) {
            session.run("MATCH(n:Place) DETACH DELETE n;");
        }
    }

    public void create_graph(int n) {

        var query = switch (n) {
            case TODAY:
                yield """
                CREATE 
                    (left:Place{name:"left"}),
                    (right:Place{name:"right"}),
                    (up:Place{name:"up"}),
                    (down:Place{name:"down"}),
                    (left)-[:Bridge]->(up),
                    (left)-[:Bridge]->(down),
                    (left)-[:Bridge]->(right),
                    (right)-[:Bridge]->(up),
                    (right)-[:Bridge]->(down)
                RETURN left, right , up, down;
                """;
            case EULAR:
            default:
                yield """
                CREATE 
                    (left:Place{name:"left"}),
                    (right:Place{name:"right"}),
                    (up:Place{name:"up"}),
                    (down:Place{name:"down"}),
                    (left)-[:Bridge]->(up),
                    (up)-[:Bridge]->(left),
                    (left)-[:Bridge]->(down),
                    (down)-[:Bridge]->(left),
                    (left)-[:Bridge]->(right),
                    (right)-[:Bridge]->(up),
                    (right)-[:Bridge]->(down)
                RETURN left, right , up, down;
                """;
        };

        try (var session = driver.session()) {
            session.run(query);
        }
    }

    public int path_count(String place, Integer num) {

        var params = Values.parameters("place", place, "num", num);
        try (var session = driver.session()) {
            return session.run("""
                        MATCH (p)
                        WHERE p.name=$place
                        CALL apoc.path.expand(p, "", "", $num, $num)
                        YIELD path
                        RETURN count(path);
                        """, params)
                    .single()
                    .get("count(path)")
                    .asInt();
        }
    }

    public int numberOfBridges() {
        try (var session = driver.session()) {
            return session.run("""
                        MATCH ()-[bridge:Bridge]->()
                        RETURN count(bridge);
                        """)
                    .single()
                    .get("count(bridge)")
                    .asInt();
        }
    }

    @Override
    public void close() throws Exception {

        driver.close();
    }
}
