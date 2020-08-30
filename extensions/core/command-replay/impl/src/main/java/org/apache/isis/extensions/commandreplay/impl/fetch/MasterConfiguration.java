package org.apache.isis.extensions.commandreplay.impl.fetch;

import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Service;

import org.apache.isis.core.config.IsisConfiguration;

import lombok.Getter;
import lombok.val;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class MasterConfiguration {

    @Getter final String masterUser;
    @Getter final String masterPassword;
    @Getter final String masterBaseUrl;
    @Getter final int masterBatchSize;

    public MasterConfiguration(@NotNull IsisConfiguration isisConfiguration) {
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
