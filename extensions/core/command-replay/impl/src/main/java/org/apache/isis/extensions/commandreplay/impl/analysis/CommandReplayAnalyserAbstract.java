package org.apache.isis.extensions.commandreplay.impl.analysis;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.core.config.metamodel.facets.Util;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo;

public abstract class CommandReplayAnalyserAbstract implements CommandReplayAnalyser {

    private final String key;
    private final String defaultValue;

    public CommandReplayAnalyserAbstract(final String key) {
        this(key, "enabled");
    }

    public CommandReplayAnalyserAbstract(final String key, final String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    private boolean enabled;

    @PostConstruct
    public void init(final Map<String,String> properties) {
        final String anslysisStr = getPropertyElse(properties, key, defaultValue);
        enabled = Util.parseYes(anslysisStr);
    }


    @Programmatic
    public final String analyzeReplay(final CommandJdo commandJdo) {

        if(!enabled) {
            return null;
        }
        return doAnalyzeReplay(commandJdo);

    }

    protected abstract String doAnalyzeReplay(final CommandJdo command);

    private static String getPropertyElse(final Map<String, String> properties, final String key, final String dflt) {
        final String str = properties.get(key);
        return str != null ? str : dflt;
    }


}
