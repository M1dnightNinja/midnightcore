package org.wallentines.mcore;

import org.jetbrains.annotations.Nullable;
import org.wallentines.mcore.text.Component;
import org.wallentines.mcore.text.LegacySerializer;
import org.wallentines.mcore.text.ModernSerializer;
import org.wallentines.mcore.text.TextColor;
import org.wallentines.mcore.util.ItemUtil;
import org.wallentines.mdcfg.ConfigList;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigPrimitive;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.mdcfg.serializer.*;
import org.wallentines.midnightlib.math.Color;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.types.Singleton;

import java.util.*;
import java.util.stream.Stream;

/**
 * A data type representing a Minecraft item-stack
 */
public interface ItemStack {

    /**
     * Returns the ID of the type of item this is
     * @return The item type ID
     */
    Identifier getType();

    /**
     * Returns the amount of items in the stack
     * @return The item count
     */
    int getCount();

    /**
     * Returns the NBT tag of the item stack
     * @return The NBT tag of the item stack
     * @deprecated Use getCustomData() instead
     */
    @Deprecated
    default ConfigSection getTag() {
        return getCustomData();
    }

    /**
     * Returns the legacy data value of an item (may not work properly on versions >= 1.13)
     * @return The legacy data value of the item stack
     */
    byte getLegacyDataValue();

    /**
     * Changes the amount of items in the stack
     * @param count The new number of items
     */
    void setCount(int count);

    /**
     * Changes the NBT tag of the item
     * @param tag the new NBT tag
     * @deprecated Use setCustomData() or fillCustomData() instead
     */
    @Deprecated
    default void setTag(ConfigSection tag) {
        setCustomData(tag);
    }

    /**
     * Loads structured component data for the given ID
     * @param id The component ID
     * @param config The component config
     */
    void loadComponent(Identifier id, ConfigObject config);


    /**
     * Loads structured component data for the given ID
     * @param set The components to load
     */
    default void loadComponents(ComponentPatchSet set) {
        for(Identifier id : set.components.keySet()) {
            loadComponent(id, set.components.get(id));
        }
        for(Identifier id : set.removedComponents) {
            removeComponent(id);
        }
    }

    /**
     * Saves a structured component to a config object
     * @param id The component ID
     * @return The component config
     */
    @Nullable
    ConfigObject saveComponent(Identifier id);

    /**
     * Removes the component with the given ID from the item
     * @param id The ID of the component to remove
     */
    void removeComponent(Identifier id);

    /**
     * Gets a list of ids for the components on this item
     * @return A list of ids
     */
    Stream<Identifier> getComponentIds();

    /**
     * Gets a list of ids for the components on this item
     * @return A list of ids
     */
    ComponentPatchSet getComponentPatch();

    /**
     * Gets the data in the custom data component on the item. On pre-1.20.5 servers, this is the item's tag.
     * @return The item's custom data
     */
    @Nullable
    default ConfigSection getCustomData() {
        ConfigObject obj = saveComponent(CUSTOM_DATA_COMPONENT);
        if(obj == null || !obj.isSection()) return null;
        return obj.asSection();
    }

    /**
     * Sets the data in the custom data component on the item. On pre-1.20.5 servers, this is the item's tag.
     * @param section The new custom data
     */
    default void setCustomData(ConfigSection section) {
        loadComponent(CUSTOM_DATA_COMPONENT, section);
    }


    /**
     * Merges the given data into the data in the custom data component on the item. On pre-1.20.5 servers, this is the
     * item's tag.
     * @param section The new custom data
     */
    default void fillCustomData(ConfigSection section) {

        ConfigSection obj = getCustomData();
        if(obj == null) obj = new ConfigSection();

        obj.asSection().fillOverwrite(section);
        setCustomData(obj);
    }

    /**
     * Increases the amount of items in the stack by the given amount.
     * @param amount The amount to grow the stack by
     */
    void grow(int amount);

    /**
     * Decreases the amount of items in the stack by the given amount.
     * @param amount The amount to shrink the stack by
     */
    void shrink(int amount);

    /**
     * Gets the translation key for this item type, for use in translate Components.
     * @return The translation key for this item type
     */
    String getTranslationKey();


