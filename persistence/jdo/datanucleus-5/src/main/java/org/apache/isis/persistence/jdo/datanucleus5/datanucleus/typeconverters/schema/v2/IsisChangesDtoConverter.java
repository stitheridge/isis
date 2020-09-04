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
package org.apache.isis.persistence.jdo.datanucleus5.datanucleus.typeconverters.schema.v2;

import org.datanucleus.store.types.converters.TypeConverter;

import org.apache.isis.applib.util.schema.ChangesDtoUtils;
import org.apache.isis.schema.chg.v2.ChangesDto;

public class IsisChangesDtoConverter implements TypeConverter<ChangesDto, String>{

    private static final long serialVersionUID = 1L;

    @Override
    public String toDatastoreType(final ChangesDto memberValue) {
        return memberValue != null
                ? ChangesDtoUtils.toXml(memberValue)
                        : null;
    }

    @Override
    public ChangesDto toMemberType(final String datastoreValue) {
        return datastoreValue != null
                ? ChangesDtoUtils.fromXml(datastoreValue)
                        : null;
    }

}
