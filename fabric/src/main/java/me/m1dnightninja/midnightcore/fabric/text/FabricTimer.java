package me.m1dnightninja.midnightcore.fabric.text;

import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.text.MTimer;
import me.m1dnightninja.midnightcore.api.text.MComponent;
import me.m1dnightninja.midnightcore.common.util.FormatUtil;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import me.m1dnightninja.midnightcore.fabric.player.FabricPlayer;
import me.m1dnightninja.midnightcore.fabric.util.ConversionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

public class FabricTimer extends MTimer {

    private final Component textPrefix;

    public FabricTimer(MComponent prefix, int seconds, boolean countUp, TimerCallback cb) {
        super(prefix, seconds, countUp, cb);
        textPrefix = ConversionUtil.toMinecraftComponent(prefix);
    }

    @Override
    protected final void callTick(int secondsLeft) {
        MidnightCore.getServer().submit(() -> {
            try {
                if(callback != null) callback.tick(secondsLeft);
            } catch(Exception ex) {
                MidnightCoreAPI.getLogger().warn("An exception occurred while a timer was ticking!");
                ex.printStackTrace();
            }
        });
    }

    @Override
    protected final void display() {

        Component send = textPrefix.copy().append(new TextComponent(FormatUtil.formatTime(secondsLeft * 1000L)).setStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.WHITE)));

        for(MPlayer player : players) {
            ServerPlayer pl = ((FabricPlayer) player).getMinecraftPlayer();
            if(pl != null) pl.sendMessage(send, ChatType.GAME_INFO, Util.NIL_UUID);
        }
    }
}
