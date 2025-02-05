package org.wallentines.mcore.util;

import com.mojang.datafixers.util.Either;
import net.minecraft.commands.arguments.selector.SelectorPattern;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.wallentines.mcore.ConfiguringPlayer;
import org.wallentines.mcore.Player;
import org.wallentines.mcore.Server;
import org.wallentines.mcore.text.*;
import org.wallentines.midnightlib.math.Color;
import org.wallentines.midnightlib.registry.Identifier;

import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

public class ConversionUtil {

    /**
     * Converts a Minecraft {@link net.minecraft.resources.ResourceLocation ResourceLocation} to an {@link org.wallentines.midnightlib.registry.Identifier Identifier}
     * @param location The ResourceLocation to convert
     * @return A new Identifier
     */
    public static Identifier toIdentifier(ResourceLocation location) {

        return new Identifier(location.getNamespace(), location.getPath());
    }

    /**
     * Converts an Identifier to a Minecraft ResourceLocation
     * @param location The Identifier to convert
     * @return A new ResourceLocation
     */
    public static ResourceLocation toResourceLocation(Identifier location) {

        return ResourceLocation.tryBuild(location.getNamespace(), location.getPath());
    }

    /**
     * Converts a MidnightCore {@link org.wallentines.mcore.text.HoverEvent HoverEvent} to a Minecraft {@link net.minecraft.network.chat.HoverEvent HoverEvent}
     * @param event The HoverEvent to convert
     * @return A new Minecraft HoverEvent
     */
    @SuppressWarnings("unchecked")
    public static net.minecraft.network.chat.HoverEvent toMCHoverEvent(HoverEvent event) {

        if(event.action() == HoverEvent.Action.SHOW_TEXT) {
            net.minecraft.network.chat.Component out = new WrappedComponent(((HoverEvent.Simple<Component>) event).value());
            return new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, out);
        }

