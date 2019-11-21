package com.sk89q.worldedit.command.util;

import com.sk89q.worldedit.command.util.annotation.SkipQueue;
import org.enginehub.piston.Command;
import org.enginehub.piston.gen.CommandConditionGenerator;
import org.enginehub.piston.util.NonnullByDefault;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;

@NonnullByDefault
public final class CommandQueuedConditionGenerator implements CommandConditionGenerator {

    public interface Registration {
        Registration commandQueuedConditionGenerator(CommandQueuedConditionGenerator generator);
    }

    @Override
    public Command.Condition generateCondition(Method commandMethod) {
        SkipQueue skip = commandMethod.getAnnotation(SkipQueue.class);
        if (skip == null) {
            return Command.Condition.TRUE;
        }
        return new CommandQueuedCondition(!skip.value());
    }
}