    /**
     * Gets the version of the game this item is native to
     * @return The item's game version.
     */
    GameVersion getVersion();

    /**
     * Makes an exact copy of this item stack
     * @return An exact copy
     */
    default ItemStack copy() {
        return Builder.of(getVersion(), getType())
                .withCount(getCount())
                .withComponents(getComponentPatch())
                .withDataValue(getLegacyDataValue())
                .build();
    }

    /**
     * Changes the display name of the item
     * @param component The new display name
     */
    default void setName(Component component) {

        GameVersion version = getVersion();
        if(version.hasFeature(GameVersion.Feature.ITEM_COMPONENTS)) {

            if(version.hasFeature(GameVersion.Feature.COMPONENT_SNBT)) {
                loadComponent(ITEM_NAME_COMPONENT, ModernSerializer.INSTANCE.serialize(ConfigContext.INSTANCE, component).getOrThrow());
            } else if(version.hasFeature(GameVersion.Feature.ITEM_NAME_OVERRIDE)) {
                loadComponent(ITEM_NAME_COMPONENT, new ConfigPrimitive(component.toJSONString(version)));
            } else {
                loadComponent(CUSTOM_NAME_COMPONENT, new ConfigPrimitive(ItemUtil.applyItemNameBaseStyle(component).toJSONString(version)));
            }

        } else {

            component = ItemUtil.applyItemNameBaseStyle(component);
            String strName = ItemUtil.serializeName(component, getVersion(), GameVersion.Feature.COMPONENT_ITEM_NAMES);

            fillCustomData(new ConfigSection().with("display", new ConfigSection().with("Name", strName)));
        }
    }

    default Component getTypeName() {
        return Component.translate(getTranslationKey());
    }

    /**
     * Gets the display name of an item as it appears when hovering over it
     * @return The item's custom name if available, or a translate component
     */
    default Component getName() {

        Component name;
        boolean italicize = true;

        GameVersion version = getVersion();

        // 1.20.5+: Get item name from components
        if(version.hasFeature(GameVersion.Feature.ITEM_COMPONENTS)) {

            // Try custom_name first, then item_name
            ConfigObject encoded = saveComponent(CUSTOM_NAME_COMPONENT);
            if(encoded == null) {
                encoded = saveComponent(ITEM_NAME_COMPONENT);
                italicize = false; // item_name components are not automatically italicized
            }

            if(encoded == null) return getTypeName().withColor(getRarityColor());

            if(version.hasFeature(GameVersion.Feature.COMPONENT_SNBT)) {
                name = ModernSerializer.INSTANCE.deserialize(ConfigContext.INSTANCE, encoded).getOrThrow();
            } else {
                name = ModernSerializer.INSTANCE.deserialize(ConfigContext.INSTANCE, JSONCodec.minified().decode(GameVersion.context(version), encoded.asString())).getOrThrow();
            }


            // pre-1.20.5: Get item name from tag
        } else {

            ConfigSection tag = getCustomData();
            if (tag == null || !tag.has("display")) {
                return getTypeName().withColor(getRarityColor());
            }

            ConfigSection display = tag.getSection("display");
            if (!display.hasString("Name")) {
                return getTypeName().withColor(getRarityColor());
            }

            String rawName = display.getString("Name");

            if(getVersion().hasFeature(GameVersion.Feature.COMPONENT_ITEM_NAMES)) {
                name = ModernSerializer.INSTANCE.deserialize(GameVersion.context(getVersion()), JSONCodec.minified().decode(ConfigContext.INSTANCE, rawName)).getOrThrow();
            } else {
                name = LegacySerializer.INSTANCE.deserialize(ConfigContext.INSTANCE, new ConfigPrimitive(rawName)).getOrThrow();
                if(name.italic == null && name.color != null) {
                    name = name.withItalic(false);
                }
            }
        }

        if(italicize && name.italic == null) name = name.withItalic(true);
        if(name.color == null) name = name.withColor(getRarityColor());
        return name;
    }

