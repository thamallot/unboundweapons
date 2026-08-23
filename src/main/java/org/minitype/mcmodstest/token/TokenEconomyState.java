package org.minitype.mcmodstest.token;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TokenEconomyState extends PersistentState {

    public static final int STARTING_CHARGES = 3;
    public static final int MAX_CHARGES = 10;

    private static final Codec<TokenEconomyState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("mode", TokenMode.HYBRID.id())
                    .forGetter(state -> state.mode.id()),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("charges", Map.of())
                    .forGetter(state -> state.charges),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("pvp_cooldowns", Map.of())
                    .forGetter(state -> state.pvpCooldowns),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("recovery_times", Map.of())
                    .forGetter(state -> state.recoveryTimes)
    ).apply(instance, TokenEconomyState::new));

    public static final PersistentStateType<TokenEconomyState> TYPE = new PersistentStateType<>(
            "unbnd_weapons_token_economy",
            TokenEconomyState::new,
            CODEC,
            null
    );

    private TokenMode mode = TokenMode.HYBRID;
    private final Map<String, Integer> charges = new HashMap<>();
    private final Map<String, Long> pvpCooldowns = new HashMap<>();
    private final Map<String, Long> recoveryTimes = new HashMap<>();

    public TokenEconomyState() {
    }

    private TokenEconomyState(
            String mode,
            Map<String, Integer> charges,
            Map<String, Long> pvpCooldowns,
            Map<String, Long> recoveryTimes
    ) {
        this.mode = TokenMode.fromId(mode);
        this.charges.putAll(charges);
        this.pvpCooldowns.putAll(pvpCooldowns);
        this.recoveryTimes.putAll(recoveryTimes);
    }

    public TokenMode getMode() {
        return mode;
    }

    public void setMode(TokenMode mode) {
        this.mode = mode;
        markDirty();
    }

    public int getCharges(UUID playerUuid) {
        return charges.getOrDefault(playerUuid.toString(), STARTING_CHARGES);
    }

    public void ensurePlayer(UUID playerUuid, long now) {
        String key = playerUuid.toString();
        boolean changed = false;

        if (!charges.containsKey(key)) {
            charges.put(key, STARTING_CHARGES);
            changed = true;
        }

        if (!recoveryTimes.containsKey(key)) {
            recoveryTimes.put(key, now);
            changed = true;
        }

        if (changed) {
            markDirty();
        }
    }

    public void setCharges(UUID playerUuid, int amount) {
        charges.put(playerUuid.toString(), Math.max(0, Math.min(MAX_CHARGES, amount)));
        markDirty();
    }

    public long getPairCooldown(String pairKey) {
        return pvpCooldowns.getOrDefault(pairKey, 0L);
    }

    public void setPairCooldown(String pairKey, long now) {
        pvpCooldowns.put(pairKey, now);
        markDirty();
    }

    public long getRecoveryTime(UUID playerUuid) {
        return recoveryTimes.getOrDefault(playerUuid.toString(), 0L);
    }

    public void setRecoveryTime(UUID playerUuid, long now) {
        recoveryTimes.put(playerUuid.toString(), now);
        markDirty();
    }
}
