package org.wallentines.mcore.skin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.wallentines.mcore.*;
import org.wallentines.mcore.util.AuthUtil;
import org.wallentines.mcore.util.ConversionUtil;
import org.wallentines.mcore.util.MojangUtil;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.midnightlib.module.ModuleInfo;

import java.util.*;

public class FabricSkinModule extends SkinModule {

    private final HashMap<UUID, Skin> loginSkins = new HashMap<>();

    public static final ModuleInfo<Server, ServerModule> MODULE_INFO = new ModuleInfo<>(FabricSkinModule::new, ID, DEFAULT_CONFIG);

    @Override
    public boolean initialize(ConfigSection section, Server data) {

        super.initialize(section, data);

        boolean offlineModeSkins = section.getBoolean("get_skins_in_offline_mode");

        for(ServerPlayer spl : ConversionUtil.validate(data).getPlayerList().getPlayers()) {
            onLogin(spl.getGameProfile(), offlineModeSkins);
        }

        ServerConfigurationConnectionEvents.BEFORE_CONFIGURE.register((handler, server) -> {
            onLogin(handler.getOwner(), offlineModeSkins);
        });

        return true;
    }

    @Override
    public void forceUpdate(Player player) {
        player.getServer().submit(() -> {
            try {
                doUpdate(player);
            } catch (Throwable th) {
                MidnightCoreAPI.LOGGER.error("An error occurred while updating a player's skin!", th);
            }
        });
    }

    private void doUpdate(Player player) {
        ServerPlayer spl = ConversionUtil.validate(player);

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
        ClientboundRespawnPacket respawn = new ClientboundRespawnPacket(spl.createCommonSpawnInfo(world), (byte) 3);


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
            ClientboundAddEntityPacket spawn = new ClientboundAddEntityPacket(
                    spl.getId(),
                    spl.getUUID(),
                    spl.getX(),
                    spl.getY(),
                    spl.getZ(),
                    spl.getXRot(),
                    spl.getYRot(),
                    EntityType.PLAYER,
                    0,
                    velocity,
                    spl.getYHeadRot()
            );

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

        // The client waits for a game event after respawning to show the screen properly
        spl.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.LEVEL_CHUNKS_LOAD_START, 0.0f));
        server.getPlayerList().sendPlayerPermissionLevel(spl);
        server.getPlayerList().sendAllPlayerInfo(spl);

        spl.connection.teleport(PositionMoveRotation.of(spl), new HashSet<>());
        spl.connection.send(equip);
        spl.connection.send(experience);

        spl.onUpdateAbilities();

        spl.setDeltaMovement(velocity);
        spl.connection.send(new ClientboundSetEntityMotionPacket(spl));

        for(MobEffectInstance effect : spl.getActiveEffects()) {
            spl.connection.send(new ClientboundUpdateMobEffectPacket(spl.getId(), effect, true));
        }
    }

    private void onLogin(GameProfile profile, boolean offlineModeSkins) {

        MinecraftServer mc = ConversionUtil.validate(server);
        loginSkins.put(profile.getId(), AuthUtil.getProfileSkin(profile));
        if(offlineModeSkins && !mc.usesAuthentication()) {
            MojangUtil.getSkinByNameAsync(profile.getName()).thenAccept(skin -> {
                loginSkins.put(profile.getId(), skin);
                setSkin(mc.getPlayer(profile.getId()), skin);
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

        if(Objects.equals(AuthUtil.getProfileSkin(spl.getGameProfile()), skin)) return;
        AuthUtil.setProfileSkin(spl.getGameProfile(), skin);

        forceUpdate(player);
    }
}
