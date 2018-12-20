package com.sk89q.worldedit.extension.factory;

import com.boydti.fawe.command.FaweParser;
import com.boydti.fawe.object.extent.MultiTransform;
import com.boydti.fawe.object.extent.RandomTransform;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.object.random.TrueRandom;
import com.boydti.fawe.util.StringMan;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.TransformCommands;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.internal.command.ActorAuthorizer;
import com.sk89q.worldedit.internal.command.WorldEditBinding;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.util.command.Dispatcher;
import com.sk89q.worldedit.util.command.SimpleDispatcher;
import com.sk89q.worldedit.util.command.parametric.ParametricBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class DefaultTransformParser extends FaweParser<ResettableExtent> {
    private final Pattern INTERSECTION_PATTERN = Pattern.compile("[&|;]+(?![^\\[]*\\])");
    private final Dispatcher dispatcher;

    public DefaultTransformParser(WorldEdit worldEdit) {
        super(worldEdit);
        this.dispatcher = new SimpleDispatcher();
        this.register(new TransformCommands(worldEdit));
    }

    @Override
    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void register(Object clazz) {
        ParametricBuilder builder = new ParametricBuilder();
        builder.setAuthorizer(new ActorAuthorizer());
        builder.addBinding(new WorldEditBinding(worldEdit));
        builder.registerMethodsAsCommands(dispatcher, clazz);
    }

    @Override
    public ResettableExtent parseFromInput(String input, ParserContext context) throws InputParseException {
        if (input.isEmpty()) return null;

        List<Double> unionChances = new ArrayList<>();
        List<Double> intersectionChances = new ArrayList<>();

        List<ResettableExtent> intersection = new ArrayList<>();
        List<ResettableExtent> union = new ArrayList<>();
        final CommandLocals locals = new CommandLocals();
        Actor actor = context != null ? context.getActor() : null;
        if (actor != null) {
            locals.put(Actor.class, actor);
        }
        try {
            List<Map.Entry<ParseEntry, List<String>>> parsed = parse(input);
            for (Map.Entry<ParseEntry, List<String>> entry : parsed) {
                ParseEntry pe = entry.getKey();
                String command = pe.input;
                ResettableExtent transform = null;
                double chance = 1;
                if (command.isEmpty()) {
                    transform = parseFromInput(StringMan.join(entry.getValue(), ','), context);
                } else if (dispatcher.get(command) == null) {
                    // Legacy syntax
                    int percentIndex = command.indexOf('%');
                    if (percentIndex != -1) {  // Legacy percent pattern
                        chance = Expression.compile(command.substring(0, percentIndex)).evaluate();
                        command = command.substring(percentIndex + 1);
                        if (!entry.getValue().isEmpty()) {
                            if (!command.isEmpty()) command += " ";
                            command += StringMan.join(entry.getValue(), " ");
                        }
                        transform = parseFromInput(command, context);
                    } else {
                        throw new NoMatchException("See: //transforms");
                    }
                } else {
                    List<String> args = entry.getValue();
                    if (!args.isEmpty()) {
                        command += " " + StringMan.join(args, " ");
                    }
                    transform = (ResettableExtent) dispatcher.call(command, locals, new String[0]);
                }
                if (pe.and) { // &
                    intersectionChances.add(chance);
                    intersection.add(transform);
                } else {
                    if (!intersection.isEmpty()) {
                        if (intersection.size() == 1) {
                            throw new InputParseException("Error, floating &");
                        }
                        MultiTransform multi = new MultiTransform();
                        double total = 0;
                        for (int i = 0; i < intersection.size(); i++) {
                            Double value = intersectionChances.get(i);
                            total += value;
                            multi.add(intersection.get(i), value);
                        }
                        union.add(multi);
                        unionChances.add(total);
                        intersection.clear();
                        intersectionChances.clear();
                    }
                    unionChances.add(chance);
                    union.add(transform);
                }
            }
        } catch (Throwable e) {
            throw new InputParseException(e.getMessage(), e);
        }
        if (!intersection.isEmpty()) {
            if (intersection.size() == 1) {
                throw new InputParseException("Error, floating &");
            }
            MultiTransform multi = new MultiTransform();
            double total = 0;
            for (int i = 0; i < intersection.size(); i++) {
                Double value = intersectionChances.get(i);
                total += value;
                multi.add(intersection.get(i), value);
            }
            union.add(multi);
            unionChances.add(total);
            intersection.clear();
            intersectionChances.clear();
        }
        if (union.isEmpty()) {
            throw new NoMatchException("See: //transforms");
        } else if (union.size() == 1) {
            return union.get(0);
        } else {
            RandomTransform random = new RandomTransform(new TrueRandom());
            for (int i = 0; i < union.size(); i++) {
                random.add(union.get(i), unionChances.get(i));
            }
            return random;
        }
    }


}
