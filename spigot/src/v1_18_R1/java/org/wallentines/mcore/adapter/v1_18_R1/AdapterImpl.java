package org.wallentines.mcore.adapter.v1_18_R1;

import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import net.minecraft.SharedConstants;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_18_R1.CraftServer;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mcore.MidnightCoreAPI;
import org.wallentines.mcore.Skin;
import org.wallentines.mcore.adapter.Adapter;
import org.wallentines.mcore.adapter.ItemReflector;
import org.wallentines.mcore.adapter.NbtContext;
import org.wallentines.mcore.adapter.SkinUpdater;
import org.wallentines.mcore.text.Component;
import org.wallentines.mcore.text.ModernSerializer;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.serializer.GsonContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.midnightlib.math.Color;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.Objects;

public class AdapterImpl implements Adapter {

    private SkinUpdaterImpl updater;
    private ItemReflector<net.minecraft.world.item.ItemStack, CraftItemStack> reflector;

    @Override
    public boolean initialize() {

        try {
            reflector = new ItemReflector<>(CraftItemStack.class);

        } catch (Exception ex) {
            return false;
        }
        updater = new SkinUpdaterImpl();
        
        return true;
    }

    @Override
    public void runOnServer(Runnable runnable) {
        ((CraftServer) Bukkit.getServer()).getServer().f(runnable);
    }

    @Override
    public void addTickListener(Runnable runnable) {
        ((CraftServer) Bukkit.getServer()).getServer().b(runnable);
    }

    @Override
    public @Nullable Skin getPlayerSkin(Player player) {
        GameProfile profile = ((CraftPlayer) player).getProfile();
        return profile.getProperties().get("textures").stream().map(prop -> new Skin(profile.getId(), prop.getValue(), prop.getSignature())).findFirst().orElse(null);
    }

    @Override
    public SkinUpdater getSkinUpdater() {
        return updater;
    }

    @Override
    public void sendMessage(Player player, Component component) {
        EntityPlayer ep = ((CraftPlayer) player).getHandle();
        IChatBaseComponent bc = convert(component);
        if(bc != null) ep.a(bc, false); // sendMessage()
    }

    @Override
    public void sendActionBar(Player player, Component component) {
        EntityPlayer ep = ((CraftPlayer) player).getHandle();
        IChatBaseComponent bc = convert(component);
        if(bc != null) ep.a(bc, true); // sendMessage()
    }

    @Override
    public void sendTitle(Player player, Component component) {
        EntityPlayer ep = ((CraftPlayer) player).getHandle();
        IChatBaseComponent bc = convert(component);
        if(bc != null) ep.b.a(new ClientboundSetTitleTextPacket(bc));
    }

    @Override
    public void sendSubtitle(Player player, Component component) {
        EntityPlayer ep = ((CraftPlayer) player).getHandle();
        IChatBaseComponent bc = convert(component);
        if(bc != null) ep.b.a(new ClientboundSetSubtitleTextPacket(bc));
    }

    @Override
    public void setTitleAnimation(Player player, int i, int i1, int i2) {
        EntityPlayer ep = ((CraftPlayer) player).getHandle();
        ep.b.a(new ClientboundSetTitlesAnimationPacket(i, i1, i2));
    }

    @Override
    public void clearTitles(Player player) {
        EntityPlayer ep = ((CraftPlayer) player).getHandle();
        ep.b.a(new ClientboundClearTitlesPacket(false));
    }

    @Override
    public void resetTitles(Player player) {
        EntityPlayer ep = ((CraftPlayer) player).getHandle();
        ep.b.a(new ClientboundClearTitlesPacket(true));
    }

    @Override
    public boolean hasOpLevel(Player player, int i) {
        EntityPlayer ep = ((CraftPlayer) player).getHandle();
        return ep.l(i);
    }

    @Override
    public ConfigSection getTag(Player player) {
        EntityPlayer ep = ((CraftPlayer) player).getHandle();
        NBTTagCompound nbt = new NBTTagCompound();
        ep.f(nbt);
        return convert(nbt);
    }

    @Override
    public void loadTag(Player player, ConfigSection configSection) {
        ((CraftPlayer) player).getHandle().a(convert(configSection));
    }

    @Override
    public ItemStack buildItem(Identifier id, int count, byte data) {
        net.minecraft.world.item.ItemStack is = new net.minecraft.world.item.ItemStack(IRegistry.aa.a(MinecraftKey.a(id.toString())), count);
        return CraftItemStack.asCraftMirror(is);
    }

    @Override
    public Identifier getItemId(ItemStack is) {
        return Identifier.parse(Objects.requireNonNull(IRegistry.aa.b(reflector.getHandle(is).c())).toString());
    }

    @Override
    public void setTag(ItemStack itemStack, ConfigSection configSection) {
        reflector.getHandle(itemStack).c(convert(configSection));
    }


    @Override
    public String getTranslationKey(ItemStack is) {
        net.minecraft.world.item.ItemStack mis = reflector.getHandle(is);
        return mis.c().a();
    }

    @Override
    public ConfigSection getTag(ItemStack itemStack) {

        net.minecraft.world.item.ItemStack mis = reflector.getHandle(itemStack);
        NBTTagCompound nbt = mis.t();
        if(nbt == null) return null;

        return convert(nbt);
    }

    @Override
    public ItemStack setupInternal(ItemStack itemStack) {
        return CraftItemStack.asCraftCopy(itemStack);
    }

    @Override
    public GameVersion getGameVersion() {
        return new GameVersion(SharedConstants.b().getId(), SharedConstants.b().getProtocolVersion());
    }

    @Override
    public void kickPlayer(Player player, Component message) {
        ((CraftPlayer) player).getHandle().b.a(convert(message));
    }

    @Override
    public Color getRarityColor(ItemStack itemStack) {
        Integer clr = reflector.getHandle(itemStack).z().e.e();
        return clr == null ? Color.WHITE : new Color(clr);
    }

    private ConfigSection convert(NBTTagCompound internal) {
        if(internal == null) return null;
        return NbtContext.fromMojang(NBTCompressedStreamTools::a, internal);
    }

    private NBTTagCompound convert(ConfigSection section) {
        return NbtContext.toMojang(
                section,
                NBTCompressedStreamTools::a);
    }

    private IChatBaseComponent convert(Component component) {

        SerializeResult<JsonElement> serialized = ModernSerializer.INSTANCE.serialize(GsonContext.INSTANCE, component, getGameVersion());
        if(!serialized.isComplete()) {
            MidnightCoreAPI.LOGGER.error("An error occurred while serializing a component! " + serialized.getError());
            return null;
        }
        return IChatBaseComponent.ChatSerializer.a(serialized.getOrThrow()); // fromJsonTree()
    }
}
