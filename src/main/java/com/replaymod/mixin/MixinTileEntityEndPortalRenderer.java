package com.replaymod.mixin;

import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.minecraft.client.renderer.RenderState.PortalTexturingState.class)
public class MixinTileEntityEndPortalRenderer {
    @Redirect(method = "lambda$new$0", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;milliTime()J"))
    static
    private long replayModReplay_getEnchantmentTime() {
        ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
        if (replayHandler != null) {
            return replayHandler.getReplaySender().currentTimeStamp();
        }
        return Util.milliTime();
    }
}
