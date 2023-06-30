package com.github.mishaninss.arma.uidriver;

import com.github.mishaninss.arma.data.UiCommonsProperties;
import com.github.mishaninss.arma.http.ISelenoidClient;
import com.github.mishaninss.arma.reporting.IReporter;
import com.github.mishaninss.arma.reporting.Reporter;
import com.github.mishaninss.arma.uidriver.interfaces.IDownloadsManager;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("selenoid")
@Primary
public class SelenoidDownloadsManager implements IDownloadsManager {

  public static final String DOWNLOADS_DIR = "arma.driver.downloads.dir";

  @Autowired
  private UiCommonsProperties properties;
  @Reporter
  private IReporter reporter;
  @Autowired
  @Lazy
  private ISelenoidClient selenoidClient;

  @Value("${" + DOWNLOADS_DIR + ":}")
  public String downloadsDir;

  @Override
  public String getDownloadsDir() {
    return downloadsDir;
  }

  @Override
  public File getDownloadedFile(String fileName) throws IOException {
    byte[] content = selenoidClient.getDownloadedFile(fileName,
        properties.driver().timeoutsPageLoad, ChronoUnit.MILLIS);
    File file = Paths.get(properties.driver().downloadsDir, fileName).toFile();
    FileUtils.writeByteArrayToFile(file, content);
    return file;
  }

  @Override
  public File getDownloadedFile(String fileName, int timeout) throws IOException {
    byte[] content = selenoidClient.getDownloadedFile(fileName,
        timeout, ChronoUnit.SECONDS);
    File file = Paths.get(properties.driver().downloadsDir, fileName).toFile();
    FileUtils.writeByteArrayToFile(file, content);
    return file;
  }

  @Override
  public List<String> getDownloadedFileNames() {
    String content = selenoidClient.getDownloadedFilesList();
    try {
      Map<String, Object> files = new Gson().fromJson(content, Map.class);
      return (List<String>) files.get("value");
    } catch (Exception ex) {
      Document doc = Jsoup.parse(content);
      return doc.getElementsByTag("a").stream().map(Element::text).collect(Collectors.toList());
    }
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
