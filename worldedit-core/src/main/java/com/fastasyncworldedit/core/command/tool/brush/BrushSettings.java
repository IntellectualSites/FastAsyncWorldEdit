package com.fastasyncworldedit.core.command.tool.brush;

import com.fastasyncworldedit.core.command.tool.scroll.Scroll;
import com.fastasyncworldedit.core.extent.ResettableExtent;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.expression.EvaluationException;
import com.sk89q.worldedit.internal.expression.Expression;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.fastasyncworldedit.core.command.tool.brush.BrushSettings.SettingType.BRUSH;
import static com.google.common.base.Preconditions.checkNotNull;

public class BrushSettings {

    public enum SettingType {
        BRUSH,
        SIZE,
        MASK,
        SOURCE_MASK,
        TRANSFORM,
        FILL,
        PERMISSIONS,
        SCROLL_ACTION,
    }


    private static final Expression DEFAULT_SIZE = Expression.compile("1");

    private final Map<SettingType, Object> constructor = new ConcurrentHashMap<>();

    private Brush brush;
    private Mask mask;
    private Mask sourceMask;
    private ResettableExtent transform;
    private Pattern material;
    private Expression size = DEFAULT_SIZE;
    private final Set<String> permissions;
    private Scroll scrollAction;

    public BrushSettings() {
        this.permissions = new HashSet<>();
        this.constructor.put(SettingType.PERMISSIONS, permissions);
    }

    // TODO: Ping @MattBDev to reimplement 2020-02-04
    //    public static BrushSettings get(BrushTool tool, Player player, LocalSession session, Map<String, Object> settings) throws InputParseException {
    //        PlatformCommandManager manager = PlatformCommandManager.getInstance();
    //        String constructor = (String) settings.get(SettingType.BRUSH.name());
    //        if (constructor == null) {
    //            return new BrushSettings();
    //        }
    //        BrushSettings bs = manager.parseCommand(constructor, player);
    //        bs.constructor.put(SettingType.BRUSH, constructor);
    //        if (settings.containsKey(SettingType.PERMISSIONS.name())) {
    //            bs.permissions.addAll((Collection<? extends String>) settings.get(SettingType.PERMISSIONS.name()));
    //        }
    //        if (settings.containsKey(SettingType.SIZE.name())) {
    //            try {
    //                bs.size = Expression.compile((String) settings.getOrDefault(SettingType.SIZE.name(), -1));
    //                bs.size.optimize();
    //            } catch (ExpressionException e) {
    //                throw new RuntimeException(e);
    //            }
    //        }
    //
    //        ParserContext parserContext = new ParserContext();
    //        parserContext.setActor(player);
    //        parserContext.setWorld(player.getWorld());
    //        parserContext.setSession(session);
    //
    //        if (settings.containsKey(SettingType.MASK.name())) {
    //            String maskArgs = (String) settings.get(SettingType.MASK.name());
    //            Mask mask = WorldEdit.getInstance().getMaskFactory().parseFromInput(maskArgs, parserContext);
    //            bs.setMask(mask);
    //            bs.constructor.put(SettingType.MASK, maskArgs);
    //        }
    //        if (settings.containsKey(SettingType.SOURCE_MASK.name())) {
    //            String maskArgs = (String) settings.get(SettingType.SOURCE_MASK.name());
    //            Mask mask = WorldEdit.getInstance().getMaskFactory().parseFromInput(maskArgs, parserContext);
    //            bs.setSourceMask(mask);
    //            bs.constructor.put(SettingType.SOURCE_MASK, maskArgs);
    //        }
    //        if (settings.containsKey(SettingType.TRANSFORM.name())) {
    //            String transformArgs = (String) settings.get(SettingType.TRANSFORM.name());
    //            ResettableExtent extent = Fawe.get().getTransformParser().parseFromInput(transformArgs, parserContext);
    //            bs.setTransform(extent);
    //            bs.constructor.put(SettingType.TRANSFORM, transformArgs);
    //        }
    //        if (settings.containsKey(SettingType.FILL.name())) {
    //            String fillArgs = (String) settings.get(SettingType.FILL.name());
    //            Pattern pattern = WorldEdit.getInstance().getPatternFactory().parseFromInput(fillArgs, parserContext);
    //            bs.setFill(pattern);
    //            bs.constructor.put(SettingType.FILL, fillArgs);
    //        }
    //        if (settings.containsKey(SettingType.SCROLL_ACTION.name())) {
    //            String actionArgs = (String) settings.get(SettingType.SCROLL_ACTION.name());
    //            Scroll action = Scroll.fromArguments(tool, player, session, actionArgs, false);
    //            if (action != null) {
    //                bs.setScrollAction(action);
    //                bs.constructor.put(SettingType.SCROLL_ACTION, actionArgs);
    //            }
    //        }
    //        return bs;
    //    }

