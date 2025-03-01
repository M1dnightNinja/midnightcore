package org.wallentines.mcore;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.wallentines.mcore.adapter.Adapter;
import org.wallentines.mcore.text.Component;
import org.wallentines.mcore.util.ConversionUtil;
import org.wallentines.mcore.util.ResourcePackUtil;
import org.wallentines.midnightlib.math.Vec3d;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SpigotPlayer implements Player {

    private final Server server;
    private final org.bukkit.entity.Player internal;

    public SpigotPlayer(Server server, @NotNull org.bukkit.entity.Player internal) {
        this.server = server;
        this.internal = internal;
    }

    public org.bukkit.entity.Player getInternal() {
        return internal;
    }

    @Override
    public UUID getUUID() {
        return internal.getUniqueId();
    }

    @Override
    public Identifier getType() {
        return new Identifier("minecraft", "player");
    }

    @Override
    public Server getServer() {
        return server;
    }

    @Override
    public Component getDisplayName() {
        return Component.text(internal.getDisplayName());
    }

    @Override
    public Identifier getDimensionId() {
        return Identifier.parseOrDefault(internal.getWorld().getName(), "minecraft");
    }

    @Override
    public Vec3d getPosition() {
        org.bukkit.Location location = internal.getLocation();
        return new Vec3d(location.getX(), location.getY(), location.getZ());
    }

    @Override
    public float getYaw() {
        return internal.getLocation().getYaw();
    }

    @Override
    public float getPitch() {
        return internal.getLocation().getPitch();
    }

    @Override
    public boolean isRemoved() {
        return !internal.isOnline();
    }

    @Override
    public void teleport(Location location) {

        String name = location.dimension.getPath();

        World world = Bukkit.getWorld(name);
        if(world == null) {
            throw new IllegalArgumentException("Attempt to teleport player to invalid dimension! No dimension with ID " + name + " was found!");
        }
        internal.teleport(new org.bukkit.Location(
                world,
                location.position.getX(),
                location.position.getY(),
                location.position.getZ(),
                location.yaw,
                location.pitch
        ));
    }

    @Override
    public void runCommand(String command) {
        Bukkit.getServer().dispatchCommand(internal, command);
    }

    @Override
    public void setItem(EquipmentSlot slot, ItemStack item) {

        SpigotItem si = (SpigotItem) item;

        switch(slot) {
            case FEET -> internal.getInventory().setBoots(si.getInternal());
            case LEGS -> internal.getInventory().setLeggings(si.getInternal());
            case CHEST -> internal.getInventory().setChestplate(si.getInternal());
            case HEAD -> internal.getInventory().setHelmet(si.getInternal());
            case MAINHAND -> internal.getInventory().setItemInMainHand(si.getInternal());
            case OFFHAND -> internal.getInventory().setItemInOffHand(si.getInternal());
        }
    }

    @Override
    public ItemStack getItem(EquipmentSlot slot) {

        return new SpigotItem(switch (slot) {
            case FEET -> internal.getInventory().getBoots();
            case LEGS -> internal.getInventory().getLeggings();
            case CHEST -> internal.getInventory().getChestplate();
            case HEAD -> internal.getInventory().getHelmet();
            case MAINHAND -> internal.getInventory().getItemInMainHand();
            case OFFHAND -> internal.getInventory().getItemInOffHand();
        });
    }

    @Override
    public String getUsername() {
        return internal.getName();
    }

    @Override
    public void sendMessage(Component component) {
        Adapter.INSTANCE.get().sendMessage(internal, component);
    }

    @Override
    public void sendActionBar(Component component) {
        Adapter.INSTANCE.get().sendActionBar(internal, component);
    }

    @Override
    public void sendTitle(Component component) {
        Adapter.INSTANCE.get().sendTitle(internal, component);
    }

    @Override
    public void sendSubtitle(Component component) {
        Adapter.INSTANCE.get().sendSubtitle(internal, component);
    }

    @Override
    public void clearTitles() {
        Adapter.INSTANCE.get().clearTitles(internal);
    }

    @Override
    public void setTitleTimes(int fadeIn, int stay, int fadeOut) {
        Adapter.INSTANCE.get().setTitleAnimation(internal, fadeIn, stay, fadeOut);
    }

    @Override
    public void resetTitles() {
        Adapter.INSTANCE.get().resetTitles(internal);
    }

    @Override
    public void giveItem(ItemStack item) {
        SpigotItem si = ConversionUtil.validate(item);
        internal.getInventory().addItem(si.getInternal());
    }

    @Override
    public String getLanguage() {
        return Adapter.INSTANCE.get().getLocale(internal);
    }

    @Override
    public GameMode getGameMode() {
        return GameMode.valueOf(internal.getGameMode().name());
    }

    @Override
    public void setGameMode(GameMode mode) {
        internal.setGameMode(org.bukkit.GameMode.valueOf(mode.name()));
    }

    @Override
    public boolean isOnline() {
        return internal.isOnline();
    }

    @Override
    public boolean hasPermission(String permission) {
        return internal.hasPermission(permission);
    }

    @Override
    public boolean hasPermission(String permission, int defaultOpLevel) {
        return internal.hasPermission(permission) || Adapter.INSTANCE.get().hasOpLevel(internal, defaultOpLevel);
    }

    @Override
    public void kick(Component message) {
        Adapter.INSTANCE.get().kickPlayer(internal, message);
    }

    @Override
    public CompletableFuture<byte[]> getCookie(Identifier id) {
        return getInternal().retrieveCookie(NamespacedKey.fromString(id.toString()));
    }

    @Override
    public void setCookie(Identifier id, byte[] value) {
        getInternal().storeCookie(NamespacedKey.fromString(id.toString()), value);
    }

    @Override
    public void clearCookie(Identifier id) {
        getInternal().storeCookie(NamespacedKey.fromString(id.toString()), null);
    }

    @Override
    public void transfer(String hostname, int port) {
        getInternal().transfer(hostname, port);
    }

    @Override
    public void addResourcePack(ResourcePack pack) {
        ResourcePackUtil.addResourcePack(this, pack);
    }

    @Override
    public void removeResourcePack(UUID uuid) {
        ResourcePackUtil.removeResourcePack(this, uuid);
    }

    @Override
    public void clearResourcePacks() {
        ResourcePackUtil.clearResourcePacks(this);
    }

    @Override
    public Skin getSkin() {
        return Adapter.INSTANCE.get().getPlayerSkin(internal);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpigotPlayer that = (SpigotPlayer) o;
        return Objects.equals(server, that.server) && Objects.equals(internal, that.internal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(server, internal);
    }
}
