package org.wallentines.mcore;

import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.serializer.*;
import org.wallentines.midnightlib.types.DefaultedSingleton;
import org.wallentines.midnightlib.types.Singleton;

import java.util.function.Function;

/**
 * A data type which stores basic information about a Minecraft version, including version ID (i.e. 1.20.1) and
 * protocol version (i.e. 763)
 */
public class GameVersion {

    private final String id;
    private final int protocolVersion;

    /**
     * The maximum protocol version for release versions. Starting at 1.16.4-pre1, snapshot versions use numbers
     * greater than this.
     */
    public static final int RELEASE_MAX_VERSION = 0x40000000;

    /**
     * Constructs a GameVersion object with the given ID and protocol version.
     * @param id The ID of the version
     * @param protocolVersion The protocol version of the game version.
     */
    public GameVersion(String id, int protocolVersion) {
        this.id = id;
        this.protocolVersion = protocolVersion;
    }

    /**
     * Returns the GameVersion's version ID
     * @return The version ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the GameVersion's protocol version
     * @return The protocol version
     */
    public int getProtocolVersion() {
        return protocolVersion;
    }


    public boolean isSnapshot() {
        return protocolVersion > RELEASE_MAX_VERSION;
    }

    public int getSnapshotVersion() {
        return protocolVersion - RELEASE_MAX_VERSION;
    }

    /**
     * Checks whether this GameVersion supports a particular {@link Feature Feature}
     * @param feature The feature to check
     * @return Whether this GameVersion supports the given Feature
     */
    public boolean hasFeature(Feature feature) {
        return feature.check(protocolVersion);
    }


    @Override
    public String toString() {
        if(isSnapshot()) {
            return id + " (Snapshot " + getSnapshotVersion() + ")";
        }
        return id + " (" + protocolVersion + ")";
    }

    /**
     * A singleton containing the GameVersion of the currently running server or client.
     */
    public static final Singleton<GameVersion> CURRENT_VERSION = new DefaultedSingleton<>(new GameVersion("Unknown", RELEASE_MAX_VERSION));

    public static final GameVersion VERSION_1_8 = new GameVersion("1.8", 47);
    public static final GameVersion MAX = new GameVersion("Maximum", RELEASE_MAX_VERSION);

    public static class VersionContext<T> extends DelegatedContext<T, GameVersion> {
        public VersionContext(SerializeContext<T> delegate) {
            super(delegate, CURRENT_VERSION.getOr(MAX));
        }

        public VersionContext(SerializeContext<T> delegate, GameVersion value) {
            super(delegate, value);
        }
    }

    public static VersionContext<ConfigObject> context() {
        return new VersionContext<>(ConfigContext.INSTANCE);
    }

    public static VersionContext<ConfigObject> context(GameVersion version) {
        return new VersionContext<>(ConfigContext.INSTANCE, version);
    }

    public static <T, C extends SerializeContext<T>> VersionContext<T> context(C value) {
        return new VersionContext<>(value);
    }

    public static <T, C extends SerializeContext<T>> VersionContext<T> context(C value, GameVersion version) {
        return new VersionContext<>(value, version);
    }

    public static GameVersion getVersion(SerializeContext<?> context) {
        if(context instanceof VersionContext) {
            return ((VersionContext<?>) context).getContextValue();
        }
        return CURRENT_VERSION.getOr(MAX);
    }

    public static <T> Serializer<T> serializerFor(Function<GameVersion, Serializer<T>> map) {
        return new Serializer<T>() {
            @Override
            public <O> SerializeResult<O> serialize(SerializeContext<O> context, T value) {
                GameVersion ver = getVersion(context);
                Serializer<T> ser = map.apply(ver);
                if(ser == null) return SerializeResult.failure("No serializer for " + ver);
                return ser.serialize(context, value);
            }

            @Override
            public <O> SerializeResult<T> deserialize(SerializeContext<O> context, O value) {
                GameVersion ver = getVersion(context);
                Serializer<T> ser = map.apply(ver);
                if(ser == null) return SerializeResult.failure("No serializer for " + ver);
                return ser.deserialize(context, value);
            }
        };
    }


    public static class Feature {


        /**
         * Starting in 1.16 (20w17a), Text component support full RGB colors
         */
        public static final Feature RGB_TEXT = new Feature(713);

        /**
         * Starting in 1.16 (20w17a), Hover events started using the field "contents" rather than "value"
         */
        public static final Feature HOVER_CONTENTS = new Feature(713);

