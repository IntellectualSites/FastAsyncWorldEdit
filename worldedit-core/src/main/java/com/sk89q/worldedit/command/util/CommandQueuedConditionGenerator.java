package com.sk89q.worldedit.command.util;

import org.enginehub.piston.Command;
import org.enginehub.piston.gen.CommandConditionGenerator;
import org.enginehub.piston.util.NonnullByDefault;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;

@NonnullByDefault
public final class CommandQueuedConditionGenerator implements CommandConditionGenerator {

    public interface Registration {
        Registration commandQueuedConditionGenerator(CommandPermissionsConditionGenerator generator);
    }

    @Override
    public Command.Condition generateCondition(Method commandMethod) {
        CommandQueued annotation = commandMethod.getAnnotation(CommandQueued.class);
        checkNotNull(annotation, "Annotation is missing from commandMethod");
        return new CommandQueuedCondition(annotation.value());
    }
}