    public BrushSettings setBrush(Brush brush) {
        Brush tmp = this.brush;
        if (tmp != brush) {
            if (brush == null || (tmp != null && tmp.getClass() != brush.getClass())) {
                // clear
                clear();
            }
            this.brush = brush;
        }
        return this;
    }

    public BrushSettings clear() {
        brush = null;
        mask = null;
        sourceMask = null;
        transform = null;
        material = null;
        scrollAction = null;
        size = DEFAULT_SIZE;
        permissions.clear();
        constructor.clear();
        return this;
    }

    public BrushSettings clearPerms() {
        permissions.clear();
        return this;
    }

    public BrushSettings addSetting(SettingType type, String args) {
        constructor.put(type, args);
        return this;
    }

    public Map<SettingType, Object> getSettings() {
        return (constructor);
    }

    public BrushSettings setMask(Mask mask) {
        if (mask == null) {
            constructor.remove(SettingType.MASK);
        }
        this.mask = mask;
        return this;
    }

    public BrushSettings setSourceMask(Mask mask) {
        if (mask == null) {
            constructor.remove(SettingType.SOURCE_MASK);
        }
        this.sourceMask = mask;
        return this;
    }

    public BrushSettings setTransform(ResettableExtent transform) {
        if (transform == null) {
            constructor.remove(SettingType.TRANSFORM);
        }
        this.transform = transform;
        return this;
    }

    public BrushSettings setFill(Pattern pattern) {
        if (pattern == null) {
            constructor.remove(SettingType.FILL);
        }
        this.material = pattern;
        return this;
    }

    public BrushSettings setSize(Expression size) {
        checkNotNull(size);
        this.size = size;
        if (size == DEFAULT_SIZE) {
            constructor.remove(SettingType.SIZE);
        } else {
            constructor.put(SettingType.SIZE, size.toString());
        }
        return this;
    }

    public BrushSettings setSize(double size) {
        return setSize(Expression.compile(Double.toString(size)));
    }

    public BrushSettings setScrollAction(Scroll scrollAction) {
        if (scrollAction == null) {
            constructor.remove(SettingType.SCROLL_ACTION);
        }
        this.scrollAction = scrollAction;
        return this;
    }

    public BrushSettings addPermission(String permission) {
        this.permissions.add(permission);
        return this;
    }

    public BrushSettings addPermissions(String... perms) {
        Collections.addAll(permissions, perms);
        return this;
    }

    public Brush getBrush() {
        return brush;
    }

    public Mask getMask() {
        return mask;
    }

    public Mask getSourceMask() {
        return sourceMask;
    }

    public ResettableExtent getTransform() {
        return transform;
    }

    public Pattern getMaterial() {
        return material;
    }

    public double getSize() {
        try {
            return size.evaluate();
        } catch (EvaluationException e) {
            throw new RuntimeException(e);
        }
    }

    public Expression getSizeExpression() {
        return this.size;
    }

    public Set<String> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public Scroll getScrollAction() {
        return scrollAction;
    }

    public boolean canUse(Actor actor) {
        Set<String> perms = getPermissions();
        for (String perm : perms) {
            if (actor.hasPermission(perm)) {
                return true;
            }
        }
        return perms.isEmpty();
    }

    @Override
    public String toString() {
        String name = (String) getSettings().get(BRUSH);
        if (name != null) {
            return name;
        }
        name = brush.getClass().getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

}
