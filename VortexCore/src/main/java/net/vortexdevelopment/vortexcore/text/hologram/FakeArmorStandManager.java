package net.vortexdevelopment.vortexcore.text.hologram;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.kyori.adventure.text.Component;
import net.vortexdevelopment.vortexcore.VortexPlugin;
import net.vortexdevelopment.vortexcore.compatibility.folia.SchedulerUtils;
import net.vortexdevelopment.vortexcore.text.AdventureUtils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends client-only armor stands. The server never owns these entities, so the
 * manager must keep a small per-player view of what has been sent.
 */
final class FakeArmorStandManager extends HologramBackend {

    private static final byte ENTITY_INVISIBLE_FLAG = 0x20;
    private static final byte ARMOR_STAND_MARKER_FLAG = 0x10;
    private static final String ENTITY_ID_CURSOR_PROPERTY =
            "net.vortexdevelopment.vortexcore.hologram.nextFakeEntityId";
    private static final int ENTITY_ID_BLOCK_SIZE = 1_000_000;

    private final Plugin plugin;
    private final ProtocolManager protocolManager;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private int nextEntityId;
    private int entityIdFloor;

    private final Method positionMoveRotationModifier;
    private final Method positionMoveRotationCreate;

    private volatile boolean warnedSendFailure;
    private PacketAdapter packetListener;

    FakeArmorStandManager(Plugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        reserveEntityIdBlock();

        Method modifier = null;
        Method create = null;
        try {
            modifier = PacketContainer.class.getMethod("getPositionMoveRotation");
            Class<?> wrapper = Class.forName(
                    "com.comphenix.protocol.wrappers.WrappedPositionMoveRotation");
            create = wrapper.getMethod("create", Vector.class, Vector.class, float.class, float.class);
        } catch (ReflectiveOperationException ignored) {
            // Older ProtocolLib versions use three doubles for ENTITY_TELEPORT.
        }
        this.positionMoveRotationModifier = modifier;
        this.positionMoveRotationCreate = create;
    }