        if(event.action() == HoverEvent.Action.SHOW_ITEM) {

            ItemStack is = ConversionUtil.validate(((HoverEvent.Simple<org.wallentines.mcore.ItemStack>) event).value());
            return new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_ITEM, new net.minecraft.network.chat.HoverEvent.ItemStackInfo(is));
        }

        if(event.action() == HoverEvent.Action.SHOW_ENTITY) {
            HoverEvent.EntityInfo info = ((HoverEvent.Simple<HoverEvent.EntityInfo>) event).value();
            net.minecraft.network.chat.HoverEvent.EntityTooltipInfo out = new net.minecraft.network.chat.HoverEvent.EntityTooltipInfo(
                    BuiltInRegistries.ENTITY_TYPE.get(toResourceLocation(info.type)).get().value(),
                    info.uuid,
                    Optional.ofNullable(info.name).map(WrappedComponent::new)
            );
            return new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_ENTITY, out);
        }

        throw new IllegalArgumentException("Don't know how to convert HoverEvent of type " + event.action() + " to a Minecraft Hover event!");
    }

    /**
     * Converts a Minecraft {@link net.minecraft.network.chat.HoverEvent HoverEvent} to a MidnightCore {@link org.wallentines.mcore.text.HoverEvent HoverEvent}
     * @param event The HoverEvent to convert
     * @return A new MidnightCore HoverEvent
     */
    public static HoverEvent toHoverEvent(net.minecraft.network.chat.HoverEvent event) {

        net.minecraft.network.chat.HoverEvent.Action<?> act = event.getAction();
        if(act == net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT) {
            net.minecraft.network.chat.Component txt = (net.minecraft.network.chat.Component) event.getValue(act);
            return HoverEvent.create(toComponent(txt));
        }

        if(act == net.minecraft.network.chat.HoverEvent.Action.SHOW_ITEM) {
            net.minecraft.network.chat.HoverEvent.ItemStackInfo is = (net.minecraft.network.chat.HoverEvent.ItemStackInfo) event.getValue(act);
            return HoverEvent.forItem(is.getItemStack());
        }

        if(act == net.minecraft.network.chat.HoverEvent.Action.SHOW_ENTITY) {
            net.minecraft.network.chat.HoverEvent.EntityTooltipInfo ent = (net.minecraft.network.chat.HoverEvent.EntityTooltipInfo) event.getValue(act);
            HoverEvent.EntityInfo out = new HoverEvent.EntityInfo(
                    ent.name.map(ConversionUtil::toComponent).orElse(null),
                    toIdentifier(BuiltInRegistries.ENTITY_TYPE.getKey(ent.type)),
                    ent.id
            );
            return HoverEvent.forEntity(out);
        }

        throw new IllegalStateException("Don't know how to turn Minecraft Hover event of type " + event.getAction().getSerializedName() + " to a MidnightCore hover event!");
    }

    /**
     * Converts a MidnightCore {@link org.wallentines.mcore.text.ClickEvent ClickEvent} to a Minecraft {@link net.minecraft.network.chat.ClickEvent ClickEvent}
     * @param event The ClickEvent to convert
     * @return A new Minecraft ClickEvent
     */
    @SuppressWarnings("unchecked")
    public static net.minecraft.network.chat.ClickEvent toMCClickEvent(ClickEvent event) {

        if(event.action() == ClickEvent.Action.OPEN_URL) {
            return new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.OPEN_URL, ((ClickEvent.Simple<URI>) event).value().toString());
        }
        if(event.action() == ClickEvent.Action.OPEN_FILE) {
            return new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.OPEN_FILE, ((ClickEvent.Simple<String>) event).value());
        }
        if(event.action() == ClickEvent.Action.CHANGE_PAGE) {
            return new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.CHANGE_PAGE, ((ClickEvent.Simple<Integer>) event).value().toString());
        }
        if(event.action() == ClickEvent.Action.COPY_TO_CLIPBOARD) {
            return new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, ((ClickEvent.Simple<String>) event).value());
        }
        if(event.action() == ClickEvent.Action.RUN_COMMAND) {
            return new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, ((ClickEvent.Simple<String>) event).value());
        }
        if(event.action() == ClickEvent.Action.SUGGEST_COMMAND) {
            return new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, ((ClickEvent.Simple<String>) event).value());
        }

        throw new IllegalArgumentException("Don't know how to convert ClickEvent of type " + event.action() + " to a Minecraft Click event!");
    }

    /**
     * Converts a Minecraft {@link net.minecraft.network.chat.ClickEvent ClickEvent} to a MidnightCore {@link org.wallentines.mcore.text.ClickEvent ClickEvent}
     * @param event The ClickEvent to convert
     * @return A new MidnightCore ClickEvent
     */
    public static ClickEvent toClickEvent(net.minecraft.network.chat.ClickEvent event) {

        switch (event.getAction()) {
            case OPEN_URL: {
                return ClickEvent.create(ClickEvent.Action.OPEN_URL, URI.create(event.getValue()));
            }
            case OPEN_FILE: {
                return ClickEvent.create(ClickEvent.Action.OPEN_FILE, event.getValue());
            }
            case CHANGE_PAGE: {
                return ClickEvent.create(ClickEvent.Action.CHANGE_PAGE, Integer.parseInt(event.getValue()));
            }
            case COPY_TO_CLIPBOARD: {
                return ClickEvent.create(ClickEvent.Action.COPY_TO_CLIPBOARD, event.getValue());
            }
            case RUN_COMMAND: {
                return ClickEvent.create(ClickEvent.Action.RUN_COMMAND, event.getValue());
            }
            case SUGGEST_COMMAND: {
                return ClickEvent.create(ClickEvent.Action.SUGGEST_COMMAND, event.getValue());
            }
        }

        throw new IllegalStateException("Don't know how to turn Minecraft click event of type " + event.getAction().getSerializedName() + " to a MidnightCore click event!");
    }

    public static Component toComponent(net.minecraft.network.chat.Component other) {

        Content contents = toContent(other.getContents());
        Component out = new Component(contents);

        Style style = other.getStyle();
        if(!style.isEmpty()) {

            if(style.isBold()) out = out.withBold(true);
            if(style.isItalic()) out = out.withItalic(true);
            if(style.isUnderlined()) out = out.withUnderlined(true);
            if(style.isStrikethrough()) out = out.withStrikethrough(true);
            if(style.isObfuscated()) out = out.withObfuscated(true);

            if(style.getFont() != Style.DEFAULT_FONT) out = out.withFont(ConversionUtil.toIdentifier(style.getFont()));

            out = out.withInsertion(style.getInsertion());
            if(style.getColor() != null) out = out.withColor(ConversionUtil.toColor(style.getColor()));
            if(style.getHoverEvent() != null) out = out.withHoverEvent(ConversionUtil.toHoverEvent(style.getHoverEvent()));
            if(style.getClickEvent() != null) out = out.withClickEvent(ConversionUtil.toClickEvent(style.getClickEvent()));
        }

        for(net.minecraft.network.chat.Component child : other.getSiblings()) {
            out = out.addChild(toComponent(child));
        }

        return out;
    }

    public static ComponentContents toContents(Content content) {

        switch (content.getType().getId()) {
            case "text":
                return new PlainTextContents.LiteralContents(((Content.Text) content).text);
            case "translate": {
                Content.Translate md = (Content.Translate) content;
                return new TranslatableContents(
                        md.key,
                        md.fallback,
                        md.with == null ? null : md.with.stream().map(WrappedComponent::new).toArray());
            }
            case "keybind":
                return new KeybindContents(((Content.Keybind) content).key);
            case "score": {
                Content.Score md = (Content.Score) content;
                return new ScoreContents(Either.right(md.name), md.objective);
            }
            case "selector": {
                Content.Selector md = (Content.Selector) content;
                return new SelectorContents(
                        SelectorPattern.parse(md.value).getOrThrow(),
                        Optional.ofNullable(md.separator == null ? null : new WrappedComponent(md.separator)));
            }
            case "nbt": {
                Content.NBT md = (Content.NBT) content;
                DataSource source = switch (md.type) {
                    case BLOCK -> new BlockDataSource(md.data);
                    case ENTITY -> new EntityDataSource(md.data);
                    default -> new StorageDataSource(ResourceLocation.parse(md.data));
                };
                return new NbtContents(
                        md.path,
                        md.interpret,
                        Optional.ofNullable(md.separator == null ? null : new WrappedComponent(md.separator)),
                        source
                );
            }
            default:
                throw new IllegalArgumentException("Don't know how to turn " + content + " into a Minecraft content!");
        }
    }

    public static Content toContent(ComponentContents contents) {

        if(contents instanceof PlainTextContents pt) {
            return new Content.Text(pt.text());
        }
        else if(contents instanceof TranslatableContents mc) {
            return new Content.Translate(
                    mc.getKey(),
                    mc.getFallback(),
                    mc.getArgs().length == 0 ? null : Stream.of(mc.getArgs()).map(obj -> toComponent((net.minecraft.network.chat.Component) obj)).toList());
        }
        else if(contents instanceof KeybindContents mc) {
            return new Content.Keybind(mc.getName());
        }
        else if(contents instanceof ScoreContents(Either<SelectorPattern, String> name, String objective)) {
            return new Content.Score(name.right().orElse(name.left().orElseThrow().pattern()), objective, null);
        }
        else if(contents instanceof SelectorContents(SelectorPattern selector, Optional<net.minecraft.network.chat.Component> separator)) {
            return new Content.Selector(
                    selector.pattern(),
                    separator.map(ConversionUtil::toComponent).orElse(null));
        }
        else if(contents instanceof NbtContents mc) {
            String pattern;
            Content.NBT.DataSourceType type;
            if(mc.getDataSource() instanceof BlockDataSource) {
                pattern = ((BlockDataSource) mc.getDataSource()).posPattern();
                type = Content.NBT.DataSourceType.BLOCK;
            }
            else if(mc.getDataSource() instanceof EntityDataSource) {
                pattern = ((EntityDataSource) mc.getDataSource()).selectorPattern();
                type = Content.NBT.DataSourceType.ENTITY;
            }
            else {
                pattern = ((StorageDataSource) mc.getDataSource()).id().toString();
                type = Content.NBT.DataSourceType.STORAGE;
            }
            return new Content.NBT(mc.getNbtPath(), mc.isInterpreting(), mc.getSeparator().map(ConversionUtil::toComponent).orElse(null), type, pattern);
        }

        throw new IllegalArgumentException("Don't know how to convert " + contents + " into a MidnightCore content!");
    }


    public static EquipmentSlot toMCEquipmentSlot(org.wallentines.mcore.EquipmentSlot slot) {
        return switch(slot) {
            case MAINHAND -> net.minecraft.world.entity.EquipmentSlot.MAINHAND;
            case OFFHAND -> net.minecraft.world.entity.EquipmentSlot.OFFHAND;
            case FEET -> net.minecraft.world.entity.EquipmentSlot.FEET;
            case LEGS -> net.minecraft.world.entity.EquipmentSlot.LEGS;
            case CHEST -> net.minecraft.world.entity.EquipmentSlot.CHEST;
            case HEAD -> net.minecraft.world.entity.EquipmentSlot.HEAD;
        };
    }

    /**
     * Converts an RGB {@link org.wallentines.midnightlib.math.Color Color} to a Minecraft {@link net.minecraft.network.chat.TextColor TextColor}
     * @param color The Color to convert
     * @return A new TextColor
     */
    public static TextColor toTextColor(Color color) {

        return TextColor.fromRgb(color.toDecimal());
    }

    /**
     * Converts a Minecraft {@link net.minecraft.network.chat.TextColor TextColor} to an RGB {@link org.wallentines.midnightlib.math.Color Color}
     * @param color The TextColor to convert
     * @return A new Color
     */
    public static Color toColor(TextColor color) {
        return new Color(color.getValue());
    }

    /**
     * Creates a Minecraft Style from a MidnightCore component
     * @param component The component to read
     * @return A new Style
     */
    public static Style getStyle(Component component) {

        Style out = Style.EMPTY
                .withBold(component.bold)
                .withItalic(component.italic)
                .withUnderlined(component.underlined)
                .withStrikethrough(component.strikethrough)
                .withObfuscated(component.obfuscated)
                .withInsertion(component.insertion)
                .withFont(component.font == null ? null : ConversionUtil.toResourceLocation(component.font))
                .withHoverEvent(component.hoverEvent == null ? null : ConversionUtil.toMCHoverEvent(component.hoverEvent))
                .withClickEvent(component.clickEvent == null ? null : ConversionUtil.toMCClickEvent(component.clickEvent))
                .withColor(component.color == null ? null : ConversionUtil.toTextColor(component.color));

        if(component.shadowColor != null) {
            out = out.withShadowColor(component.shadowColor.toDecimal());
        }

        return out;
    }

    /**
     * Validates that the given player is actually a Minecraft ServerPlayer
     * @param player The player to check
     * @return The player casted to a ServerPlayer
     */
    public static ServerPlayer validate(Player player) {

        if(!(player instanceof ServerPlayer spl)) {
            throw new IllegalArgumentException("Attempt to access non-Minecraft Player!");
        }
        return spl;
    }


    /**
     * Validates that the given configuring player is actually a Minecraft ServerConfigurationPacketListenerImpl
     * @param player The configuring player to check
     * @return The player casted to a ServerConfigurationPacketListenerImpl
     */
    public static ServerConfigurationPacketListenerImpl validate(ConfiguringPlayer player) {

        if(!(player instanceof ServerConfigurationPacketListenerImpl cpl)) {
            throw new IllegalArgumentException("Attempt to access non-Minecraft Player!");
        }
        return cpl;
    }

    /**
     * Validates that the given ItemStack is actually a Minecraft ItemStack
     * @param is The ItemStack to check
     * @return The ItemStack casted to a Minecraft ItemStack
     */
    public static ItemStack validate(org.wallentines.mcore.ItemStack is) {

        if(!((Object) is instanceof ItemStack mis)) {
            throw new IllegalArgumentException("Attempt to access non-Minecraft ItemStack!");
        }
        return mis;
    }

    /**
     * Validates that the given Server is actually a Minecraft Server
     * @param srv The Server to check
     * @return The server casted to a Minecraft Server
     */
    public static MinecraftServer validate(Server srv) {

        if(!(srv instanceof MinecraftServer msv)) {
            throw new IllegalArgumentException("Attempt to access non-Minecraft Server!");
        }
        return msv;
    }

}
