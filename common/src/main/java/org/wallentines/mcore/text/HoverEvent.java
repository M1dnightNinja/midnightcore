package org.wallentines.mcore.text;

import org.wallentines.mcore.GameVersion;
import org.wallentines.mcore.ItemStack;
import org.wallentines.mcore.util.ItemUtil;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.SNBTCodec;
import org.wallentines.mdcfg.serializer.*;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;

import java.util.*;

public interface HoverEvent {

    static String getKey(GameVersion version) {
        return version.hasFeature(GameVersion.Feature.COMPONENT_SNBT) ? "hover_event" : "hoverEvent";
    }

    Action<?> action();

    static Simple<Component> create(Component component) {
        return new Simple<>(Action.SHOW_TEXT, component);
    }

    static Simple<ItemStack> forItem(ItemStack item) {
        return new Simple<>(Action.SHOW_ITEM, item);
    }

    static Simple<EntityInfo> forEntity(EntityInfo ent) {
        return new Simple<>(Action.SHOW_ENTITY, ent);
    }

    class Simple<T> implements HoverEvent {

        private final Action<T> action;
        private final T value;

        public Simple(Action<T> action, T value) {
            this.action = action;
            this.value = value;
        }

        public Action<T> action() {
            return action;
        }

        public T value() {
            return value;
        }

        public static class Action<T> implements HoverEvent.Action<Simple<T>> {
            private final Serializer<Simple<T>> serializer;

            Action(Serializer<T> serializer) {
                this.serializer = serializer.flatMap(Simple::value, value -> new Simple<>(this, value));
            }

            @Override
            public Serializer<Simple<T>> serializer() {
                return serializer;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Simple<?> simple = (Simple<?>) o;
            return Objects.equals(action, simple.action) && Objects.equals(value, simple.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(action, value);
        }

        @Override
        public String toString() {
            return "Simple{" +
                    "action=" + action +
                    ", value=" + value +
                    '}';
        }
    }

    interface Action<T extends HoverEvent> {
        Serializer<T> serializer();
        Registry<String, Action<?>> REGISTRY = Registry.createStringRegistry();

        static <T extends HoverEvent, A extends Action<T>> A register(String name, A action) {
            REGISTRY.register(name, action);
            return action;
        }

        Simple.Action<Component> SHOW_TEXT = register("show_text", new Simple.Action<>(
                GameVersion.serializerFor(gv -> {
                    if(gv.hasFeature(GameVersion.Feature.HOVER_CONTENTS) && !gv.hasFeature(GameVersion.Feature.COMPONENT_SNBT)) {
                        return ModernSerializer.INSTANCE.fieldOf("contents");
                    }
                    return ModernSerializer.INSTANCE.fieldOf("value");
                })
        ));

