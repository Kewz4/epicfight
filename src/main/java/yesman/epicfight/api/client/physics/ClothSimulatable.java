package yesman.epicfight.api.client.physics;

import javax.annotation.Nullable;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.model.Armature;

@OnlyIn(Dist.CLIENT)
public interface ClothSimulatable {
	@Nullable
	Armature getArmature();
}