package org.wallentines.mcore.adapter.v1_20_R1;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.wallentines.mcore.Skin;
import org.wallentines.mcore.adapter.SkinUpdater;

import java.util.*;


public class SkinUpdaterImpl implements SkinUpdater {


    private void setProfileSkin(GameProfile profile, @Nullable Skin skin) {
        profile.getProperties().get("textures").clear();
        if(skin != null) {
            profile.getProperties().put("textures", new Property("textures", skin.getValue(), skin.getSignature()));
        }
    }

    @Override
    public void changePlayerSkin(Player player, @Nullable Skin skin) {

        EntityPlayer epl = ((CraftPlayer) player).getHandle();
        GameProfile gameProfile = ((CraftPlayer) player).getProfile(); // getGameProfile()
        setProfileSkin(gameProfile, skin);

        MinecraftServer server = epl.d;
        if(server == null) return;

        // Make sure player is ready to receive a respawn packet
        epl.Y(); // stopRiding()

        // Store velocity so it can be re-applied later
        Vec3D velocity = epl.dl(); // getDeltaMovement()

        // Create Packets
        ClientboundPlayerInfoRemovePacket remove = new ClientboundPlayerInfoRemovePacket(List.of(player.getUniqueId()));
        ClientboundPlayerInfoUpdatePacket add = ClientboundPlayerInfoUpdatePacket.a(List.of(epl)); // createPlayerInitializing()

        List<Pair<EnumItemSlot, ItemStack>> items = Arrays.stream(EnumItemSlot.values()).map(eis -> new Pair<>(eis, epl.c(eis))).toList(); // getItemBySlot

        int entityId = epl.af(); // getId()
        PacketPlayOutEntityEquipment equip = new PacketPlayOutEntityEquipment(entityId, items);

        WorldServer world = epl.x(); // serverLevel()

        PacketPlayOutRespawn respawn = new PacketPlayOutRespawn(
                world.aa(), // dimensionType()
                world.ac(), // dimension()
                BiomeManager.a(world.A()), // obfuscateSeed(), getSeed()
                epl.e.b(), // gameMode, getGameModeForPlayer()
                epl.e.c(), // gameMode, getPreviousGameModeForPlayer()
                world.af(), // isDebug()
                world.z(), // isFlat()
                (byte) 3, // Preserve metadata
                Optional.empty(),
                0
        );

        Location location = player.getLocation();
        PacketPlayOutPosition position = new PacketPlayOutPosition(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(), Set.of(), 0);

        // Player information should be sent to everyone
        for(EntityPlayer obs : server.ac().t()) { // getPlayerList(), getPlayers()

            obs.c.a(remove); // connection, send
            obs.c.a(add);
        }

        // Entity information should be sent to observers in the same world
        Collection<EntityPlayer> observers = world.a(pl -> pl != epl);
        if(!observers.isEmpty()) {

            PacketPlayOutEntityDestroy destroy = new PacketPlayOutEntityDestroy(entityId);
            PacketPlayOutNamedEntitySpawn spawn = new PacketPlayOutNamedEntitySpawn(epl);

            PacketPlayOutEntityMetadata tracker = null;
            List<DataWatcher.b<?>> entityData = epl.aj().c(); // DataValue<?>, getEntityData(), getNonDefaultValues()
            if(entityData != null) {
                tracker = new PacketPlayOutEntityMetadata(entityId, entityData);
            }

            float headRot = player.getEyeLocation().getYaw();
            int rot = (int) headRot;
            if(headRot < (float) rot) rot -= 1;
            PacketPlayOutEntityHeadRotation head = new PacketPlayOutEntityHeadRotation(epl, (byte) ((rot * 256.0f) / 360.0f));


            for(EntityPlayer obs : observers) {

                obs.c.a(destroy);
                obs.c.a(spawn);
                obs.c.a(head);
                obs.c.a(equip);
                if(tracker != null) obs.c.a(tracker);
            }
        }

        // The remaining packets should only be sent to the updated player
        epl.c.a(respawn);
        epl.c.a(position);
        epl.c.a(equip);

        server.g(() -> {
            server.ac().d(epl); // sendPlayerPermissionLevel
            server.ac().e(epl); // sendAllLevelInfo

            epl.w(); // onUpdateAbilities
            epl.fN().j(); // getInventory(), tick()

            epl.f(velocity); // setDeltaMovement()
            epl.c.a(new PacketPlayOutEntityVelocity(epl));
        });

    }
}
