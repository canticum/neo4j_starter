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
package info.muspoe.c1730.neo4j;

import info.muspoe.c1730.neo4j.service.Neo4jService;
import info.muspoe.c1730.neo4j.vo.NovelCOVIDValue;
import info.muspoe.c1730.neo4j.wuhan.CSSEGISandData_TimeSeries;
import info.muspoe.c1730.neo4j.wuhan.NovelCOVIDReader;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Jonathan Chang, Chun-yien <ccy@musicapoetica.org>
 */
@SpringBootApplication
@RestController
public class WuhanVirus extends Neo4jService {

    NovelCOVIDReader nc;

    public WuhanVirus() {
    }

    @GetMapping("/wv")
    public String hello() {

        return """
               <pre>
               /wv/nc - novelCOVID dataset
                 parameters: sortedby=[todayCases*,casesPerOneMillion,cases,deaths,todayDeaths,country]
               /wv/cssetime - CSSEGISandData
                 parameters: type=[country*,update,date]
                             date=[latest*,{MM/dd/yy}] or country=[Taiwan**,...]
               </pre>
               """;
    }

    @GetMapping("/wv/nc")
    public String novelCOVID(
            @RequestParam(value = "sortedby", defaultValue = "todayCases") String sortedby) {

        nc = (nc == null) ? new NovelCOVIDReader() : nc;

        String subtitle = sortedby;
        String result = switch (sortedby.toLowerCase()) {
            case "casespom","casesperonemillion":
                subtitle = "casesPerOneMillion";
                yield nc.list(NovelCOVIDValue::getCasesPerOneMillion);
            case "cases":
                subtitle = "cases";
                yield nc.list(NovelCOVIDValue::getCases);
            case "deaths":
                subtitle = "deaths";
                yield nc.list(NovelCOVIDValue::getDeaths);
            case "todaydeaths":
                subtitle = "todayDeaths";
                yield nc.list(NovelCOVIDValue::getTodayDeaths);
            case "country":
                subtitle = "country";
                yield nc.listNaturalOrder(NovelCOVIDValue::getCountry);
            case "todaycases":
            default:
                subtitle = "todayCases";
                yield nc.list(NovelCOVIDValue::getTodayCases);
        };
        /*
        instance.list((t) -> t.getCasesPerOneMillion()*(t.getDeaths())/(t.getRecovered()+1));
        instance.list((t) -> t.getDeaths() / t.getCases() * 100);
         */

        return String.format(
                "<pre>Wuhan Virus NovelCOVID DataSet, sorted by %s\n\n%s</pre>",
                subtitle, result);
    }

    CSSEGISandData_TimeSeries cssetime;

    @GetMapping("/wv/cssetime")
    public String csseGISandData_TimeSeries(
            @RequestParam(value = "type", defaultValue = "country") String type,
            @RequestParam(value = "date", defaultValue = "latest") String date,
            @RequestParam(value = "country", defaultValue = "Taiwan*") String country) {

        String result = "";
        var subtitle = type.toLowerCase();
        try {
            cssetime = (cssetime == null) ? new CSSEGISandData_TimeSeries() : cssetime;
            var needUpdated = cssetime.updateRequired();
            if (needUpdated || "update".equals(subtitle)) {
                cssetime.reset_graph();
                cssetime.update_data();
            }
            var list_date = date;
            switch (type.toLowerCase()) {
                case "date" -> {
                    var dates = cssetime.getMetadata().getTimeSeries_dates();
                    if ("latest".equals(date) || !dates.contains(date)) {
                        list_date = dates.get(dates.size() - 1);
                    }
                    subtitle = "list of date " + list_date;
                    result = cssetime.list_by_time(list_date);
                }
                case "country" -> {
                    subtitle = "list by country " + country;
                    result = cssetime.list_by_country(country);
                }
                default ->
                    subtitle = "data updated.";
            }
        } catch (Exception ex) {
            result = Arrays.stream(ex.getStackTrace())
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n"));
        }
        return String.format(
                """
                <pre>
                Wuhan Virus CSSEGISandData TimeSeries DataSet, %s
                
                %s
                </pre>
                """, subtitle, result);
    }

//        return """
//               <pre>
//               ******************
//                Wuhan Virus Data 
//               ******************""";
}
