package me.m1dnightninja.midnightcore.api.module.lang;

import me.m1dnightninja.midnightcore.api.config.ConfigProvider;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.module.IModule;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.text.MComponent;

import java.io.File;
import java.util.UUID;

public interface ILangModule extends IModule {

    void registerInlinePlaceholderSupplier(String search, PlaceholderSupplier<String> supplier);

    void registerPlaceholderSupplier(String search, PlaceholderSupplier<MComponent> supplier);

    MComponent getPlaceholderValue(String key, Object... args);

    String getInlinePlaceholderValue(String key, Object... args);

    String getPlayerLocale(MPlayer u);

    String getServerLanguage();

    ILangProvider createLangProvider(File langFolder, ConfigSection defaults);

    @Deprecated
    default ILangProvider createLangProvider(File langFolder, ConfigProvider provider, ConfigSection defaults) {
        return createLangProvider(langFolder, defaults);
    }

    MComponent applyPlaceholders(MComponent input, Object... args);

    String applyInlinePlaceholders(String input, Object... args);

    MComponent parseText(String input, Object... args);

    String applyPlaceholdersFlattened(String input, Object... args);

}
