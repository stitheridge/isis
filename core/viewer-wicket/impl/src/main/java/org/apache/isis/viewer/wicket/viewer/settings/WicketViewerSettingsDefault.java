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

package org.apache.isis.viewer.wicket.viewer.settings;

import org.springframework.stereotype.Service;

import org.apache.isis.applib.annotation.PromptStyle;
import org.apache.isis.config.IsisConfiguration;
import org.apache.isis.config.IsisConfigurationLegacy;
import org.apache.isis.runtime.system.context.IsisContext;
import org.apache.isis.viewer.wicket.model.isis.WicketViewerSettings;

@Service
public class WicketViewerSettingsDefault implements WicketViewerSettings {

    private static final long serialVersionUID = 1L;

    IsisConfigurationLegacy getConfigurationLegacy() {
        return IsisContext.getConfigurationLegacy();
    }

    IsisConfiguration getConfiguration() {
        return IsisContext.getConfiguration();
    }

    @Override
    public int getMaxTitleLengthInStandaloneTables() {
        return getConfiguration().getViewer().getWicket().getMaxTitleLengthInStandaloneTables();
    }

    @Override
    public int getMaxTitleLengthInParentedTables() {
        return getConfiguration().getViewer().getWicket().getMaxTitleLengthInParentedTables();
    }

    /**
     * Fallback for either {@link #getMaxTitleLengthInParentedTables()} and {@link #getMaxTitleLengthInParentedTables()}
     */
    private int getMaxTitleLengthInTables() {
        return getConfiguration().getViewer().getWicket().getMaxTitleLengthInTables();
    }

    @Override
    public String getDatePattern() {
        return getConfigurationLegacy().getString("isis.viewer.wicket.datePattern", "dd-MM-yyyy");
    }

    @Override
    public String getDateTimePattern() {
        return getConfigurationLegacy().getString("isis.viewer.wicket.dateTimePattern", "dd-MM-yyyy HH:mm");
    }

    @Override
    public String getTimestampPattern() {
        return getConfigurationLegacy().getString("isis.viewer.wicket.timestampPattern", "yyyy-MM-dd HH:mm:ss.SSS");
    }

    @Override
    public boolean isReplaceDisabledTagWithReadonlyTag() {
        return getConfigurationLegacy().getBoolean("isis.viewer.wicket.replaceDisabledTagWithReadonlyTag", true);
    }

    @Override
    public boolean isPreventDoubleClickForFormSubmit() {
        return getConfigurationLegacy().getBoolean("isis.viewer.wicket.preventDoubleClickForFormSubmit", true);
    }

    @Override
    public boolean isPreventDoubleClickForNoArgAction() {
        return getConfigurationLegacy().getBoolean("isis.viewer.wicket.preventDoubleClickForNoArgAction", true);
    }

    @Override
    public boolean isUseIndicatorForFormSubmit() {
        return getConfigurationLegacy().getBoolean("isis.viewer.wicket.useIndicatorForFormSubmit", true);
    }

    @Override
    public boolean isUseIndicatorForNoArgAction() {
        return getConfigurationLegacy().getBoolean("isis.viewer.wicket.useIndicatorForNoArgAction", true);
    }

    @Override
    public PromptStyle getPromptStyle() {
        return getConfiguration().getViewer().getWicket().getPromptStyle();
    }

    @Override
    public boolean isRedirectEvenIfSameObject() {
        return getConfigurationLegacy().getBoolean("isis.viewer.wicket.redirectEvenIfSameObject", false);
    }
}
