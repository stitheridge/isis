package org.apache.isis.extensions.commandreplay.impl.quartz;

import javax.inject.Inject;

import com.google.common.base.Splitter;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.isis.core.config.IsisConfiguration;
import org.apache.isis.core.runtime.iactn.template.AbstractIsisInteractionTemplate;
import org.apache.isis.core.security.authentication.AuthenticationSession;
import org.apache.isis.core.security.authentication.standard.SimpleSession;

import org.apache.isis.extensions.commandreplay.impl.clock.TickingClockService;
import org.apache.isis.extensions.commandreplay.impl.util.Holder;
import org.apache.isis.extensions.commandreplay.impl.executor.ReplayableCommandExecution;
import org.apache.isis.extensions.commandreplay.impl.fetch.MasterConfiguration;
import org.apache.isis.extensions.commandreplay.impl.SlaveStatus;

import static org.apache.isis.extensions.commandreplay.impl.quartz.QuartzConfigKeys.SLAVE_ROLES_DEFAULT;
import static org.apache.isis.extensions.commandreplay.impl.quartz.QuartzConfigKeys.SLAVE_ROLES_QUARTZ_KEY;
import static org.apache.isis.extensions.commandreplay.impl.quartz.QuartzConfigKeys.SLAVE_USER_DEFAULT;
import static org.apache.isis.extensions.commandreplay.impl.quartz.QuartzConfigKeys.SLAVE_USER_QUARTZ_KEY;

import lombok.val;
import lombok.extern.log4j.Log4j2;

@DisallowConcurrentExecution
@PersistJobDataAfterExecution
@Log4j2
public class RunBackgroundCommandsWithReplicationAndReplayJob implements Job {

    AuthenticationSession authSession;
    MasterConfiguration masterConfiguration;

    public void execute(final JobExecutionContext quartzContext) {

        // figure out if this instance is configured to run as master or slave
        authSession = new SimpleSessionFromQuartz(quartzContext);
        final IsisConfiguration isisConfiguration = lookupIsisConfiguration(authSession);
        masterConfiguration = new MasterConfiguration(isisConfiguration);

        if(masterConfiguration.isConfigured()) {
            runBackgroundCommandsOnSlave(quartzContext);
        }
    }

    private void runBackgroundCommandsOnSlave(final JobExecutionContext quartzContext) {
        final SlaveStatus slaveStatus =
                getSlaveStatus(quartzContext, SlaveStatus.TICKING_CLOCK_STATUS_UNKNOWN);

        switch (slaveStatus) {

            case TICKING_CLOCK_STATUS_UNKNOWN:
            case TICKING_CLOCK_NOT_YET_INITIALIZED:
                setSlaveStatus(quartzContext,
                        lookupTickingClockServiceStatus(authSession)
                            ? SlaveStatus.OK
                            : SlaveStatus.TICKING_CLOCK_NOT_YET_INITIALIZED);
                return;

            case OK:
                Holder<SlaveStatus> holder = new Holder<>();
                new ReplayableCommandExecution(masterConfiguration).execute(authSession, holder);
                final SlaveStatus newStatus = holder.getObject();
                if(newStatus != null) {
                    setSlaveStatus(quartzContext, newStatus);
                }
                return;


            case REST_CALL_FAILING:
            case FAILED_TO_UNMARSHALL_RESPONSE:
            case UNKNOWN_STATE:
                log.warn("skipped - configured as slave, however: {}" ,slaveStatus);
                return;
            default:
                throw new IllegalStateException("Unrecognised status: " + slaveStatus);
        }

    }

    private IsisConfiguration lookupIsisConfiguration(final AuthenticationSession authSession) {

        final Holder<IsisConfiguration> holder = new Holder<>();
        new AbstractIsisInteractionTemplate() {
            @Override
            protected void doExecuteWithTransaction(final Object unused) {
                holder.setObject(isisConfiguration);
            }

            @Inject
            IsisConfiguration isisConfiguration;
        }.execute(authSession, null);

        return holder.getObject();
    }

    private boolean lookupTickingClockServiceStatus(final AuthenticationSession authSession) {

        final Holder<Boolean> holder = new Holder<>();
        new AbstractIsisInteractionTemplate() {
            @Override
            protected void doExecuteWithTransaction(final Object unused) {
                holder.setObject(tickingClockService.isInitialized());
            }

            @Inject
            TickingClockService tickingClockService;
        }.execute(authSession, null);

        return holder.getObject();
    }

    static class SimpleSessionFromQuartz extends SimpleSession {

        private static String getUser(final JobExecutionContext quartzContext) {
            return getString(quartzContext, SLAVE_USER_QUARTZ_KEY, SLAVE_USER_DEFAULT);
        }

        private static Iterable<String> getRoles(final JobExecutionContext quartzContext) {
            val slaveRoles = getString(quartzContext, SLAVE_ROLES_QUARTZ_KEY, SLAVE_ROLES_DEFAULT);
            return Splitter.on(",").split(
                    slaveRoles);
        }

        SimpleSessionFromQuartz(final JobExecutionContext quartzContext) {
            super(getUser(quartzContext), getRoles(quartzContext));
        }
    }


    private static final String KEY_SLAVE_STATUS = "slaveStatus";

    /**
     * Lookup from quartz configuration for this job.
     */
    private static String getString(JobExecutionContext context, String key, final String defaultValue) {
        try {
            String v = context.getJobDetail().getJobDataMap().getString(key);
            return v != null ? v : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }


    /**
     * Save into quartz configuration for this job, for next invocation.
     */
    private static void setString(JobExecutionContext context, String key, String value) {
        context.getJobDetail().getJobDataMap().put(key, value);
    }
    private static SlaveStatus getSlaveStatus(
            final JobExecutionContext context,
            final SlaveStatus defaultStatus) {
        String mode = getString(context, KEY_SLAVE_STATUS, defaultStatus.name());
        return SlaveStatus.valueOf(mode);
    }

    private static void setSlaveStatus(final JobExecutionContext context, final SlaveStatus mode) {
        setString(context, KEY_SLAVE_STATUS, mode.name());
    }

}

