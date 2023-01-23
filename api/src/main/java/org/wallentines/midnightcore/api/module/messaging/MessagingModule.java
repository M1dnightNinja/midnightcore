package org.wallentines.midnightcore.api.module.messaging;

import org.wallentines.midnightcore.api.module.ServerModule;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightlib.config.ConfigSection;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.function.Consumer;

public interface MessagingModule extends ServerModule {

    void registerHandler(Identifier id, MessageHandler handler);

    void sendMessage(MPlayer player, Identifier id, ConfigSection data);

    void sendRawMessage(MPlayer player, Identifier id, byte[] data);

    void addLoginListener(Consumer<LoginNegotiator> onLogin);

}
