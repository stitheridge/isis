package org.apache.isis.extensions.commandlog.impl.jdo;

public enum ReplayState {
    PENDING,
    OK,
    FAILED,
    EXCLUDED,
    ;

    public boolean isFailed() { return this == FAILED;}
}
