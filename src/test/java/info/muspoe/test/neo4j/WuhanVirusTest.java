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
package info.muspoe.test.neo4j;

import java.io.FileNotFoundException;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class WuhanVirusTest {

    static WuhanVirus instance;

    @BeforeAll
    public static void setUpClass() {

        System.out.println("******************");
        System.out.println(" Wuhan Virus Data ");
        System.out.println("******************");
        instance = new WuhanVirus();
    }

    @AfterAll
    public static void tearDownClass() throws Exception {

        instance.close();
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
    }

    /**
     * Test of reset_graph method, of class WuhanVirus.
     */
    @Test
//    @Disabled
    public void testCreate_graph_time() {
        System.out.println("create_graph_time");

        try {
            instance.create_graph(WuhanVirus.TIME_CONFIRMED);
        } catch (FileNotFoundException ex) {
            Logger.getGlobal().severe("ERROR - Specified dataset not exists.");
        } catch (Exception ex) {
            Logger.getGlobal().severe("ERROR when fetching specified dataset.");
        }
    }

    /**
     * Test of create_graph method, of class WuhanVirus.
     */
    @Test
    //@Disabled
    public void testCreate_graph_daily() {
        System.out.println("create_graph");

        try {
            instance.create_graph(WuhanVirus.DAILY, "03-19-2020");
        } catch (FileNotFoundException ex) {
            Logger.getGlobal().severe("ERROR - Specified dataset not exists.");
        } catch (Exception ex) {
            Logger.getGlobal().severe("ERROR when fetching specified dataset.");
        }
    }

    @Test
    public void testList_by_time() throws Exception {
        System.out.println("list_by_time");
        
        var date = "03/19/20";
        instance.list_by_time(date);
    }

    /**
     * Test of list_ninja method, of class WuhanVirus.
     */
    @Test
    public void testList_ninja() {
        System.out.println("list_ninja");

        instance.list_ninja();
    }

    /**
     * Test of province_death_rate method, of class WuhanVirus.Based on
     * daily-graph pre-imported data.
     *
     * @throws java.lang.Exception
     */
    @Test
    //@Disabled
    public void testList_confirmed_String() throws Exception {
        System.out.println("list_confirmed");

        String country = "US";
        instance.list_confirmed(country);
    }

    /**
     * Test of country_death_rate method, of class WuhanVirus.Based on
     * daily-graph pre-imported data.
     *
     * @throws java.lang.Exception
     */
    @Test
    //@Disabled
    public void testList_confirmed_0args() throws Exception {
        System.out.println("list_confirmed");

        instance.list_confirmed();
    }
}
