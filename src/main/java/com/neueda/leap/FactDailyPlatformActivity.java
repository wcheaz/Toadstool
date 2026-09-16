package com.neueda.leap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class FactDailyPlatformActivity {

    private LocalDate activityDate;
    private Integer orderCount;
    private BigDecimal filledQuantity;
    private BigDecimal grossAmount;
    private OffsetDateTime updatedAt;

    // Constructors
    public FactDailyPlatformActivity() {
    }

    public FactDailyPlatformActivity(LocalDate activityDate, Integer orderCount, BigDecimal filledQuantity, BigDecimal grossAmount) {
        this.activityDate = activityDate;
        this.orderCount = orderCount;
        this.filledQuantity = filledQuantity;
        this.grossAmount = grossAmount;
    }

    public FactDailyPlatformActivity(LocalDate activityDate, Integer orderCount, BigDecimal filledQuantity, BigDecimal grossAmount, OffsetDateTime updatedAt) {
        this.activityDate = activityDate;
        this.orderCount = orderCount;
        this.filledQuantity = filledQuantity;
        this.grossAmount = grossAmount;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public LocalDate getActivityDate() {
        return activityDate;
    }

    public void setActivityDate(LocalDate activityDate) {
        this.activityDate = activityDate;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getFilledQuantity() {
        return filledQuantity;
    }

    public void setFilledQuantity(BigDecimal filledQuantity) {
        this.filledQuantity = filledQuantity;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
