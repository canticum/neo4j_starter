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

import static info.muspoe.test.neo4j.wuhan.CSSEGISandDataReader.DAILY_REPORT_FORMATTER;
import org.junit.jupiter.api.*;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class CSSEGISandDataReaderTest {

    static CSSEGISandDataReader instance;

    public CSSEGISandDataReaderTest() {
    }

    @BeforeAll
    public static void setUpClass() throws Exception {

        System.out.println("*******************");
        System.out.println("    Wuhan Virus    ");
        System.out.println(" CSSEGISandDataSet ");
        System.out.println("*******************");
        instance = new CSSEGISandDataReader();
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
     * Test of update_daily_report_data method, of class CSSEGISandDataReader.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Disabled
    public void testUpdate_daily_report_data() throws Exception {
        System.out.println("update_daily_report_data");

        instance.update_daily_report_data(instance.date.format(DAILY_REPORT_FORMATTER));
    }

    /**
     * Test of list_confirmed method, of class CSSEGISandDataReader.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Disabled
    public void testList_confirmed_String() throws Exception {
        System.out.println("list_confirmed");

        String country = "US";
        instance.list_confirmed(country);
    }

    /**
     * Test of list_confirmed method, of class CSSEGISandDataReader.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Disabled
    public void testList_confirmed_0args() throws Exception {
        System.out.println("list_confirmed");

        instance.list_confirmed();
    }

    /**
     * Test of list_by_time method, of class CSSEGISandDataReader.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testList_by_time() throws Exception {
        System.out.println("list_by_time");

        var date = "3/22/20";
        System.out.println("Date = " + date);
        instance.list_by_time(date);
    }

}
