package com.cloudkitchen.rbac.dto.merchant;

import java.time.LocalDateTime;

public class FolderCreationStatus {

    public enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    private Integer merchantId;
    private Status status;
    private Integer totalFolders;
    private Integer createdFolders;
    private Long durationMs;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public FolderCreationStatus() {
    }

    public FolderCreationStatus(Integer merchantId, Status status) {
        this.merchantId = merchantId;
        this.status = status;
    }

    public Integer getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Integer merchantId) {
        this.merchantId = merchantId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Integer getTotalFolders() {
        return totalFolders;
    }

    public void setTotalFolders(Integer totalFolders) {
        this.totalFolders = totalFolders;
    }

    public Integer getCreatedFolders() {
        return createdFolders;
    }

    public void setCreatedFolders(Integer createdFolders) {
        this.createdFolders = createdFolders;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
