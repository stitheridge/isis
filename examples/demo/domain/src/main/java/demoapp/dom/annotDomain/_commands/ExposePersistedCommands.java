package demoapp.dom.annotDomain._commands;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Collection;
import org.apache.isis.applib.annotation.CollectionLayout;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdoRepository;

import lombok.RequiredArgsConstructor;
import lombok.val;

import demoapp.dom.annotDomain.Action.command.ActionCommandJdo;

/**
 * Marker interface for mixins to contribute to.
 */
//tag::class[]
public interface ExposePersistedCommands {
}
//end::class[]
