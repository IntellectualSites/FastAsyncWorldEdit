package com.fastasyncworldedit.util;

import com.destroystokyo.paper.entity.ai.MobGoals;
import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.datapack.DatapackManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.BanList;
import org.bukkit.GameMode;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.StructureType;
import org.bukkit.Tag;
import org.bukkit.UnsafeValues;
import org.bukkit.Warning;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.advancement.Advancement;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.help.HelpMap;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.Recipe;
import org.bukkit.loot.LootTable;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.CachedServerIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class StubServer implements Server {

    @Override
    public @NotNull
    String getName() {
        return null;
    }

    @Override
    public @NotNull
    String getVersion() {
        return "STUB";
    }

    @Override
    public @NotNull
    String getBukkitVersion() {
        return null;
    }

    @Override
    public @NotNull
    String getMinecraftVersion() {
        return null;
    }

    @Override
    public @NotNull
    Collection<? extends Player> getOnlinePlayers() {
        return null;
    }

    @Override
    public int getMaxPlayers() {
        return 0;
    }

    @Override
    public void setMaxPlayers(int i) {

    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public int getViewDistance() {
        return 0;
    }

    @Override
    public int getSimulationDistance() {
        return 12;
    }

    @Override
    public @NotNull
    String getIp() {
        return null;
    }

    @Override
    public @NotNull
    String getWorldType() {
        return null;
    }

    @Override
    public boolean getGenerateStructures() {
        return false;
    }

    @Override
    public int getMaxWorldSize() {
        return 0;
    }

    @Override
    public boolean getAllowEnd() {
        return false;
    }

    @Override
    public boolean getAllowNether() {
        return false;
    }

    @Override
    public @NotNull String getResourcePack() {
        return null;
    }

    @Override
    public @NotNull String getResourcePackHash() {
        return null;
    }

    @Override
    public @NotNull String getResourcePackPrompt() {
        return null;
    }

    @Override
    public boolean isResourcePackRequired() {
        return false;
    }

    @Override
    public boolean hasWhitelist() {
        return false;
    }

    @Override
    public void setWhitelist(boolean b) {

    }

    @Override
    public boolean isWhitelistEnforced() {
        return false;
    }

    @Override
    public void setWhitelistEnforced(final boolean b) {

    }

    @Override
    public @NotNull
    Set<OfflinePlayer> getWhitelistedPlayers() {
        return null;
    }

    @Override
    public void reloadWhitelist() {

    }

    /**
     * @param s
     * @deprecated
     */
    @Override
    public int broadcastMessage(@NotNull String s) {
        return 0;
    }

    @Override
    public @NotNull
    String getUpdateFolder() {
        return null;
    }

    @Override
    public @NotNull
    File getUpdateFolderFile() {
        return null;
    }

    @Override
    public long getConnectionThrottle() {
        return 0;
    }

    @Override
    public int getTicksPerAnimalSpawns() {
        return 0;
    }

    @Override
    public int getTicksPerMonsterSpawns() {
        return 0;
    }

    @Override
    public int getTicksPerWaterSpawns() {
        return 0;
    }

    @Override
    public int getTicksPerWaterAmbientSpawns() {
        return 0;
    }

    @Override
    public int getTicksPerWaterUndergroundCreatureSpawns() {
        return 0;
    }

    @Override
    public int getTicksPerAmbientSpawns() {
        return 0;
    }

    @Override
    public @Nullable
    Player getPlayer(@NotNull String s) {
        return null;
    }

    @Override
    public @Nullable
    Player getPlayerExact(@NotNull String s) {
        return null;
    }

    @Override
    public @NotNull
    List<Player> matchPlayer(@NotNull String s) {
        return null;
    }

    @Override
    public @Nullable
    Player getPlayer(@NotNull UUID uuid) {
        return null;
    }

    @Override
    public @Nullable
    UUID getPlayerUniqueId(@NotNull String s) {
        return null;
    }

    @Override
    public @NotNull
    PluginManager getPluginManager() {
        return null;
    }

    @Override
    public @NotNull
    BukkitScheduler getScheduler() {
        return null;
    }

    @Override
    public @NotNull
    ServicesManager getServicesManager() {
        return null;
    }

    @Override
    public @NotNull
    List<World> getWorlds() {
        return null;
    }

    @Override
    public @Nullable
    World createWorld(@NotNull WorldCreator worldCreator) {
        return null;
    }

    @Override
    public boolean unloadWorld(@NotNull String s, boolean b) {
        return false;
    }

    @Override
    public boolean unloadWorld(@NotNull World world, boolean b) {
        return false;
    }

    @Override
    public @Nullable
    World getWorld(@NotNull String s) {
        return null;
    }

    @Override
    public @Nullable
    World getWorld(@NotNull UUID uuid) {
        return null;
    }

    @Override
    public @Nullable
    World getWorld(@NotNull NamespacedKey namespacedKey) {
        return null;
    }

    /**
     * @param i
     * @deprecated
     */
    @Override
    public @Nullable
    MapView getMap(int i) {
        return null;
    }

    @Override
    public @NotNull
    MapView createMap(@NotNull World world) {
        return null;
    }

    @Override
    public @NotNull
    ItemStack createExplorerMap(
            @NotNull World world,
            @NotNull Location location,
            @NotNull StructureType structureType
    ) {
        return null;
    }

    @Override
    public @NotNull
    ItemStack createExplorerMap(
            @NotNull World world,
            @NotNull Location location,
            @NotNull StructureType structureType,
            int i,
            boolean b
    ) {
        return null;
    }

    @Override
    public void reload() {

    }

    @Override
    public void reloadData() {

    }

    @Override
    public @NotNull
    Logger getLogger() {
        return Logger.getLogger("StubServer");
    }

    @Override
    public @Nullable
    PluginCommand getPluginCommand(@NotNull String s) {
        return null;
    }

    @Override
    public void savePlayers() {

    }

    @Override
    public boolean dispatchCommand(@NotNull CommandSender commandSender, @NotNull String s) throws CommandException {
        return false;
    }

    @Override
    public boolean addRecipe(@Nullable Recipe recipe) {
        return false;
    }

    @Override
    public @NotNull
    List<Recipe> getRecipesFor(@NotNull ItemStack itemStack) {
        return null;
    }

    @Override
    public @Nullable
    Recipe getRecipe(@NotNull NamespacedKey namespacedKey) {
        return null;
    }

    @Override
    public @Nullable
    Recipe getCraftingRecipe(final @NotNull ItemStack[] itemStacks, @NotNull final World world) {
        return null;
    }

    @Override
    public @NotNull
    ItemStack craftItem(
            final @NotNull ItemStack[] itemStacks,
            @NotNull final World world,
            @NotNull final Player player
    ) {
        return null;
    }

    @Override
    public @NotNull
    Iterator<Recipe> recipeIterator() {
        return null;
    }

    @Override
    public void clearRecipes() {

    }

    @Override
    public void resetRecipes() {

    }

    @Override
    public boolean removeRecipe(@NotNull NamespacedKey namespacedKey) {
        return false;
    }

    @Override
    public @NotNull
    Map<String, String[]> getCommandAliases() {
        return null;
    }

    @Override
    public int getSpawnRadius() {
        return 0;
    }

    @Override
    public void setSpawnRadius(int i) {

    }

    @Override
    public boolean getHideOnlinePlayers() {
        return false;
    }

    @Override
    public boolean getOnlineMode() {
        return false;
    }

    @Override
    public boolean getAllowFlight() {
        return false;
    }

    @Override
    public boolean isHardcore() {
        return false;
    }

    @Override
    public void shutdown() {

    }

    /**
     * @param s
     * @param s1
     * @deprecated
     */
    @Override
    public int broadcast(@NotNull String s, @NotNull String s1) {
        return 0;
    }

    @Override
    public int broadcast(@NotNull final Component component) {
        return 0;
    }

    @Override
    public int broadcast(@NotNull Component component, @NotNull String s) {
        return 0;
    }

    /**
     * @param s
     * @deprecated
     */
    @Override
    public @NotNull
    OfflinePlayer getOfflinePlayer(@NotNull String s) {
        return null;
    }

    @Override
    public @Nullable
    OfflinePlayer getOfflinePlayerIfCached(@NotNull String s) {
        return null;
    }

    @Override
    public @NotNull
    OfflinePlayer getOfflinePlayer(@NotNull UUID uuid) {
        return null;
    }

    @Override
    public @NotNull
    Set<String> getIPBans() {
        return null;
    }

    @Override
    public void banIP(@NotNull String s) {

    }

    @Override
    public void unbanIP(@NotNull String s) {

    }

    @Override
    public @NotNull
    Set<OfflinePlayer> getBannedPlayers() {
        return null;
    }

    @Override
    public @NotNull
    BanList getBanList(BanList.@NotNull Type type) {
        return null;
    }

    @Override
    public @NotNull
    Set<OfflinePlayer> getOperators() {
        return null;
    }

    @Override
    public @NotNull
    GameMode getDefaultGameMode() {
        return null;
    }

    @Override
    public void setDefaultGameMode(@NotNull GameMode gameMode) {

    }

    @Override
    public @NotNull
    ConsoleCommandSender getConsoleSender() {
        return null;
    }

    @Override
    public @NotNull CommandSender createCommandSender(@NotNull final Consumer<? super Component> feedback) {
        return null;
    }

    @Override
    public @NotNull
    File getWorldContainer() {
        return null;
    }

    @Override
    public @NotNull
    OfflinePlayer[] getOfflinePlayers() {
        return new OfflinePlayer[0];
    }

    @Override
    public @NotNull
    Messenger getMessenger() {
        return null;
    }

    @Override
    public @NotNull
    HelpMap getHelpMap() {
        return null;
    }

    @Override
    public @NotNull
    Inventory createInventory(@Nullable InventoryHolder inventoryHolder, @NotNull InventoryType inventoryType) {
        return null;
    }

    @Override
    public @NotNull
    Inventory createInventory(
            @Nullable InventoryHolder inventoryHolder,
            @NotNull InventoryType inventoryType,
            @NotNull Component component
    ) {
        return null;
    }

    /**
     * @param inventoryHolder
     * @param inventoryType
     * @param s
     * @deprecated
     */
    @Override
    public @NotNull
    Inventory createInventory(
            @Nullable InventoryHolder inventoryHolder,
            @NotNull InventoryType inventoryType,
            @NotNull String s
    ) {
        return null;
    }

    @Override
    public @NotNull
    Inventory createInventory(@Nullable InventoryHolder inventoryHolder, int i) throws IllegalArgumentException {
        return null;
    }

    @Override
    public @NotNull
    Inventory createInventory(
            @Nullable InventoryHolder inventoryHolder,
            int i,
            @NotNull Component component
    ) throws IllegalArgumentException {
        return null;
    }

    /**
     * @param inventoryHolder
     * @param i
     * @param s
     * @deprecated
     */
    @Override
    public @NotNull
    Inventory createInventory(@Nullable InventoryHolder inventoryHolder, int i, @NotNull String s) throws
            IllegalArgumentException {
        return null;
    }

    @Override
    public @NotNull
    Merchant createMerchant(@Nullable Component component) {
        return null;
    }

    /**
     * @param s
     * @deprecated
     */
    @Override
    public @NotNull
    Merchant createMerchant(@Nullable String s) {
        return null;
    }

    @Override
    public int getMonsterSpawnLimit() {
        return 0;
    }

    @Override
    public int getAnimalSpawnLimit() {
        return 0;
    }

    @Override
    public int getWaterAnimalSpawnLimit() {
        return 0;
    }

    @Override
    public int getWaterAmbientSpawnLimit() {
        return 0;
    }

    @Override
    public int getWaterUndergroundCreatureSpawnLimit() {
        return 0;
    }

    @Override
    public int getAmbientSpawnLimit() {
        return 0;
    }

    @Override
    public boolean isPrimaryThread() {
        return false;
    }

    @Override
    public @NotNull
    Component motd() {
        return null;
    }

    /**
     * @deprecated
     */
    @Override
    public @NotNull
    String getMotd() {
        return null;
    }

    @Override
    public @Nullable
    Component shutdownMessage() {
        return null;
    }

    /**
     * @deprecated
     */
    @Override
    public @Nullable
    String getShutdownMessage() {
        return null;
    }

    @Override
    public Warning.@NotNull WarningState getWarningState() {
        return null;
    }

    @Override
    public @NotNull
    ItemFactory getItemFactory() {
        return null;
    }

    @Override
    public @NotNull
    ScoreboardManager getScoreboardManager() {
        return null;
    }

    @Override
    public @Nullable
    CachedServerIcon getServerIcon() {
        return null;
    }

    @Override
    public @NotNull
    CachedServerIcon loadServerIcon(@NotNull File file) throws IllegalArgumentException, Exception {
        return null;
    }

    @Override
    public @NotNull
    CachedServerIcon loadServerIcon(@NotNull BufferedImage bufferedImage) throws IllegalArgumentException,
            Exception {
        return null;
    }

    @Override
    public void setIdleTimeout(int i) {

    }

    @Override
    public int getIdleTimeout() {
        return 0;
    }

    @Override
    public ChunkGenerator.@NotNull ChunkData createChunkData(@NotNull World world) {
        return null;
    }

    @Override
    public ChunkGenerator.@NotNull ChunkData createVanillaChunkData(@NotNull World world, int i, int i1) {
        return null;
    }

    @Override
    public @NotNull
    BossBar createBossBar(
            @Nullable String s,
            @NotNull BarColor barColor,
            @NotNull BarStyle barStyle,
            @NotNull BarFlag... barFlags
    ) {
        return null;
    }

    @Override
    public @NotNull
    KeyedBossBar createBossBar(
            @NotNull NamespacedKey namespacedKey,
            @Nullable String s,
            @NotNull BarColor barColor,
            @NotNull BarStyle barStyle,
            @NotNull BarFlag... barFlags
    ) {
        return null;
    }

    @Override
    public @NotNull
    Iterator<KeyedBossBar> getBossBars() {
        return null;
    }

    @Override
    public @Nullable
    KeyedBossBar getBossBar(@NotNull NamespacedKey namespacedKey) {
        return null;
    }

    @Override
    public boolean removeBossBar(@NotNull NamespacedKey namespacedKey) {
        return false;
    }

    @Override
    public @Nullable
    Entity getEntity(@NotNull UUID uuid) {
        return null;
    }

    @Override
    public @NotNull
    double[] getTPS() {
        return new double[0];
    }

    @Override
    public @NotNull
    long[] getTickTimes() {
        return new long[0];
    }

    @Override
    public double getAverageTickTime() {
        return 0;
    }

    @Override
    public @NotNull
    CommandMap getCommandMap() {
        return null;
    }

    @Override
    public @Nullable
    Advancement getAdvancement(@NotNull NamespacedKey namespacedKey) {
        return null;
    }

    @Override
    public @NotNull
    Iterator<Advancement> advancementIterator() {
        return null;
    }

    @Override
    public @NotNull
    BlockData createBlockData(@NotNull Material material) {
        return null;
    }

    @Override
    public @NotNull
    BlockData createBlockData(@NotNull Material material, @Nullable Consumer<BlockData> consumer) {
        return null;
    }

    @Override
    public @NotNull
    BlockData createBlockData(@NotNull String s) throws IllegalArgumentException {
        return null;
    }

    @Override
    public @NotNull
    BlockData createBlockData(@Nullable Material material, @Nullable String s) throws IllegalArgumentException {
        return null;
    }

    @Override
    public <T extends Keyed> Tag<T> getTag(@NotNull String s, @NotNull NamespacedKey namespacedKey, @NotNull Class<T> aClass) {
        return null;
    }

    @Override
    public @NotNull <T extends Keyed> Iterable<Tag<T>> getTags(@NotNull String s, @NotNull Class<T> aClass) {
        return null;
    }

    @Override
    public @Nullable
    LootTable getLootTable(@NotNull NamespacedKey namespacedKey) {
        return null;
    }

    @Override
    public @NotNull
    List<Entity> selectEntities(@NotNull CommandSender commandSender, @NotNull String s) throws
            IllegalArgumentException {
        return null;
    }

    @Override
    public @NotNull StructureManager getStructureManager() {
        return null;
    }

    /**
     * @deprecated
     */
    @Override
    public @NotNull
    UnsafeValues getUnsafe() {
        return null;
    }

    @Override
    public @NotNull
    Spigot spigot() {
        return null;
    }

    @Override
    public void reloadPermissions() {

    }

    @Override
    public boolean reloadCommandAliases() {
        return false;
    }

    @Override
    public boolean suggestPlayerNamesWhenNullTabCompletions() {
        return false;
    }

    @Override
    public @NotNull
    String getPermissionMessage() {
        return null;
    }

    @Override
    public @NotNull
    PlayerProfile createProfile(@NotNull UUID uuid) {
        return null;
    }

    @Override
    public @NotNull
    PlayerProfile createProfile(@NotNull String s) {
        return null;
    }

    @Override
    public @NotNull
    PlayerProfile createProfile(@Nullable UUID uuid, @Nullable String s) {
        return null;
    }

    @Override
    public int getCurrentTick() {
        return 0;
    }

    @Override
    public @NotNull File getPluginsFolder() {
        return null;
    }

    @Override
    public boolean isStopping() {
        return false;
    }

    @Override
    public @NotNull
    MobGoals getMobGoals() {
        return null;
    }

    @Override
    public @NotNull
    DatapackManager getDatapackManager() {
        return null;
    }

    @Override
    public @NotNull
    Iterable<? extends Audience> audiences() {
        return null;
    }

    @Override
    public void sendPluginMessage(@NotNull Plugin plugin, @NotNull String s, @NotNull byte[] bytes) {

    }

    @Override
    public @NotNull
    Set<String> getListeningPluginChannels() {
        return null;
    }

}
