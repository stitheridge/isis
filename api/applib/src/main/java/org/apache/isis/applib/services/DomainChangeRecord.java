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
package org.apache.isis.applib.services;

import java.sql.Timestamp;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.Optionality;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.Property;
import org.apache.isis.applib.annotation.PropertyLayout;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.annotation.Where;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.bookmark.BookmarkService;
import org.apache.isis.applib.services.message.MessageService;
import org.apache.isis.applib.services.metamodel.BeanSort;
import org.apache.isis.applib.services.metamodel.MetaModelService;

import lombok.Getter;
import lombok.Setter;
import lombok.val;
import lombok.extern.log4j.Log4j2;


/**
 * An abstraction of some sort of recorded change to a domain object: commands, audit entries or published events.
 */
public interface DomainChangeRecord extends HasUniqueId, HasUsername {

    enum ChangeType {
        COMMAND,
        AUDIT_ENTRY,
        PUBLISHED_INTERACTION;
        @Override
        public String toString() {
            return name().replace("_", " ");
        }
    }

    /**
     * Distinguishes commands from audit entries from published events/interactions (when these are shown mixed together in a (standalone) table).
     */
    @Property
    @PropertyLayout(hidden = Where.ALL_EXCEPT_STANDALONE_TABLES)
    @MemberOrder(name="Identifiers", sequence = "1")
    ChangeType getType();


    /**
     * The unique identifier (a GUID) of the transaction in which this change occurred.
     */
    @Property
    @MemberOrder(name="Identifiers",sequence = "50")
    UUID getUniqueId();


    /**
     * The user that caused the change.
     */
    @Property
    @MemberOrder(name="Identifiers", sequence = "10")
    String getUsername();


    /**
     * The time that the change occurred.
     */
    @Property
    @MemberOrder(name="Identifiers", sequence = "20")
    Timestamp getTimestamp();


    /**
     * The object type of the domain object being changed.
     */
    @Property
    @PropertyLayout(named="Class")
    @MemberOrder(name="Target", sequence = "10")
    default String getTargetObjectType() {
        return getTarget().getObjectType();
    }



    /**
     * The {@link Bookmark} identifying the domain object that has changed.
     */
    @Property
    @PropertyLayout(named="Object")
    @MemberOrder(name="Target", sequence="30")
    Bookmark getTarget();


    /**
     * The member interaction (ie action invocation or property edit) which caused the domain object to be changed.
     *
     * <p>
     *     Populated for commands and for published events that represent action invocations or property edits.
     * </p>
     */
    @Property(optionality = Optionality.OPTIONAL)
    @PropertyLayout(named="Member", hidden = Where.ALL_EXCEPT_STANDALONE_TABLES)
    @MemberOrder(name="Target", sequence = "20")
    String getTargetMember();


    /**
     * The value of the property prior to it being changed.
     *
     * <p>
     * Populated only for audit entries.
     * </p>
     */
    @Property(optionality = Optionality.OPTIONAL)
    @PropertyLayout(hidden = Where.ALL_EXCEPT_STANDALONE_TABLES)
    @MemberOrder(name="Detail",sequence = "6")
    String getPreValue();


    /**
     * The value of the property after it has changed.
     *
     * <p>
     * Populated only for audit entries.
     * </p>
     */
    @Property(optionality = Optionality.MANDATORY)
    @PropertyLayout(hidden = Where.ALL_EXCEPT_STANDALONE_TABLES)
    @MemberOrder(name="Detail",sequence = "7")
    String getPostValue();


}
