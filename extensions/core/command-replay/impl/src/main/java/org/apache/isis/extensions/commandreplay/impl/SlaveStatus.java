package org.apache.isis.extensions.commandreplay.impl;

public enum SlaveStatus {
    TICKING_CLOCK_STATUS_UNKNOWN,
    TICKING_CLOCK_NOT_YET_INITIALIZED,
    OK,
    REST_CALL_FAILING,
    FAILED_TO_UNMARSHALL_RESPONSE,
    UNKNOWN_STATE,
}
