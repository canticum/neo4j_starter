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

    public int count(String place, Integer num) {

        var params = Values.parameters("place", place, "num", num);
        try (Session session = driver.session()) {
            return session.run("""
                        MATCH (p)
                        WHERE p.position=$place
                        CALL apoc.path.expand(p, "", "", $num, $num)
                        YIELD path
                        RETURN count(path);
                        """, params)
                    .single()
                    .get("count(path)")
                    .asInt();
        }

    }

    @Override
    public void close() throws Exception {

        driver.close();
    }
}
