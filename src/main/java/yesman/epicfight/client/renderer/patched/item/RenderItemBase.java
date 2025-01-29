package yesman.epicfight.client.renderer.patched.item;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@OnlyIn(Dist.CLIENT)
public class RenderItemBase {
	protected final OpenMatrix4f mainhandcorrectionMatrix;
	protected final OpenMatrix4f offhandCorrectionMatrix;
	public static RenderEngine renderEngine;
	
	public RenderItemBase() {
		this(new OpenMatrix4f().translate(0F, 0F, -0.13F).rotateDeg(-90.0F, Vec3f.X_AXIS), new OpenMatrix4f().translate(0F, 0F, -0.13F).rotateDeg(-90.0F, Vec3f.X_AXIS));
	}
	
	public RenderItemBase(OpenMatrix4f mainhandcorrectionMatrix, OpenMatrix4f offhandCorrectionMatrix) {
		this.mainhandcorrectionMatrix = mainhandcorrectionMatrix;
		this.offhandCorrectionMatrix = offhandCorrectionMatrix;
	}
	
	public void renderItemInHand(ItemStack stack, LivingEntityPatch<?> entitypatch, InteractionHand hand, HumanoidArmature armature, OpenMatrix4f[] poses, MultiBufferSource buffer, PoseStack poseStack, int packedLight, float partialTicks) {
		OpenMatrix4f modelMatrix = this.getCorrectionMatrix(stack, entitypatch, hand);
		boolean isInMainhand = (hand == InteractionHand.MAIN_HAND);
		Joint holdingHand = isInMainhand ? armature.toolR : armature.toolL;
		modelMatrix.mulFront(poses[holdingHand.getId()]);
		
		poseStack.pushPose();
		this.mulPoseStack(poseStack, modelMatrix);
		ItemDisplayContext transformType = isInMainhand ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
		Minecraft mc = Minecraft.getInstance();
		mc.gameRenderer.itemInHandRenderer.renderItem(entitypatch.getOriginal(), stack, transformType, !isInMainhand, poseStack, buffer, packedLight);
		poseStack.popPose();
	}
	
	protected void mulPoseStack(PoseStack poseStack, OpenMatrix4f pose) {
		MathUtils.mulStack(poseStack, pose);
	}
	
	public OpenMatrix4f getCorrectionMatrix(ItemStack stack, LivingEntityPatch<?> itemHolder, InteractionHand hand) {
		return new OpenMatrix4f(hand == InteractionHand.MAIN_HAND ? this.mainhandcorrectionMatrix : this.offhandCorrectionMatrix);
	}
}