package yesman.epicfight.client.renderer.patched.layer;

import java.util.function.Function;

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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.physics.SimulationTypes;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.main.EpicFightMod;

@OnlyIn(Dist.CLIENT)
public class PatchedCapeLayer extends PatchedLayer<AbstractClientPlayer, AbstractClientPlayerPatch<AbstractClientPlayer>, PlayerModel<AbstractClientPlayer>, CapeLayer>  {
	public static final ResourceLocation DUMMY_CAPE_TEXTURE = new ResourceLocation(EpicFightMod.MODID, "textures/entity/cape.png");
	
	@Override
	protected void renderLayer(AbstractClientPlayerPatch<AbstractClientPlayer> entitypatch, AbstractClientPlayer entityliving, CapeLayer vanillaLayer, PoseStack poseStack, MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTick) {
		// Prevent simulating cape in inventory screen
		if (Minecraft.getInstance().screen instanceof EffectRenderingInventoryScreen && entityliving == Minecraft.getInstance().player && partialTick == 1.0F) {
			return;
		}
		
		entitypatch.getSimulator(SimulationTypes.CLOTH).ifPresent((simulator) -> {
			simulator.getRunningSimulationData(Meshes.CAPE).ifPresent((clothObj) -> {
	            Function<Float, OpenMatrix4f> partialRootTransformProvider = (partialFrame) -> {
		            Vec3 pos = entitypatch.getAccuratePartialLocation(partialFrame);
		            Vec3 cloakPos = entitypatch.getAccurateCloakLocation(partialFrame);
		            
		            float f = Mth.rotLerp(partialFrame, entitypatch.getYRotO(), entitypatch.getYRot());
		            double d3 = (double)Mth.sin(f * ((float)Math.PI / 180F));
		            double d4 = (double)(-Mth.cos(f * ((float)Math.PI / 180F)));
		            float f1 = (float)cloakPos.y * 10.0F;
		            f1 = Mth.clamp(f1, -6.0F, 32.0F);
		            float f2 = (float)(cloakPos.x * d3 + cloakPos.z * d4) * 100.0F;
		            f2 = Mth.clamp(f2, 0.0F, 150.0F);
		            float f3 = (float)(cloakPos.x * d4 - cloakPos.z * d3) * 100.0F;
		            f3 = Mth.clamp(f3, -20.0F, 20.0F);
		            
		            if (f2 < 0.0F) {
		            	f2 = 0.0F;
		            }
		            
		            float f4 = Mth.lerp(partialFrame, entitypatch.getOriginal().oBob, entitypatch.getOriginal().bob);
		            f1 += Mth.sin(Mth.lerp(partialFrame, entitypatch.getOriginal().walkDistO, entitypatch.getOriginal().walkDist) * 6.0F) * 32.0F * f4;
					
					return OpenMatrix4f.createTranslation((float)pos.x, (float)pos.y, (float)pos.z)
									   .rotateDeg(180.0F - f, Vec3f.Y_AXIS)
									   .mulBack(poses[Armatures.BIPED.chest.getId()])
									   .mulBack(Armatures.BIPED.chest.getLocalTrasnform())
									   .translate(0.0F, 0.0F, 0.125F)
									   .rotateDeg(-(6.0F + f2 / 2.0F + f1), Vec3f.X_AXIS)
									   .rotateDeg(f3 / 2.0F, Vec3f.Y_AXIS);
	            };
	            
	            Function<Float, OpenMatrix4f> partialColliderTransformProvider = (partialFrame) -> {
					Vec3 pos = entitypatch.getAccuratePartialLocation(partialFrame);
					float yRotLerp = entitypatch.getAccurateYRot(partialFrame);
					
					return OpenMatrix4f.createTranslation((float)pos.x, (float)pos.y, (float)pos.z).rotateDeg(180.0F - yRotLerp, Vec3f.Y_AXIS);
	            };
	            
				clothObj.tick(entitypatch, partialRootTransformProvider, partialColliderTransformProvider, poses, partialTick);
				
				ResourceLocation capeTexture = EpicFightMod.CLIENT_CONFIGS.enableDummyCape.getValue() ? DUMMY_CAPE_TEXTURE : entityliving.getCloakTextureLocation();
				
				if (capeTexture != null) {
					VertexConsumer vertexconsumer = buffer.getBuffer(EpicFightRenderTypes.getTriangulated(RenderType.entitySolid(capeTexture)));
					clothObj.draw(entitypatch, buffer, vertexconsumer, Mesh.DrawingFunction.ENTITY_TEXTURED, packedLight, 1.0F, 1.0F, 1.0F, 1.0F, OverlayTexture.NO_OVERLAY, partialTick);
				}
			});
		});
	}
}