package com.replaymod.mixin;

import com.replaymod.replay.InputReplayTimer;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.MouseHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MouseHelper.class)
public abstract class MixinMouseHelper {
    @Inject(method = "scrollCallback",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/player/ClientPlayerEntity;isSpectator()Z"),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true)
    private void handleReplayModScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            InputReplayTimer.handleScroll((int) (yoffset * 120));
            ci.cancel();
        }
    }
}
