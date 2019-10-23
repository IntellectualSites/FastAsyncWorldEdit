package com.boydti.fawe.object.brush;

import static com.google.common.base.Preconditions.checkNotNull;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.brush.scroll.ScrollAction;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.expression.runtime.Constant;
import com.sk89q.worldedit.internal.expression.runtime.EvaluationException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<SettingType, Object> constructor = new ConcurrentHashMap<>();

    private Brush brush;
    private Mask mask;
    private Mask sourceMask;
    private ResettableExtent transform;
    private Pattern material;
    private Expression size = new Expression(1);
    private Set<String> permissions;
    private ScrollAction scrollAction;
    private String lastWorld;

    public BrushSettings() {
        this.permissions = new HashSet<>();
        this.constructor.put(SettingType.PERMISSIONS, permissions);
    }

    public static BrushSettings get(BrushTool tool, Player player, LocalSession session, Map<String, Object> settings) throws InputParseException {
        PlatformCommandManager manager = PlatformCommandManager.getInstance();
        String constructor = (String) settings.get(SettingType.BRUSH.name());
        if (constructor == null) {
            return new BrushSettings();
        }
        BrushSettings bs = manager.parse(constructor, player);
        bs.constructor.put(SettingType.BRUSH, constructor);
        if (settings.containsKey(SettingType.PERMISSIONS.name())) {
            bs.permissions.addAll((Collection<? extends String>) settings.get(SettingType.PERMISSIONS.name()));
        }
        if (settings.containsKey(SettingType.SIZE.name())) {
            try {
                bs.size = Expression.compile((String) settings.getOrDefault(SettingType.SIZE.name(), -1));
                bs.size.optimize();
            } catch (ExpressionException e) {
                throw new RuntimeException(e);
            }
        }

        ParserContext parserContext = new ParserContext();
        parserContext.setActor(player);
        parserContext.setWorld(player.getWorld());
        parserContext.setSession(session);

        if (settings.containsKey(SettingType.MASK.name())) {
            String maskArgs = (String) settings.get(SettingType.MASK.name());
            Mask mask = WorldEdit.getInstance().getMaskFactory().parseFromInput(maskArgs, parserContext);
            bs.setMask(mask);
            bs.constructor.put(SettingType.MASK, maskArgs);
        }
        if (settings.containsKey(SettingType.SOURCE_MASK.name())) {
            String maskArgs = (String) settings.get(SettingType.SOURCE_MASK.name());
            Mask mask = WorldEdit.getInstance().getMaskFactory().parseFromInput(maskArgs, parserContext);
            bs.setSourceMask(mask);
            bs.constructor.put(SettingType.SOURCE_MASK, maskArgs);
        }
        if (settings.containsKey(SettingType.TRANSFORM.name())) {
            String transformArgs = (String) settings.get(SettingType.TRANSFORM.name());
            ResettableExtent extent = Fawe.get().getTransformParser().parseFromInput(transformArgs, parserContext);
            bs.setTransform(extent);
            bs.constructor.put(SettingType.TRANSFORM, transformArgs);
        }
        if (settings.containsKey(SettingType.FILL.name())) {
            String fillArgs = (String) settings.get(SettingType.FILL.name());
            Pattern pattern = WorldEdit.getInstance().getPatternFactory().parseFromInput(fillArgs, parserContext);
            bs.setFill(pattern);
            bs.constructor.put(SettingType.FILL, fillArgs);
        }
        if (settings.containsKey(SettingType.SCROLL_ACTION.name())) {
            String actionArgs = (String) settings.get(SettingType.SCROLL_ACTION.name());
            ScrollAction action = ScrollAction.fromArguments(tool, player, session, actionArgs, false);
            if (action != null) {
                bs.setScrollAction(action);
                bs.constructor.put(SettingType.SCROLL_ACTION, actionArgs);
            }
        }
        return bs;
    }

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
        size = new Expression(1);
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
        if (mask == null) constructor.remove(SettingType.MASK);
        this.mask = mask;
        return this;
    }

    public BrushSettings setSourceMask(Mask mask) {
        if (mask == null) constructor.remove(SettingType.SOURCE_MASK);
        this.sourceMask = mask;
        return this;
    }

    public BrushSettings setTransform(ResettableExtent transform) {
        if (transform == null) constructor.remove(SettingType.TRANSFORM);
        this.transform = transform;
        return this;
    }

    public BrushSettings setFill(Pattern pattern) {
        if (pattern == null) constructor.remove(SettingType.FILL);
        this.material = pattern;
        return this;
    }

    public BrushSettings setSize(Expression size) {
        checkNotNull(size);
        this.size = size;
        if (size.getRoot() instanceof Constant && ((Constant) size.getRoot()).getValue() == -1) {
            constructor.remove(SettingType.SIZE);
        } else {
            constructor.put(SettingType.SIZE, size.toString());
        }
        return this;
    }

    public BrushSettings setSize(double size) {
        return setSize(new Expression(size));
    }

    public BrushSettings setScrollAction(ScrollAction scrollAction) {
        if (scrollAction == null) constructor.remove(SettingType.SCROLL_ACTION);
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

    /**
     * Set the world the brush is being used in
     * @param world
     * @return true if the world differs from the last used world
     */
    public boolean setWorld(String world) {
        boolean result = false;
        if (this.lastWorld != null && !this.lastWorld.equalsIgnoreCase(world)) {
            result = true;
        }
        this.lastWorld = world;
        return result;
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

    public ScrollAction getScrollAction() {
        return scrollAction;
    }

    public boolean canUse(Actor actor) {
        Set<String> perms = getPermissions();
        for (String perm : perms) {
            if (actor.hasPermission(perm)) return true;
        }
        return perms.isEmpty();
    }

}
