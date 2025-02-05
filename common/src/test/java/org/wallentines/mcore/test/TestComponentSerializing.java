package org.wallentines.mcore.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wallentines.mcore.text.*;
import org.wallentines.mdcfg.ConfigList;
import org.wallentines.mdcfg.ConfigPrimitive;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.midnightlib.math.Color;

import java.util.Arrays;

public class TestComponentSerializing {

    @Test
    public void testParsing() {

        String unparsedLegacy = "&6Hello, &dWorld";
        Component comp = Component.parse(unparsedLegacy);

        Assertions.assertEquals(TextColor.GOLD, comp.color);
        Assertions.assertEquals(1, comp.children.size());
        Assertions.assertInstanceOf(Content.Text.class, comp.content);

        Assertions.assertEquals(TextColor.LIGHT_PURPLE, comp.children.get(0).color);
        Assertions.assertEquals(0, comp.children.get(0).children.size());
        Assertions.assertInstanceOf(Content.Text.class, comp.children.get(0).content);

    }

    @Test
    public void testLegacySerializer() {

        ConfigPrimitive unparsed = new ConfigPrimitive("\u00A7aTest \u00A7bParsing");
        SerializeResult<Component> parsed = LegacySerializer.INSTANCE.deserialize(ConfigContext.INSTANCE, unparsed);

        Assertions.assertTrue(parsed.isComplete());

        Component comp = parsed.getOrThrow();
        Assertions.assertEquals(TextColor.GREEN, comp.color);
        Assertions.assertEquals(1, comp.children.size());
        Assertions.assertInstanceOf(Content.Text.class, comp.content);

        Assertions.assertEquals(TextColor.AQUA, comp.children.get(0).color);
        Assertions.assertEquals(0, comp.children.get(0).children.size());
        Assertions.assertInstanceOf(Content.Text.class, comp.children.get(0).content);

        String serialized = comp.toLegacyText();
        String serialized2 = LegacySerializer.INSTANCE.serialize(ConfigContext.INSTANCE, comp).getOrThrow().asString();

        Assertions.assertEquals(unparsed.asString(), serialized);
        Assertions.assertEquals(unparsed.asString(), serialized2);

    }


    @Test
    public void testModernSerializer() {

        Serializer<Component> ser = ModernSerializer.INSTANCE;
        testModern(ser);

        Common.VERSION.setProtocolVersion(770);

        // Complex
        Component cmp = Component.text("Hello")
                .withColor(TextColor.GREEN)
                .withBold(true)
                .addChild(Component.translate("item.minecraft.diamond_sword")
                        .withHoverEvent(HoverEvent.create(Component.text("Test"))));

        ConfigSection serialized = ser.serialize(ConfigContext.INSTANCE, cmp).getOrThrow().asSection();

        Component comp = ser.deserialize(ConfigContext.INSTANCE, serialized).getOrThrow();

        Assertions.assertEquals(cmp, comp);

    }

