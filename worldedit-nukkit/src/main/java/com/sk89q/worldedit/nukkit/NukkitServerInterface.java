package com.sk89q.worldedit.nukkit;

import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.Level;
import com.fastasyncworldedit.core.extent.processor.lighting.RelighterFactory;
import com.fastasyncworldedit.nukkit.NukkitRelighter;
import com.fastasyncworldedit.nukkit.adapter.NukkitImplLoader;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.PermissionCondition;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.AbstractPlatform;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.MultiUserPlatform;
import com.sk89q.worldedit.extension.platform.Preference;
import com.sk89q.worldedit.internal.Constants;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.Registries;
import org.enginehub.piston.CommandManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class NukkitServerInterface extends AbstractPlatform implements MultiUserPlatform {

    private final WorldEditNukkitPlugin plugin;
    private final Server server;
    private boolean hookingEvents;

    public NukkitServerInterface(WorldEditNukkitPlugin plugin, Server server) {
        this.plugin = plugin;
        this.server = server;
    }

    @Override
    public Registries getRegistries() {
        return NukkitRegistries.getInstance();
    }

    @Override
    public int getDataVersion() {
        // Must match the JE block registry version (je_blocks.json from misode/mcmeta)
        return Constants.DATA_VERSION_MC_1_21_10;
    }

    @Override
    public boolean isValidMobType(String type) {
        return type != null && type.startsWith("minecraft:");
    }

    @Override
    public void reload() {
        plugin.loadConfiguration();
    }

    @Override
    public int schedule(long delay, long period, Runnable task) {
        return server.getScheduler()
                .scheduleDelayedRepeatingTask(plugin, task, (int) delay, (int) period)
                .getTaskId();
    }

    @Override
    public List<World> getWorlds() {
        List<World> ret = new ArrayList<>();
        for (Level level : server.getLevels().values()) {
            ret.add(NukkitAdapter.adapt(level));
        }
        return ret;
    }

    @Nullable
    @Override
    public Player matchPlayer(Player player) {
        if (player instanceof NukkitPlayer) {
            return player;
        }
        cn.nukkit.Player nukkitPlayer = server.getPlayerExact(player.getName());
        return nukkitPlayer != null ? NukkitAdapter.adapt(nukkitPlayer) : null;
    }

    @Nullable
    @Override
    public World matchWorld(World world) {
        if (world instanceof NukkitWorld) {
            return world;
        }
        Level level = server.getLevelByName(world.getName());
        return level != null ? NukkitAdapter.adapt(level) : null;
    }

    @Override
    public void registerCommands(CommandManager dispatcher) {
        dispatcher.getAllCommands().forEach(command -> {
            String[] permissionsArray = command.getCondition()
                    .as(PermissionCondition.class)
                    .map(PermissionCondition::getPermissions)
                    .map(s -> s.toArray(new String[0]))
                    .orElseGet(() -> new String[0]);

            String[] aliases = Stream.concat(
                    Stream.of(command.getName()),
                    command.getAliases().stream()
            ).toArray(String[]::new);

            NukkitCommand nukkitCommand = new NukkitCommand(
                    command.getName(),
                    aliases,
                    permissionsArray
            );
            server.getCommandMap().register("worldedit", nukkitCommand);
        });
    }

    @Override
    public void setGameHooksEnabled(boolean enabled) {
        this.hookingEvents = enabled;
    }

    boolean isHookingEvents() {
        return hookingEvents;
    }

    @Override
    public LocalConfiguration getConfiguration() {
        return plugin.getLocalConfiguration();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String getPlatformName() {
        return NukkitImplLoader.get().getPlatformName();
    }

    @Override
    public String getPlatformVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String id() {
        return "intellectualsites:nukkit";
    }

    @Override
    public Map<Capability, Preference> getCapabilities() {
        Map<Capability, Preference> capabilities = new EnumMap<>(Capability.class);
        capabilities.put(Capability.CONFIGURATION, Preference.NORMAL);
        capabilities.put(Capability.WORLDEDIT_CUI, Preference.NORMAL);
        capabilities.put(Capability.GAME_HOOKS, Preference.PREFERRED);
        capabilities.put(Capability.PERMISSIONS, Preference.PREFERRED);
        capabilities.put(Capability.USER_COMMANDS, Preference.PREFERRED);
        capabilities.put(Capability.WORLD_EDITING, Preference.PREFERRED);
        return capabilities;
    }

    @Override
    public Set<SideEffect> getSupportedSideEffects() {
        return Set.of(SideEffect.HEIGHTMAPS, SideEffect.LIGHTING);
    }

    @Override
    public Collection<Actor> getConnectedUsers() {
        List<Actor> users = new ArrayList<>();
        for (cn.nukkit.Player player : server.getOnlinePlayers().values()) {
            users.add(NukkitAdapter.adapt(player));
        }
        return users;
    }

    @Nonnull
    @Override
    public RelighterFactory getRelighterFactory() {
        // Nukkit handles lighting internally; use NukkitRelighter (not NullRelighter) to pass RelightProcessor check
        return (relightMode, world, queue) -> new NukkitRelighter();
    }

    @Override
    public int versionMinY() {
        return -64;
    }

    @Override
    public int versionMaxY() {
        return 319;
    }

    /**
     * A Nukkit Command wrapper that dispatches to WorldEdit.
     */
    private static class NukkitCommand extends Command {

        NukkitCommand(String name, String[] aliases, String[] permissions) {
            super(name);
            if (aliases.length > 1) {
                setAliases(Arrays.copyOfRange(aliases, 1, aliases.length));
            }
            if (permissions.length > 0) {
                setPermission(String.join(";", permissions));
            }
            setDescription("WorldEdit command");
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            StringBuilder sb = new StringBuilder(label);
            for (String arg : args) {
                sb.append(" ").append(arg);
            }
            String commandLine = sb.toString();

            Actor actor;
            if (sender instanceof cn.nukkit.Player player) {
                actor = NukkitAdapter.adapt(player);
            } else {
                actor = new NukkitCommandSender(sender);
            }

            WorldEdit.getInstance().getEventBus().post(
                    new CommandEvent(actor, commandLine)
            );
            return true;
        }

    }

}