        Simple.Action<ItemStack> SHOW_ITEM = register("show_item", new Simple.Action<>(
                new Serializer<ItemStack>() {
                    @Override
                    public <O> SerializeResult<O> serialize(SerializeContext<O> ctx, ItemStack value) {

                        GameVersion version = GameVersion.getVersion(ctx);

                        ConfigSection out = new ConfigSection();
                        out.set("id", value.getType().toString());

                        String countKey = version.hasFeature(GameVersion.Feature.HOVER_CONTENTS) ? "count" : "Count";
                        out.set(countKey, value.getCount());

                        if(!version.hasFeature(GameVersion.Feature.NAMESPACED_IDS)) {
                            out.set("Damage", value.getLegacyDataValue());
                        }

                        if(version.hasFeature(GameVersion.Feature.ITEM_COMPONENTS)) {
                            out.set("components", value.getComponentPatch(), ItemStack.ComponentPatchSet.SERIALIZER);
                        } else {
                            if(version.hasFeature(GameVersion.Feature.HOVER_CONTENTS)) {
                                SNBTCodec snbt = new SNBTCodec();
                                if(version.getProtocolVersion() < 477) {
                                    snbt.useDoubleQuotes();
                                }

                                out.set("tag", snbt.encodeToString(ConfigContext.INSTANCE, value.getCustomData()));
                            } else {
                                out.set("tag", value.getCustomData());
                            }
                        }

                        if(!version.hasFeature(GameVersion.Feature.COMPONENT_SNBT)) {
                            out = new ConfigSection().with("contents", out);
                        }

                        return SerializeResult.success(ConfigContext.INSTANCE.convert(ctx, out));
                    }

                    @Override
                    public <O> SerializeResult<ItemStack> deserialize(SerializeContext<O> ctx, O value) {

                        GameVersion version = GameVersion.getVersion(ctx);
                        if(!version.hasFeature(GameVersion.Feature.COMPONENT_SNBT) && ctx.isString(value)) {
                            return ctx.asString(value).map(str -> {
                                try {
                                    return SerializeResult.success(ItemStack.Builder.of(version, Identifier.parseOrDefault(str, "minecraft")).build());
                                } catch (Exception ex) {
                                    return SerializeResult.failure("Unable to parse item ID!", ex);
                                }
                            });
                        }

                        ConfigSection sec = ctx.convert(ConfigContext.INSTANCE, value).asSection();
                        if(!version.hasFeature(GameVersion.Feature.COMPONENT_SNBT)) {
                            sec = sec.getSection("contents");
                        }

                        String sid = sec.getOrDefault("id", (String) null);
                        if(sid == null) {
                            return SerializeResult.failure("Item id is required!");
                        }

                        Identifier id;
                        try {
                            id = Identifier.parseOrDefault(sid, "minecraft");
                        } catch (IllegalArgumentException ex) {
                            return SerializeResult.failure("Unable to parse item ID!", ex);
                        }
                        ItemStack.Builder builder = ItemStack.Builder.of(version, id);

                        String countKey = version.hasFeature(GameVersion.Feature.HOVER_CONTENTS) ? "count" : "Count";

                        if(sec.has(countKey)) {
                            builder.withCount(sec.getOrDefault(countKey, 1).intValue());
                        }

                        if(!version.hasFeature(GameVersion.Feature.NAMESPACED_IDS) && sec.has("Damage")) {
                            builder.withDataValue(sec.getOrDefault("Damage", 0).byteValue());
                        }

                        if(version.hasFeature(GameVersion.Feature.ITEM_COMPONENTS)) {
                            if(sec.has("components")) {
                                Optional<ItemStack.ComponentPatchSet> opt = sec.getOptional("components", ItemStack.ComponentPatchSet.SERIALIZER);
                                if(opt.isPresent()) {
                                    builder.withComponents(opt.get());
                                } else {
                                    return SerializeResult.failure("Unable to parse item components!");
                                }
                            }
                        } else {
                            if(sec.has("tag")) {
                                ConfigSection tag;
                                if (version.hasFeature(GameVersion.Feature.HOVER_CONTENTS)) {

                                    SNBTCodec snbt = new SNBTCodec();
                                    if(version.getProtocolVersion() < 477) {
                                        snbt.useDoubleQuotes();
                                    }

                                    ConfigObject obj = snbt.decode(ConfigContext.INSTANCE, sec.getString("tag"));
                                    if (!obj.isString()) {
                                        return SerializeResult.failure("Expected item tag to be an SNBT string!");
                                    }
                                    tag = obj.asSection();
                                } else {
                                    tag = sec.getSection("tag");
                                }
                                builder.withCustomData(tag);
                            }
                        }

                        return SerializeResult.success(builder.build());
                    }
                }
        ));

        Simple.Action<EntityInfo> SHOW_ENTITY = register("show_entity", new Simple.Action<>(
                GameVersion.serializerFor(gv -> {
                    if(gv.hasFeature(GameVersion.Feature.COMPONENT_SNBT)) {
                        return EntityInfo.SERIALIZER;
                    }
                    return EntityInfo.LEGACY_SERIALIZER.fieldOf("contents");
                })
        ));
    }

    Serializer<HoverEvent> SERIALIZER = Action.REGISTRY.byIdSerializer().fieldOf("action").dispatch(Action::serializer, HoverEvent::action);

    class EntityInfo {

        public final Component name;
        public final Identifier type;
        public final UUID uuid;

        public EntityInfo(Component name, Identifier type, UUID uuid) {
            this.name = name;
            this.type = type;
            this.uuid = uuid;
        }

        private static final Serializer<EntityInfo> LEGACY_SERIALIZER = ObjectSerializer.create(
                ModernSerializer.INSTANCE.entry("name", (ei) -> ei.name),
                Identifier.serializer("minecraft").<EntityInfo>entry("type", (ei) -> ei.type).orElse(new Identifier("minecraft", "pig")),
                ItemUtil.UUID_SERIALIZER.entry("id", (ei) -> ei.uuid),
                EntityInfo::new
        );

        private static final Serializer<EntityInfo> SERIALIZER = ObjectSerializer.create(
                ModernSerializer.INSTANCE.entry("name", (ei) -> ei.name),
                Identifier.serializer("minecraft").<EntityInfo>entry("id", (ei) -> ei.type).orElse(new Identifier("minecraft", "pig")),
                ItemUtil.UUID_SERIALIZER.entry("uuid", (ei) -> ei.uuid),
                EntityInfo::new
        );
    }

}
