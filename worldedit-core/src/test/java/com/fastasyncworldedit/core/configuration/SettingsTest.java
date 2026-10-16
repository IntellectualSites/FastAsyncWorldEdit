package com.fastasyncworldedit.core.configuration;

import com.fastasyncworldedit.core.limit.FaweLimit;
import com.fastasyncworldedit.core.regions.FaweMaskManager;
import com.sk89q.worldedit.extension.platform.Actor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsTest {

    private static final Settings.BuildInfo BUILD = new Settings.BuildInfo("DATE", "BUILD", "COMMIT", "TEST");

    @TempDir
    Path directory;

    private Path file() {
        return directory.resolve("config.yml");
    }

    private Settings load(String content) throws IOException {
        Files.writeString(file(), content, StandardCharsets.UTF_8);
        return Settings.load(file());
    }

    /**
     * Guards the names, order, comments and defaults of every option. If this fails because an option was added or
     * changed on purpose, update {@code config.yml} in the test resources to match.
     */
    @Test
    void defaultsAreWrittenAsExpected() throws IOException {
        // Pin what depends on the machine and on WorldEdit's own configuration
        Settings settings = Settings.defaults()
                .with("queue.parallel-threads", 16)
                .with("queue.target-size", 128)
                .with("queue.thread-target-size-percent", 12)
                .with("limits.default.max-radius", 0)
                .with("limits.default.max-super-pickaxe-size", 0)
                .with("limits.default.max-brush-radius", 0)
                .with("limits.default.max-butcher-radius", 0);
        settings.save(file(), BUILD);

        String expected;
        try (InputStream resource = SettingsTest.class.getResourceAsStream("config.yml")) {
            expected = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertEquals(expected.replace("\r\n", "\n"), Files.readString(file()).replace("\r\n", "\n"));
    }

    @Test
    void savedSettingsLoadBackUnchanged() throws IOException {
        Settings settings = Settings.defaults()
                .with("paths.clipboard", "C:\\shared\\clipboards")
                .with("web.url", "say \"hi\" # not a comment: \u00e9")
                .with("queue.progress.display", "")
                .with("region-restrictions-options.mode", "owner")
                .with("limits.default.max-changes", Long.MAX_VALUE)
                .with("limits.default.strip-nbt", List.of("Items", " padded ", "a: b", "- c"));
        settings.save(file(), BUILD);

        assertEquals(settings, Settings.load(file()));
        assertEquals(FaweMaskManager.MaskType.OWNER, settings.regionRestrictionsOptions().mode());
    }

    @Test
    void limitGroupsKeepTheirNamesWhenSaved() throws IOException {
        // Names YAML would read as something other than text, or could not write without quotes
        Settings settings = load("""
                limits:
                  "on":
                    max-radius: 1
                  "123":
                    max-radius: 2
                  "VIP group":
                    max-radius: 3
                  "a: b":
                    max-radius: 4
                """);
        settings.save(file(), BUILD);

        Settings loaded = Settings.load(file());
        assertEquals(settings, loaded);
        assertEquals(List.of("on", "123", "VIP group", "a: b", Settings.DEFAULT_LIMIT), List.copyOf(loaded.limits().keySet()));
    }

    @Test
    void missingFileGivesDefaults() throws IOException {
        assertEquals(Settings.defaults(), Settings.load(file()));
        assertFalse(Files.exists(file()));
    }

    @Test
    void unchangedFileIsNotRewritten() throws IOException {
        Settings.defaults().save(file(), BUILD);
        FileTime longAgo = FileTime.fromMillis(1_000_000_000_000L);
        Files.setLastModifiedTime(file(), longAgo);

        Settings.load(file()).save(file(), BUILD);

        assertEquals(longAgo, Files.getLastModifiedTime(file()));
    }

    @Test
    void changedFileIsRewrittenAndMissingDirectoriesAreCreated() throws IOException {
        Path nested = directory.resolve("plugins").resolve("FastAsyncWorldEdit").resolve("config.yml");
        Settings.defaults().save(nested, BUILD);
        assertEquals(Settings.defaults(), Settings.load(nested));

        // Same length, different content: only comparing the bytes tells them apart
        Settings changed = Settings.defaults().with(Settings.Clipboard::compressionLevel, 2);
        changed.save(nested, BUILD);
        assertEquals(changed, Settings.load(nested));
    }

    @Test
    void unusableValuesKeepTheirDefault() throws IOException {
        Settings defaults = Settings.defaults();
        Settings settings = load("""
                max-memory-percent: "not a number"
                slower-memory-percent: 12345678901
                region-restrictions: 1
                unknown-option: true
                web:
                  url:
                  max-image-size: 1.5
                  allowed-image-hosts: i.imgur.com
                extent: 5
                region-restrictions-options:
                  mode: nobody
                  exclusive-managers:
                  - PlotSquared
                  - 12
                experimental:
                  other: yes
                """);

        assertEquals(defaults.maxMemoryPercent(), settings.maxMemoryPercent());
        assertEquals(defaults.slowerMemoryPercent(), settings.slowerMemoryPercent());
        assertEquals(defaults.regionRestrictions(), settings.regionRestrictions());
        assertEquals(defaults.web(), settings.web());
        assertEquals(defaults.extent(), settings.extent());
        assertEquals(FaweMaskManager.MaskType.MEMBER, settings.regionRestrictionsOptions().mode());
        // Values that can be read as text are, as they would be if quoted
        assertEquals(List.of("PlotSquared", "12"), settings.regionRestrictionsOptions().exclusiveManagers());
        assertTrue(settings.experimental().other());
    }

    @Test
    void optionsOfOlderVersionsAreMoved() throws IOException {
        Settings settings = load("""
                issues: "ignored"
                enabled-components:
                  update-notification: false
                  release-update-notifications: true
                experimental:
                  per-player-file-size-limit: 123
                  per-player-file-num-limit: 45
                queue:
                  parallel-threads: 5
                history:
                  delete-on-logout: true
                limits:
                  default:
                    schem-file-num-limit: 7
                  builder:
                    max-changes: -1
                """);

        assertFalse(settings.enabledComponents().snapshotUpdateNotifications());
        assertTrue(settings.enabledComponents().releaseUpdateNotifications());
        assertFalse(settings.enabledComponents().notifyUpdateIngame());
        assertEquals(123, settings.defaultLimits().schemFileSizeLimit());
        assertEquals(7, settings.defaultLimits().schemFileNumLimit());
        assertEquals(123, settings.limits().get("builder").schemFileSizeLimit());
        assertEquals(45, settings.limits().get("builder").schemFileNumLimit());
        assertEquals(40, settings.queue().threadTargetSizePercent());
        assertTrue(settings.history().deleteDiskOnLogout());
    }

    @Test
    void schematicLimitsMoveIntoTheDefaultGroupWhenThereAreNoGroups() throws IOException {
        Settings settings = load("""
                experimental:
                  per-player-file-size-limit: 123
                """);

        assertEquals(Set.of(Settings.DEFAULT_LIMIT), settings.limits().keySet());
        assertEquals(123, settings.defaultLimits().schemFileSizeLimit());
    }

    @Test
    void newerOptionsWinOverTheOnesTheyReplaced() throws IOException {
        Settings settings = load("""
                queue:
                  parallel-threads: 5
                  thread-target-size-percent: 90
                history:
                  delete-on-logout: true
                  delete-disk-on-logout: false
                """);

        assertEquals(90, settings.queue().threadTargetSizePercent());
        assertFalse(settings.history().deleteDiskOnLogout());
    }

    @Test
    void invalidYamlIsCopiedAsideAndDefaultsAreUsed() throws IOException {
        String broken = "queue:\n  parallel-threads: [1,\n";

        assertEquals(Settings.defaults(), load(broken));
        assertEquals(broken, Files.readString(directory.resolve("config.yml_broken")));

        Settings.load(file());
        assertEquals(broken, Files.readString(directory.resolve("config.yml_broken_0")));
    }

    @Test
    void withReplacesOneOption() {
        Settings defaults = Settings.defaults();
        Settings settings = defaults.with("clipboard.use-disk", false);

        assertFalse(settings.clipboard().useDisk());
        assertTrue(defaults.clipboard().useDisk());
        assertEquals(defaults, settings.with("clipboard.use-disk", true));
        assertThrows(IllegalArgumentException.class, () -> defaults.with("clipboard.no-such-option", false));
        assertThrows(IllegalArgumentException.class, () -> defaults.with("clipboard.use-disk", "sometimes"));
        assertThrows(IllegalArgumentException.class, () -> defaults.with("clipboard", false));
        assertThrows(IllegalArgumentException.class, () -> defaults.with("limits.no-such-group.max-changes", 1L));
    }

    @Test
    void overrideChangesTheSettingsInUse() {
        Settings before = Settings.settings();
        try {
            Settings.override(Settings.Experimental::persistentBrushes, !before.experimental().persistentBrushes());

            assertEquals(!before.experimental().persistentBrushes(), Settings.settings().experimental().persistentBrushes());
            assertEquals(before.queue(), Settings.settings().queue());
        } finally {
            Settings.set(before);
        }
    }

    @Test
    void anOptionIsNamedByItsAccessor() {
        Settings defaults = Settings.defaults();
        Settings settings = defaults
                .with(Settings::maxMemoryPercent, 50)
                .with(Settings.Clipboard::compressionLevel, 6)
                .with(Settings.History::compressionLevel, 9)
                .with(Settings.Queue.Progress::display, "chat")
                .with(Settings.Web::allowedImageHosts, List.of("example.com"))
                .with(Settings.RegionRestrictionsOptions::mode, FaweMaskManager.MaskType.OWNER);

        assertEquals(50, settings.maxMemoryPercent());
        assertEquals(6, settings.clipboard().compressionLevel());
        assertEquals(9, settings.history().compressionLevel());
        assertEquals("chat", settings.queue().progress().display());
        assertEquals(List.of("example.com"), settings.web().allowedImageHosts());
        assertEquals(FaweMaskManager.MaskType.OWNER, settings.regionRestrictionsOptions().mode());
        assertEquals(defaults.history().bufferSize(), settings.history().bufferSize());
        // A lambda does not say which accessor it calls, and an accessor does not say which limit group is meant
        assertThrows(IllegalArgumentException.class, () -> defaults.with((Settings.Queue queue) -> queue.pool(), false));
        assertThrows(IllegalArgumentException.class, () -> defaults.with(Settings.Limits::maxRadius, 5));
    }

    @Test
    void targetSizeIsAtLeastParallelThreads() throws IOException {
        Settings settings = load("""
                queue:
                  parallel-threads: 6
                  target-size: 3
                """);

        assertEquals(6, settings.queue().targetSize());
    }

    @Test
    void theDefaultLimitGroupAlwaysExists() throws IOException {
        Settings settings = load("""
                limits:
                  builder:
                    max-changes: 10
                """);

        assertEquals(List.of("builder", Settings.DEFAULT_LIMIT), List.copyOf(settings.limits().keySet()));
        assertEquals(10, settings.limits().get("builder").maxChanges());
        // What a group does not set comes from the built-in defaults, not from the default group
        assertEquals(Settings.defaults().defaultLimits().maxChecks(), settings.limits().get("builder").maxChecks());
    }

    @Test
    void anActorGetsTheMostGenerousValueOfTheirGroups() throws IOException {
        Settings settings = load("""
                limits:
                  default:
                    max-changes: 10
                    max-radius: 5
                    inventory-mode: 2
                    confirm-large: true
                    strip-nbt: [Items, Lock]
                    disallowed-blocks: ["minecraft:wheat", "minecraft:fire"]
                  builder:
                    max-changes: 500
                    max-radius: -1
                    inventory-mode: 1
                    confirm-large: false
                    strip-nbt: [Items]
                    disallowed-blocks: [WHEAT]
                  other:
                    max-changes: 100000
                """);
        Actor guest = Mockito.mock(Actor.class);
        Actor builder = Mockito.mock(Actor.class);
        Mockito.when(builder.hasPermission("fawe.limit.builder")).thenReturn(true);
        Mockito.when(builder.hasPermission("fawe.limit.default")).thenReturn(true);
        Actor builderOnly = Mockito.mock(Actor.class);
        Mockito.when(builderOnly.hasPermission("fawe.limit.builder")).thenReturn(true);

        FaweLimit guestLimit = settings.getLimit(guest);
        assertEquals(10, guestLimit.MAX_CHANGES.get());
        assertEquals(5, guestLimit.MAX_RADIUS);
        assertEquals(2, guestLimit.INVENTORY_MODE);
        assertTrue(guestLimit.CONFIRM_LARGE);
        assertEquals(Set.of("Items", "Lock"), guestLimit.STRIP_NBT);
        assertEquals(Set.of("minecraft:wheat", "minecraft:fire"), guestLimit.DISALLOWED_BLOCKS);
        assertEquals(Set.of(), guestLimit.REMAP_PROPERTIES);

        FaweLimit builderLimit = settings.getLimit(builder);
        assertEquals(500, builderLimit.MAX_CHANGES.get());
        assertEquals(Integer.MAX_VALUE, builderLimit.MAX_RADIUS);
        assertEquals(1, builderLimit.INVENTORY_MODE);
        assertFalse(builderLimit.CONFIRM_LARGE);
        assertEquals(Set.of("Items"), builderLimit.STRIP_NBT);
        assertEquals(Set.of("minecraft:wheat"), builderLimit.DISALLOWED_BLOCKS);

        // The default group only applies to those in no other group, unless they have its permission too
        assertEquals(500, settings.getLimit(builderOnly).MAX_CHANGES.get());
        assertEquals(Integer.MAX_VALUE, settings.getLimit(builderOnly).MAX_RADIUS);

        // Each call starts from fresh counters
        guestLimit.MAX_CHANGES.set(0);
        assertEquals(10, settings.getLimit(guest).MAX_CHANGES.get());
    }

    @Test
    void theUnlimitedPermissionBypassesTheGroups() {
        Actor admin = Mockito.mock(Actor.class);
        Mockito.when(admin.hasPermission("fawe.limit.unlimited")).thenReturn(true);

        assertTrue(Settings.defaults().getLimit(admin).isUnlimited());
    }

}
