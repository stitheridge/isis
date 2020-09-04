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
package org.apache.isis.applib.services.command;

import java.sql.Timestamp;

import org.apache.isis.applib.Identifier;
import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.events.domain.ActionDomainEvent;
import org.apache.isis.applib.services.HasUniqueId;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.bookmark.BookmarkService;
import org.apache.isis.applib.services.iactn.Interaction;
import org.apache.isis.applib.services.wrapper.WrapperFactory;
import org.apache.isis.schema.cmd.v2.CommandDto;

/**
 * Represents the <i>intention to</i> invoke either an action or modify a property.  There can be only one such
 * intention per (web) request, so a command is in effect request-scoped.  Note that {@link CommandContext} domain
 * service - from which the current {@link Command} can be obtained - is indeed annotated with
 * {@link javax.enterprise.context.RequestScoped @RequestScoped}.
 *
 * <p>
 * Each Command can be reified into a {@link Command#getMemento() memento} by way of the (internal)
 * <tt>CommandDtoServiceInternal</tt> domain service; typically corresponding to the XML equivalent of a
 * {@link CommandDto} and conforming to the Apache Isis <a href="http://isis.apache.org/schema/cmd/">cmd</a> schema.
 * </p>
 *
 * <p>
 *     The {@link Command} interface also captures details of the corresponding action invocation (or property edit),
 *     specifically when that action/edit {@link Command#getStartedAt() started} or
 *     {@link Command#getCompletedAt() completed}, and its result, either a {@link Command#getResult() return value}
 *     or an {@link Command#getException() exception}.  Also captures a stack of {@link ActionDomainEvent}s.
 * </p>
 *
 * <p>
 *     Note that when invoking an action, other actions may be invoked courtesy of the {@link WrapperFactory}.  These
 *     "sub-actions" do <i>not</i> modify the contents of the command object; in other words think of the command
 *     object as representing the outer-most originating action.
 * </p>
 *
 * <p>
 *     <b>NOTE</b>: in Isis v1.x, one of the responsibilities of {@link Command} was to generate unique sequence numbers for
 *     a given transactionId, where there were three possible sequences that might be generated.
 *     <ul>
 *         <li>
 *         <p>the sequence of changed domain objects being published by the
 *         {@link org.apache.isis.applib.services.publish.PublisherService#publish(Interaction.Execution)}
 *         </p>
 *         <p>
 *         In v2 ... TODO[2158] - what replaces this?
 *         </p>
 *         </li>
 *
 *         <li>
 *         <p>
 *         The sequence of wrapped action invocations (each being published)
 *         </p>
 *         <p>
 *         In v2 this is now done by {@link Interaction#next(String) Interaction} itself.
 *         </p>
 *         </li>
 *
 *         <li>
 *         <p>
 *         and finally, one or more background commands that might be scheduled via the <code>BackgroundService</code>.
 *         </p>
 *         <p>
 *         In v2 ... TODO[2158] - what replaces this?
 *         </p>
 *         </li>
 *     </ul>
 *
 * </p>
 *
 */
// tag::refguide[]
public interface Command extends HasUniqueId {

    // end::refguide[]
    /**
     * The user that created the command.
     */
    // tag::refguide[]
    String getUser();                           // <.>
    // end::refguide[]

    /**
     * The date/time at which this command was created.
     */
    // tag::refguide[]
    Timestamp getTimestamp();                   // <.>
    // end::refguide[]

    /**
     * {@link Bookmark} of the target object (entity or service) on which this action was performed.
     *
     * <p>
     * Will only be populated if a {@link BookmarkService} has been configured.
     * </p>
     */
    // tag::refguide[]
    Bookmark getTarget();                       // <.>
    // end::refguide[]

    /**
     * Holds a string representation of the invoked action, or the edited property, equivalent to
     * {@link Identifier#toClassAndNameIdentityString()}.
     */
    // tag::refguide[]
    String getMemberIdentifier();               // <.>
    // end::refguide[]

    /**
     * A human-friendly description of the class of the target object.
     */
    // tag::refguide[]
    String getTargetClass();                    // <.>
    // end::refguide[]

    /**
     * The human-friendly name of the action invoked/property edited on the target object.
     */
    // tag::refguide[]
    String getTargetAction();                   // <.>
    // end::refguide[]

    /**
     * A human-friendly description of the arguments with which the action was invoked.
     */
    // tag::refguide[]
    String getArguments();                      // <.>
    // end::refguide[]

    /**
     *
     * A formal (XML or similar) specification of the action to invoke/being invoked.
     */
    // tag::refguide[]
    String getMemento();                        // <.>
    // end::refguide[]

    /**
     *
     * The mechanism by which this command is to be executed, either synchronously &quot;in the
     * {@link CommandExecuteIn#FOREGROUND foreground}&quot; or is to be executed asynchronously &quot;in the
     * {@link CommandExecuteIn#BACKGROUND background}&quot; through the {@link BackgroundCommandService}.
     */
    // tag::refguide[]
    CommandExecuteIn getExecuteIn();            // <.>
    // end::refguide[]

