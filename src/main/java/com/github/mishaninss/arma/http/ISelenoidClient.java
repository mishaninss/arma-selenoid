package com.github.mishaninss.arma.http;

import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;

public interface ISelenoidClient {
    boolean isVideoEnabled();

    void setEnableVideo(boolean enableVideo);

    byte[] getVideoFile(String fileName, int timeout, TemporalUnit timeUnit);

    void deleteVideoFile(String fileName);

    void deleteVideoFile(String fileName, int timeout, TimeUnit timeUnit);

    byte[] getDownloadedFile(String fileName);

    String getDownloadedFilesList();

    byte[] getDownloadedFile(String fileName, int timeout, TemporalUnit timeUnit);
}