    @Override
    boolean isSupported() {
        try {
            PacketContainer spawn = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            PacketContainer metadata = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            PacketContainer teleport = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);

            boolean spawnSupported = spawn.getIntegers().size() > 0
                    && spawn.getUUIDs().size() > 0
                    && spawn.getDoubles().size() >= 3
                    && spawn.getEntityTypeModifier().size() > 0;
            boolean metadataSupported = metadata.getIntegers().size() > 0
                    && metadata.getDataValueCollectionModifier().size() > 0;
            boolean teleportSupported = teleport.getDoubles().size() >= 3
                    || canWritePositionMoveRotation();
            boolean destroySupported = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY)
                    .getIntLists().size() > 0;
            return spawnSupported && metadataSupported && teleportSupported && destroySupported;
        } catch (Throwable throwable) {
            VortexPlugin.getInstance().getLogger().warning(
                    "Could not inspect ProtocolLib fake hologram packet support: " + throwable.getMessage());
            return false;
        }
    }

    @Override
    void init() {
        packetListener = new PacketAdapter(
                plugin,
                ListenerPriority.MONITOR,
                List.of(
                        PacketType.Play.Server.MAP_CHUNK,
                        PacketType.Play.Server.UNLOAD_CHUNK,
                        PacketType.Play.Server.RESPAWN
                ),
                ListenerOptions.ASYNC
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null) {
                    return;
                }

                PacketType type = event.getPacketType();
                if (type == PacketType.Play.Server.MAP_CHUNK) {
                    handleChunkPacket(player, event.getPacket(), true);
                } else if (type == PacketType.Play.Server.UNLOAD_CHUNK) {
                    handleChunkPacket(player, event.getPacket(), false);
                } else if (type == PacketType.Play.Server.RESPAWN) {
                    resetPlayer(player);
                }
            }
        };
        protocolManager.addPacketListener(packetListener);

        // Holograms can be created after VortexCore has enabled. Reconcile players
        // that were already online when this backend was initialized as well.
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            onPlayerJoin(player);
            reconcileAlreadyLoadedChunks(player);
        }
    }

    /**
     * ProtocolLib cannot replay MAP_CHUNK packets that were sent before this listener was installed. During a
     * plugin live reload, seed the session with server-loaded chunks in the player's client view so holograms in
     * adjacent, already-visible chunks are rendered immediately instead of waiting for a teleport/chunk resend.
     */
    private void reconcileAlreadyLoadedChunks(Player player) {
        Session session = sessions.computeIfAbsent(player.getUniqueId(), Session::new);
        int radius;
        try {
            radius = Math.max(2, Math.min(player.getClientViewDistance(), org.bukkit.Bukkit.getViewDistance()));
        } catch (Throwable ignored) {
            radius = Math.max(2, org.bukkit.Bukkit.getViewDistance());
        }

        int centerX = player.getLocation().getBlockX() >> 4;
        int centerZ = player.getLocation().getBlockZ() >> 4;
        UUID worldId = player.getWorld().getUID();
        for (Chunk chunk : player.getWorld().getLoadedChunks()) {
            if (Math.abs(chunk.getX() - centerX) <= radius && Math.abs(chunk.getZ() - centerZ) <= radius) {
                session.loadedChunks.add(new ChunkPosition(worldId, chunk.getX(), chunk.getZ()));
            }
        }
    }

    @Override
    void render(Hologram hologram, boolean force) {
        Hologram.HologramPacketSnapshot snapshot = hologram.packetSnapshot();
        for (Session session : sessions.values()) {
            render(session, hologram, snapshot);
        }
    }

    @Override
    void remove(Hologram hologram) {
        for (Session session : sessions.values()) {
            synchronized (session.sendLock) {
                ClientHologram state = session.holograms.remove(hologram.getId());
                if (state != null) {
                    destroy(session, state);
                }
            }
        }
    }

    @Override
    void clear() {
        PacketAdapter listener = packetListener;
        if (listener != null) {
            protocolManager.removePacketListener(listener);
            packetListener = null;
        }
        for (Session session : sessions.values()) {
            synchronized (session.sendLock) {
                for (ClientHologram state : session.holograms.values()) {
                    destroy(session, state);
                }
                session.holograms.clear();
            }
        }
        sessions.clear();
    }

    @Override
    void onPlayerJoin(Player player) {
        Session session = sessions.computeIfAbsent(player.getUniqueId(), Session::new);
        session.player = player;
        session.worldId = player.getWorld().getUID();
        session.loadedChunks.add(new ChunkPosition(
                session.worldId,
                player.getLocation().getChunk().getX(),
                player.getLocation().getChunk().getZ()
        ));

        SchedulerUtils.runTaskLaterAsynchronously(plugin, () -> renderPlayer(session), 1L);
    }

    @Override
    void onPlayerQuit(UUID playerId) {
        sessions.remove(playerId);
    }

    @Override
    void onPlayerChangedWorld(Player player) {
        Session session = sessions.computeIfAbsent(player.getUniqueId(), Session::new);
        synchronized (session.sendLock) {
            session.loadedChunks.clear();
            session.worldId = player.getWorld().getUID();
            session.player = player;
            session.loadedChunks.add(new ChunkPosition(
                    session.worldId,
                    player.getLocation().getChunk().getX(),
                player.getLocation().getChunk().getZ()
            ));
        }
        SchedulerUtils.runTaskLaterAsynchronously(plugin, () -> {
            synchronized (session.sendLock) {
                destroyAll(session);
                renderPlayer(session);
            }
        }, 1L);
    }

    @Override
    void onServerChunkLoad(Chunk chunk) {
        ChunkPosition position = new ChunkPosition(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        for (Session session : sessions.values()) {
            if (session.loadedChunks.contains(position)) {
                SchedulerUtils.runTaskAsynchronously(plugin, () -> renderChunk(session, position));
            }
        }
    }

    @Override
    void onServerChunkUnload(Chunk chunk) {
        ChunkPosition position = new ChunkPosition(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        for (Session session : sessions.values()) {
            session.loadedChunks.remove(position);
            SchedulerUtils.runTaskAsynchronously(plugin, () -> {
                synchronized (session.sendLock) {
                    for (ClientHologram state : session.holograms.values()) {
                        if (state.chunkPosition != null && state.chunkPosition.equals(position)) {
                            destroy(session, state);
                        }
                    }
                }
            });
        }
    }

    private void handleChunkPacket(Player player, PacketContainer packet, boolean load) {
        if (packet.getIntegers().size() < 2) {
            return;
        }
        int chunkX = packet.getIntegers().read(0);
        int chunkZ = packet.getIntegers().read(1);
        Session session = sessions.computeIfAbsent(player.getUniqueId(), Session::new);
        session.player = player;
        ChunkPosition position = new ChunkPosition(session.worldId, chunkX, chunkZ);
        if (load) {
            session.loadedChunks.add(position);
            SchedulerUtils.runTaskLaterAsynchronously(plugin, () -> renderChunk(session, position), 1L);
        } else {
            session.loadedChunks.remove(position);
            SchedulerUtils.runTaskAsynchronously(plugin, () -> destroyChunk(session, position));
        }
    }

    private void resetPlayer(Player player) {
        Session session = sessions.computeIfAbsent(player.getUniqueId(), Session::new);
        synchronized (session.sendLock) {
            destroyAll(session);
            session.loadedChunks.clear();
        }
        // The packet callback is asynchronous. Resolve the player's new world
        // and chunk on the server thread, then render packets asynchronously.
        SchedulerUtils.runTaskLater(plugin, () -> onPlayerChangedWorld(player), 1L);
    }

    private void renderPlayer(Session session) {
        for (Hologram hologram : HologramManager.getHologramSnapshot()) {
            render(session, hologram, hologram.packetSnapshot());
        }
    }

    private void renderChunk(Session session, ChunkPosition position) {
        if (!session.loadedChunks.contains(position)) {
            return;
        }
        for (Hologram hologram : HologramManager.getHologramSnapshot()) {
            Hologram.HologramPacketSnapshot snapshot = hologram.packetSnapshot();
            if (sameChunk(snapshot, position)) {
                render(session, hologram, snapshot);
            }
        }
    }

    private void render(Session session, Hologram hologram, Hologram.HologramPacketSnapshot snapshot) {
        synchronized (session.sendLock) {
            boolean visible = snapshot.visible()
                    && snapshot.worldId().equals(session.worldId)
                    && hologram.canSee(session.playerId)
                    && session.loadedChunks.contains(chunkPosition(snapshot));
            ClientHologram state = session.holograms.computeIfAbsent(
                    hologram.getId(), ignored -> new ClientHologram());

            if (!visible) {
                destroy(session, state);
                state.chunkPosition = null;
                return;
            }

            ChunkPosition position = chunkPosition(snapshot);
            state.chunkPosition = position;
            reconcile(session, state, snapshot);
        }
    }

    private void reconcile(Session session, ClientHologram state,
                           Hologram.HologramPacketSnapshot snapshot) {
        while (state.lines.size() > snapshot.lines().size()) {
            int index = state.lines.size() - 1;
            ClientLine line = state.lines.remove(index);
            destroy(session, line.entityId);
        }

        for (int index = 0; index < snapshot.lines().size(); index++) {
            ClientLine line = index < state.lines.size() ? state.lines.get(index) : null;
            if (line == null) {
                line = new ClientLine(nextEntityId(), UUID.randomUUID());
                state.lines.add(line);
            }

            Location location = snapshot.location().clone().add(
                    0,
                    (snapshot.lines().size() - 1 - index) * 0.25,
                    0
            );
            Component name = snapshot.lines().get(index);
            boolean locationChanged = !line.sent
                    || line.x != location.getX()
                    || line.y != location.getY()
                    || line.z != location.getZ()
                    || line.yaw != location.getYaw()
                    || line.pitch != location.getPitch();
            boolean nameChanged = !line.sent || !name.equals(line.name);

            if (!line.sent) {
                send(session, spawnPacket(line, location), metadataPacket(line, name));
                line.sent = true;
            } else {
                if (locationChanged) {
                    send(session, teleportPacket(line, location));
                }
                if (nameChanged) {
                    send(session, metadataPacket(line, name));
                }
            }

            line.x = location.getX();
            line.y = location.getY();
            line.z = location.getZ();
            line.yaw = location.getYaw();
            line.pitch = location.getPitch();
            line.name = name;
        }
    }

    private void destroyChunk(Session session, ChunkPosition position) {
        synchronized (session.sendLock) {
            for (ClientHologram state : session.holograms.values()) {
                if (position.equals(state.chunkPosition)) {
                    destroy(session, state);
                    state.chunkPosition = null;
                }
            }
        }
    }

    private void destroyAll(Session session) {
        for (ClientHologram state : session.holograms.values()) {
            destroy(session, state);
            state.chunkPosition = null;
        }
    }

    private void destroy(Session session, ClientHologram state) {
        for (ClientLine line : state.lines) {
            if (line.sent) {
                destroy(session, line.entityId);
                line.sent = false;
            }
        }
    }

    private void destroy(Session session, int entityId) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        packet.getIntLists().write(0, List.of(entityId));
        send(session, packet);
    }

    private PacketContainer spawnPacket(ClientLine line, Location location) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        packet.getModifier().writeDefaults();
        packet.getIntegers().write(0, line.entityId);
        packet.getUUIDs().write(0, line.uuid);
        packet.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);
        packet.getDoubles().write(0, location.getX());
        packet.getDoubles().write(1, location.getY());
        packet.getDoubles().write(2, location.getZ());
        writeRotation(packet, location.getYaw(), location.getPitch());
        return packet;
    }

    private PacketContainer metadataPacket(ClientLine line, Component name) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getModifier().writeDefaults();
        packet.getIntegers().write(0, line.entityId);

        List<WrappedDataValue> metadata = new ArrayList<>(6);
        metadata.add(new WrappedDataValue(
                0,
                WrappedDataWatcher.Registry.get(Byte.class),
                ENTITY_INVISIBLE_FLAG
        ));
        metadata.add(new WrappedDataValue(
                2,
                WrappedDataWatcher.Registry.getChatComponentSerializer(true),
                Optional.of(WrappedChatComponent.fromJson(AdventureUtils.convertToJson(name)).getHandle())
        ));
        metadata.add(new WrappedDataValue(
                3,
                WrappedDataWatcher.Registry.get(Boolean.class),
                true
        ));
        metadata.add(new WrappedDataValue(
                4,
                WrappedDataWatcher.Registry.get(Boolean.class),
                true
        ));
        metadata.add(new WrappedDataValue(
                5,
                WrappedDataWatcher.Registry.get(Boolean.class),
                true
        ));
        metadata.add(new WrappedDataValue(
                15,
                WrappedDataWatcher.Registry.get(Byte.class),
                ARMOR_STAND_MARKER_FLAG
        ));
        packet.getDataValueCollectionModifier().write(0, metadata);
        return packet;
    }

    private PacketContainer teleportPacket(ClientLine line, Location location) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
        packet.getModifier().writeDefaults();
        packet.getIntegers().write(0, line.entityId);
        if (packet.getDoubles().size() >= 3) {
            packet.getDoubles().write(0, location.getX());
            packet.getDoubles().write(1, location.getY());
            packet.getDoubles().write(2, location.getZ());
            writeRotation(packet, location.getYaw(), location.getPitch());
            return packet;
        }

        if (!canWritePositionMoveRotation()) {
            throw new IllegalStateException("ProtocolLib cannot write ENTITY_TELEPORT on this server");
        }
        try {
            Object rotation = positionMoveRotationCreate.invoke(
                    null,
                    new Vector(location.getX(), location.getY(), location.getZ()),
                    new Vector(0, 0, 0),
                    location.getYaw(),
                    location.getPitch()
            );
            Object modifier = positionMoveRotationModifier.invoke(packet);
            Method write = modifier.getClass().getMethod("write", int.class, Object.class);
            write.invoke(modifier, 0, rotation);
            return packet;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not write modern ENTITY_TELEPORT", exception);
        }
    }

    private void writeRotation(PacketContainer packet, float yaw, float pitch) {
        if (packet.getBytes().size() >= 2) {
            packet.getBytes().write(0, angle(yaw));
            packet.getBytes().write(1, angle(pitch));
        }
        if (packet.getBooleans().size() > 0) {
            packet.getBooleans().write(0, false);
        }
    }

    private boolean canWritePositionMoveRotation() {
        return positionMoveRotationModifier != null && positionMoveRotationCreate != null;
    }

    private void send(Session session, PacketContainer... packets) {
        try {
            for (PacketContainer packet : packets) {
                protocolManager.sendServerPacket(session.player, packet);
            }
        } catch (Throwable throwable) {
            if (!warnedSendFailure) {
                warnedSendFailure = true;
                VortexPlugin.getInstance().getLogger().warning(
                        "Could not send a fake hologram packet: " + throwable.getMessage());
            }
        }
    }

    private synchronized int nextEntityId() {
        if (nextEntityId <= entityIdFloor) {
            reserveEntityIdBlock();
        }
        return nextEntityId--;
    }

    /**
     * VortexCore is shaded into multiple plugins, so class statics are not shared between their classloaders.
     * Reserve IDs through the JVM-wide system properties object to prevent their client-only entities from
     * replacing one another. Reserving a new range on every backend initialization also protects live reloads
     * from IDs belonging to packets sent by the previous plugin instance.
     */
    private void reserveEntityIdBlock() {
        synchronized (System.getProperties()) {
            int start;
            try {
                start = Integer.parseInt(System.getProperty(ENTITY_ID_CURSOR_PROPERTY, "-1"));
            } catch (NumberFormatException ignored) {
                start = -1;
            }

            long floor = (long) start - ENTITY_ID_BLOCK_SIZE;
            if (floor < Integer.MIN_VALUE) {
                throw new IllegalStateException("Exhausted fake hologram entity ID ranges");
            }

            nextEntityId = start;
            entityIdFloor = (int) floor;
            System.setProperty(ENTITY_ID_CURSOR_PROPERTY, Integer.toString(entityIdFloor));
        }
    }

    private static byte angle(float degrees) {
        return (byte) Math.floor(degrees * 256.0f / 360.0f);
    }

    private static ChunkPosition chunkPosition(Hologram.HologramPacketSnapshot snapshot) {
        return new ChunkPosition(
                snapshot.worldId(),
                snapshot.location().getBlockX() >> 4,
                snapshot.location().getBlockZ() >> 4
        );
    }

    private static boolean sameChunk(Hologram.HologramPacketSnapshot snapshot, ChunkPosition position) {
        return chunkPosition(snapshot).equals(position);
    }

    private static final class Session {
        private final UUID playerId;
        private final Set<ChunkPosition> loadedChunks = ConcurrentHashMap.newKeySet();
        private final Map<String, ClientHologram> holograms = new HashMap<>();
        private final Object sendLock = new Object();
        private volatile Player player;
        private volatile UUID worldId;

        private Session(UUID playerId) {
            this.playerId = playerId;
        }
    }

    private static final class ClientHologram {
        private final List<ClientLine> lines = new ArrayList<>();
        private ChunkPosition chunkPosition;
    }

    private static final class ClientLine {
        private final int entityId;
        private final UUID uuid;
        private boolean sent;
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;
        private Component name;

        private ClientLine(int entityId, UUID uuid) {
            this.entityId = entityId;
            this.uuid = uuid;
        }
    }

    private record ChunkPosition(UUID worldId, int x, int z) {
    }
}
