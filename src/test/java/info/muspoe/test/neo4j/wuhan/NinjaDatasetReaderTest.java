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

import org.junit.jupiter.api.*;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class NinjaDatasetReaderTest {

    static NinjaDatasetReader instance;

    public NinjaDatasetReaderTest() {
    }

    @BeforeAll
    public static void setUpClass() {

        System.out.println("***************");
        System.out.println("  Wuhan Virus  ");
        System.out.println(" Ninja DataSet ");
        System.out.println("***************");
        instance = new NinjaDatasetReader();
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
     * Test of list_ninja method, of class NinjaDatasetReader.
     */
    @org.junit.jupiter.api.Test
    public void testList_ninja() {
        System.out.println("list_ninja");

        instance.list_ninja();
    }
}
