package org.minitype.mcmodstest;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BlackFlashPayload() implements CustomPayload {

    public static final Id<BlackFlashPayload> ID =
            new Id<>(Identifier.of("unbnd_weapons", "black_flash"));

    public static final PacketCodec<RegistryByteBuf, BlackFlashPayload> CODEC =
            PacketCodec.unit(new BlackFlashPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
