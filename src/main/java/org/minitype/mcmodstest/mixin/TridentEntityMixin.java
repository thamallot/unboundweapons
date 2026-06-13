package org.minitype.mcmodstest.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TridentEntity.class)
public class TridentEntityMixin {

    @Inject(
            method = "onEntityHit",
            at = @At("HEAD")
    )
    private void megaWeapons$boostDamage(
            net.minecraft.util.hit.EntityHitResult entityHitResult,
            CallbackInfo ci
    ) {

        TridentEntity trident = (TridentEntity)(Object)this;

        Entity target = entityHitResult.getEntity();

        // Current trident velocity
        Vec3d velocity = trident.getVelocity();

        double speed = velocity.length();

        // BONUS DAMAGE BASED ON SPEED
        float bonusDamage = (float)(speed * 4);

        // Direct extra damage
        if (trident.getEntityWorld() instanceof ServerWorld serverWorld) {
            target.damage(
                    serverWorld,
                    trident.getDamageSources().trident(
                            trident,
                            trident.getOwner()
                    ),
                    bonusDamage
            );
        }
    }
}