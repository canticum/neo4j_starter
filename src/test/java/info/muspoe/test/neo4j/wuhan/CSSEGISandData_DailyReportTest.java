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

import static info.muspoe.test.neo4j.wuhan.CSSEGISandData_TimeSeries.DAILY_REPORT_FORMATTER;
import org.junit.jupiter.api.*;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class CSSEGISandData_DailyReportTest {

    static CSSEGISandData_DailyReport instance;

    public CSSEGISandData_DailyReportTest() {
    }

    @BeforeAll
    public static void setUpClass() throws Exception {

        System.out.println("************************************************");
        System.out.println(" Wuhan Virus CSSEGISandData DailyReport Dataset ");
        System.out.println("************************************************");
        instance = new CSSEGISandData_DailyReport();
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
     * Test of update_daily_report_data method, of class
     * CSSEGISandData_TimeSeries.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Disabled
    public void testUpdate_daily_report_data() throws Exception {
        System.out.println("update_daily_report_data");

        var needUpdated = instance.updateRequired();
        System.out.println("needUpdated = " + needUpdated);
//        instance.update_data();

//        instance.update_daily_report_data(instance.dailyReport_updated_date.format(DAILY_REPORT_FORMATTER));
    }

    /**
     * Test of list_confirmed method, of class CSSEGISandData_TimeSeries.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Disabled
    public void testList_confirmed_String() throws Exception {
        System.out.println("list_confirmed");

        String country = "US";
//        instance.list_confirmed(country);
    }

    /**
     * Test of list_confirmed method, of class CSSEGISandData_TimeSeries.
     *
     * @throws java.lang.Exception
     */
    @Test
    @Disabled
    public void testList_confirmed_0args() throws Exception {
        System.out.println("list_confirmed");

//        instance.list_confirmed();
    }
}
