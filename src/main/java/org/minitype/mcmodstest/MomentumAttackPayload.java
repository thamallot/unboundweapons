package org.minitype.mcmodstest;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MomentumAttackPayload() implements CustomPayload {

    public static final Id<MomentumAttackPayload> ID =
            new Id<>(Identifier.of("unbnd_weapons", "momentum_attack"));

    public static final PacketCodec<RegistryByteBuf, MomentumAttackPayload> CODEC =
            PacketCodec.unit(new MomentumAttackPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
