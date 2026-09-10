package com.fastasyncworldedit.core.util;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.FaweVersion;
import com.fastasyncworldedit.core.configuration.Caption;
import com.fastasyncworldedit.core.configuration.Settings;
import com.sk89q.util.StringUtil;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.VisibleForTesting;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateNotification {

    private static final String GITHUB_LAST_RELEASE = "https://api.github.com/repos/IntellectualSites/FastAsyncWorldEdit/releases/latest";
    private static final String JENKINS_LAST_BUILD = "https://ci.athion.net/job/FastAsyncWorldEdit/lastSuccessfulBuild/api/json";

    private static final String LINK_DOWNLOAD_JENKINS = "https://ci.athion.net/job/FastAsyncWorldEdit";
    private static final String LINK_DOWNLOAD_MODRINTH = "https://modrinth.com/plugin/fastasyncworldedit";
    private static final String LINK_DOWNLOAD_HANGAR = "https://hangar.papermc.io/IntellectualSites/FastAsyncWorldEdit";

    private static final String CONSOLE_NOTIFICATION_OUTDATED_RELEASE = """
            A new release for FastAsyncWorldEdit is available: {}. You are currently on {}.
            Download from {} or {}""";
    private static final String CONSOLE_NOTIFICATION_OUTDATED_BUILD = """
                                An update for FastAsyncWorldEdit is available. You are {} build(s) out of date.
                                You are running build {}, the latest version is build {}.
                                Update at {}""";

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Pattern GITHUB_RESPONSE_TAG_NAME_PATTERN = Pattern.compile("\"tag_name\":\"([\\d.]+)\"");
    private static final Pattern JENKINS_RESPONSE_BUILD_PATTERN = Pattern.compile("\"number\":(\\d+)");

    private static volatile int[] lastRelease;
    private static volatile int lastBuild = -1;

    /**
     * Check whether a new build with a higher build number than the current build is available.
     */
    public static void doUpdateCheck() {
        if (hasUpdateInfo()) {
            return;
        }
        final FaweVersion installedVersion = Fawe.instance().getVersion();
        if (installedVersion == null || (installedVersion.build == 0 && installedVersion.snapshot)) {
            LOGGER.warn("You are using a snapshot or a custom version of FAWE. " +
                    "This is not an official build distributed via https://ci.athion.net/job/FastAsyncWorldEdit/");
            return;
        }
        if (Settings.settings().ENABLED_COMPONENTS.SNAPSHOT_UPDATE_NOTIFICATIONS && installedVersion.build > 0) {
            checkLatestBuild().orTimeout(10, TimeUnit.SECONDS).whenComplete((build, throwable) -> {
                if (throwable != null) {
                    LOGGER.error("Failed to check for latest build", throwable);
                    return;
                }
                lastBuild = build;
                int difference = lastBuild - installedVersion.build;
                if (difference < 1) {
                    return;
                }
                LOGGER.warn(CONSOLE_NOTIFICATION_OUTDATED_BUILD, difference, installedVersion.build, lastBuild, LINK_DOWNLOAD_JENKINS);
            });
        }
        if (Settings.settings().ENABLED_COMPONENTS.RELEASE_UPDATE_NOTIFICATIONS && installedVersion.semver != null) {
            checkLatestRelease().orTimeout(10, TimeUnit.SECONDS).whenComplete((version, throwable) -> {
                if (throwable != null) {
                    LOGGER.error("Failed to check for latest release", throwable);
                    return;
                }
                lastRelease = version;
                if (hasUpdateSemver(installedVersion.semver, version)) {
                    LOGGER.warn(CONSOLE_NOTIFICATION_OUTDATED_RELEASE,
                            StringUtil.joinString(lastRelease, ".", 0),
                            StringUtil.joinString(installedVersion.semver, ".", 0),
                            LINK_DOWNLOAD_MODRINTH, LINK_DOWNLOAD_HANGAR
                    );
                }
            });
        }
    }

    private static CompletableFuture<int[]> checkLatestRelease() {
        return HTTP_CLIENT.sendAsync(
                HttpRequest.newBuilder().GET().uri(URI.create(GITHUB_LAST_RELEASE)).build(),
                HttpResponse.BodyHandlers.ofString()
        ).thenApply(response -> {
            if (response.statusCode() != 200) {
                throw new UpdateCheckException("GitHub returned status code " + response.statusCode());
            }
            return response.body();
        }).thenApply(body -> {
            final Matcher matcher = GITHUB_RESPONSE_TAG_NAME_PATTERN.matcher(body);
            if (!matcher.find()) {
                throw new UpdateCheckException("Couldn't find tag name in response");
            }
            try {
                return Arrays.stream(matcher.group(1).split("\\.")).toList().stream().mapToInt(Integer::parseInt).toArray();
            } catch (NumberFormatException e) {
                throw new UpdateCheckException("Couldn't parse version", e);
            }
        }).thenApply(version -> {
            if (version.length != 3) {
                throw new UpdateCheckException("Retrieved malformed version (%s)".formatted(Arrays.toString(version)));
            }
            return version;
        });
    }

    private static CompletableFuture<Integer> checkLatestBuild() {
        return HTTP_CLIENT.sendAsync(
                HttpRequest.newBuilder().GET().uri(URI.create(JENKINS_LAST_BUILD)).build(),
                HttpResponse.BodyHandlers.ofString()
        ).thenApply(response -> {
            if (response.statusCode() != 200) {
                throw new UpdateCheckException("Jenkins returned status code " + response.statusCode());
            }
            return response.body();
        }).thenApply(body -> {
            final Matcher matcher = JENKINS_RESPONSE_BUILD_PATTERN.matcher(body);
            if (!matcher.find()) {
                throw new UpdateCheckException("Couldn't find latest build in response");
            }
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                throw new UpdateCheckException("Couldn't parse build", e);
            }
        });
    }

    /**
     * Trigger an update notification based on captions. Useful to notify server administrators ingame.
     *
     * @param actor The player to notify.
     */
    public static void doUpdateNotification(Actor actor) {
        if (!isAnyUpdateCheckEnabled() || !actor.hasPermission("fawe.admin") || !hasUpdateInfo()) {
            return;
        }
        final FaweVersion installed = Fawe.instance().getVersion();
        if (installed == null) {
            return;
        }
        if (lastBuild != -1 && Settings.settings().ENABLED_COMPONENTS.SNAPSHOT_UPDATE_NOTIFICATIONS) {
            int difference = lastBuild - installed.build;
            if (difference > 0) {
                actor.print(Caption.of(
                        "fawe.info.update-available.build",
                        difference, installed.build, lastBuild,
                        TextComponent.of(LINK_DOWNLOAD_JENKINS).clickEvent(ClickEvent.openUrl(LINK_DOWNLOAD_JENKINS))
                ));
            }
        }
        if (installed.semver != null && lastRelease != null && Settings.settings().ENABLED_COMPONENTS.RELEASE_UPDATE_NOTIFICATIONS) {
            if (hasUpdateSemver(installed.semver, lastRelease)) {
                actor.print(Caption.of(
                        "fawe.info.update-available.release",
                        StringUtil.joinString(lastRelease, ".", 0),
                        StringUtil.joinString(installed.semver, ".", 0),
                        TextComponent.empty().children(List.of(
                                TextComponent
                                        .of("Modrinth")
                                        .color(TextColor.GREEN)
                                        .clickEvent(ClickEvent.openUrl(LINK_DOWNLOAD_MODRINTH)),
                                TextComponent.empty().color(TextColor.GRAY)
                        )),
                        TextComponent.empty().children(List.of(
                                TextComponent
                                        .of("Hangar")
                                        .color(TextColor.BLUE)
                                        .clickEvent(ClickEvent.openUrl(LINK_DOWNLOAD_HANGAR)),
                                TextComponent.empty().color(TextColor.GRAY)
                        ))
                ));
            }
        }
    }

    @VisibleForTesting
    static boolean hasUpdateSemver(int[] installed, int[] latest) {
        for (int i = 0; i < Math.max(installed.length, latest.length); i++) {
            final int installedPart = i < installed.length ? installed[i] : 0;
            final int latestPart = i < latest.length ? latest[i] : 0;
            if (installedPart != latestPart) {
                return installedPart < latestPart;
            }
        }
        return false;
    }

    private static boolean hasUpdateInfo() {
        return lastRelease != null || lastBuild != -1;
    }

    private static boolean isAnyUpdateCheckEnabled() {
        return Settings.settings().ENABLED_COMPONENTS.RELEASE_UPDATE_NOTIFICATIONS
                || Settings.settings().ENABLED_COMPONENTS.SNAPSHOT_UPDATE_NOTIFICATIONS;
    }

    private static final class UpdateCheckException extends RuntimeException {

        public UpdateCheckException(final String message) {
            super("Failed to check for update: " + message);
        }

        public UpdateCheckException(final String message, final Throwable cause) {
            super("Failed to check for update: " + message, cause);
        }

    }

}