    // tag::refguide2[]
    enum Executor {
        // end::refguide2[]
        /**
         * Command being executed by the end-user.
         */
        // tag::refguide2[]
        USER,
        // end::refguide2[]
        /**
         * Command being executed by a background execution service.
         */
        // tag::refguide2[]
        BACKGROUND,
        // end::refguide2[]
        /**
         * Command being executed for some other reason, eg as result of redirect-after-post, or the homePage action.
         */
        // tag::refguide2[]
        OTHER
    }
    // end::refguide2[]

    /**
     * The (current) executor of this command.
     *
     * <p>
     * Note that (even for implementations of {@link BackgroundCommandService} that persist {@link Command}s), this
     * property is never (likely to be) persisted, because it is always updated to indicate how the command is
     * currently being executed.
     *
     * <p>
     * If the {@link #getExecutor() executor} matches the required {@link #getExecuteIn() execution policy}, then the
     * command actually is executed.  The combinations are:
     * <ul>
     * <li>executor = USER, executeIn = FOREGROUND, then execute</li>
     * <li>executor = USER, executeIn = BACKGROUND, then persist and return persisted command as a placeholder for the result</li>
     * <li>executor = BACKGROUND, executeIn = FOREGROUND, then ignore</li>
     * <li>executor = BACKGROUND, executeIn = BACKGROUND, then execute, update the command with result</li>
     * </ul>
     *
     */
    // tag::refguide[]
    Executor getExecutor();                     // <.>
    // end::refguide[]

    /**
     * For an command that has actually been executed, holds the date/time at which the {@link Interaction} that
     * executed the command started.
     *
     * <p>
     *     Previously this field was deprecated (on the basis that the startedAt is also held in
     *     {@link Interaction.Execution#getStartedAt()}). However, this property is now used in master/slave
     *     replay scenarios which may query a persisted Command.
     * </p>
     *
     * See also {@link Interaction#getCurrentExecution()} and
     * {@link Interaction.Execution#getStartedAt()}.
     */
    // tag::refguide[]
    Timestamp getStartedAt();                   // <.>
    // end::refguide[]

    /**
     * For an command that has actually been executed, holds the date/time at which the {@link Interaction} that
     * executed the command completed.
     *
     * <p>
     *     Previously this field was deprecated (on the basis that the completedAt is also held in
     *     {@link Interaction.Execution#getCompletedAt()}). However, this property is now used in master/slave
     *     replay scenarios which may query a persisted Command.
     * </p>
     *
     * See also {@link Interaction#getCurrentExecution()} and
     * {@link Interaction.Execution#getCompletedAt()}.
     */
    // tag::refguide[]
    Timestamp getCompletedAt();                 // <.>
    // end::refguide[]

    /**
     * For actions created through the {@link BackgroundService} and {@link BackgroundCommandService},
     * captures the parent action.
     */
    // tag::refguide[]
    Command getParent();                        // <.>
    // end::refguide[]

    /**
     * For an command that has actually been executed, holds the exception stack
     * trace if the action invocation/property modification threw an exception.
     *
     * <p>
     *     Previously this field was deprecated (on the basis that the exception is also held in
     *     {@link Interaction.Execution#getThrew()}). However, this property is now used in master/slave
     *     replay scenarios which may query a persisted Command.
     * </p>
     *
     * See also {@link Interaction#getCurrentExecution()} and  {@link org.apache.isis.applib.services.iactn.Interaction.Execution#getThrew()}.
     */
    // tag::refguide[]
    String getException();                      // <.>
    // end::refguide[]

    /**
     * For an command that has actually been executed, holds a {@link Bookmark} to the object returned by the corresponding action/property modification.
     *
     * <p>
     *     Previously this field was deprecated (on the basis that the returned value is also held in
     *     {@link Interaction.Execution#getReturned()}). However, this property is now used in master/slave
     *     replay scenarios which may query a persisted Command.
     * </p>
     *
     * See also  {@link Interaction#getCurrentExecution()} and  {@link org.apache.isis.applib.services.iactn.Interaction.Execution#getReturned()}.
     */
    // tag::refguide[]
    Bookmark getResult();                       // <.>
    // end::refguide[]

    /**
     * Whether this command should ultimately be persisted (if the configured {@link BackgroundCommandService} supports
     * it) or not.
     *
     * <p>
     * If the action to be executed has been annotated with the {@link Action#command()} attribute
     * then (unless its {@link Action#commandPersistence()} persistence} attribute has been set to a different value
     * than its default of {@link org.apache.isis.applib.annotation.CommandPersistence#PERSISTED persisted}), the
     * {@link Command} object will be persisted.
     *
     * <p>
     * However, it is possible to prevent the {@link Command} object from ever being persisted by setting the
     * {@link org.apache.isis.applib.annotation.Action#commandPersistence() persistence} attribute to
     * {@link org.apache.isis.applib.annotation.CommandPersistence#NOT_PERSISTED}, or it can be set to
     * {@link org.apache.isis.applib.annotation.CommandPersistence#IF_HINTED}, meaning it is dependent
     * on whether {@link #setPersistHint(boolean) a hint has been set} by some other means.
     *
     * <p>
     * For example, a {@link BackgroundCommandService} implementation that creates persisted background commands ought
     * associate them (via its {@link Command#getParent() parent}) to an original persisted
     * {@link Command}.  The hinting mechanism allows the service to suggest that the parent command be persisted so
     * that the app can then provide a mechanism to find all child background commands for that original parent command.
     */
    // tag::refguide[]
    CommandPersistence getPersistence();        // <.>
    // end::refguide[]