    /**
     * Gets the lore of an item as it appears when hovering over it
     * @return The item's lore.
     */
    default List<Component> getLore() {

        ConfigList lore;
        GameVersion ver = getVersion();
        if(ver.hasFeature(GameVersion.Feature.ITEM_COMPONENTS)) {

            ConfigObject encoded = saveComponent(LORE_COMPONENT);
            if(encoded == null) return Collections.emptyList();

            lore = encoded.asList();

        } else {
            ConfigSection tag = getCustomData();
            if (tag == null || !tag.has("display")) {
                return Collections.emptyList();
            }

            ConfigSection display = tag.getSection("display");
            if (!display.hasList("Lore")) {
                return Collections.emptyList();
            }

            lore = display.get("Lore").asList();
        }

        List<Component> out = new ArrayList<>();
        if(ver.hasFeature(GameVersion.Feature.COMPONENT_SNBT)) {
            for(ConfigObject o : lore.values()) {
                out.add(ModernSerializer.INSTANCE.deserialize(GameVersion.context(getVersion()), o).getOrThrow());
            }
        } else {
            for(ConfigObject o : lore.values()) {

                String s = o.asString();
                if(ver.hasFeature(GameVersion.Feature.COMPONENT_ITEM_LORE)) {
                    out.add(ModernSerializer.INSTANCE.deserialize(GameVersion.context(getVersion()), new ConfigPrimitive(s)).getOrThrow());
                } else {
                    out.add(LegacySerializer.INSTANCE.deserialize(ConfigContext.INSTANCE, new ConfigPrimitive(s)).getOrThrow());
                }
            }
        }
        return out;
    }

    /**
     * Changes the item's lore to match the given components
     * @param components The item's new lore
     */
    default void setLore(List<Component> components) {
        ConfigList out = new ConfigList();
        GameVersion ver = getVersion();

        if(ver.hasFeature(GameVersion.Feature.ITEM_COMPONENTS)) {
            for (Component cmp : components) {
                out.add(ModernSerializer.INSTANCE.serialize(ConfigContext.INSTANCE, cmp).getOrThrow());
            }
        } else {
            for (Component line : components) {
                out.add(ItemUtil.serializeName(line, ver, GameVersion.Feature.COMPONENT_ITEM_LORE));
            }
        }

        if(ver.hasFeature(GameVersion.Feature.ITEM_COMPONENTS)) {
            loadComponent(LORE_COMPONENT, out);
        } else {
            fillCustomData(new ConfigSection().with("display", new ConfigSection().with("Lore", out)));
        }
    }

    /**
     * Converts the ItemStack into an item builder with the same values
     * @return An item builder
     */
    default Builder asBuilder() {
        GameVersion version = getVersion();
        Builder builder = new Builder(version, getType())
                .withCount(getCount());

        if (version.hasFeature(GameVersion.Feature.ITEM_COMPONENTS)) {
            builder.withComponents(getComponentPatch());
        } else {
            builder.withCustomData(getCustomData());
        }

        if(!version.hasFeature(GameVersion.Feature.NAMESPACED_IDS)) {
            builder.withDataValue(getLegacyDataValue());
        }

        return builder;
    }

    /**
     * Gets the color associated with the item's rarity
     * @return The item's rarity color.
     */
    Color getRarityColor();

    /**
     * Constructs an empty (Air) item stack
     * @return An empty item stack
     */
    static ItemStack empty() {
        return Builder.of(GameVersion.CURRENT_VERSION.get(), new Identifier("minecraft", "air")).build();
    }


    /**
     * Will contain an item factory by the time the game loads
     */
    Singleton<Factory> FACTORY = new Singleton<>();

    Identifier CUSTOM_DATA_COMPONENT = new Identifier("minecraft", "custom_data");
    Identifier CUSTOM_NAME_COMPONENT = new Identifier("minecraft", "custom_name");
    Identifier ITEM_NAME_COMPONENT = new Identifier("minecraft", "item_name");
    Identifier LORE_COMPONENT = new Identifier("minecraft", "lore");
    Identifier ENCHANTMENT_COMPONENT = new Identifier("minecraft", "enchantments");
    Identifier PROFILE_COMPONENT = new Identifier("minecraft", "profile");


