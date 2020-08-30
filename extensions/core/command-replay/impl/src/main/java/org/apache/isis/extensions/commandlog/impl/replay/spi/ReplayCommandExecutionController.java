package org.apache.isis.extensions.commandlog.impl.replay.spi;

import org.apache.isis.applib.annotation.Programmatic;

public interface ReplayCommandExecutionController {

    enum State {
        RUNNING,
        PAUSED
    }

    /**
     * The current state, or <tt>null</tt> if the service implementing this SPI has not yet been initialized.
     * @return
     */
    State getState();

}
