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

package com.github.mishaninss.uidriver;

import com.github.mishaninss.data.UiCommonsProperties;
import com.github.mishaninss.http.ISelenoidClient;
import com.github.mishaninss.reporting.IReporter;
import com.github.mishaninss.reporting.Reporter;
import com.github.mishaninss.uidriver.interfaces.IDownloadsManager;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Profile("selenoid")
@Primary
public class SelenoidDownloadsManager implements IDownloadsManager {

    @Autowired
    private UiCommonsProperties properties;
    @Reporter
    private IReporter reporter;
    @Autowired
    private ISelenoidClient selenoidClient;

    @Override
    public File getDownloadedFile(String fileName) throws IOException {
        byte[] content = selenoidClient.getDownloadedFile(fileName, properties.driver().timeoutsDriverOperation, ChronoUnit.MILLIS);
        File file = Paths.get(properties.driver().downloadsDir, fileName).toFile();
        FileUtils.writeByteArrayToFile(file, content);
        return file;
    }

    @Override
    public List<String> getDownloadedFileNames() {
        String content = selenoidClient.getDownloadedFilesList();
        Document doc = Jsoup.parse(content);
        return doc.getElementsByTag("a").stream().map(Element::text).collect(Collectors.toList());
    }

    @Override
    public List<File> getDownloadedFiles() {
        return getDownloadedFileNames().stream().map(fileName -> {
            try {
                return getDownloadedFile(fileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }
}
