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
import org.junit.jupiter.api.*;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class CSSEGISandData_TimeSeriesTest {

    static CSSEGISandData_TimeSeries instance;
    static boolean needUpdated;

    public CSSEGISandData_TimeSeriesTest() {
    }

    @BeforeAll
    public static void setUpClass() throws Exception {

        System.out.println("***********************************************");
        System.out.println(" Wuhan Virus CSSEGISandData TimeSeries DataSet ");
        System.out.println("***********************************************");
        instance = new CSSEGISandData_TimeSeries();
        needUpdated = instance.updateRequired();
        System.out.println("needUpdated = " + needUpdated);
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
     * Test of list_by_time method, of class CSSEGISandData_TimeSeries.
     *
     */
    @Test
    public void testUpdate_data() throws Exception {
        System.out.println("update_data");

        if (needUpdated) {
            instance.reset_graph();
            instance.update_data();
        }
    }

    /**
     * Test of list_by_country method, of class CSSEGISandData_TimeSeries.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testList_by_country() throws Exception {
        System.out.println("list_by_country");

        var country = "US";
//        var country = "Taiwan*";
        System.out.println("Country = " + country);
        instance.list_by_country(country);
    }

    /**
     * Test of list_by_time method, of class CSSEGISandData_TimeSeries.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testList_by_time() throws Exception {
        System.out.println("list_by_time");

        var date = "3/23/20";
        System.out.println("Date = " + date);
        try {
            instance.list_by_time(date);
        } catch (SpecifiedDateNotExistsException ex) {
            System.out.println(ex.getMessage());
        }
    }

}
