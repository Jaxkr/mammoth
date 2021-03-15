package com.worldql.client.ghost;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.worldql.client.compiled_protobuf.MinecraftPlayer;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Hashtable;
import java.util.UUID;

public class PacketNPC {
    private static Hashtable<UUID, String[]> skinCache = new Hashtable<>();

    private static Hashtable<UUID, ExpiringEntityPlayer> hashtableNPCs = new Hashtable<>();

    public static void updateNPC(MinecraftPlayer.PlayerState state) {
        UUID playerUUID = UUID.fromString(state.getUUID());
        // Do we have this NPC in our expiring entity player?
        ExpiringEntityPlayer player;
        if (hashtableNPCs.containsKey(playerUUID)) {
            player = hashtableNPCs.get(playerUUID);
        } else {
            // TODO: Change this when there is support for multiple worlds, accidentally left this out of the protobuf.
            player = PacketNPC.createNPC(state.getName(), playerUUID, new Location(Bukkit.getServer().getWorld("world"), state.getX(), state.getY(), state.getZ()));
            sendNPCJoinPacket(player.grab());
        }

        EntityPlayer e = player.grab();

        moveEntity(state, e);


    }

    private static ExpiringEntityPlayer createNPC(String name, UUID uuid, Location location) {

        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer world = ((CraftWorld) location.getWorld()).getHandle();
        GameProfile profile = new GameProfile(uuid, name);
        EntityPlayer npc = new EntityPlayer(server, world, profile, new PlayerInteractManager(world));
        npc.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

        String[] skinData = getSkin(uuid);
        profile.getProperties().put("textures",
                new Property("textures", skinData[0], skinData[1])
        );

        /* Moved this logic to updateNPC
        NPC.put(uuid, new ExpiringEntityPlayer(npc));
        sendNPCJoinPacket(npc);

         */

        return new ExpiringEntityPlayer(npc);
    }


    private static void sendNPCJoinPacket(EntityPlayer npc) {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
            connection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, npc));
            connection.sendPacket(new PacketPlayOutNamedEntitySpawn(npc));
            connection.sendPacket(new PacketPlayOutEntityHeadRotation(npc, (byte) ((npc.yaw * 256) / 360)));
        }
    }

    private static short computeMovementDelta(float current, float previous) {
        return (short) ((short) (current * 32 - previous * 32) * 128);
    }

    private static String[] getSkin(UUID uuid) {
        URL url = null;
        try {
            url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
        } catch (Exception e) {
            e.printStackTrace();
        }
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(url.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // get player skin blob and signature in hex.
        JsonObject prop = new JsonParser().parse(reader).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
        String texture = prop.get("value").getAsString();
        String signature = prop.get("signature").getAsString();
        return new String[]{texture, signature};

    }

    public static void moveEntity(MinecraftPlayer.PlayerState state, EntityPlayer e) {
        Location old = ((Player) e).getLocation();

        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
            e.setLocation(
                    state.getX(),
                    state.getY(),
                    state.getZ(),
                    state.getPitch(),
                    state.getYaw()
            );
            connection.sendPacket(
                    new PacketPlayOutEntityTeleport(e)
            );
        }


    }


    @Deprecated
    public static void moveForwards(Player player) {
        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
        /*
        for (ExpiringEntityPlayer expiringEntityPlayer : NPC) {
            EntityPlayer e = expiringEntityPlayer.grab();

            connection.sendPacket(
                    new PacketPlayOutEntity.PacketPlayOutRelEntityMove(e.getId(), computeMovementDelta(1, 1.00F), computeMovementDelta(0, 0), computeMovementDelta(1, 0), true)
            );


            double targetX = 60.950;
            double targetY = 63.000;
            double targetZ = 253.391;

            e.setLocation(targetX, targetY, targetZ, 25, 25);

            connection.sendPacket(
                    new PacketPlayOutEntityTeleport(e)
            );

        }
        */
    }
}