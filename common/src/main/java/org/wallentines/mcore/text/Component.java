package org.wallentines.mcore.text;

import org.wallentines.mcore.GameVersion;
import org.wallentines.mdcfg.ConfigPrimitive;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.midnightlib.math.Color;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A data type representing a Minecraft chat component. These components are immutable, meaning any change to them
 * will result in a new component being created.
 */
public class Component {

    /**
     * The color of the component
     */
    public final Color color;

    /**
     * Whether the component is bold
     */
    public final Boolean bold;

    /**
     * Whether the component is italic
     */
    public final Boolean italic;

    /**
     * Whether the component is underlined
     */
    public final Boolean underlined;

    /**
     * Whether the component has strikethrough
     */
    public final Boolean strikethrough;

    /**
     * Whether the component is obfuscated
     */
    public final Boolean obfuscated;

    /**
     * If set to true, the component will have its color reset to the default for the given context.
     */
    public final Boolean reset;

    /**
     * The font of the component
     */
    public final Identifier font;

    /**
     * The text that will be inserted when the player shift-clicks the component
     */
    public final String insertion;

    /**
     * The event which occurs when a player hovers over the component
     */
    public final HoverEvent hoverEvent;

    /**
     * The event which occurs when a player clicks on the component
     */
    public final ClickEvent clickEvent;

    /**
     * (24w44a) The color of the component outline
     */
    public final Color.RGBA shadowColor;

    /**
     * The content which will be displayed to the player
     */
    public final Content content;

    /**
     * The 'extra' components as children of the component
     */
    public final List<Component> children;


    Component(Color color, Boolean bold, Boolean italic, Boolean underlined, Boolean strikethrough,
              Boolean obfuscated, Boolean reset, Identifier font, String insertion, HoverEvent hoverEvent,
              ClickEvent clickEvent, Color.RGBA shadowColor, Content content, Collection<Component> children) {
        this.color = color;
        this.bold = bold;
        this.italic = italic;
        this.underlined = underlined;
        this.strikethrough = strikethrough;
        this.obfuscated = obfuscated;
        this.reset = reset;
        this.font = font;
        this.insertion = insertion;
        this.hoverEvent = hoverEvent;
        this.clickEvent = clickEvent;
        this.shadowColor = shadowColor;
        this.content = content;
        this.children = children == null ? List.of() : List.copyOf(children);
    }

    /**
     * Constructs a component with the given content
     * @param content The content of the new component
     */
    public Component(Content content) {
        this(null, null, null, null, null, null, null, null, null, null, null, null, content, null);
    }

    /**
     * Creates a component with an empty text content
     * @return An empty component
     */
    public static Component empty() { return new Component(new Content.Text("")); }

    /**
     * Creates a component with content of type Text with the given String
     * @param text The text to fill the content with
     * @return A new text component
     */
    public static Component text(String text) {
        return new Component(new Content.Text(text));
    }

    /**
     * Creates a component with content of type Translate with the given key
     * @param text The key to fill the content with
     * @return A new translate component
     */
    public static Component translate(String text) {
        return new Component(new Content.Translate(text));
    }

    /**
     * Creates a component with content of type Keybind with the given key
     * @param text The key to fill the content with
     * @return A new Keybind component
     */
    public static Component keybind(String text) {
        return new Component(new Content.Keybind(text));
    }

    /**
     * Parses a component from a String. The string may be JSON-formatted, or formatted using '&'-based color codes.
     * @param unparsed The string to parse
     * @return A component parsed from the String
     */
    public static Component parse(String unparsed) {
        return ConfigSerializer.INSTANCE.deserialize(ConfigContext.INSTANCE, new ConfigPrimitive(unparsed)).getOrThrow();
    }

    /**
     * The content of the component converted to plaintext
     * @return The content of the component
     */
    public String text() {
        return PlainSerializer.serializeContent(content);
    }

    /**
     * The content of the component converted to plaintext, as well as all child components
     * @return The content of the component and its children
     */
    public String allText() {
        return PlainSerializer.INSTANCE.serialize(ConfigContext.INSTANCE, this).getOrThrow().asString();
    }

