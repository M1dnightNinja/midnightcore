package org.wallentines.mcore.util;

import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.resources.RegistryOps;
import org.wallentines.mcore.Server;
import org.wallentines.mdcfg.ConfigObject;

public class ItemComponentUtil {

    private static DynamicOps<ConfigObject> getConfigOps() {
        return RegistryOps.create(ConfigOps.INSTANCE, RegistryUtil.access());
    }

    public static <T> ConfigObject encode(DataComponentType<T> type, T value) {
        return type.codecOrThrow().encodeStart(getConfigOps(), value).getOrThrow();
    }

    public static <T> ConfigObject encodeTyped(TypedDataComponent<T> type) {
        return type.encodeValue(getConfigOps()).getOrThrow();
    }

    public static <T> T decode(DataComponentType<T> type, ConfigObject value) {
        try {
            return type.codecOrThrow().decode(getConfigOps(), value).getOrThrow().getFirst();
        } catch (Exception ex) {
            throw new RuntimeException("Unable to decode component with type " + type, ex);
        }
    }

    public static <T> TypedDataComponent<T> decodeTyped(DataComponentType<T> type, ConfigObject value) {
        return new TypedDataComponent<>(type, decode(type, value));
    }

}
