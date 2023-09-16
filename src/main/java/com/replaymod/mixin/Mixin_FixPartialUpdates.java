package com.replaymod.mixin;

import com.replaymod.replay.ext.EntityExt;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetHandler.class)
public class Mixin_FixPartialUpdates {
    @Redirect(method = "handleEntityMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationYaw:F", opcode = Opcodes.GETFIELD))
    private float getTrackedYaw(Entity instance) {
        return ((EntityExt) instance).replaymod$getTrackedYaw();
    }
    @Redirect(method = "handleEntityMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationPitch:F", opcode = Opcodes.GETFIELD))
    private float getTrackedPitch(Entity instance) {
        return ((EntityExt) instance).replaymod$getTrackedPitch();
    }
    @Redirect(method = "handleEntityMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getPosX()D"))
    private double getTrackedX(Entity instance) {
        return instance.positionOffset().getX();
    }
    @Redirect(method = "handleEntityMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getPosY()D"))
    private double getTrackedY(Entity instance) {
        return instance.positionOffset().getY();
    }
    @Redirect(method = "handleEntityMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getPosZ()D"))
    private double getTrackedZ(Entity instance) {
        return instance.positionOffset().getZ();
    }

    private static final String ENTITY_UPDATE = "Lnet/minecraft/entity/Entity;setPositionAndRotationDirect(DDDFFIZ)V";

    @Unique
    private Entity entity;

    @ModifyVariable(method = {"handleEntityMovement","handleEntityTeleport"}, at = @At(value = "INVOKE", target = ENTITY_UPDATE), ordinal = 0)
    private Entity captureEntity(Entity entity) {
        return this.entity = entity;
    }

    @Inject(method = {"handleEntityMovement","handleEntityTeleport"}, at = @At("RETURN"))
    private void resetEntityField(CallbackInfo ci) {
        this.entity = null;
    }

    @ModifyArg(method = {"handleEntityMovement","handleEntityTeleport"}, at = @At(value = "INVOKE", target = ENTITY_UPDATE), index = 3)
    private float captureTrackedYaw(float value) {
        ((EntityExt) this.entity).replaymod$setTrackedYaw(value);
        return value;
    }

    @ModifyArg(method = {"handleEntityMovement","handleEntityTeleport"}, at = @At(value = "INVOKE", target = ENTITY_UPDATE), index = 4)
    private float captureTrackedPitch(float value) {
        ((EntityExt) this.entity).replaymod$setTrackedPitch(value);
        return value;
    }
}
