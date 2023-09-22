package org.wallentines.mcore.skin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.phys.Vec3;
import org.wallentines.fbev.player.PlayerJoinEvent;
import org.wallentines.mcore.Player;
import org.wallentines.mcore.Server;
import org.wallentines.mcore.ServerModule;
import org.wallentines.mcore.Skin;
import org.wallentines.mcore.util.AuthUtil;
import org.wallentines.mcore.util.ConversionUtil;
import org.wallentines.mcore.util.MojangUtil;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.midnightlib.event.Event;
import org.wallentines.midnightlib.module.ModuleInfo;

import java.util.*;

public class FabricSkinModule extends SkinModule {

    private final HashMap<UUID, Skin> loginSkins = new HashMap<>();

    public static final ModuleInfo<Server, ServerModule> MODULE_INFO = new ModuleInfo<>(FabricSkinModule::new, ID, DEFAULT_CONFIG);

    @Override
    public boolean initialize(ConfigSection section, Server data) {

        super.initialize(section, data);

        boolean offlineModeSkins = section.getBoolean("get_skins_in_offline_mode");

        for(Player player : data.getPlayers()) {
            onLogin(player, offlineModeSkins);
        }

        Event.register(PlayerJoinEvent.class, this, 10, ev -> onLogin((Player) ev.getPlayer(), offlineModeSkins));

        return true;
    }

    private void onLogin(Player player, boolean offlineModeSkins) {

        MinecraftServer mc = ConversionUtil.validate(server);
        ServerPlayer spl = ConversionUtil.validate(player);
        loginSkins.put(spl.getUUID(), AuthUtil.getProfileSkin(spl.getGameProfile()));
        if(offlineModeSkins && !mc.usesAuthentication()) {
            MojangUtil.getSkinByNameAsync(spl.getGameProfile().getName()).thenAccept(skin -> {
                loginSkins.put(spl.getUUID(), skin);
                setSkin((Player) spl, skin);
            });
        }
    }


    @Override
    public void resetSkin(Player player) {

        setSkin(player, loginSkins.get(player.getUUID()));
    }

    @Override
    public void setSkin(Player player, Skin skin) {

        ServerPlayer spl = ConversionUtil.validate(player);

        if(AuthUtil.getProfileSkin(spl.getGameProfile()) == skin) return;

        AuthUtil.setProfileSkin(spl.getGameProfile(), skin);

        // Update
        MinecraftServer server = spl.getServer();
        if(server == null) return;

        // Make sure the player is ready to receive a respawn packet
        spl.stopRiding();
        Vec3 velocity = spl.getDeltaMovement();

        // Create Packets
        ClientboundPlayerInfoRemovePacket remove = new ClientboundPlayerInfoRemovePacket(List.of(spl.getUUID()));
        ClientboundPlayerInfoUpdatePacket add = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(spl));

        List<Pair<EquipmentSlot, ItemStack>> items = Arrays.stream(EquipmentSlot.values()).map(es -> new Pair<>(es, spl.getItemBySlot(es))).toList();

        ClientboundSetEquipmentPacket equip = new ClientboundSetEquipmentPacket(spl.getId(), items);

        ServerLevel world = spl.serverLevel();

        CommonPlayerSpawnInfo spawnInfo = new CommonPlayerSpawnInfo(
                world.dimensionTypeId(),
                world.dimension(),
                BiomeManager.obfuscateSeed(world.getSeed()),
                spl.gameMode.getGameModeForPlayer(),
                spl.gameMode.getPreviousGameModeForPlayer(),
                world.isDebug(),
                world.isFlat(),
                Optional.empty(),
                0
        );

        ClientboundRespawnPacket respawn = new ClientboundRespawnPacket(spawnInfo, (byte) 3);

        ClientboundPlayerPositionPacket position = new ClientboundPlayerPositionPacket(spl.getX(), spl.getY(), spl.getZ(), spl.getRotationVector().y, spl.getRotationVector().x, new HashSet<>(), 0);
        ClientboundSetExperiencePacket experience = new ClientboundSetExperiencePacket(spl.experienceProgress, spl.totalExperience, spl.experienceLevel);

        // Player information packets should be sent to everyone
        for(ServerPlayer obs : server.getPlayerList().getPlayers()) {

            obs.connection.send(remove);
            obs.connection.send(add);
        }

        // Entity information packets should only be sent to observers in the same world
        Collection<ServerPlayer> observers = world.getPlayers(pl -> pl != spl);
        if(!observers.isEmpty()) {

            ClientboundRemoveEntitiesPacket destroy = new ClientboundRemoveEntitiesPacket(spl.getId());
            Packet<?> spawn = spl.getAddEntityPacket();

            List<SynchedEntityData.DataValue<?>> entityData = spl.getEntityData().getNonDefaultValues();
            ClientboundSetEntityDataPacket tracker = null;
            if (entityData != null) {
                tracker = new ClientboundSetEntityDataPacket(spl.getId(), entityData);
            }

            float headRot = spl.getYHeadRot();
            int rot = (int) headRot;
            if (headRot < (float) rot) rot -= 1;
            ClientboundRotateHeadPacket head = new ClientboundRotateHeadPacket(spl, (byte) ((rot * 256.0F) / 360.0F));

            for (ServerPlayer obs : observers) {

                obs.connection.send(destroy);
                obs.connection.send(spawn);
                obs.connection.send(head);
                obs.connection.send(equip);
                if(tracker != null) obs.connection.send(tracker);
            }
        }

        // The remaining packets should only be sent to the player themselves
        spl.connection.send(respawn);
        spl.connection.send(position);
        spl.connection.send(equip);
        spl.connection.send(experience);

        server.getPlayerList().sendPlayerPermissionLevel(spl);
        server.getPlayerList().sendAllPlayerInfo(spl);

        spl.onUpdateAbilities();
        spl.getInventory().tick();

        spl.setDeltaMovement(velocity);
        spl.connection.send(new ClientboundSetEntityMotionPacket(spl));

    }
}
