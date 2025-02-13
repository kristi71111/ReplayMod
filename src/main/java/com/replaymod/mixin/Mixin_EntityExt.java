package com.replaymod.mixin;

import com.replaymod.replay.ext.EntityExt;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class Mixin_EntityExt implements EntityExt {

    @Shadow
    public float rotationYaw;

    @Shadow
    public float rotationPitch;

    @Unique
    private float trackedYaw = Float.NaN;

    @Unique
    private float trackedPitch = Float.NaN;

    @Override
    public float replaymod$getTrackedYaw() {
        return !Float.isNaN(this.trackedYaw) ? this.trackedYaw : this.rotationYaw;
    }

    @Override
    public float replaymod$getTrackedPitch() {
        return !Float.isNaN(this.trackedPitch) ? this.trackedPitch : this.rotationPitch;
    }

    @Override
    public void replaymod$setTrackedYaw(float value) {
        this.trackedYaw = value;
    }

    @Override
    public void replaymod$setTrackedPitch(float value) {
        this.trackedPitch = value;
    }
}
