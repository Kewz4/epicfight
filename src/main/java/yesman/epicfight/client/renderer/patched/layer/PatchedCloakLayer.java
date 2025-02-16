package yesman.epicfight.client.renderer.patched.layer;

import java.util.function.Function;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.online.EpicSkins;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator;
import yesman.epicfight.api.physics.SimulationTypes;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.main.EpicFightMod;

@OnlyIn(Dist.CLIENT)
public class PatchedCloakLayer extends PatchedLayer<AbstractClientPlayer, AbstractClientPlayerPatch<AbstractClientPlayer>, PlayerModel<AbstractClientPlayer>, CapeLayer> {
	public static final ResourceLocation DUMMY_CLOAK_TEXTURE = ResourceLocation.tryBuild(EpicFightMod.MODID, "textures/entity/cloak.png");
	
	@SuppressWarnings("unchecked")
	@Override
	protected void renderLayer(AbstractClientPlayerPatch<AbstractClientPlayer> entitypatch, AbstractClientPlayer entityliving, CapeLayer vanillaLayer, PoseStack poseStack, MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTick) {
		// Prevent simulating cape in inventory screen
		if (Minecraft.getInstance().screen instanceof EffectRenderingInventoryScreen && entityliving == Minecraft.getInstance().player && partialTick == 1.0F) {
			return;
		}
		
		entitypatch.getSimulator(SimulationTypes.CLOTH).ifPresent((simulator) -> {
			simulator.getRunningObject(ClothSimulator.PLAYER_CLOAK).ifPresent((clothObj) -> {
	            Function<Float, OpenMatrix4f> partialColliderTransformProvider = (partialFrame) -> {
					Vec3 pos = entitypatch.getOriginal().getPosition(partialFrame);
					float yRotLerp = Mth.rotLerp(partialFrame, entitypatch.getYRotO(), entitypatch.getYRot());
					
					return OpenMatrix4f.createTranslation((float)pos.x, (float)pos.y, (float)pos.z).rotateDeg(180.0F - yRotLerp, Vec3f.Y_AXIS);
	            };
	            
				ResourceLocation cloakTexture = entitypatch.isEpicSkinsLoaded() ? entitypatch.getEpicSkinsInformation().cloakTexture().get() : entityliving.getCloakTextureLocation();
				//cloakTexture = ResourceLocation.tryBuild(EpicFightMod.MODID, "animmodels/layer/cape.png");
				//REMOVE
				if (cloakTexture != null) {
					clothObj.tick(entitypatch, partialColliderTransformProvider, partialTick, entitypatch.getArmature(), poses);
					
					double entityX = Mth.lerp((double)partialTick, entityliving.xOld, entityliving.getX());
					double entityY = Mth.lerp((double)partialTick, entityliving.yOld, entityliving.getY());
					double entityZ = Mth.lerp((double)partialTick, entityliving.zOld, entityliving.getZ());
					var renderer = ClientEngine.getInstance().renderEngine.getEntityRenderer(EntityType.PLAYER);
					
					PoseStack posestack$2 = new PoseStack();
					renderer.mulPoseStack(posestack$2, entitypatch.getArmature(), entityliving, entitypatch, partialTick);
					Matrix4f lastpose = posestack$2.last().pose();
					Matrix4f inverted = posestack$2.last().pose().invert();
					
					poseStack.pushPose();
					
					float scaler = entitypatch.getScale();
					poseStack.scale(scaler, scaler, scaler);
					
					if (entitypatch.getOriginal().hasItemInSlot(EquipmentSlot.CHEST)) {
						OpenMatrix4f poseMat = poses[Armatures.BIPED.get().chest.getId()];
						poseStack.translate(poseMat.m30, poseMat.m31, poseMat.m32);
						poseStack.scale(1.17F, 1.17F, 1.17F);
						poseStack.translate(-poseMat.m30, -poseMat.m31, -poseMat.m32);
					}
					
					poseStack.pushPose();
					poseStack.mulPoseMatrix(inverted);
					poseStack.translate(-lastpose.m30() - entityX, -lastpose.m31() - entityY, -lastpose.m32() - entityZ);
					
					VertexConsumer vertexconsumer = buffer.getBuffer(EpicFightRenderTypes.getTriangulated(RenderType.entityCutoutNoCull(cloakTexture)));
					
					if (entitypatch.isEpicSkinsLoaded()) {
						EpicSkins epicskinsInfo = entitypatch.getEpicSkinsInformation();
						clothObj.drawPosed(poseStack, vertexconsumer, Mesh.DrawingFunction.NEW_ENTITY, packedLight, epicskinsInfo.r(), epicskinsInfo.g(), epicskinsInfo.b(), 1.0F, OverlayTexture.NO_OVERLAY, entitypatch.getArmature(), poses);
					} else {
						clothObj.drawPosed(poseStack, vertexconsumer, Mesh.DrawingFunction.NEW_ENTITY, packedLight, 1.0F, 1.0F, 1.0F, 1.0F, OverlayTexture.NO_OVERLAY, entitypatch.getArmature(), poses);
					}
					
					poseStack.popPose();
					poseStack.popPose();
				}
			});
		});
	}
}