        /**
         * Starting in 1.16 (20w12a), When UUIDs are stored in NBT, they are stored as arrays of integers rather than strings
         */
        public static final Feature INT_ARRAY_UUIDS = new Feature(707);

        /**
         * Starting in 1.14 (18w43a), Items now use components for their lore rather than legacy text
         */
        public static final Feature COMPONENT_ITEM_LORE = new Feature(440);

        /**
         * Starting in 1.13 (17w47a), Items no longer have numeric IDs (1=stone, 2=grass, etc.) and use namespaced IDs
         * (minecraft:stone, minecraft:grass) exclusively
         */
        public static final Feature NAMESPACED_IDS = new Feature(346);

        /**
         * Starting in 1.13 (18w01a), Items now use components for their custom names rather than legacy text
         */
        public static final Feature COMPONENT_ITEM_NAMES = new Feature(346);

        /**
         * In 1.12 (17w16a), The keybind component type was added
         */
        public static final Feature KEY_BIND_COMPONENT = new Feature(322);

        /**
         * In 1.12, The show_achievement action in Hover events was removed
         */
        public static final Feature HOVER_SHOW_ACHIEVEMENT = new Feature(0, 316);

        /**
         * In 1.9, the offhand slot was added
         */
        public static final Feature OFF_HAND = new Feature(49);

        /**
         * In 1.20.2, A new network protocol state called "Configuration" was added.
         */
        public static final Feature CONFIGURATION_PROTOCOL = new Feature(764, -1, 144);

        /**
         * Starting in 1.20.3, Server resource packs were identified by UUIDs, and multiple could be applied
         */
        public static final Feature RESOURCE_PACK_IDS = new Feature(765, -1, 164);

        /**
         * In 1.20.5, Cookies and server transfer packets were added.
         */
        public static final Feature TRANSFER_PACKETS = new Feature(766, GameVersion.RELEASE_MAX_VERSION, 171);

        /**
         * In 1.20.5, Item components were added to replace NBT tags
         */
        public static final Feature ITEM_COMPONENTS = new Feature(766, GameVersion.RELEASE_MAX_VERSION, 178);

        /**
         * In 24w13a, the item_name item component was added
         */
        public static final Feature ITEM_NAME_OVERRIDE = new Feature(766, GameVersion.RELEASE_MAX_VERSION, 182);

        /**
         * In 24w44a, the shadow_color text component field was added
         */
        public static final Feature COMPONENT_SHADOW_COLOR = new Feature(769, GameVersion.RELEASE_MAX_VERSION, 220);

        /**
         * In 25w02a, hover events and click event contents were inlined
         */
        public static final Feature COMPONENT_SNBT = new Feature(770, GameVersion.RELEASE_MAX_VERSION, 229);

        public final int minVersion;
        public final int maxVersion;
        public final int minSnapshotVersion;
        public final int maxSnapshotVersion;

        public Feature(int minVersion) {
            this(minVersion, -1, 1, -1);
        }

        public Feature(int minVersion, int maxVersion) {
            this(minVersion, maxVersion, maxVersion >= 0 ? -1 : 1, -1);
        }

        public Feature(int minVersion, int maxVersion, int minSnapshotVersion) {
            this(minVersion, maxVersion, minSnapshotVersion, -1);
        }

        public Feature(int minVersion, int maxVersion, int minSnapshotVersion, int maxSnapshotVersion) {
            this.minVersion = minVersion;
            this.maxVersion = maxVersion >= 0 ? maxVersion : RELEASE_MAX_VERSION;
            this.minSnapshotVersion = minSnapshotVersion >= 0 ? RELEASE_MAX_VERSION + minSnapshotVersion : Integer.MAX_VALUE;
            this.maxSnapshotVersion = maxSnapshotVersion >= 0 ? RELEASE_MAX_VERSION + maxSnapshotVersion : Integer.MAX_VALUE;
        }

        /**
         * Checks whether the given protocol version supports the given feature
         * @param protocolVersion The protocol version to check
         * @return Whether that protocol version supports the feature
         */
        public boolean check(int protocolVersion) {

            if(protocolVersion > RELEASE_MAX_VERSION) {
                return protocolVersion >= minSnapshotVersion && protocolVersion <= maxSnapshotVersion;
            }

            return protocolVersion >= minVersion && protocolVersion <= maxVersion;
        }

    }

}
