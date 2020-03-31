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
package info.muspoe.c1730.neo4j.vo;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
public class Location {

    protected String province, country;
    protected double latitude, longitude;

    public String getProvince() {
        
        return province;
    }

    public String getCountry() {
        
        return country;
    }

    public double getLatitude() {
        
        return latitude;
    }

    public double getLongitude() {
     
        return longitude;
    }

    public String getName() {

        return (this.province != null ? this.province + ", " : "") + this.country;
    }
}
