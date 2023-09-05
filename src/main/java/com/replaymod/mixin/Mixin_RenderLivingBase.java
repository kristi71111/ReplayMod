package com.replaymod.mixin;

import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.replaymod.core.versions.MCVer.getMinecraft;

@Mixin(LivingRenderer.class)
public abstract class Mixin_RenderLivingBase {
    @Inject(method = "canRenderName(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void replayModReplay_canRenderInvisibleName(LivingEntity entity, CallbackInfoReturnable<Boolean> ci) {
        PlayerEntity thePlayer = getMinecraft().player;
        if (thePlayer instanceof CameraEntity && entity.isInvisible()) {
            ci.setReturnValue(false);
        }
    }
}
