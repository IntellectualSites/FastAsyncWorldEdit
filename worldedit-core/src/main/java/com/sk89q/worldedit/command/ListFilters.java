package com.sk89q.worldedit.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.StringMan;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import java.io.File;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.exception.StopExecutionException;

//TODO This class breaks compilation
//@CommandContainer
public class ListFilters {
    public class Filter {
        public boolean listPrivate() {
            return true;
        }

        public boolean listPublic() {
            return false;
        }

        public File getPath(File root) {
            return null;
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
            name = "*", //TODO originally this was left blank but doing so causes a major compilation error
            desc = "wildcard"
    )
    public Filter wildcard(Actor actor, File root, String arg) {
        arg = arg.replace("/", File.separator);
        String argLower = arg.toLowerCase(Locale.ROOT);
        if (arg.endsWith(File.separator)) {
            String finalArg = arg;
            return new Filter() {
                @Override
                public File getPath(File root) {
                    File newRoot = new File(root, finalArg);
                    if (newRoot.exists()) return newRoot;
                    String firstArg = finalArg.substring(0, finalArg.length() - File.separator.length());
                    if (firstArg.length() > 3 && firstArg.length() <= 16) {
                        UUID fromName = Fawe.imp().getUUID(finalArg);
                        if (fromName != null) {
                            newRoot = new File(root, finalArg);
                            if (newRoot.exists()) return newRoot;
                        }
                    }
                    throw new StopExecutionException(TextComponent.of("Cannot find path: " + finalArg));
                }
            };
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
    }
}
