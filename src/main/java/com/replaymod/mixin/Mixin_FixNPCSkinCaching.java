package com.replaymod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class Mixin_FixNPCSkinCaching {
    @Shadow
    @Nullable
    protected abstract NetworkPlayerInfo getPlayerInfo();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void forceCachePlayerListEntry(CallbackInfo ci) {
        if (Minecraft.getInstance().getConnection() != null) { // will be null if this is the client player
            this.getPlayerInfo();
        }
    }
}