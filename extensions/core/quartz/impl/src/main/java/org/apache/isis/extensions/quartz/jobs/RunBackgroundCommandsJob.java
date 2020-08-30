package org.apache.isis.extensions.quartz.jobs;


import com.google.common.base.Splitter;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

import org.apache.isis.core.security.authentication.AuthenticationSession;
import org.apache.isis.core.security.authentication.standard.SimpleSession;

import org.apache.isis.extensions.commandlog.impl.background.BackgroundCommandExecutionFromBackgroundCommandServiceJdo;

import lombok.val;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class RunBackgroundCommandsJob implements Job {

    public void execute(final JobExecutionContext context) {

        final AuthenticationSession authSession = newAuthSession(context);

        log.debug("Running background commands");
        new BackgroundCommandExecutionFromBackgroundCommandServiceJdo().execute(authSession, null);
    }

    protected String getKey(JobExecutionContext context, String key) {
        return context.getMergedJobDataMap().getString(key);
    }

    protected AuthenticationSession newAuthSession(JobExecutionContext context) {
        val user = getKey(context, "user");
        val rolesStr = getKey(context, "roles");
        val roles = Splitter.on(",").split(rolesStr);
        return new SimpleSession(user, roles);
    }


}
