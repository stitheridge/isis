package org.isisaddons.module.command.replay.impl;

import java.util.Map;

import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Service;

import org.apache.isis.core.config.IsisConfiguration;

import static org.isisaddons.module.command.replay.impl.ConfigurationKeys.MASTER_BASE_URL_ISIS_KEY;
import static org.isisaddons.module.command.replay.impl.ConfigurationKeys.MASTER_BATCH_SIZE_ISIS_DEFAULT;
import static org.isisaddons.module.command.replay.impl.ConfigurationKeys.MASTER_BATCH_SIZE_ISIS_KEY;
import static org.isisaddons.module.command.replay.impl.ConfigurationKeys.MASTER_PASSWORD_ISIS_KEY;
import static org.isisaddons.module.command.replay.impl.ConfigurationKeys.MASTER_USER_ISIS_KEY;

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