    Serializer<ItemStack> SERIALIZER = Builder.SERIALIZER.flatMap(ItemStack::asBuilder, Builder::build);

    /**
     * An interface to construct an item stack
     */
    interface Factory {
        ItemStack buildLegacy(Identifier id, int count, byte damage, ConfigSection tag, GameVersion version);
        ItemStack buildTagged(Identifier id, int count, ConfigSection tag, GameVersion version);
        ItemStack buildStructured(Identifier id, int count, ComponentPatchSet components, GameVersion version);
    }


    class ComponentPatchSet {
        public final HashMap<Identifier, ConfigObject> components = new HashMap<>();
        public final Set<Identifier> removedComponents = new HashSet<>();

        public ConfigObject get(Identifier id) {
            if(removedComponents.contains(id)) return null;
            return components.get(id);
        }

        public void set(Identifier id, ConfigObject obj) {

            if(obj == null) {
                clear(id);
                return;
            }

            components.put(id, obj);
            removedComponents.remove(id);
        }

        public void set(Identifier id, String obj) {
            if(obj == null) {
                clear(id);
                return;
            }
            set(id, new ConfigPrimitive(obj));
        }

        public void set(Identifier id, Number obj) {
            if(obj == null) {
                clear(id);
                return;
            }
            set(id, new ConfigPrimitive(obj));
        }

        public void set(Identifier id, Boolean obj) {
            if(obj == null) {
                clear(id);
                return;
            }
            set(id, new ConfigPrimitive(obj));
        }

        public void clear(Identifier id) {
            components.remove(id);
        }

        public void remove(Identifier id) {
            components.remove(id);
            removedComponents.add(id);
        }

        public void add(ComponentPatchSet other) {
            for(Identifier id : other.components.keySet()) {
                set(id, other.components.get(id));
            }
            for(Identifier id : other.removedComponents) {
                remove(id);
            }
        }

        public ComponentPatchSet copy() {
            ComponentPatchSet out = new ComponentPatchSet();
            for(Identifier id : components.keySet()) {
                out.components.put(id, components.get(id).copy());
            }
            out.removedComponents.addAll(removedComponents);
            return out;
        }


        public static ComponentPatchSet fromItem(ItemStack is) {
            ComponentPatchSet out = new ComponentPatchSet();
            is.getComponentIds().forEach(id -> {
                ConfigObject obj = is.saveComponent(id);
                if(obj != null) out.set(id, is.saveComponent(id));
            });
            return out;
        }

        public static final Serializer<ComponentPatchSet> SERIALIZER = ConfigObject.SERIALIZER.mapOf().flatMap(set -> {

            Map<String, ConfigObject> out = new HashMap<>();
            for(Identifier s : set.components.keySet()) {
                out.put(s.toString(), set.components.get(s));
            }
            for(Identifier s : set.removedComponents) {
                out.put("!" + s.toString(), new ConfigSection());
            }

            return out;
        }, map -> {

            ComponentPatchSet out = new ComponentPatchSet();
            for(String s : map.keySet()) {
                if(s.startsWith("!")) {
                    out.removedComponents.add(Identifier.parseOrDefault(s.substring(1), "minecraft"));
                } else {
                    out.components.put(Identifier.parseOrDefault(s, "minecraft"), map.get(s));
                }
            }

            return out;
        });
    }

    /**
     * A builder type to easily create item stacks.
     */
    class Builder {

        private final GameVersion version;
        private final Identifier id;
        private int count = 1;
        private byte dataValue = 0;
        private ComponentPatchSet components = new ComponentPatchSet();

        private Builder(GameVersion version, Identifier id) {
            this.version = version;
            this.id = id;
        }

        private ConfigSection getCustomData() {
            ConfigObject obj = components.get(CUSTOM_DATA_COMPONENT);
            if(obj == null || !obj.isSection()) {
                ConfigSection out = new ConfigSection();
                components.set(CUSTOM_DATA_COMPONENT, out);
                return out;
            }
            return obj.asSection();
        }

