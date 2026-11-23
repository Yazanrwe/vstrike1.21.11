package com.vstrike.mixin.client;

import com.vstrike.config.VStrikeConfig;
import com.vstrike.util.LatencyProbe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.MouseInput;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla only polls the attack key inside MinecraftClient.handleInputEvents(), once per client
 * tick (20 Hz). This hook runs after vanilla has registered the press; if the tick path would
 * accept it anyway, it is consumed here and doAttack() is called immediately.
 * Blocks, misses (10-tick lockout), item use, screens, riding etc. stay on the tick path.
 *
 * 1.21.9+ note: Mouse.onMouseButton now takes (long window, MouseInput input, int action).
 */
@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private double x;

    @Shadow
    private double y;

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void vstrike$markPress(long window, MouseInput input, int action, CallbackInfo ci) {
        if (VStrikeConfig.debugLatencyLog
                && action == GLFW.GLFW_PRESS
                && this.client.options.attackKey.matchesMouse(new Click(this.x, this.y, input))) {
            LatencyProbe.markPress();
        }
    }

    @Inject(method = "onMouseButton", at = @At("TAIL"))
    private void vstrike$earlyAttack(long window, MouseInput input, int action, CallbackInfo ci) {
        if (!VStrikeConfig.earlyAttackDispatch || action != GLFW.GLFW_PRESS) {
            return;
        }

        MinecraftClient mc = this.client;
        if (!mc.options.attackKey.matchesMouse(new Click(this.x, this.y, input))) {
            return;
        }

        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }
        if (mc.currentScreen != null || mc.getOverlay() != null || mc.isPaused() || !mc.mouse.isCursorLocked()) {
            return;
        }
        if (mc.player.isUsingItem()) {
            return;
        }

        HitResult hit = mc.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) {
            return;
        }

        MinecraftClientAccessor accessor = (MinecraftClientAccessor) mc;
        if (accessor.vstrike$getAttackCooldown() > 0) {
            return;
        }

        // Consume the press vanilla just queued so the tick path does not attack twice.
        if (!mc.options.attackKey.wasPressed()) {
            return;
        }
        accessor.vstrike$invokeDoAttack();
    }
}
