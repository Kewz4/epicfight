package yesman.epicfight.api.client.physics;

import javax.annotation.Nullable;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.model.Armature;

@OnlyIn(Dist.CLIENT)
public interface ClothSimulatable {
	@Nullable
	Armature getArmature();
	boolean valid();
	
	// Cloth object requires providing location info for 2 steps before for accurate continuous collide detection.
	public Vec3 getAccuratePartialLocation(float partialFrame);
	public Vec3 getObjectVelocity(float partialFrame);
	public float getAccurateYRot(float partialFrame);
	public float getYRotDelta(float partialFrame);
}