        /**
         * Changes the count of the item stack to be built
         * @param count The new count of the item stack
         * @return A reference to self
         */
        public Builder withCount(int count) {
            this.count = count;
            return this;
        }

        /**
         * Changes the NBT tag of the item stack to be built
         * @param tag The new NBT tag of the item stack
         * @return A reference to self
         */
        @Deprecated
        public Builder setTag(ConfigSection tag) {
            return withCustomData(tag);
        }

        /**
         * Adds the given data to the NBT tag of the item stack to be built
         * @param tag The new NBT tag of the item stack
         * @return A reference to self
         */
        @Deprecated
        public Builder withTag(ConfigSection tag) {
            return fillCustomData(tag);
        }

        /**
         * Sets the value of the component with the given ID
         * @param id The component ID
         * @param value The serialized value of the component config
         * @return A reference to self
         */
        public Builder withComponent(Identifier id, @Nullable ConfigObject value) {
            components.set(id, value);
            return this;
        }

        /**
         * Sets the components of the item to the given components
         * @param patches The item's new components
         * @return A reference to self
         */
        public Builder withComponents(@Nullable ComponentPatchSet patches) {
            if(patches != null) {
                components = patches;
            }
            return this;
        }

        /**
         * Adds the given components to the item
         * @param patches The item's extra components
         * @return A reference to self
         */
        public Builder addComponents(ComponentPatchSet patches) {
            this.components.add(patches);
            return this;
        }

        /**
         * Changes the custom data component of the item stack to be built
         * @param customData The new custom data component of the item stack
         * @return A reference to self
         */
        public Builder withCustomData(@Nullable ConfigSection customData) {
            return withComponent(CUSTOM_DATA_COMPONENT, customData);
        }

        /**
         * Adds the given data to the custom data component of the item stack to be built
         * @param customData The new custom data component of the item stack
         * @return A reference to self
         */
        public Builder fillCustomData(ConfigSection customData) {
            getCustomData().fillOverwrite(customData);
            return this;
        }

        /**
         * Changes the legacy data value of the item stack to be built
         * @param dataValue The new NBT tag of the item stack
         * @return A reference to self
         */
        public Builder withDataValue(@Nullable Byte dataValue) {
            if(dataValue == null) return this;
            if(dataValue != 0 && version.hasFeature(GameVersion.Feature.NAMESPACED_IDS)) {
                MidnightCoreAPI.LOGGER.warn("ItemStack data value specified on a version which does not support it!");
            }

            this.dataValue = dataValue;
            return this;
        }

        /**
         * Changes the item's display name to the given value
         * @param name The new custom name for the item
         * @return A reference to self
         */
        public Builder withName(Component name) {

            if (version.hasFeature(GameVersion.Feature.ITEM_COMPONENTS)) {
                if(version.hasFeature(GameVersion.Feature.COMPONENT_SNBT)) {
                    return withComponent(ITEM_NAME_COMPONENT, ModernSerializer.INSTANCE.serialize(ConfigContext.INSTANCE, name).getOrThrow());
                } else if(version.hasFeature(GameVersion.Feature.ITEM_NAME_OVERRIDE)) {
                    return withComponent(ITEM_NAME_COMPONENT, new ConfigPrimitive(name.toJSONString(version)));
                } else {
                    return withComponent(CUSTOM_NAME_COMPONENT, new ConfigPrimitive(ItemUtil.applyItemNameBaseStyle(name).toJSONString(version)));
                }
            }

            name = ItemUtil.applyItemNameBaseStyle(name);
            getCustomData().getOrCreateSection("display").set("Name", ItemUtil.serializeName(name, version, GameVersion.Feature.COMPONENT_ITEM_NAMES));
            return this;
        }

