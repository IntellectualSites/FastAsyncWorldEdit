//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.sk89q.worldedit.util.formatting.text;

import com.sk89q.worldedit.util.formatting.text.TranslatableComponentImpl.BuilderImpl;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.util.formatting.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public interface TranslatableComponent extends BuildableComponent<TranslatableComponent, TranslatableComponent.Builder>, ScopedComponent<TranslatableComponent> {
    @Nonnull
    static TranslatableComponent.Builder builder() {
        return new BuilderImpl();
    }

    @Nonnull
    static TranslatableComponent.Builder builder(@Nonnull final String key) {
        return builder().key(key);
    }

    @Nonnull
    static TranslatableComponent of(@Nonnull final String key) {
        return (TranslatableComponent)builder(key).build();
    }

    @Nonnull
    static TranslatableComponent of(@Nonnull final String key, @Nullable final TextColor color) {
        return (TranslatableComponent)((TranslatableComponent.Builder)builder(key).color(color)).build();
    }

    @Nonnull
    static TranslatableComponent of(@Nonnull final String key, @Nullable final TextColor color, @Nonnull final TextDecoration... decorations) {
        Set<TextDecoration> activeDecorations = new HashSet(decorations.length);
        Collections.addAll(activeDecorations, decorations);
        return of(key, color, (Set)activeDecorations);
    }

    @Nonnull
    static TranslatableComponent of(@Nonnull final String key, @Nullable final TextColor color, @Nonnull final Set<TextDecoration> decorations) {
        return (TranslatableComponent)((TranslatableComponent.Builder)((TranslatableComponent.Builder)builder(key).color(color)).decorations(decorations, true)).build();
    }

    @Nonnull
    static TranslatableComponent of(@Nonnull final String key, final Component... args) {
        return of(key, (TextColor)null, (Component[])args);
    }

    @Nonnull
    static TranslatableComponent of(@Nonnull final String key, @Nullable final TextColor color, final Component... args) {
        return of(key, color, Collections.emptySet(), args);
    }

    @Nonnull
    static TranslatableComponent of(@Nonnull final String key, @Nullable final TextColor color, @Nonnull final Set<TextDecoration> decorations, final Component... args) {
        return of(key, color, decorations, Arrays.asList(args));
    }

    @Nonnull
    static TranslatableComponent of(@Nonnull final String key, @Nonnull final List<? extends Component> args) {
        return of(key, (TextColor)null, (List)args);
    }

    static TranslatableComponent of(@Nonnull final String key, @Nullable final TextColor color, @Nonnull final List<? extends Component> args) {
        return of(key, color, Collections.emptySet(), args);
    }

    @Nonnull
    static TranslatableComponent of(@Nonnull final String key, @Nullable final TextColor color, @Nonnull final Set<TextDecoration> decorations, @Nonnull final List<? extends Component> args) {
        return (TranslatableComponent)((TranslatableComponent.Builder)((TranslatableComponent.Builder)builder(key).color(color)).decorations(decorations, true)).args(args).build();
    }

    @Nonnull
    static TranslatableComponent make(@Nonnull final Consumer<? super TranslatableComponent.Builder> consumer) {
        TranslatableComponent.Builder builder = builder();
        consumer.accept(builder);
        return (TranslatableComponent)builder.build();
    }

    @Nonnull
    static TranslatableComponent make(@Nonnull final String key, @Nonnull final Consumer<? super TranslatableComponent.Builder> consumer) {
        TranslatableComponent.Builder builder = builder(key);
        consumer.accept(builder);
        return (TranslatableComponent)builder.build();
    }

    @Nonnull
    static TranslatableComponent make(@Nonnull final String key, @Nonnull final List<? extends Component> args, @Nonnull final Consumer<? super TranslatableComponent.Builder> consumer) {
        TranslatableComponent.Builder builder = builder(key).args(args);
        consumer.accept(builder);
        return (TranslatableComponent)builder.build();
    }

    @Nonnull
    String key();

    @Nonnull
    TranslatableComponent key(@Nonnull final String key);

    @Nonnull
    List<Component> args();

    @Nonnull
    TranslatableComponent args(@Nonnull final List<? extends Component> args);

    public interface Builder extends ComponentBuilder<TranslatableComponent, TranslatableComponent.Builder> {
        @Nonnull
        TranslatableComponent.Builder key(@Nonnull final String key);

        @Nonnull
        TranslatableComponent.Builder args(final ComponentBuilder... args);

        @Nonnull
        TranslatableComponent.Builder args(final Component... args);

        @Nonnull
        TranslatableComponent.Builder args(@Nonnull final List<? extends Component> args);
    }

    // FAWE added
    @Nonnull
    public static TranslatableComponent of(@Nonnull final String key, final Object... args) {
        List<Component> components = Arrays.stream(args)
                .map(arg -> arg instanceof Component ? (Component) arg : TextComponent.of(Objects.toString(arg)))
                .collect(Collectors.toList());
        return of(key, components);
    }
}
