package org.wallentines.mcore;

import org.wallentines.mcore.lang.LangManager;
import org.wallentines.mcore.lang.LangRegistry;
import org.wallentines.mcore.lang.PlaceholderManager;
import org.wallentines.mcore.lang.PlaceholderSupplier;
import org.wallentines.mcore.requirement.CooldownRequirement;
import org.wallentines.mcore.requirement.PlayerCheck;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.midnightlib.math.Region;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightlib.requirement.CheckType;
import org.wallentines.midnightlib.requirement.CompositeCheck;
import org.wallentines.midnightlib.requirement.StringCheck;
import org.wallentines.midnightlib.types.ResettableSingleton;
import org.wallentines.midnightlib.types.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MidnightCoreServer {

    private final LangManager langManager;
    private final boolean testCommand;

    private final FileWrapper<ConfigObject> config;
    private final Path dataDirectory;

    public static final ConfigSection DEFAULT_CONFIG = new ConfigSection()
            .with("register_test_command", false);

    public MidnightCoreServer(Server server, LangRegistry langDefaults) {

        ConfigSection defaultConfig = DEFAULT_CONFIG;

        Path directory = server.getConfigDirectory().resolve("MidnightCore");
        Path langDirectory = directory.resolve("lang");
        Path globalConfig = MidnightCoreAPI.GLOBAL_CONFIG_DIRECTORY.get().resolve("MidnightCore");

        if(!server.isDedicatedServer() && globalConfig != directory && !directory.toFile().exists()) {

            FileWrapper<ConfigObject> config = MidnightCoreAPI.FILE_CODEC_REGISTRY.findOrCreate(ConfigContext.INSTANCE, "config", globalConfig.toFile(), DEFAULT_CONFIG);
            defaultConfig.fillOverwrite(config.getRoot().asSection());

            Path langFolder = globalConfig.resolve("lang");
            if(Files.isDirectory(langFolder)) {
                try {
                    Files.copy(langFolder, langDirectory);
                } catch (IOException ex) {
                    MidnightCoreAPI.LOGGER.warn("Unable to copy lang defaults to world!");
                }
            }

        }

        dataDirectory = directory;
        if(!Files.isDirectory(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException ex) {
                throw new RuntimeException("Unable to create config directory!", ex);
            }
        }

        langManager = new LangManager(langDefaults, langDirectory);
        langManager.saveLanguageDefaults("en_us", langDefaults);

        this.config = MidnightCoreAPI.FILE_CODEC_REGISTRY.findOrCreate(ConfigContext.INSTANCE, "config", dataDirectory, defaultConfig);
        testCommand = config.getRoot().asSection().getBoolean("register_test_command");

        config.save();
    }


    public static void registerPlaceholders(PlaceholderManager manager) {

        Player.registerPlaceholders(manager);
        Server.registerPlaceholders(manager);
        Entity.registerPlaceholders(manager);
        LangManager.registerPlaceholders(manager);

        manager.registerSupplier("mcore_config_dir", PlaceholderSupplier.inline(ctx -> INSTANCE.get().dataDirectory.toString()));

    }


    static void registerRequirements(Registry<Identifier, CheckType<Player, ?>> registry) {

        registry.tryRegister("composite", new CompositeCheck.Type<>(registry));

        registry.tryRegister("username", new StringCheck.Type<>(Player::getUsername));
        registry.tryRegister("uuid", new StringCheck.Type<>(pl -> pl.getUUID().toString()));
        registry.tryRegister("game_mode", new StringCheck.Type<>(pl -> pl.getGameMode().getId()));

        registry.tryRegister("permission", new PlayerCheck.Type<>(Player::hasPermission, Serializer.STRING));
        registry.tryRegister("world", new PlayerCheck.Type<>((pl, id) -> pl.getDimensionId().equals(id), Identifier.serializer("minecraft")));
        registry.tryRegister("locale", new PlayerCheck.Type<>((pl, str) -> str.contains("_") ? pl.getLanguage().equals(str) : pl.getLanguage().startsWith(str), Serializer.STRING));
        registry.tryRegister("region", new PlayerCheck.Type<>((pl, reg) -> reg.isWithin(pl.getPosition()), Region.SERIALIZER));

        registry.tryRegister("cooldown", new CooldownRequirement.Type<>());
    }


    public LangManager getLangManager() {
        return langManager;
    }

    public boolean registerTestCommand() {
        return testCommand;
    }

    public ConfigSection getConfig() {
        return config.getRoot().asSection();
    }


    public static final Singleton<MidnightCoreServer> INSTANCE = new ResettableSingleton<>();

}
