package com.cloudkitchen.rbac.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Counter authLoginAttempts;
    private final Counter authLoginSuccess;
    private final Counter authLoginFailures;
    private final Counter otpRequests;
    private final Counter s3Uploads;
    private final Timer s3UploadDuration;
    private final Timer databaseQueryDuration;

    public MetricsService(Counter authLoginAttempts, Counter authLoginSuccess, 
                         Counter authLoginFailures, Counter otpRequests, Counter s3Uploads,
                         Timer s3UploadDuration, Timer databaseQueryDuration) {
        this.authLoginAttempts = authLoginAttempts;
        this.authLoginSuccess = authLoginSuccess;
        this.authLoginFailures = authLoginFailures;
        this.otpRequests = otpRequests;
        this.s3Uploads = s3Uploads;
        this.s3UploadDuration = s3UploadDuration;
        this.databaseQueryDuration = databaseQueryDuration;
    }

    public void recordLoginAttempt() {
        authLoginAttempts.increment();
    }

    public void recordLoginSuccess() {
        authLoginSuccess.increment();
    }

    public void recordLoginFailure() {
        authLoginFailures.increment();
    }

    public void recordOtpRequest() {
        otpRequests.increment();
    }

    public void recordS3Upload() {
        s3Uploads.increment();
    }

    public Timer.Sample startS3UploadTimer() {
        return Timer.start();
    }

    public void recordS3UploadDuration(Timer.Sample sample) {
        sample.stop(s3UploadDuration);
    }

    public Timer.Sample startDatabaseTimer() {
        return Timer.start();
    }

    public void recordDatabaseDuration(Timer.Sample sample) {
        sample.stop(databaseQueryDuration);
    }
}