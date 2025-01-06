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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator;
import yesman.epicfight.api.physics.SimulationTypes;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.main.EpicFightMod;

@OnlyIn(Dist.CLIENT)
public class PatchedCloakLayer extends PatchedLayer<AbstractClientPlayer, AbstractClientPlayerPatch<AbstractClientPlayer>, PlayerModel<AbstractClientPlayer>, CapeLayer> {
	public static final ResourceLocation DUMMY_CLOAK_TEXTURE = new ResourceLocation(EpicFightMod.MODID, "textures/entity/cloak.png");
	
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
					Vec3 pos = entitypatch.getAccuratePartialLocation(partialFrame);
					float yRotLerp = entitypatch.getAccurateYRot(partialFrame);
					float scale = entitypatch.getScale();
					
					return OpenMatrix4f.createTranslation((float)pos.x, (float)pos.y, (float)pos.z).rotateDeg(180.0F - yRotLerp, Vec3f.Y_AXIS).scale(scale, scale, scale);
	            };
	            
				clothObj.tick(entitypatch, partialColliderTransformProvider, partialTick, entitypatch.getArmature(), poses);
				ResourceLocation cloakTexture = entitypatch.isEpicSkinsLoaded() ? entitypatch.getEpicSkinsInformation().cloakTexture().get() : entityliving.getCloakTextureLocation();
				
				if (cloakTexture != null) {
					double entityX = Mth.lerp((double)partialTick, entityliving.xOld, entityliving.getX());
					double entityY = Mth.lerp((double)partialTick, entityliving.yOld, entityliving.getY());
					double entityZ = Mth.lerp((double)partialTick, entityliving.zOld, entityliving.getZ());
					var renderer = ClientEngine.getInstance().renderEngine.getEntityRenderer(EntityType.PLAYER);
					
					PoseStack posestack$2 = new PoseStack();
					renderer.mulPoseStack(posestack$2, entitypatch.getArmature(), entityliving, entitypatch, partialTick);
					Matrix4f lastpose = posestack$2.last().pose();
					Matrix4f inverted = posestack$2.last().pose().invert();
					
					poseStack.pushPose();
					poseStack.mulPoseMatrix(inverted);
					poseStack.translate(-lastpose.m30(), -lastpose.m31(), -lastpose.m32());
					poseStack.translate(-entityX, -entityY, -entityZ);
					
					VertexConsumer vertexconsumer = buffer.getBuffer(EpicFightRenderTypes.getTriangulated(RenderType.entitySolid(cloakTexture)));
					
					if (entitypatch.isEpicSkinsLoaded()) {
						clothObj.drawPosed(poseStack, vertexconsumer, Mesh.DrawingFunction.NEW_ENTITY, packedLight, entitypatch.getEpicSkinsInformation().r(), entitypatch.getEpicSkinsInformation().g(), entitypatch.getEpicSkinsInformation().b(),
											1.0F, OverlayTexture.NO_OVERLAY, entitypatch.getArmature(), poses);
					} else {
						clothObj.drawPosed(poseStack, vertexconsumer, Mesh.DrawingFunction.NEW_ENTITY, packedLight, 1.0F, 1.0F, 1.0F, 1.0F, OverlayTexture.NO_OVERLAY, entitypatch.getArmature(), poses);
					}
					
					poseStack.popPose();
				}
			});
		});
	}
}