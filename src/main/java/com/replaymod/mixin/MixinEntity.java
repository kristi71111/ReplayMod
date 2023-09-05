package com.replaymod.mixin;

import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {
    //Replaces redirect in LivingRenderer
    @Inject(method = "isInvisibleToPlayer(Lnet/minecraft/entity/player/PlayerEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void isInvisibleToPlayerInject(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if(player instanceof CameraEntity){
            cir.setReturnValue(true);
        }
    }
}
