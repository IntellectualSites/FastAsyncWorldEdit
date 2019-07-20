package com.sk89q.worldedit.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.StringMan;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@CommandContainer
public class SchemListFilters {
    public class Filter {
        public boolean listPrivate() {
            return true;
        }

        public boolean listPublic() {
            return false;
        }

        public File getPath(File root) {
            return root;
        }

        public boolean applies(File file) {
            return true;
        }
    }

    @Command(
            name = "all",
            desc = "List both public and private schematics"
    )
    public Filter all() {
        return new Filter() {
            @Override
            public boolean listPublic() {
                return true;
            }
        };
    }

    @Command(
            name = "private",
            aliases = {"me", "mine", "local"},
            desc = "List your personal schematics"
    )
    public Filter local() {
        return new Filter();
    }

    @Command(
            name = "public",
            aliases = {"global"},
            desc = "List public schematics"
    )
    public Filter global() {
        return new Filter() {
            @Override
            public boolean listPrivate() {
                return false;
            }

            @Override
            public boolean listPublic() {
                return true;
            }
        };
    }

    @Command(
            name = "",
            desc = "wildcard"
    )
    public Filter wildcard(Actor actor, String arg) {
        arg = arg.replace("/", File.separator);
        String argLower = arg.toLowerCase(Locale.ROOT);
        if (arg.endsWith("/") || arg.endsWith(File.separator)) {
            if (arg.length() > 3 && arg.length() <= 16) {
                // possible player name
            }
        } else {
            if (StringMan.containsAny(arg, "\\^$.|?+(){}<>~$!%^&*+-/")) {
                Pattern pattern;
                try {
                    pattern = Pattern.compile(argLower);
                } catch (PatternSyntaxException ignore) {
                    pattern = Pattern.compile(Pattern.quote(argLower));
                }
                Pattern finalPattern = pattern;
                return new Filter() {
                    @Override
                    public boolean applies(File file) {
                        String path = file.getPath().toLowerCase(Locale.ROOT);
                        return finalPattern.matcher(path).find();
                    }
                };
            }
            return new Filter() {
                @Override
                public boolean applies(File file) {
                    return StringMan.containsIgnoreCase(file.getPath(), argLower);
                }
            };
        }
        if (arg.endsWith("/") || arg.endsWith(File.separator)) {
            arg = arg.replace("/", File.separator);
            String newDirFilter = dirFilter + arg;
            boolean exists = new File(dir, newDirFilter).exists() || playerFolder && MainUtil.resolveRelative(new File(dir, actor.getUniqueId() + newDirFilter)).exists();
            if (!exists) {
                arg = arg.substring(0, arg.length() - File.separator.length());
                if (arg.length() > 3 && arg.length() <= 16) {
                    UUID fromName = Fawe.imp().getUUID(arg);
                    if (fromName != null) {
                        newDirFilter = dirFilter + fromName + File.separator;
                        listGlobal = true;
                    }
                }
            }
            dirFilter = newDirFilter;
        }
        else {
            filters.add(arg);
        }
    }
}
