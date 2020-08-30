package org.apache.isis.extensions.commandreplay.impl.quartz;

import lombok.experimental.UtilityClass;

@UtilityClass
public class QuartzConfigKeys {

    static final String SLAVE_USER_QUARTZ_KEY  = "user";
    static final String SLAVE_USER_DEFAULT     = "replay_user";
    static final String SLAVE_ROLES_QUARTZ_KEY = "roles";
    static final String SLAVE_ROLES_DEFAULT    = "replay_role";

}
