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
package org.apache.isis.client.kroviz.core.aggregator

import org.apache.isis.client.kroviz.core.event.LogEntry
import org.apache.isis.client.kroviz.core.model.SystemDM
import org.apache.isis.client.kroviz.to.DomainTypes
import org.apache.isis.client.kroviz.to.User
import org.apache.isis.client.kroviz.to.Version

class SystemAggregator() : BaseAggregator() {

    init {
        dsp = SystemDM("not filled (yet)")
    }

    override fun update(logEntry: LogEntry, subType: String) {

        when (val obj = logEntry.getTransferObject()) {
            is User -> dsp.addData(obj)
            is Version -> dsp.addData(obj)
            is DomainTypes -> dsp.addData(obj)
            else -> log(logEntry)
        }

        if (dsp.canBeDisplayed()) {
//  TODO          UiManager.openObjectView(this)
        }
    }

    override fun reset(): SystemAggregator {
        dsp.isRendered = false
        return this
    }

}
