package org.apache.isis.extensions.commandlog.impl.replay.spi;

import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.schema.cmd.v2.CommandDto;

public interface CommandReplayAnalyser {

    @Programmatic
    String analyzeReplay(final Command command, final CommandDto dto);

}
