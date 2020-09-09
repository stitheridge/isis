package org.apache.isis.extensions.commandreplay.impl.analysis;

import java.util.Optional;

import com.google.common.base.Objects;

import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.conmap.command.UserDataKeys;
import org.apache.isis.applib.util.schema.CommandDtoUtils;

import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo;
import org.apache.isis.schema.cmd.v2.CommandDto;

@DomainService()
public class CommandReplayAnalyserResultStr extends CommandReplayAnalyserAbstract {

    public static final String ANALYSIS_KEY = "isis.services."
            + CommandReplayAnalyserResultStr.class.getSimpleName() +
            ".analysis";

    public CommandReplayAnalyserResultStr() {
        super(ANALYSIS_KEY);
    }

    protected String doAnalyzeReplay(final CommandJdo commandJdo) {

        final CommandDto dto = commandJdo.getCommandDto();

        // see if the outcome was the same...
        // ... either the same result when replayed
        final String primaryResultStr =
                CommandDtoUtils.getUserData(dto, UserDataKeys.RESULT);

        final Bookmark secondaryResult = commandJdo.getResult();
        final String secondaryResultStr =
                secondaryResult != null ? secondaryResult.toString() : null;
        return Objects.equal(primaryResultStr, secondaryResultStr)
                ? null
                : String.format(
                        "Results differ.  Primary was '%s', secondary is '%s'",
                        primaryResultStr, secondaryResultStr);
    }

}
