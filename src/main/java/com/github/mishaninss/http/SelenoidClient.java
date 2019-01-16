/*
 * Copyright 2019 Sergey Mishanin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.mishaninss.http;

import com.github.mishaninss.data.WebDriverProperties;
import com.github.mishaninss.reporting.IReporter;
import com.github.mishaninss.reporting.Reporter;
import com.github.mishaninss.uidriver.Arma;
import com.github.mishaninss.uidriver.webdriver.IWebDriverFactory;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.temporal.TemporalUnit;

@Component
public class SelenoidClient implements ISelenoidClient {
    @Autowired
    private Arma arma;
    @Autowired
    private WebDriverProperties webDriverProperties;
    @Autowired
    private IWebDriverFactory webDriverFactory;
    @Reporter
    protected IReporter reporter;

    public byte[] getDownloadedFile(String fileName) {
        String sessionId = webDriverFactory.getSessionId();

        try {
            return RestAssured.given()
                    .baseUri(webDriverProperties.driver().gridUrl.replace("/wd/hub", ""))
                    .log().all(true)
                    .pathParam("sessionId", sessionId)
                    .pathParam("fileName", fileName)
                    .expect()
                    .statusCode(200)
                    .log().all(true)
                    .get("/download/{sessionId}/{fileName}")
                    .andReturn()
                    .body().asByteArray();
        } catch (Throwable e) {
            throw new RuntimeException(String.format("Could not get downloaded file [%s] from Selenoid session [%s]", fileName, sessionId));
        }
    }

    public String getDownloadedFilesList() {
        String sessionId = webDriverFactory.getSessionId();

        return RestAssured.given()
                .baseUri(webDriverProperties.driver().gridUrl.replace("/wd/hub", ""))
                .log().all(true)
                .pathParam("sessionId", sessionId)
                .expect()
                .statusCode(200)
                .contentType(ContentType.HTML)
                .log().all(true)
                .get("/download/{sessionId}")
                .andReturn().body().asString();
    }

    public byte[] getDownloadedFile(String fileName, int timeout, TemporalUnit timeUnit) {
        return arma.waiting().waitForCondition(() -> {
            try {
                return getDownloadedFile(fileName);
            } catch (Exception ex) {
                reporter.ignoredException(ex);
            }
            return null;
        }, timeout, timeUnit);
    }
}
