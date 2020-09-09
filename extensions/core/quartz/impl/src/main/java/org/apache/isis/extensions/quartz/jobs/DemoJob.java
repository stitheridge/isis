package org.apache.isis.extensions.quartz.jobs;


import javax.inject.Inject;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

import org.apache.isis.core.config.IsisConfiguration;
import org.apache.isis.core.security.authentication.AuthenticationSession;
import org.apache.isis.core.security.authentication.standard.SimpleSession;

import lombok.val;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DemoJob implements Job {

    public void execute(final JobExecutionContext context) {

        final AuthenticationSession authSession = newAuthSession(context);

        log.debug("Running job");
    }

    protected AuthenticationSession newAuthSession(JobExecutionContext context) {
        val user = isisConfiguration.getExtensions().getQuartz().getRunBackgroundCommands().getUser();
        val roles = isisConfiguration.getExtensions().getQuartz().getRunBackgroundCommands().getRoles();
        log.debug("background user : {}", user);
        log.debug("background roles: {}", roles);
        return new SimpleSession(user, roles);
    }

    @Inject IsisConfiguration isisConfiguration;

}
