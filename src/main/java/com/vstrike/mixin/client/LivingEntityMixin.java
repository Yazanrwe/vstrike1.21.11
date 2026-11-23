package com.vstrike.mixin.client;

import com.vstrike.config.VStrikeConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Starts the local player swing animation one tick sooner (cosmetic only).
 * Swing duration, damage, attack packets and attack-strength cooldown are not touched.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    public boolean handSwinging;

    @Shadow
    public int handSwingTicks;

    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;Z)V", at = @At("TAIL"))
    private void vstrike$startSwingOneTickSooner(Hand hand, boolean fromServerPlayer, CallbackInfo ci) {
        if (VStrikeConfig.swingStartCompensation
                && (Object) this instanceof ClientPlayerEntity
                && this.handSwinging
                && this.handSwingTicks == -1) {
            this.handSwingTicks = 0;
        }
    }
}
