package org.apache.isis.extensions.commandlog.impl.replay.impl;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.isis.applib.ApplicationException;
import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.clock.Clock;
import org.apache.isis.core.config.IsisConfiguration;
import org.apache.isis.testing.fixtures.applib.clock.TickingFixtureClock;

import lombok.val;
import lombok.extern.log4j.Log4j2;

/**
 * If configured as the slave, then sets up to use {@link TickingFixtureClock} so that time can be changed dynamically
 * when running.
 *
 * <p>
 *     If the configuration keys for a replay slave are not provided, then the service will not initialize.
 * </p>
 *
 * <p>
 *     IMPORTANT: the methods provided by this service are not thread-safe, because the clock is a globally-scoped
 *     singleton rather than a thread-local.  This method should therefore only be used in single-user systems,
 *     eg a replay slave.
 * </p>
 */
@DomainService()
@Log4j2
public class TickingClockService {

    @PostConstruct
    public void init() {
        Optional<String> baseUrl = isisConfiguration.getExtensions().getCommandReplay().getMaster().getBaseUrl();
        Optional<String> user = isisConfiguration.getExtensions().getCommandReplay().getMaster().getUser();
        Optional<String> password = isisConfiguration.getExtensions().getCommandReplay().getMaster().getPassword();

        if( !baseUrl.isPresent()||
            !user.isPresent() ||
            !password.isPresent()) {
            log.info(
                    "init() - skipping, one or more {}.* configuration constants missing",
                    ConfigurationKeys.ISIS_KEY_PREFIX);
            return;
        }

        log.info("init() - replacing existing clock with TickingFixtureClock");
        TickingFixtureClock.replaceExisting();
    }

    @Programmatic
    public boolean isInitialized() {
        return Clock.getInstance() instanceof TickingFixtureClock;
    }


    /**
     * Executes the runnable, setting the clock to be the specified time beforehand (and reinstating it to its original
     * time afterwards).
     *
     * <p>
     *     IMPORTANT: this method is not thread-safe, because the clock is a globally-scoped singleton rather than a
     *     thread-local.  This method should therefore only be used in single-user systems, eg a replay slave.
     * </p>
     */
    @Programmatic
    public void at(Timestamp timestamp, Runnable runnable) {
        ensureInitialized();

        val tickingFixtureClock = (TickingFixtureClock) TickingFixtureClock.getInstance();
        val previous = TickingFixtureClock.getEpochMillis();
        val wallTime0 = System.currentTimeMillis();
        try {
            tickingFixtureClock.setTime(timestamp);
            runnable.run();
        } finally {
            final long wallTime1 = System.currentTimeMillis();
            tickingFixtureClock.setTime(previous + wallTime1 - wallTime0);
        }
    }

    /**
     * Executes the callable, setting the clock to be the specified time beforehand (and reinstating it to its original
     * time afterwards).
     *
     * <p>
     *     IMPORTANT: this method is not thread-safe, because the clock is a globally-scoped singleton rather than a
     *     thread-local.  This method should therefore only be used in single-user systems, eg a replay slave.
     * </p>
     */
    @Programmatic
    public <T> T at(Timestamp timestamp, Callable<T> callable) {
        ensureInitialized();

        val tickingFixtureClock = (TickingFixtureClock) TickingFixtureClock.getInstance();

        val previous = TickingFixtureClock.getEpochMillis();
        val wallTime0 = System.currentTimeMillis();

        try {
            tickingFixtureClock.setTime(timestamp);
            return callable.call();
        } catch (Exception e) {
            throw new ApplicationException(e);
        } finally {
            final long wallTime1 = System.currentTimeMillis();
            tickingFixtureClock.setTime(previous + wallTime1 - wallTime0);
        }
    }

    private void ensureInitialized() {
        if(!isInitialized()) {
            throw new IllegalStateException(
                    "Not initialized.  Make sure that the application is configured to run as a replay slave");
        }
    }

    @Inject IsisConfiguration isisConfiguration;

}