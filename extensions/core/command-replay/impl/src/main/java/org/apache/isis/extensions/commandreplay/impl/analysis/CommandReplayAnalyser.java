package org.apache.isis.extensions.commandreplay.impl.analysis;

import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo;

public interface CommandReplayAnalyser {

    String analyzeReplay(
            final CommandJdo commandJdo);

}
