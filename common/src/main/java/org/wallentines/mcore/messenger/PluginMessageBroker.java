package org.wallentines.mcore.messenger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.wallentines.mcore.MidnightCoreAPI;
import org.wallentines.mcore.util.PacketBufferUtil;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.midnightlib.registry.Identifier;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public abstract class PluginMessageBroker {

    /**
     * The ID of the plugin message type used by this messenger
     */
    public static final Identifier MESSAGE_ID = new Identifier(MidnightCoreAPI.MOD_ID, "msg");

    protected static final byte REGISTER = 1;
    protected static final byte UNREGISTER = 2;
    protected static final byte REQUEST = 3;
    protected static final byte ONLINE = 4;


    protected BufCipher key;

    protected final List<PluginMessenger> messengers;

    private boolean isShutdown;

    protected PluginMessageBroker() {
        messengers = new ArrayList<>();
    }

    protected void init(boolean encrypt) {
        if(encrypt) key = readKey(getKeyFile());
    }

    public void send(String channel, String namespace, int ttl, ByteBuf message) {
        send(new PluginMessageBroker.Packet(channel, key != null, namespace, ttl, message));
    }

    protected abstract void send(Packet packet);

    protected void handle(Packet packet) {

        for(PluginMessenger msg : messengers) {
            if(Objects.equals(msg.namespace, packet.namespace)) {
                msg.handle(packet);
            }
        }
    }

    public void register(PluginMessenger messenger) {
        messengers.add(messenger);
    }

    public void shutdown() {
        if(isShutdown) return;
        isShutdown = true;

        onShutdown();
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    protected abstract void onShutdown();

    protected abstract File getKeyFile();

    protected static BufCipher readKey(File file) {
        try(FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int read;
            while((read = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }

            return new BufCipher(new SecretKeySpec(bos.toByteArray(), "AES"));
        } catch (IOException | GeneralSecurityException ex) {
            MidnightCoreAPI.LOGGER.error("Unable to read encryption key!", ex);
            return null;
        }
    }

    /**
     * Flags encoded in message packets
     */
    public enum Flag {
        ENCRYPTED(0b00000001),
        NAMESPACED(0b00000010),
        QUEUE(0b00000100),
        SYSTEM(0b00001000);

        final byte mask;

        Flag(int mask) {
            this.mask = (byte) mask;
        }

        public static EnumSet<Flag> unpack(byte flags) {

            EnumSet<Flag> out = EnumSet.noneOf(Flag.class);
            for(Flag f : values()) {
                if((flags & f.mask) == f.mask) out.add(f);
            }

            return out;
        }

        public static byte pack(EnumSet<Flag> flags) {

            byte out = 0b00000000;
            for(Flag f : flags) {
                out |= f.mask;
            }
            return out;
        }

        public static final Serializer<EnumSet<Flag>> SERIALIZER = Serializer.BYTE.map(Flag::pack, Flag::unpack);

    }

    protected class Packet implements org.wallentines.mcore.pluginmsg.Packet {

        protected final String channel;
        protected final ByteBuf payload;
        protected final String namespace;
        protected final Instant sent;
        protected final boolean encrypt;
        protected final int ttl;
        protected final byte systemChannel;

        public Packet(String channel, boolean encrypt, String namespace, int ttl, ByteBuf payload) {
            this(channel, encrypt, namespace, ttl, payload, Instant.now(Clock.systemUTC()));
        }

        private Packet(String channel, boolean encrypt, String namespace, int ttl, ByteBuf payload, Instant sent) {
            this.channel = channel;
            this.sent = sent;
            this.payload = payload;
            this.namespace = namespace;
            this.encrypt = encrypt;
            this.ttl = ttl;
            this.systemChannel = -1;
        }


        private Packet(byte systemChannel, boolean encrypt, ByteBuf payload, Instant sent) {
            this.channel = null;
            this.sent = sent;
            this.payload = payload;
            this.namespace = null;
            this.ttl = 0;
            this.encrypt = encrypt;
            this.systemChannel = systemChannel;
        }

        Packet(byte systemChannel, ByteBuf payload) {
            this(systemChannel, key != null, payload, Instant.now(Clock.systemUTC()));
        }

        public boolean isSystemMessage() {
            return systemChannel > 0;
        }

        public Message toMessage(PluginMessenger messenger) {

            return new Message(messenger, channel, payload);
        }

        public Packet encrypted(boolean encrypt) {
            if(this.encrypt == encrypt) return this;
            return new Packet(channel, encrypt, namespace, ttl, payload, sent);
        }

        public EnumSet<Flag> getFlags() {
            EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
            if(namespace != null) flags.add(Flag.NAMESPACED);
            if(encrypt) flags.add(Flag.ENCRYPTED);
            if(ttl > 0) flags.add(Flag.QUEUE);
            if(systemChannel > 0) flags.add(Flag.SYSTEM);
            return flags;
        }

        public boolean isExpired() {
            return ttl > 0 && sent.plus(ttl, ChronoUnit.MILLIS).isBefore(Instant.now(Clock.systemUTC()));
        }

        @Override
        public Identifier getId() {
            return MESSAGE_ID;
        }

        @Override
        public void write(ByteBuf buffer) {

            // Flags
            buffer.writeByte(Flag.pack(getFlags()));

            ByteBuf real = Unpooled.buffer();

            // Sent
            MidnightCoreAPI.LOGGER.warn("Sent seconds: {}", sent.getEpochSecond());
            real.writeLong(sent.getEpochSecond());

            // TTL
            if(ttl > 0) {
                real.writeInt(ttl);
            }

            // Namespace
            if(namespace != null) {
                PacketBufferUtil.writeUtf(real, namespace, 255);
            }

            // Channel
            if(systemChannel > 0) {
                buffer.writeByte(systemChannel);
            } else {
                if(channel == null) {
                    PacketBufferUtil.writeVarInt(real, 0);
                } else {
                    PacketBufferUtil.writeUtf(real, channel, 255);
                }
            }

            // Payload
            if(payload == null) {
                PacketBufferUtil.writeVarInt(real, 0);
            } else {
                PacketBufferUtil.writeVarInt(real, payload.writerIndex());
                real.writeBytes(payload);
            }

            // Encryption
            if(encrypt) {
                try {
                    key.encrypt(real, buffer);
                } catch (ShortBufferException ex) {
                    throw new RuntimeException("Unable to encrypt buffer!", ex);
                }
            } else {
                buffer.writeBytes(real);
            }
        }

    }

    protected Packet readPacket(ByteBuf buffer) {

        // Flags
        EnumSet<Flag> flags = Flag.unpack(buffer.readByte());

        // Decrypt
        boolean encrypt = flags.contains(Flag.ENCRYPTED);
        if(encrypt) {

            ByteBuf decrypted = Unpooled.buffer(key.getDecryptedLength(buffer.readableBytes()));
            try {
                key.decrypt(buffer, decrypted);
            } catch (ShortBufferException ex) {
                throw new RuntimeException("Unable to decrypt message!", ex);
            }
            buffer = decrypted;
        }

        // Timestamp
        long time = buffer.readLong();
        Instant sent = Instant.ofEpochSecond(time);

        // TTL
        int ttl = 0;
        if (flags.contains(Flag.QUEUE)) {
            ttl = buffer.readInt();
        }

        // Namespace
        String ns = null;
        if(flags.contains(Flag.NAMESPACED)) {
            ns = PacketBufferUtil.readUtf(buffer, 255);
        }

        // Channel
        String channel = null;
        byte systemChannel = 0;
        if(flags.contains(Flag.SYSTEM)) {
            systemChannel = buffer.readByte();
        } else {
            channel = PacketBufferUtil.readUtf(buffer, 255);
        }

        // Payload
        int length = PacketBufferUtil.readVarInt(buffer);
        ByteBuf payload = buffer.readRetainedSlice(length);

        Packet out;
        if(systemChannel > 0) {
            out = new Packet(systemChannel, encrypt, payload, sent);
        } else {
            out = new Packet(channel, encrypt, ns, ttl, payload, sent);
        }

        return out;
    }

    public interface Factory {
        PluginMessageBroker create(MessengerModule module, ConfigSection config);
    }

}
