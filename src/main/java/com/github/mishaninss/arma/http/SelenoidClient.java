package com.github.mishaninss.arma.http;

import com.github.mishaninss.arma.data.WebDriverProperties;
import com.github.mishaninss.arma.reporting.IReporter;
import com.github.mishaninss.arma.reporting.Reporter;
import com.github.mishaninss.arma.uidriver.annotations.WaitingDriver;
import com.github.mishaninss.arma.uidriver.interfaces.IWaitingDriver;
import com.github.mishaninss.arma.uidriver.webdriver.IWebDriverFactory;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static org.awaitility.Awaitility.await;

@Component
public class SelenoidClient implements ISelenoidClient {

  @Reporter
  protected IReporter reporter;
  @Autowired
  @Lazy
  private WebDriverProperties webDriverProperties;
  @Autowired
  @Lazy
  private IWebDriverFactory webDriverFactory;
  @WaitingDriver
  @Lazy
  private IWaitingDriver waitingDriver;
  @Value("${arma.selenoid.enableVideo:false}")
  private boolean enableVideo;

  private RequestSpecification request() {
    return RestAssured.given()
        .baseUri(getSelenoidUrl())
        .log().all(true);
  }

  private String getSelenoidUrl() {
    return webDriverProperties.driver().gridUrl.replace("/wd/hub", "");
  }

  @Override
  public boolean isVideoEnabled() {
    return enableVideo;
  }

  @Override
  public void setEnableVideo(boolean enableVideo) {
    System.setProperty("arma.selenoid.enableVideo", String.valueOf(enableVideo));
    this.enableVideo = enableVideo;
  }

  public byte[] getVideoFile(String fileName) {
    try {
      return request()
          .pathParam("fileName", fileName)
          .expect()
          .statusCode(200)
          .when()
          .get("/video/{fileName}.mp4")
          .andReturn()
          .body().asByteArray();
    } catch (Throwable e) {
      throw new RuntimeException(String.format("Could not get video file [%s]", fileName));
    }
  }

  @Override
  public byte[] getVideoFile(String fileName, int timeout, TemporalUnit timeUnit) {
    return waitingDriver.waitForCondition(() -> {
      try {
        return getVideoFile(fileName);
      } catch (Exception ex) {
        reporter.ignoredException(ex);
      }
      return null;
    }, timeout, timeUnit);
  }

  @Override
  public void deleteVideoFile(String fileName) {
    try {
      request()
          .pathParam("fileName", fileName)
          .expect()
          .statusCode(200)
          .log().all(true)
          .when()
          .delete("/video/{fileName}.mp4");
    } catch (Throwable e) {
      throw new RuntimeException(String.format("Could not get video file [%s]", fileName));
    }
  }

  @Override
  public void deleteVideoFile(String fileName, int timeout, TimeUnit timeUnit) {
    await()
        .atMost(timeout, timeUnit)
        .until(() -> {
          try {
            deleteVideoFile(fileName);
            return true;
          } catch (Exception ex) {
            reporter.ignoredException(ex);
          }
          return false;
        });
  }

  @Override
  public byte[] getDownloadedFile(String fileName) {
    String sessionId = webDriverFactory.getSessionId();

    try {
      return request()
          .pathParam("sessionId", sessionId)
          .pathParam("fileName", fileName)
          .expect()
          .statusCode(200)
          .log().all(true)
          .when()
          .get("/download/{sessionId}/{fileName}")
          .andReturn()
          .body().asByteArray();
    } catch (Throwable e) {
      throw new RuntimeException(
          String.format("Could not get downloaded file [%s] from Selenoid session [%s]", fileName,
              sessionId));
    }
  }

  @Override
  public String getDownloadedFilesList() {
    String sessionId = webDriverFactory.getSessionId();

    return request()
        .pathParam("sessionId", sessionId)
        .expect()
        .statusCode(200)
        .contentType(ContentType.HTML)
        .log().all(true)
        .when()
        .get("/download/{sessionId}")
        .andReturn().body().asString();
  }

  @Override
  public byte[] getDownloadedFile(String fileName, int timeout, TemporalUnit timeUnit) {
    return waitingDriver.waitForCondition(() -> {
      try {
        return getDownloadedFile(fileName);
      } catch (Exception ex) {
        reporter.ignoredException(ex);
      }
      return null;
    }, timeout, timeUnit);
  }
}