    private void testModern(Serializer<Component> serializer) {

        // String
        ConfigPrimitive unparsedString = new ConfigPrimitive("Hello");
        SerializeResult<Component> parsedString = serializer.deserialize(ConfigContext.INSTANCE, unparsedString);

        Assertions.assertTrue(parsedString.isComplete());

        Component comp = parsedString.getOrThrow();
        Assertions.assertNull(comp.color);
        Assertions.assertInstanceOf(Content.Text.class, comp.content);
        Assertions.assertEquals(0, comp.children.size());


        // Number
        ConfigPrimitive unparsedNumber = new ConfigPrimitive(33.5);
        SerializeResult<Component> parsedNumber = serializer.deserialize(ConfigContext.INSTANCE, unparsedNumber);

        Assertions.assertTrue(parsedNumber.isComplete());

        comp = parsedNumber.getOrThrow();
        Assertions.assertNull(comp.color);
        Assertions.assertInstanceOf(Content.Text.class, comp.content);
        Assertions.assertEquals(0, comp.children.size());


        // Bool
        ConfigPrimitive unparsedBool = new ConfigPrimitive(true);
        SerializeResult<Component> parsedBool = serializer.deserialize(ConfigContext.INSTANCE, unparsedBool);

        Assertions.assertTrue(parsedBool.isComplete());

        comp = parsedBool.getOrThrow();
        Assertions.assertNull(comp.color);
        Assertions.assertInstanceOf(Content.Text.class, comp.content);
        Assertions.assertEquals(0, comp.children.size());


        // Object
        ConfigSection unparsedObject = new ConfigSection().with("text", "Hello").with("color", "green");
        SerializeResult<Component> parsedObject = serializer.deserialize(ConfigContext.INSTANCE, unparsedObject);

        Assertions.assertTrue(parsedObject.isComplete());

        comp = parsedObject.getOrThrow();
        Assertions.assertEquals(TextColor.GREEN, comp.color);
        Assertions.assertInstanceOf(Content.Text.class, comp.content);
        Assertions.assertEquals(0, comp.children.size());


        // List
        ConfigList unparsedList = new ConfigList().append(unparsedObject).append(unparsedObject);
        SerializeResult<Component> parsedList = serializer.deserialize(ConfigContext.INSTANCE, unparsedList);

        Assertions.assertTrue(parsedList.isComplete());

        comp = parsedList.getOrThrow();
        Assertions.assertEquals(TextColor.GREEN, comp.color);
        Assertions.assertInstanceOf(Content.Text.class, comp.content);
        Assertions.assertEquals(1, comp.children.size());

        Assertions.assertEquals(TextColor.GREEN, comp.children.get(0).color);
        Assertions.assertEquals(0, comp.children.get(0).children.size());
        Assertions.assertInstanceOf(Content.Text.class, comp.children.get(0).content);

    }

    @Test
    public void testHoverEvent() {

        String encoded = "{\"text\":\"Hello\",\"hover_event\":{\"action\":\"show_text\",\"value\":{\"text\":\"Test\"}}}";
        Component cmp = ModernSerializer.INSTANCE.deserialize(ConfigContext.INSTANCE, JSONCodec.loadConfig(encoded)).getOrThrow();

        Assertions.assertInstanceOf(Content.Text.class, cmp.content);
        Assertions.assertEquals("Hello", cmp.text());
        Assertions.assertNotNull(cmp.hoverEvent);
        Assertions.assertEquals(HoverEvent.create(Component.text("Test")), cmp.hoverEvent);

        String reencoded = cmp.toJSONString();
        Assertions.assertEquals(encoded, reencoded);

    }


    @Test
    public void testConfigSerializer() {

        ConfigPrimitive unparsed = new ConfigPrimitive("&aHello, &#354a56World");
        SerializeResult<Component> parsed = ConfigSerializer.INSTANCE.deserialize(ConfigContext.INSTANCE, unparsed);

        Assertions.assertTrue(parsed.isComplete());

        Component comp = parsed.getOrThrow();
        Assertions.assertEquals(TextColor.GREEN, comp.color);
        Assertions.assertInstanceOf(Content.Text.class, comp.content);
        Assertions.assertEquals(1, comp.children.size());

        Assertions.assertEquals(new Color(0x354a56), comp.children.get(0).color);
        Assertions.assertEquals(0, comp.children.get(0).children.size());
        Assertions.assertInstanceOf(Content.Text.class, comp.children.get(0).content);


        testModern(ConfigSerializer.INSTANCE);


        unparsed = new ConfigPrimitive("&aHello, #354a56World");
        parsed = ConfigSerializer.INSTANCE.deserialize(ConfigContext.INSTANCE, unparsed);

        Assertions.assertTrue(parsed.isComplete());

        comp = parsed.getOrThrow();
        Assertions.assertEquals(TextColor.GREEN, comp.color);
        Assertions.assertEquals("Hello, #354a56World", comp.text());
        Assertions.assertInstanceOf(Content.Text.class, comp.content);
        Assertions.assertEquals(0, comp.children.size());


        unparsed = new ConfigPrimitive("&aHello, &&6World");
        parsed = ConfigSerializer.INSTANCE.deserialize(ConfigContext.INSTANCE, unparsed);

        Assertions.assertTrue(parsed.isComplete());

        comp = parsed.getOrThrow();
        Assertions.assertEquals(TextColor.GREEN, comp.color);
        Assertions.assertEquals("Hello, &6World", comp.text());
        Assertions.assertInstanceOf(Content.Text.class, comp.content);
        Assertions.assertEquals(0, comp.children.size());


        testConfigString("Hello", Component.text("Hello"));

        testConfigString("&lHello", Component.text("Hello").withBold(true));
        testConfigString("&oHello", Component.text("Hello").withItalic(true));
        testConfigString("&nHello", Component.text("Hello").withUnderlined(true));
        testConfigString("&mHello", Component.text("Hello").withStrikethrough(true));
        testConfigString("&kHello", Component.text("Hello").withObfuscated(true));

        testConfigString("&6Hello", Component.text("Hello").withColor(Color.fromRGBI(6)));
        testConfigString("&6&lHello", Component.text("Hello").withColor(Color.fromRGBI(6)).withBold(true));
        testConfigString("&6&l&oHello", Component.text("Hello").withColor(Color.fromRGBI(6)).withBold(true).withItalic(true));
        testConfigString("&6&l&o&nHello", Component.text("Hello").withColor(Color.fromRGBI(6)).withBold(true).withItalic(true).withUnderlined(true));
        testConfigString("&6&l&o&n&mHello", Component.text("Hello").withColor(Color.fromRGBI(6)).withBold(true).withItalic(true).withUnderlined(true).withStrikethrough(true));
        testConfigString("&6&l&o&n&m&kHello", Component.text("Hello").withColor(Color.fromRGBI(6)).withBold(true).withItalic(true).withUnderlined(true).withStrikethrough(true).withObfuscated(true));

        testConfigString("&6Hello, &eWorld", Component.text("Hello, ").withColor(Color.fromRGBI(6)).addChild(Component.text("World").withColor(Color.fromRGBI(14))));
        testConfigString("&6&lHello, &eWorld", Component.empty().withColor(Color.fromRGBI(6)).addChild(Component.text("Hello, ").withBold(true)).addChild(Component.text("World").withColor(Color.fromRGBI(14))));

        testConfigString("&6&lHello, &oWorld", Component.empty().withColor(Color.fromRGBI(6)).addChild(Component.text("Hello, ").withBold(true)).addChild(Component.text("World").withItalic(true)));

    }


