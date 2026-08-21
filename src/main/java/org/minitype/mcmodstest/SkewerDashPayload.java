package org.minitype.mcmodstest;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SkewerDashPayload() implements CustomPayload {

    public static final Id<SkewerDashPayload> ID =
            new Id<>(Identifier.of("unbnd_weapons", "skewer_dash"));

    public static final PacketCodec<RegistryByteBuf, SkewerDashPayload> CODEC =
            PacketCodec.unit(new SkewerDashPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