    /**
     * Converts the component to a ConfigSection in the component JSON format for the maximum supported game version
     * @return A ConfigSection representing the component
     */
    public ConfigSection toConfigSection() {
        return ModernSerializer.INSTANCE.serialize(GameVersion.context(), this).getOrThrow().asSection();
    }

    /**
     * Converts the component to a ConfigSection in the component JSON format for the given game version
     * @param version The version to serialize the component for
     * @return A ConfigSection representing the component
     */
    public ConfigSection toConfigSection(GameVersion version) {
        return ModernSerializer.INSTANCE.serialize(GameVersion.context(version), this).getOrThrow().asSection();
    }

    /**
     * Converts the component to a JSON-encoded String for the maximum supported game version
     * @return The component in JSON
     */
    public String toJSONString() {
        return JSONCodec.minified().encodeToString(ConfigContext.INSTANCE, toConfigSection());
    }

    /**
     * Converts the component to a JSON-encoded String format for the given game version
     * @param version The version to serialize the component for
     * @return The component in JSON
     */
    public String toJSONString(GameVersion version) {
        return JSONCodec.minified().encodeToString(ConfigContext.INSTANCE, toConfigSection(version));
    }

    /**
     * Converts the component to legacy text with '§'-based color codes for legacy clients
     * @return A component as legacy text
     */
    public String toLegacyText() {
        return LegacySerializer.INSTANCE.serialize(ConfigContext.INSTANCE, this).getOrThrow().asString();
    }

    /**
     * Converts the component to a string optimized for configuration files. The string will be formatted using
     * '&'-based color/formatting codes if possible, or as JSON otherwise
     * @return A component as config text
     */
    public String toConfigText() {
        return ConfigSerializer.INSTANCE.serialize(ConfigContext.INSTANCE, this).getOrThrow().asString();
    }

    /**
     * Determines whether the component has formatting such as bold, italics, etc.
     * @return Whether the component has formatting
     */
    public boolean hasFormatting() {
        return bold != null || italic != null || underlined != null || strikethrough != null || obfuscated != null;
    }

    /**
     * Determines whether the component has components which cannot be converted to legacy/config text.
     * @return Whether the component has components which cannot be converted to legacy/config text.
     */
    public boolean hasNonLegacyComponents() {
        if(font != null || hoverEvent != null || clickEvent != null || insertion != null) return true;
        for(Component comp : children) {
            if(comp.hasNonLegacyComponents()) return true;
        }
        return false;
    }