        /**
         * Changes the item's lore to the given value
         * @param lore The new lore for the item
         * @return A reference to self
         */
        public Builder withLore(Collection<Component> lore) {

            ConfigList list = new ConfigList();
            if(version.hasFeature(GameVersion.Feature.COMPONENT_SNBT)) {
                for (Component cmp : lore) {
                    list.add(ModernSerializer.INSTANCE.serialize(ConfigContext.INSTANCE, cmp).getOrThrow());
                }
            } else {
                for (Component cmp : lore) {
                    cmp = ItemUtil.applyItemLoreBaseStyle(cmp);
                    String str = ItemUtil.serializeName(cmp, version, GameVersion.Feature.COMPONENT_ITEM_LORE);

                    list.add(str);
                }
            }

            if(version.hasFeature(GameVersion.Feature.ITEM_COMPONENTS)) {
                return withComponent(LORE_COMPONENT, list);
            }
            else {
                getCustomData().getOrCreateSection("display").set("Lore", list);
            }
            return this;
        }


        public Builder withEnchantment(Identifier id, int level) {

            if(!version.hasFeature(GameVersion.Feature.NAMESPACED_IDS)) {
                return this;
            }

            if (version.hasFeature(GameVersion.Feature.ITEM_COMPONENTS)) {

                ConfigObject obj = components.get(ENCHANTMENT_COMPONENT);
                if(obj == null || !obj.isSection()) {
                    obj = new ConfigSection();
                    components.set(ENCHANTMENT_COMPONENT, obj);
                }

                obj.asSection().getOrCreateSection("levels").set(id.toString(), level);

            } else {

                ConfigSection tag = getCustomData();
                if(!tag.hasList("Enchantments")) {
                    tag.set("Enchantments", new ConfigList());
                }
                tag.getList("Enchantments").add(new ConfigSection().with("id", id.toString()).with("lvl", level));
            }

            return this;
        }

        public Builder withLegacyEnchantment(int id, int level) {

            if(version.hasFeature(GameVersion.Feature.NAMESPACED_IDS)) {
                return this;
            }

            ConfigSection tag = getCustomData();
            if(!tag.hasList("ench")) {
                tag.set("ench", new ConfigList());
            }

            tag.getList("ench").add(new ConfigSection().with("id", id).with("lvl", level));
            return this;
        }

        /**
         * Makes an exact copy of this Builder
         * @return An exact copy of the Builder
         */
        public Builder copy() {

            Builder out = new Builder(version, id);
            out.count = count;
            out.dataValue = dataValue;
            out.components = components.copy();

            return out;
        }

        /**
         * Constructs a new builder with the given type ID
         * @param id The type ID
         * @return A new builder
         */
        public static Builder of(Identifier id) {
            return new Builder(GameVersion.CURRENT_VERSION.get(), id);
        }

        /**
         * Constructs a new builder with the given type ID
         * @param version The game version to make the item for
         * @param id The type ID
         * @return A new builder
         */
        public static Builder of(GameVersion version, Identifier id) {
            return new Builder(version, id);
        }


        /**
         * Constructs a new builder for a specific color of the item with the given ID
         * @param color The color of item to build
         * @param base The base ID of the item (i.e. wool for green_wool, concrete for red_concrete, etc.)
         * @return A new builder
         */
        public static Builder ofColor(Color color, Identifier base) {
            return ofColor(GameVersion.CURRENT_VERSION.get(), color, base);
        }

        /**
         * Constructs a new builder for a specific color of the item with the given ID
         * @param version The game version to make the item for
         * @param color The color of item to build
         * @param base The base ID of the item (i.e. wool for green_wool, concrete for red_concrete, etc.)
         * @return A new builder
         */
        public static Builder ofColor(GameVersion version, Color color, Identifier base) {

            Builder out;
            if(version.hasFeature(GameVersion.Feature.NAMESPACED_IDS)) {
                out = new Builder(version,new Identifier(base.getNamespace(), TextColor.toDyeColor(color) + "_" + base.getPath()));
            } else {
                out = new Builder(version,base).withDataValue(TextColor.getLegacyDataValue(color));
            }

            return out;
        }

        /**
         * Constructs a new builder for a wool block with the given color
         * @param color The color of the wool block
         * @return A new builder
         */
        public static Builder woolWithColor(Color color) {
            return woolWithColor(GameVersion.CURRENT_VERSION.get(), color);
        }

        /**
         * Constructs a new builder for a wool block with the given color
         * @param version The game version to make the item for
         * @param color The color of the wool block
         * @return A new builder
         */
        public static Builder woolWithColor(GameVersion version, Color color) {
            return ofColor(version, color, new Identifier("minecraft", "wool"));
        }

