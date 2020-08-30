package org.apache.isis.extensions.commandreplay.impl;


public class StatusException extends Exception {
    public final SlaveStatus slaveStatus;

    public StatusException(SlaveStatus slaveStatus) {
        this(slaveStatus, null);
    }
    public StatusException(SlaveStatus slaveStatus, final Exception ex) {
        super(ex);
        this.slaveStatus = slaveStatus;
    }
}
