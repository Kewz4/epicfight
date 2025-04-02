package yesman.epicfight.client.renderer.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.WitherSkeletonRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.main.EpicFightMod;

@OnlyIn(Dist.CLIENT)
public class WitherSkeletonMinionRenderer extends WitherSkeletonRenderer {
	private static final ResourceLocation WITHER_SKELETON_LOCATION = ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID, "textures/entity/wither_skeleton_minion.png");

	public WitherSkeletonMinionRenderer(Context context) {
		super(context);
	}

	public ResourceLocation getTextureLocation(AbstractSkeleton entity) {
		return WITHER_SKELETON_LOCATION;
	}
}