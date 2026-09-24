package com.neueda.leap;

import com.neueda.leap.enums.InstrumentStatus;

/**
 * Request DTO for updating instrument status
 */
public class InstrumentStatusUpdateRequest {
    private InstrumentStatus status;

    // Constructors
    public InstrumentStatusUpdateRequest() {
    }

    public InstrumentStatusUpdateRequest(InstrumentStatus status) {
        this.status = status;
    }

    // Getters and Setters
    public InstrumentStatus getStatus() {
        return status;
    }

    public void setStatus(InstrumentStatus status) {
        this.status = status;
    }
}