        /**
         * Constructs a new builder for a stained glass block with the given color
         * @param color The color of the stained glass block
         * @return A new builder
         */
        public static Builder glassWithColor(Color color) {
            return glassWithColor(GameVersion.CURRENT_VERSION.get(), color);
        }

        /**
         * Constructs a new builder for a stained glass block with the given color
         * @param version The game version to make the item for
         * @param color The color of the stained glass block
         * @return A new builder
         */
        public static Builder glassWithColor(GameVersion version, Color color) {
            return ofColor(version, color, new Identifier("minecraft", "stained_glass"));
        }

        /**
         * Constructs a new builder for a stained glass pane block with the given color
         * @param color The color of the stained glass pane block
         * @return A new builder
         */
        public static Builder glassPaneWithColor(Color color) {
            return glassPaneWithColor(GameVersion.CURRENT_VERSION.get(), color);
        }

        /**
         * Constructs a new builder for a stained glass pane block with the given color
         * @param version The game version to make the item for
         * @param color The color of the stained glass pane block
         * @return A new builder
         */
        public static Builder glassPaneWithColor(GameVersion version, Color color) {
            return ofColor(version, color, new Identifier("minecraft", "stained_glass_pane"));
        }

        /**
         * Constructs a new builder for a concrete block with the given color
         * @param color The color of the concrete block
         * @return A new builder
         */
        public static Builder concreteWithColor(Color color) {
            return concreteWithColor(GameVersion.CURRENT_VERSION.get(), color);
        }

        /**
         * Constructs a new builder for a concrete block with the given color
         * @param version The game version to make the item for
         * @param color The color of the concrete block
         * @return A new builder
         */
        public static Builder concreteWithColor(GameVersion version, Color color) {
            return ofColor(version, color, new Identifier("minecraft", "concrete"));
        }

        /**
         * Constructs a new builder for a concrete powder block with the given color
         * @param color The color of the concrete powder block
         * @return A new builder
         */
        public static Builder concretePowderWithColor(Color color) {
            return concretePowderWithColor(GameVersion.CURRENT_VERSION.get(), color);
        }

        /**
         * Constructs a new builder for a concrete powder block with the given color
         * @param version The game version to make the item for
         * @param color The color of the concrete powder block
         * @return A new builder
         */
        public static Builder concretePowderWithColor(GameVersion version, Color color) {
            return ofColor(version, color, new Identifier("minecraft", "concrete_powder"));
        }

        /**
         * Constructs a new builder for a terracotta block with the given color
         * @param color The color of the terracotta block
         * @return A new builder
         */
        public static Builder terracottaWithColor(Color color) {
            return terracottaWithColor(GameVersion.CURRENT_VERSION.get(), color);
        }

        /**
         * Constructs a new builder for a terracotta block with the given color
         * @param version The game version to make the item for
         * @param color The color of the terracotta block
         * @return A new builder
         */
        public static Builder terracottaWithColor(GameVersion version, Color color) {

            String id = version.hasFeature(GameVersion.Feature.NAMESPACED_IDS) ? "terracotta" : "stained_hardened_clay";
            return ofColor(version, color, new Identifier("minecraft", id));
        }

        /**
         * Constructs a new builder for a player head with the given skin
         * @param skin The skin to apply to the player head item
         * @return A new builder
         */
        public static Builder headWithSkin(Skin skin) {
            return headWithSkin(GameVersion.CURRENT_VERSION.get(), skin);
        }

        /**
         * Constructs a new builder for a player head with the given skin
         * @param version The game version to make the item for
         * @param skin The skin to apply to the player head item
         * @return A new builder
         */
        public static Builder headWithSkin(GameVersion version, Skin skin) {
            return headWithSkin(version, skin, false);
        }

