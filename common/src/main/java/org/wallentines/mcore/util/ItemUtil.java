package org.wallentines.mcore.util;

import org.wallentines.mcore.GameVersion;
import org.wallentines.mcore.MidnightCoreAPI;
import org.wallentines.mcore.text.Component;
import org.wallentines.mcore.text.ModernSerializer;
import org.wallentines.mcore.text.TextColor;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.mdcfg.serializer.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * A utility class with many item-related functions
 */
public class ItemUtil {

    /**
     * Splits a UUID into four 32-bit integers
     * @param uuid The UUID to split
     * @return An integer array of length 4
     */
    public static int[] splitUUID(UUID uuid) {
        long u1 = uuid.getMostSignificantBits();
        long u2 = uuid.getLeastSignificantBits();
        return new int[] { (int) (u1 >> 32), (int) u1, (int) (u2 >> 32), (int) u2 };
    }

    /**
     * Creates a UUID from four 32-bit integers
     * @param ints The data to create a UUID from
     * @return A UUID
     */
    public static UUID joinUUID(int[] ints) {

        if(ints.length != 4) {
            MidnightCoreAPI.LOGGER.warn("Attempt to form UUID from array of length {}! Expected length 4!", ints.length);
            return null;
        }

        return new UUID(
                (long) ints[0] << 32 | (long) ints[1] & 0xFFFFFFFFL,
                (long) ints[2] << 32 | (long) ints[3] & 0xFFFFFFFFL
        );
    }

    public static final Serializer<UUID> UUID_SERIALIZER = new Serializer<UUID>() {
        @Override
        public <O> SerializeResult<O> serialize(SerializeContext<O> serializeContext, UUID uuid) {

            GameVersion version = GameVersion.getVersion(serializeContext);
            if(version.hasFeature(GameVersion.Feature.INT_ARRAY_UUIDS)) {
                List<O> out = Arrays.stream(splitUUID(uuid)).boxed().map(serializeContext::toNumber).toList();
                return SerializeResult.success(serializeContext.toList(out));

            } else {
                return SerializeResult.success(serializeContext.toString(uuid.toString()));
            }
        }

        @Override
        public <O> SerializeResult<UUID> deserialize(SerializeContext<O> serializeContext, O o) {

            GameVersion version = GameVersion.getVersion(serializeContext);
            if(version.hasFeature(GameVersion.Feature.INT_ARRAY_UUIDS)) {

                return serializeContext.asList(o).map(os -> {

                    int[] arr = new int[4];
                    int index = 0;
                    if(os.size() != 4) {
                        return SerializeResult.failure("Unable to deserialize UUID as an int-array! Array was not the right length");
                    }

                    for(O oo : os) {
                        SerializeResult<Number> num = serializeContext.asNumber(oo);
                        if(!num.isComplete()) return SerializeResult.failure(num.getError());

                        arr[index++] = num.getOrThrow().intValue();
                    }
                    return SerializeResult.success(joinUUID(arr));
                });

            } else {
                try {
                    return SerializeResult.success(java.util.UUID.fromString(serializeContext.asString(o).getOrThrow()));
                } catch (IllegalArgumentException ex) {
                    return SerializeResult.failure("Unable to read UUID from string! " + ex.getMessage());
                } catch (SerializeException ex) {
                    return SerializeResult.failure(ex);
                }
            }

        }
    };

    /**
     * Set italic to false on the component if unset, so components appear as intended on ItemStacks
     * @param component The component to modify
     * @return A modified component
     */
    public static Component applyItemNameBaseStyle(Component component) {
        Component out = component;
        if(out.italic == null) {
            out = out.withItalic(false);
        }
        return out;
    }

    /**
     * Set italic to false and color to white on the component if unset, so components appear as intended on ItemStacks
     * @param component The component to modify
     * @return A modified component
     */
    public static Component applyItemLoreBaseStyle(Component component) {
        Component out = applyItemNameBaseStyle(component);
        if(out.color == null) {
            out = out.withColor(TextColor.WHITE);
        }
        return out;
    }



    public static String serializeName(Component component, GameVersion version, GameVersion.Feature feature) {
        String strName;
        if(version.hasFeature(feature)) {
            try(ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                JSONCodec.minified().encode(new GameVersion.VersionContext<>(ConfigContext.INSTANCE, version), ModernSerializer.INSTANCE, component, bos);
                strName = bos.toString();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            strName = component.toLegacyText();
            if(component.color == null) {
                strName = "\u00A7f" + strName;
            }
        }
        return strName;
    }

}