    /**
     * Creates a copy of this component with a new color.
     * @param color The color to set
     * @return A new component with the given color
     */
    public Component withColor(Color color) {
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, children);
    }

    /**
     * Creates a copy of this component with a new bold state.
     * @param bold The bold state to set
     * @return A new component with the given bold state
     */
    public Component withBold(Boolean bold) {
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, children);
    }

    /**
     * Creates a copy of this component with a new italic state.
     * @param italic The italic state to set
     * @return A new component with the given italic state
     */
    public Component withItalic(Boolean italic) {
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, children);
    }

    /**
     * Creates a copy of this component with a new underline state.
     * @param underline The underline state to set
     * @return A new component with the given underline state
     */
    public Component withUnderlined(Boolean underline) {
        return new Component(color, bold, italic, underline, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, children);
    }

    /**
     * Creates a copy of this component with a new strikethrough state.
     * @param strikethrough The strikethrough state to set
     * @return A new component with the given strikethrough state
     */
    public Component withStrikethrough(Boolean strikethrough) {
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, children);
    }

    /**
     * Creates a copy of this component with a new obfuscated state.
     * @param obfuscated The obfuscated state to set
     * @return A new component with the given obfuscated state
     */
    public Component withObfuscated(Boolean obfuscated) {
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, children);
    }

    /**
     * Creates a copy of this component with a new reset state.
     * @param reset The reset state to set
     * @return A new component with the given reset state
     */
    public Component withReset(Boolean reset) {
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, children);
    }

    /**
     * Creates a copy of this component with a new font.
     * @param font The new font
     * @return A new component with the given font
     */
    public Component withFont(Identifier font) {
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, children);
    }

    /**
     * Creates a copy of this component with a new insertion string.
     * @param insertion The new insertion
     * @return A new component with the given insertion string
     */
    public Component withInsertion(String insertion) {
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, children);
    }

    /**
     * Creates a copy of this component with a new hoverEvent.
     * @param hoverEvent The hoverEvent to set
     * @return A new component with the given hoverEvent
     */
    public Component withHoverEvent(HoverEvent hoverEvent) {
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, children);
    }

    /**
     * Creates a copy of this component with a new clickEvent.
     * @param clickEvent The clickEvent to set
     * @return A new component with the given clickEvent
     */
    public Component withClickEvent(ClickEvent clickEvent) {
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, children);
    }

    /**
     * Creates a copy of this component with a new clickEvent.
     * @param shadowColor The shadowColor to set
     * @return A new component with the given clickEvent
     */
    public Component withShadowColor(Color.RGBA shadowColor) {
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, children);
    }
    /**
     * Creates a copy of this component with a new clickEvent.
     * @param shadowColor The shadowColor to set
     * @return A new component with the given clickEvent
     */
    public Component withShadowColor(Color shadowColor) {
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor.asRGBA(), content, children);
    }

    /**
     * Creates a copy of this component with a new content.
     * @param content The clickEvent to set
     * @return A new component with the given content
     */
    public Component withContent(Content content) {
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, children);
    }

    /**
     * Creates a copy of this component with only the given children.
     * @param children The new children
     * @return A new component with the given children
     */
    public Component withChildren(Collection<Component> children) {
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, children);
    }

    /**
     * Creates a copy of this component with an extra child
     * @param child The child to add
     * @return A new component with the given child
     */
    public Component addChild(Component child) {

        List<Component> out = new ArrayList<>(children);
        out.add(child);
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, out);
    }

    /**
     * Creates a copy of this component with some extra children
     * @param children The children to add
     * @return A new component with the given children
     */
    public Component addChildren(Collection<Component> children) {

        List<Component> out = new ArrayList<>(this.children);
        out.addAll(children);
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, out);
    }

    /**
     * Creates an exact copy of this component
     * @return An exact copy of this component
     */
    public Component copy() {
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, children);
    }

    /**
     * Creates a copy of this component without any children
     * @return A copy of this component without children
     */
    public Component baseCopy() {
        return new Component(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion,
                hoverEvent, clickEvent, shadowColor, content, null);
    }

    public Component fillStyle(Component other) {
        return new Component(
                other.color == null ? color : other.color,
                other.bold == null ? bold : other.bold,
                other.italic == null ? italic : other.italic,
                other.underlined == null ? underlined : other.underlined,
                other.strikethrough == null ? strikethrough : other.strikethrough,
                other.obfuscated == null ? obfuscated : other.obfuscated,
                other.reset == null ? reset : other.reset,
                other.font == null ? font : other.font,
                other.insertion == null ? insertion : other.insertion,
                other.hoverEvent == null ? hoverEvent : other.hoverEvent,
                other.clickEvent == null ? clickEvent : other.clickEvent,
                other.shadowColor == null ? shadowColor : other.shadowColor,
                content,
                children
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Component component = (Component) o;
        boolean out = Objects.equals(color, component.color) &&
                Objects.equals(bold, component.bold) &&
                Objects.equals(italic, component.italic) &&
                Objects.equals(underlined, component.underlined) &&
                Objects.equals(strikethrough, component.strikethrough) &&
                Objects.equals(obfuscated, component.obfuscated) &&
                Objects.equals(reset, component.reset) &&
                Objects.equals(font, component.font) &&
                Objects.equals(insertion, component.insertion) &&
                Objects.equals(hoverEvent, component.hoverEvent) &&
                Objects.equals(clickEvent, component.clickEvent) &&
                Objects.equals(content, component.content) &&
                children.size() == component.children.size();

        if(!out) return false;

        for(int i = 0 ; i < children.size() ; i++) {
            if(!Objects.equals(children.get(i), component.children.get(i))) return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, bold, italic, underlined, strikethrough, obfuscated, reset, font, insertion, hoverEvent, clickEvent, content, children);
    }

    @Override
    public String toString() {
        return toJSONString();
    }
}
