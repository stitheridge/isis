package demoapp.dom.annotDomain.Action.command.spiimpl;

import java.util.LinkedList;

import javax.inject.Inject;

import org.apache.isis.applib.annotation.Collection;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdo;
import org.apache.isis.extensions.commandlog.impl.jdo.CommandJdoRepository;
import org.apache.isis.schema.cmd.v2.CommandDto;

import lombok.val;

import demoapp.dom.annotDomain.Action.command.ActionCommandJdo;

//tag::class[]
@Collection
public class ActionCommandJdo_commands {
    // ...
//end::class[]

    private final ActionCommandJdo actionCommandJdo;
    public ActionCommandJdo_commands(ActionCommandJdo actionCommandJdo) {
        this.actionCommandJdo = actionCommandJdo;
    }

    //tag::class[]
    public LinkedList<CommandDto> coll() {
        val list = new LinkedList<CommandDto>();
        commandJdoRepository.findCompleted()
                .stream().map(CommandJdo::getCommandDto)
                .forEach(list::push);   // reverse order
        return list;
    }

    @Inject
    CommandJdoRepository commandJdoRepository;
}
//end::class[]
