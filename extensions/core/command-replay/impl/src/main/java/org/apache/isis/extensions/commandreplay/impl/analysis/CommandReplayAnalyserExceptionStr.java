package org.apache.isis.extensions.commandreplay.impl.analysis;

import com.google.common.base.Objects;

import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.services.conmap.command.UserDataKeys;
import org.apache.isis.applib.util.schema.CommandDtoUtils;
import org.apache.isis.core.commons.internal.exceptions._Exceptions;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo;
import org.apache.isis.schema.cmd.v2.CommandDto;

@DomainService()
public class CommandReplayAnalyserExceptionStr extends CommandReplayAnalyserAbstract {

    public static final String ANALYSIS_KEY = "isis.services."
            + CommandReplayAnalyserExceptionStr.class.getSimpleName()
            + ".analysis";

    public CommandReplayAnalyserExceptionStr() {
        super(ANALYSIS_KEY);
    }

    protected String doAnalyzeReplay(final CommandJdo commandJdo) {

        final CommandDto dto = commandJdo.getCommandDto();

        final String primaryException =
                CommandDtoUtils.getUserData(dto, UserDataKeys.EXCEPTION);
        if (primaryException == null) {
            return null;
        }

        final String replayedException = commandJdo.getException();

        final String masterExceptionTrimmed = trimmed(primaryException);
        final String replayedExceptionTrimmed = trimmed(replayedException);
        return Objects.equal(masterExceptionTrimmed, replayedExceptionTrimmed)
                ? null
                : String.format("Exceptions differ.  Master was '%s'", primaryException);
    }

    private String trimmed(final String str) {
        return withoutWhitespace(initialPartOfStackTrace(str));
    }

    // we only look at beginning of the stack trace because the latter part will differ when replayed
    private String initialPartOfStackTrace(final String str) {
        final int toInspectOfStackTrace = 500;
        return str.length() > toInspectOfStackTrace ? str.substring(0, toInspectOfStackTrace) : str;
    }

    private String withoutWhitespace(final String s) {
        return s.replaceAll("\\s", "");
    }

}
