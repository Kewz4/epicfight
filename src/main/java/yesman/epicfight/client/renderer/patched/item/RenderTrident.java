package yesman.epicfight.client.renderer.patched.item;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@OnlyIn(Dist.CLIENT)
public class RenderTrident extends RenderItemBase {
	private final OpenMatrix4f correctionMatrixWhenAiming;
	
	public RenderTrident(JsonElement jsonElement) {
		super(jsonElement);
		
		this.correctionMatrixWhenAiming = new OpenMatrix4f().rotateDeg(-80F, Vec3f.X_AXIS).translate(0.0F, 0.1F, 0.0F).unmodifiable();
	}
	
	@Override
	public void renderItemInHand(ItemStack stack, LivingEntityPatch<?> entitypatch, InteractionHand hand, HumanoidArmature armature, OpenMatrix4f[] poses, MultiBufferSource buffer, PoseStack poseStack, int packedLight, float partialTicks) {
		OpenMatrix4f modelMatrix = this.getCorrectionMatrix(entitypatch, hand, poses);
		
		poseStack.pushPose();
		MathUtils.mulStack(poseStack, modelMatrix);
		ItemDisplayContext transformType = (hand == InteractionHand.MAIN_HAND) ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
		Minecraft mc = Minecraft.getInstance();
		mc.gameRenderer.itemInHandRenderer.renderItem(entitypatch.getOriginal(), stack, transformType, !(hand == InteractionHand.MAIN_HAND), poseStack, buffer, packedLight);
		poseStack.popPose();
	}
	
	@Override
	protected OpenMatrix4f getCorrectionMatrix(LivingEntityPatch<?> entitypatch, InteractionHand hand, OpenMatrix4f[] poses) {
		if (entitypatch.getOriginal().getUseItemRemainingTicks() > 0) {
			return this.correctionMatrixWhenAiming;
		}
		
		return super.getCorrectionMatrix(entitypatch, hand, poses);
	}
}