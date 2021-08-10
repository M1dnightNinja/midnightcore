package me.m1dnightninja.midnightcore.spigot.module.playerdata;

import me.m1dnightninja.midnightcore.common.module.playerdata.AbstractPlayerDataModule;
import me.m1dnightninja.midnightcore.spigot.MidnightCore;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerDataModule extends AbstractPlayerDataModule implements Listener {

    public void onDisable() {
        onShutdown();
    }

    @Override
    protected void registerListeners() {

        Bukkit.getPluginManager().registerEvents(this, MidnightCore.getInstance());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {

        onLeave(event.getPlayer().getUniqueId());
    }
}
