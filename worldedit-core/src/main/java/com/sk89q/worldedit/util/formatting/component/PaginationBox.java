/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.util.formatting.component;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.util.formatting.text.event.HoverEvent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;

public abstract class PaginationBox extends MessageBox {

    private static final int IDEAL_ROWS_FOR_PLAYER = 8;

    private String pageCommand;
    private int componentsPerPage = IDEAL_ROWS_FOR_PLAYER;
    private int currentPage = -1;

    /**
     * Creates a Paginated component
     *
     * @param title The title
     */
    protected PaginationBox(String title) {
        this(title, null);
    }

    public abstract Component getComponent(int number);

    public abstract int getComponentsSize();

    public void setComponentsPerPage(int componentsPerPage) {
        this.componentsPerPage = componentsPerPage;
    }

    public void formatForConsole() {
        this.pageCommand = null;
        this.componentsPerPage = 20;
    }

    protected final int getCurrentPage() {
        return currentPage;
    }

    /**
     * Creates a Paginated component
     *
     * @param title The title
     * @param pageCommand The command to run to switch page, with %page% representing page number
     */
    protected PaginationBox(String title, @Nullable String pageCommand) {
        super(title, new TextComponentProducer());

        if (pageCommand != null && !pageCommand.contains("%page%")) {
            if (pageCommand.contains("-p ")) {
                pageCommand = pageCommand.replaceAll("-p [0-9]+", "-p %page%");
            } else{
                pageCommand = pageCommand + " -p %page%";
            }
        }
        this.pageCommand = pageCommand;
    }

    public Component create(int page) throws InvalidComponentException {
        if (page == 1 && getComponentsSize() == 0) {
            return getContents().reset().append("No results found.").create();
        }
        int pageCount = (int) Math.ceil(getComponentsSize() / (double) componentsPerPage);
        if (page < 1 || page > pageCount) {
            throw new InvalidComponentException("Invalid page number.");
        }
        currentPage = page;
        final int lastComp = Math.min(page * componentsPerPage, getComponentsSize());
        for (int i = (page - 1) * componentsPerPage; i < lastComp; i++) {
            getContents().append(getComponent(i));
            if (i + 1 != lastComp) {
                getContents().newline();
            }
        }
        if (pageCount == 1) {
            return super.create();
        }
        getContents().newline();
        TextComponent pageNumberComponent = TextComponent.of("Page ", TextColor.YELLOW)
                .append(TextComponent.of(String.valueOf(page), TextColor.GOLD))
                .append(TextComponent.of(" of "))
                .append(TextComponent.of(String.valueOf(pageCount), TextColor.GOLD));
        if (pageCommand != null) {
            TextComponentProducer navProducer = new TextComponentProducer();
            if (page > 1) {
                TextComponent prevComponent = TextComponent.of("<<< ", TextColor.GOLD)
                        .clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND, pageCommand.replace("%page%", String.valueOf(page - 1))))
                        .hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, TextComponent.of("Click to navigate")));
                navProducer.append(prevComponent);
            }
            navProducer.append(pageNumberComponent);
            if (page < pageCount) {
                TextComponent nextComponent = TextComponent.of(" >>>", TextColor.GOLD)
                        .clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND, pageCommand.replace("%page%", String.valueOf(page + 1))))
                        .hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, TextComponent.of("Click to navigate")));
                navProducer.append(nextComponent);
            }
            getContents().append(centerAndBorder(navProducer.create()));
        } else {
            getContents().append(centerAndBorder(pageNumberComponent));
        }
        return super.create();
    }

    @Override
    public TextComponent create() {
        throw new IllegalStateException("Pagination components must be created with a page");
    }

    public static <T> PaginationBox fromStrings(String header, @Nullable String pageCommand, Collection<T> lines, Function<T, Component> adapt) {
        return fromStrings(header, pageCommand, Collections2.transform(lines, adapt));
    }

    public static PaginationBox fromStrings(String header, @Nullable String pageCommand, Collection lines) {
        return new ListPaginationBox(header, pageCommand, lines);
    }

    public static PaginationBox fromStrings(String header, @Nullable String pageCommand, List<String> lines) {
        return fromStrings(header, pageCommand, (Collection) lines);
    }

    public static class ListPaginationBox extends PaginationBox {
        private final Collection lines;
        private int iterIndex;
        private Iterator iterator;

        public ListPaginationBox(String header, String pageCommand, List<String> lines) {
            this(header, pageCommand, (Collection) lines);
        }

        public ListPaginationBox(String header, String pageCommand, Collection lines) {
            super(header, pageCommand);
            this.lines = lines;
        }

        @Override
        public Component getComponent(int number) {
            Object obj;
            if (lines instanceof List) {
                obj = ((List) lines).get(number);
            } else {
                if (iterator == null || iterIndex > number) {
                    iterator = lines.iterator();
                    iterIndex = 0;
                }
                do {
                    obj = iterator.next();
                    iterIndex++;
                } while (iterIndex < number);
            }
            if (obj instanceof Supplier) {
                obj = ((Supplier) obj).get();
            }
            if (obj instanceof Component) {
                return (Component) obj;
            }
            return TextComponent.of(obj + "");
        }

        @Override
        public int getComponentsSize() {
            return lines.size();
        }
    }
}