    /**
     * Whether that this {@link Command} should be persisted, if possible.
     */
    // tag::refguide[]
    boolean isPersistHint();                    // <.>
    // end::refguide[]

    /**
     * <b>NOT API</b>: intended to be called only by the framework.
     */
    public static interface Internal {

        /**
         * <b>NOT API</b>: intended to be called only by the framework.
         *
         * <p>
         * Implementation notes: set when the Isis PersistenceSession is opened.
         */
        void setUser(String user);

        /**
         * <b>NOT API</b>: intended to be called only by the framework.
         *
         * <p>
         * Implementation notes: set when the Isis PersistenceSession is opened.
         */
        void setTimestamp(Timestamp timestamp);

        /**
         * <b>NOT API</b>: intended to be called only by the framework.
         *
         * <p>
         * Implementation notes: set when the action is invoked (in the ActionInvocationFacet).
         */
        void setTarget(Bookmark target);

        /**
         * <b>NOT API</b>: intended to be called only by the framework.
         *
         * <p>
         * Implementation notes: set when the action is invoked (in <tt>ActionInvocationFacet</tt>) or in
         * property edited (in <tt>PropertySetterFacet</tt>).
         */
        void setMemberIdentifier(String memberIdentifier);

        /**
         * <b>NOT API</b>: intended to be called only by the framework.
         *
         * <p>
         * Implementation notes: set when the action is invoked (in the <tt>ActionInvocationFacet</tt>) or property edited
         * (in the <tt>PropertySetterOrClearFacet</tt>).
         */
        void setTargetAction(String targetAction);


        /**
         * <b>NOT API</b>: intended to be called only by the framework.
         *
         * <p>
         * Implementation notes: set when the action is invoked (in the <tt>ActionInvocationFacet</tt>).
         */
        void setArguments(final String arguments);

        /**
         * <b>NOT API</b>: intended to be called only by the framework.
         */
        void setExecutor(final Executor executor);

        /**
         * <b>NOT API</b>: intended to be called only by the framework.
         */
        void setResult(Bookmark resultBookmark);

        /**
         * <b>NOT API</b>: intended to be called only by the framework.
         */
        void setException(String stackTrace);

        /**
         * <b>NOT API</b>: intended to be called only by the framework.
         */
        void setParent(final Command parent);

        /**
         * <b>NOT API</b>: intended to be called only by the framework.
         *
         * <p>
         *     Previously this field was deprecated (on the basis that the completedAt is also held in
         *     {@link Interaction.Execution#getCompletedAt()}). However, this property is now used in master/slave
         *     replay scenarios which may query a persisted Command.
         * </p>
         *
         * See also {@link Interaction#getCurrentExecution()} and
         * {@link Interaction.Execution#setCompletedAt(Timestamp)}.
         */
        void setCompletedAt(Timestamp completedAt);

        /**
         * <b>NOT API</b>: intended to be called only by the framework.
         *
         *
         * <p>
         *     Previously this field was deprecated (on the basis that the completedAt is also held in
         *     {@link Interaction.Execution#getCompletedAt()}). However, this property is now used in master/slave
         *     replay scenarios which may query a persisted Command.
         * </p>
         *
         * See also {@link Interaction#getCurrentExecution()} and
         * {@link #setStartedAt(org.apache.isis.applib.services.clock.ClockService, org.apache.isis.applib.services.metrics.MetricsService)}.
         */
        void setStartedAt(Timestamp startedAt);

        /**
         * <b>NOT API</b>: intended to be called only by the framework.
         *
         * <p>
         * Implementation notes: set when the action is invoked (in the <tt>ActionInvocationFacet</tt>).
         */
        void setMemento(final String memento);

        /**
         * <b>NOT API</b>: intended to be called only by the framework.
         *
         * <p>
         * Implementation notes: set when the action is invoked (in the <tt>ActionInvocationFacet</t>).
         */
        void setTargetClass(String targetClass);

        /**
         * <b>NOT API</b>: intended to be called only by the framework.
         */
        void setPersistence(final CommandPersistence persistence);

        /**
         * Hint that this {@link Command} should be persisted, if possible.
         *
         * <p>
         * <b>NOT API</b>: intended to be called only by the framework.
         *
         * @see #getPersistence()
         */
        void setPersistHint(boolean persistHint);
    }

    /**
     * <b>NOT API</b>: intended to be called only by the framework.
     */
    Internal internal();

// tag::refguide[]
}
// end::refguide[]