    private void testConfigString(String str, Component expected) {
        Assertions.assertEquals(expected, ConfigSerializer.INSTANCE.deserialize(ConfigContext.INSTANCE, new ConfigPrimitive(str)).getOrThrow());
    }

    @Test
    public void testPlain() {


        ConfigPrimitive unparsed = new ConfigPrimitive("{\"text\":\"Hello, World\",\"color\":\"gold\"}");
        SerializeResult<Component> parsed = PlainSerializer.INSTANCE.deserialize(ConfigContext.INSTANCE, unparsed);

        Assertions.assertTrue(parsed.isComplete());

        Component comp = parsed.getOrThrow();
        Assertions.assertTrue(comp.content instanceof Content.Text);
        Assertions.assertEquals("{\"text\":\"Hello, World\",\"color\":\"gold\"}", ((Content.Text) comp.content).text);

    }

    @Test
    public void testToString() {

        // Set the current protocol version to match Minecraft 1.20.1's
        Common.VERSION.setProtocolVersion(763);

        Component created = Component.text("Hello").withColor(TextColor.RED).withChildren(Arrays.asList(Component.text(", World")));

        String plain = created.allText();
        Assertions.assertEquals("Hello, World", plain);

        String json = created.toJSONString();
        Assertions.assertEquals("{\"text\":\"Hello\",\"color\":\"#ff5555\",\"extra\":[{\"text\":\", World\"}]}", json);

        String cfg = created.toConfigText();
        Assertions.assertEquals("&#ff5555Hello, World", cfg);

        String legacy = created.toLegacyText();
        Assertions.assertEquals("\u00A7cHello, World", legacy);

    }

    @Test
    public void testShadowColor() {

        String unparsed = "&e:fHello, World";
        Component parsed = ConfigSerializer.INSTANCE.deserialize(ConfigContext.INSTANCE, new ConfigPrimitive(unparsed)).getOrThrow();

        Assertions.assertEquals(Component.text("Hello, World").withColor(Color.fromRGBI(14)).withShadowColor(Color.fromRGBI(15)), parsed);


        unparsed = "&e::Hello, World";
        parsed = ConfigSerializer.INSTANCE.deserialize(ConfigContext.INSTANCE, new ConfigPrimitive(unparsed)).getOrThrow();

        Assertions.assertEquals(Component.text(":Hello, World").withColor(Color.fromRGBI(14)), parsed);

    }

}