        /**
         * Constructs a new builder for a player head with the given skin
         * @param version The game version to make the item for
         * @param skin The skin to apply to the player head item
         * @param includeSignature Whether the skin signature should be included on 1.20.5+
         * @return A new builder
         */
        public static Builder headWithSkin(GameVersion version, Skin skin, boolean includeSignature) {

            Builder out;
            if(version.hasFeature(GameVersion.Feature.NAMESPACED_IDS)) {
                out = new Builder(version, new Identifier("minecraft", "player_head"));
            } else {
                out = new Builder(version, new Identifier("minecraft", "skull")).withDataValue((byte) 3);
            }

            ConfigObject uuid;
            if(version.hasFeature(GameVersion.Feature.INT_ARRAY_UUIDS)) {
                int[] parts = ItemUtil.splitUUID(skin.getUUID());
                uuid = new ConfigList().append(parts[0]).append(parts[1]).append(parts[2]).append(parts[3]);
            } else {
                uuid = new ConfigPrimitive(skin.getUUID().toString());
            }

            if(version.hasFeature(GameVersion.Feature.ITEM_COMPONENTS)) {

                out.withComponent(PROFILE_COMPONENT, new ConfigSection()
                        .with("id", uuid)
                        .with("properties", new ConfigList().append(new ConfigSection()
                                .with("name", "textures")
                                .with("value", skin.getValue())
                                .with("signature", includeSignature ? skin.getSignature() : null)
                        )));

            } else {
                out.getCustomData().set("SkullOwner", new ConfigSection()
                        .with("Id", uuid)
                        .with("Properties", new ConfigSection()
                                .with("textures", new ConfigList()
                                        .append(new ConfigSection()
                                                .with("Value", skin.getValue())))));
            }

            return out;
        }

        /**
         * Constructs an ItemStack from the data in this builder
         * @return A new ItemStack
         */
        public ItemStack build() {

            if(version.hasFeature(GameVersion.Feature.ITEM_COMPONENTS)) {
                return FACTORY.get().buildStructured(id, count, components, version);
            } else {

                ConfigObject obj = components.get(CUSTOM_DATA_COMPONENT);
                ConfigSection customData = obj == null || !obj.isSection() ? null : obj.asSection();

                if(version.hasFeature(GameVersion.Feature.NAMESPACED_IDS)) {
                    return FACTORY.get().buildTagged(id, count, customData, version);
                } else {
                    return FACTORY.get().buildLegacy(id, count, dataValue, customData, version);
                }
            }
        }


        public static final Serializer<Builder> SERIALIZER = ObjectSerializer.<Builder>builder()
                .withEntry(Identifier.serializer("minecraft").<Builder>entry(
                                "id",
                                (b) -> b.id)
                        .acceptKey("type")
                        .optional())
                .withEntry(NumberSerializer.forInt(1,99).<Builder>entry(
                                "count",
                                (b) -> b.count)
                        .acceptKey("Count")
                        .orElse(1))
                .withEntry(NumberSerializer.forByte((byte) 0,(byte) 15).<Builder>entry(
                                "data",
                                (b, ctx) -> {
                                    GameVersion ver = GameVersion.getVersion(ctx);
                                    return ver.hasFeature(GameVersion.Feature.NAMESPACED_IDS) ? null : b.dataValue;
                                })
                        .acceptKey("Damage")
                        .optional())
                .withEntry(ConfigSection.SERIALIZER.<Builder>entry(
                                "tag",
                                (b, ctx) -> {
                                    GameVersion ver = GameVersion.getVersion(ctx);
                                    return ver.hasFeature(GameVersion.Feature.ITEM_COMPONENTS) ? null : b.getCustomData();
                                })
                        .optional())
                .withEntry(ComponentPatchSet.SERIALIZER.<Builder>entry(
                                "components",
                                (b, ctx) -> {
                                    GameVersion ver = GameVersion.getVersion(ctx);
                                    return ver.hasFeature(GameVersion.Feature.ITEM_COMPONENTS) ? b.components : null;
                                })
                        .optional())
                .build((es, ctx) -> {
                    GameVersion ver = GameVersion.getVersion(ctx);
                    return SerializeResult.success(new ItemStack.Builder(ver, es.get(0))
                            .withCount(es.get(1))
                            .withDataValue(es.get(2))
                            .withCustomData(es.get(3))
                            .withComponents(es.get(4)));
                });
    }

}
