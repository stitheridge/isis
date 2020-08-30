package org.apache.isis.extensions.commandlog.impl.replay.impl;

import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Service;

import org.apache.isis.core.config.IsisConfiguration;

import lombok.val;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class SlaveConfiguration {

    final String masterUser;
    final String masterPassword;
    final String masterBaseUrl;
    final int masterBatchSize;

    public SlaveConfiguration(@NotNull IsisConfiguration isisConfiguration) {
        val masterConfig = isisConfiguration.getExtensions().getCommandReplay().getMaster();
        masterUser = masterConfig.getUser().orElse(null);
        masterPassword = masterConfig.getPassword().orElse(null);
        masterBaseUrl = masterConfig.getBaseUrl()
                            .map(x -> !x.endsWith("/") ? x + "/" : x)
                            .orElse(null);
        masterBatchSize = masterConfig.getBatchSize();
    }


    public boolean isConfigured() {
        return masterUser != null &&
               masterPassword != null &&
               masterBaseUrl != null;
    }
}
