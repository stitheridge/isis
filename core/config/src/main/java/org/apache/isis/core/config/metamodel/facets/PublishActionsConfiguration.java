/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.core.config.metamodel.facets;

import org.apache.isis.core.config.IsisConfiguration;

public enum PublishActionsConfiguration {
    ALL,
    IGNORE_SAFE,
    /**
     * alias for {@link #IGNORE_SAFE}
     */
    IGNORE_QUERY_ONLY,
    NONE;

    public static PublishActionsConfiguration from(IsisConfiguration configuration) {
        return configuration.getApplib().getAnnotation().getAction().getPublishing();
    }

}
