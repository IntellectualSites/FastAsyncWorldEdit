package com.sk89q.worldedit.command.util.annotation;

import com.boydti.fawe.config.Caption;
import com.boydti.fawe.util.task.InterruptableCondition;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.argument.Arguments;
import com.sk89q.worldedit.event.Event;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.internal.command.CommandArgParser;
import com.sk89q.worldedit.internal.util.Substring;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import org.enginehub.piston.exception.StopExecutionException;
import org.enginehub.piston.inject.InjectAnnotation;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * Indicates how the affected blocks should be hinted at in the log.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({
            ElementType.PARAMETER,
            ElementType.METHOD
        })
@InjectAnnotation
public @interface Confirm {
    Processor value() default Processor.ALWAYS;

    enum Processor {
        REGION {
            @Override
            public boolean passes(Actor actor, InjectedValueAccess context, double value) {
                Region region = context.injectedValue(Key.of(Region.class, Selection.class)).orElseThrow(IncompleteRegionException::new);
                BlockVector3 pos1 = region.getMinimumPoint();
                BlockVector3 pos2 = region.getMaximumPoint();
                long area = (pos2.getX() - pos1.getX()) * (pos2.getZ() - pos1.getZ() + 1)
                    * (long) value;
                long max = 2 << 18;
                if (max != -1 && area > max) {
                    actor.print(Caption.of("fawe.cancel.worldedit.cancel.reason.confirm.region",
                            pos1, pos2, getArgs(context), region.getHeight() * area));
                    return confirm(actor, context);
                }
                return true;
            }
        },
        RADIUS {
            @Override
            public boolean passes(Actor actor, InjectedValueAccess context, double value) {
                int max = WorldEdit.getInstance().getConfiguration().maxRadius;
                if (max != -1 && value > max) {
                    actor.print(Caption.of("fawe.cancel.worldedit.cancel.reason.confirm.radius",
                            value, max, getArgs(context)));
                    return confirm(actor, context);
                }
                return true;
            }
        },
        LIMIT {
            @Override
            public boolean passes(Actor actor, InjectedValueAccess context, double value) {
                int max = 50; //TODO configurable, get Key.of(Method.class) @Limit
                if (max != -1 && value > max) {
                    actor.print(Caption.of("fawe.cancel.worldedit.cancel.reason.confirm.limit",
                            value, max, getArgs(context)));
                    return confirm(actor, context);
                }
                return true;
            }
        },
        ALWAYS {
            @Override
            public boolean passes(Actor actor, InjectedValueAccess context, double value) {
                actor.print(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.confirm"));
                return confirm(actor, context);
            }
        };

        public boolean passes(Actor actor, InjectedValueAccess context, double value) {
            return true;
        }

        public <T extends Number> T check(Actor actor, InjectedValueAccess context, T value) {
            if (!passes(actor, context, value.doubleValue())) {
                throw new StopExecutionException(TextComponent.empty());
            }
            return value;
        }

        private static String getArgs(InjectedValueAccess context) {
            return context.injectedValue(Key.of(Arguments.class)).map(Arguments::get).get();
        }

        private static String getArg(InjectedValueAccess context, String def) {
            Stream<Substring> split = CommandArgParser.forArgString(getArgs(context)).parseArgs();
            Substring first = split.findFirst().orElse(null);
            if (first == null && def == null) {
                throw new StopExecutionException(TextComponent.of("No arguments"));
            }
            return first != null ? first.getSubstring() : def;
        }

        private static int getInt(InjectedValueAccess context, String def) {
            return Integer.parseInt(getArg(context, def));
        }

        private static boolean confirm(Actor actor, InjectedValueAccess context) {
            Event event = context.injectedValue(Key.of(Event.class)).orElse(null);
            if (!(event instanceof CommandEvent)) {
                return true;
            }
            ReentrantLock lock = new ReentrantLock();
            Condition condition = lock.newCondition();
            InterruptableCondition wait = new InterruptableCondition(lock, condition, Thread.currentThread());
            try {
                lock.lock();
                actor.setMeta("cmdConfirm", wait);
                if (condition.await(15, TimeUnit.SECONDS)) {
                    return true;
                }
            } catch (InterruptedException ignored) {
            } finally {
                if (actor.getMeta("cmdConfirm") == wait) {
                    actor.deleteMeta("cmdConfirm");
                }
            }
            return false;
        }
    }
}
