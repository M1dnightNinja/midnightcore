package org.wallentines.midnightcore.common;

import org.wallentines.midnightcore.api.Registries;
import org.wallentines.midnightcore.api.item.InventoryGUI;
import org.wallentines.midnightcore.api.item.MItemStack;
import org.wallentines.midnightcore.api.module.ServerModule;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightcore.api.player.PlayerManager;
import org.wallentines.midnightcore.api.server.MServer;
import org.wallentines.midnightcore.api.text.CustomScoreboard;
import org.wallentines.midnightcore.api.text.MComponent;
import org.wallentines.midnightcore.common.server.AbstractServer;
import org.wallentines.midnightlib.Version;
import org.wallentines.midnightlib.config.ConfigSection;
import org.wallentines.midnightlib.config.FileConfig;
import org.wallentines.midnightlib.module.ModuleManager;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightcore.api.MidnightCoreAPI;
import org.wallentines.midnightcore.common.util.FileUtil;
import org.wallentines.midnightlib.requirement.RequirementType;

import java.io.File;
import java.nio.file.Path;
import java.util.Random;

public class MidnightCoreImpl extends MidnightCoreAPI {

    private final FileConfig config;
    private final File dataFolder;
    private final Version gameVersion;
    private final Random random = new Random();


    private final MItemStack.Factory itemFactory;
    private final InventoryGUI.Factory guiFactory;
    private final CustomScoreboard.Factory scoreboardFactory;

    private AbstractServer currentServer;

    public MidnightCoreImpl(Path dataFolder, Version gameVersion, MItemStack.Factory itemFactory, InventoryGUI.Factory guiFactory, CustomScoreboard.Factory scoreboardFactory) {

        super();

        this.dataFolder = FileUtil.tryCreateDirectory(dataFolder);
        this.itemFactory = itemFactory;
        this.guiFactory = guiFactory;
        this.scoreboardFactory = scoreboardFactory;
        if(this.dataFolder == null) {
            throw new IllegalStateException("Unable to create data folder!");
        }

        this.config = FileConfig.findOrCreate("config", this.dataFolder, Constants.CONFIG_DEFAULTS);
        if(this.config == null) {
            throw new IllegalStateException("Unable to create config!");
        }
        this.config.getRoot().fill(Constants.CONFIG_DEFAULTS);
        this.config.save();

        this.gameVersion = gameVersion;
    }

    public void setActiveServer(AbstractServer server) {
        this.currentServer = server;

        if(server != null) {

            // Load Modules
            ConfigSection sec = getConfig().getOrCreateSection("modules");
            server.loadModules(sec, Registries.MODULE_REGISTRY);

            config.save();
        }
    }

    @Override
    public ConfigSection getConfig() {

        return config.getRoot();
    }

    @Override
    public void saveConfig() {

        config.getRoot().fill(Constants.CONFIG_DEFAULTS);
        config.save();
    }

    @Override
    public File getDataFolder() {
        return dataFolder;
    }

    @Override
    public Version getGameVersion() {
        return gameVersion;
    }

    @Override
    public ModuleManager<MServer, ServerModule> getModuleManager() {

        return currentServer.getModuleManager();
    }

    @Override
    public Registry<RequirementType<MPlayer>> getRequirementRegistry() {

        return Registries.REQUIREMENT_REGISTRY;
    }

    @Override
    public PlayerManager getPlayerManager() {

        return currentServer.getPlayerManager();
    }

    @Override
    public MItemStack createItem(Identifier id, int count, ConfigSection nbt) {

        return itemFactory.create(id, count, nbt);
    }

    @Override
    public InventoryGUI createGUI(MComponent title) {
        return guiFactory.create(title);
    }

    @Override
    public CustomScoreboard createScoreboard(String id, MComponent title) {
        return scoreboardFactory.create(id, title);
    }

    @Override
    public void executeConsoleCommand(String command, boolean log) {
        currentServer.executeCommand(command, log);
    }


    @Override
    public void executeOnServer(Runnable runnable) {
        currentServer.submit(runnable);
    }

    @Override
    public MServer getServer() {
        return currentServer;
    }

    @Override
    public Random getRandom() {
        return random;
    }

    @Override
    public String getServerLocale() {
        return config.getRoot().getString("locale");
    }

    @Override
    public void reload() {
        config.reload();

        ConfigSection sec = config.getRoot().getOrCreateSection("modules");
        currentServer.reloadModules(sec, Registries.MODULE_REGISTRY);

        config.save();
    }

    @Override
    public void unloadModules() {
        currentServer.getModuleManager().unloadAll();
    }
}
