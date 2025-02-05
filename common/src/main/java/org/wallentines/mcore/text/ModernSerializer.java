package org.wallentines.mcore.text;

import org.wallentines.mcore.GameVersion;
import org.wallentines.mdcfg.serializer.*;
import org.wallentines.midnightlib.math.Color;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A {@link Serializer} which serializes components into maps in the modern component format
 */
public class ModernSerializer implements Serializer<Component> {


    public static final ModernSerializer INSTANCE = new ModernSerializer();


    @Override
    public <O> SerializeResult<O> serialize(SerializeContext<O> context, Component value) {

        GameVersion version = GameVersion.getVersion(context);

        SerializeResult<O> content = serializeContent(context, value.content);
        if(!content.isComplete()) {
            return content;
        }

        O out = content.getOrThrow();

        String color = value.reset != null && value.reset ? "reset" : value.color == null ? null : TextColor.serialize(value.color, version);
        if(color != null) context.set("color", context.toString(color), out);

        if(value.bold != null) context.set("bold", context.toBoolean(value.bold), out);
        if(value.italic != null) context.set("italic", context.toBoolean(value.italic), out);
        if(value.underlined != null) context.set("underlined", context.toBoolean(value.underlined), out);
        if(value.strikethrough != null) context.set("strikethrough", context.toBoolean(value.strikethrough), out);
        if(value.obfuscated != null) context.set("obfuscated", context.toBoolean(value.obfuscated), out);

        if(value.font != null) context.set("font", context.toString(value.font.toString()), out);
        if(value.insertion != null) context.set("insertion", context.toString(value.insertion), out);

        if (value.shadowColor != null && version.hasFeature(GameVersion.Feature.COMPONENT_SHADOW_COLOR)) {
            context.set("shadow_color", context.toNumber(value.shadowColor.toDecimal()), out);
        }

        if(value.hoverEvent != null) {
            SerializeResult<O> hoverEvent = HoverEvent.SERIALIZER.serialize(context, value.hoverEvent);
            if(!hoverEvent.isComplete()) {
                return SerializeResult.failure("Unable to serialize hover event for component!" + hoverEvent.getError());
            }
            context.set(HoverEvent.getKey(version), hoverEvent.getOrThrow(), out);
        }

        if(value.clickEvent != null) {
            SerializeResult<O> clickEvent = ClickEvent.SERIALIZER.serialize(context, value.clickEvent);
            if(!clickEvent.isComplete()) {
                return SerializeResult.failure("Unable to serialize click event for component!" + clickEvent.getError());
            }
            context.set(ClickEvent.getKey(version), clickEvent.getOrThrow(), out);
        }


        // Children
        if(!value.children.isEmpty()) {

            List<O> children = new ArrayList<>();
            for(Component cmp : value.children) {
                SerializeResult<O> result = serialize(context, cmp);
                if(!result.isComplete()) {
                    return SerializeResult.failure("Unable to serialize children for component!" + result.getError());
                }
                children.add(result.getOrThrow());
            }

            context.set("extra", context.toList(children), out);

        }

        return SerializeResult.success(out);
    }

    private <O> SerializeResult<O> serializeContent(SerializeContext<O> context, Content content) {
        return content.serialize(context);
    }

    @Override
    public <O> SerializeResult<Component> deserialize(SerializeContext<O> context, O value) {

        if(context.isString(value)) {
            return SerializeResult.success(Component.text(context.asString(value).getOrThrow()));
        }
        if(context.isBoolean(value)) {
            return SerializeResult.success(Component.text(String.valueOf(context.asBoolean(value))));
        }
        if(context.isNumber(value)) {
            return SerializeResult.success(Component.text(String.valueOf(context.asNumber(value))));
        }
        if(context.isList(value)) {
            return deserializeList(context, context.asList(value).getOrThrow());
        }
        if(context.isMap(value)) {
            return deserializeMap(context, value);
        }

        return SerializeResult.failure("Unable to turn " + value + " into a component!");
    }


    private <O> SerializeResult<Component> deserializeList(SerializeContext<O> context, Collection<O> value) {
        List<Component> values = new ArrayList<>();

        if(value.isEmpty()) {
            return SerializeResult.success(Component.text(""));
        }

        for(O o : value) {

            SerializeResult<Component> res = deserialize(context, o);
            if(!res.isComplete()) return res;

            values.add(res.getOrThrow());
        }

        return SerializeResult.success(values.get(0).addChildren(values.subList(1, values.size())));
    }

    private <O> SerializeResult<Component> deserializeMap(SerializeContext<O> context, O value) {

        MutableComponent out = null;
        for(String s : context.getOrderedKeys(value)) {

            Content.Type<?> type = Content.Type.byId(s);
            if(type != null) {

                SerializeResult<? extends Content> con = type.serializer.deserialize(context, value);
                if(!con.isComplete()) {
                    return SerializeResult.failure("Unable to deserialize component contents! " + con.getError());
                }

                out = new MutableComponent(con.getOrThrow());
                break;
            }
        }

        if(out == null) {
            return SerializeResult.failure("Unable to deserialize component contents! Could not find valid content type!");
        }

        String color = Serializer.STRING.deserialize(context, context.get("color", value)).get().orElse(null);
        if(color != null) {
            if(color.equals("reset")) {
                out.reset = true;
            } else {
                out.color = TextColor.parse(color);
            }
        }

        GameVersion ver = GameVersion.getVersion(context);

        out.bold = Serializer.BOOLEAN.deserialize(context, context.get("bold", value)).get().orElse(null);
        out.italic = Serializer.BOOLEAN.deserialize(context, context.get("italic", value)).get().orElse(null);
        out.underlined = Serializer.BOOLEAN.deserialize(context, context.get("underlined", value)).get().orElse(null);
        out.strikethrough = Serializer.BOOLEAN.deserialize(context, context.get("strikethrough", value)).get().orElse(null);
        out.obfuscated = Serializer.BOOLEAN.deserialize(context, context.get("obfuscated", value)).get().orElse(null);
        out.font = Identifier.serializer("minecraft").deserialize(context, context.get("font", value)).get().orElse(null);
        out.insertion = Serializer.STRING.deserialize(context, context.get("insertion", value)).get().orElse(null);
        out.hoverEvent = HoverEvent.SERIALIZER.deserialize(context, context.get(HoverEvent.getKey(ver), value)).get().orElse(null);
        out.clickEvent = ClickEvent.SERIALIZER.deserialize(context, context.get(ClickEvent.getKey(ver), value)).get().orElse(null);
        out.shadowColor = Serializer.INT.deserialize(context, context.get("shadow_color", value)).get().map(rgb -> {
            int alpha = (rgb >> 24) & 0xFF;
            int red   = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue  = (rgb) & 0xFF;
            return new Color.RGBA(red, green, blue, alpha);
        }).orElse(null);

        O extra = context.get("extra", value);
        if(context.isList(extra)) {

            Collection<O> values = context.asList(extra).getOrThrow();
            for(O o : values) {
                SerializeResult<Component> child = deserialize(context, o);
                if(!child.isComplete()) {
                    return SerializeResult.failure("Unable to deserialize component extra! " + child.getError());
                }
                out.addChild(child.getOrThrow());
            }
        }

        return SerializeResult.success(out.toComponent());
    }
}
