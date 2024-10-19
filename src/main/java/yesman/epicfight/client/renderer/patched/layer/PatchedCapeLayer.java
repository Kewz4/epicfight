package yesman.epicfight.client.renderer.patched.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
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

@OnlyIn(Dist.CLIENT)
public class PatchedCapeLayer extends PatchedLayer<AbstractClientPlayer, AbstractClientPlayerPatch<AbstractClientPlayer>, PlayerModel<AbstractClientPlayer>, CapeLayer>  {
	@Override
	protected void renderLayer(AbstractClientPlayerPatch<AbstractClientPlayer> entitypatch, AbstractClientPlayer entityliving, CapeLayer vanillaLayer, PoseStack poseStack, MultiBufferSource buffer, int packedLight, OpenMatrix4f[] poses, float bob, float yRot, float xRot, float partialTicks) {
		entitypatch.getSimulator(SimulationTypes.CLOTH).ifPresent((simulator) -> {
			simulator.getRunningSimulationData(Meshes.CAPE).ifPresent((clothObj) -> {
				float xPos = (float)Mth.lerp(partialTicks, entitypatch.getOriginal().xOld, entitypatch.getOriginal().getX());
				float yPos = (float)Mth.lerp(partialTicks, entitypatch.getOriginal().yOld, entitypatch.getOriginal().getY());
				float zPos = (float)Mth.lerp(partialTicks, entitypatch.getOriginal().zOld, entitypatch.getOriginal().getZ());
				
				float yRotLerp = Mth.rotLerp(partialTicks, entitypatch.getYRot(), entitypatch.getYRotO());
				
				OpenMatrix4f colliderRootTransform = OpenMatrix4f.createRotatorDeg(180.0F - yRotLerp, Vec3f.Y_AXIS)
																 .mulFront(OpenMatrix4f.createTranslation(xPos, yPos, zPos));
				
				double d0 = Mth.lerp((double)partialTicks, entitypatch.getOriginal().xCloakO, entitypatch.getOriginal().xCloak) - Mth.lerp((double)partialTicks, entitypatch.getOriginal().xo, entitypatch.getOriginal().getX());
	            double d1 = Mth.lerp((double)partialTicks, entitypatch.getOriginal().yCloakO, entitypatch.getOriginal().yCloak) - Mth.lerp((double)partialTicks, entitypatch.getOriginal().yo, entitypatch.getOriginal().getY());
	            double d2 = Mth.lerp((double)partialTicks, entitypatch.getOriginal().zCloakO, entitypatch.getOriginal().zCloak) - Mth.lerp((double)partialTicks, entitypatch.getOriginal().zo, entitypatch.getOriginal().getZ());
	            float f = Mth.rotLerp(partialTicks, entitypatch.getOriginal().yBodyRotO, entitypatch.getOriginal().yBodyRot);
	            double d3 = (double)Mth.sin(f * ((float)Math.PI / 180F));
	            double d4 = (double)(-Mth.cos(f * ((float)Math.PI / 180F)));
	            float f1 = (float)d1 * 10.0F;
	            f1 = Mth.clamp(f1, -6.0F, 32.0F);
	            float f2 = (float)(d0 * d3 + d2 * d4) * 100.0F;
	            f2 = Mth.clamp(f2, 0.0F, 150.0F);
	            float f3 = (float)(d0 * d4 - d2 * d3) * 100.0F;
	            f3 = Mth.clamp(f3, -20.0F, 20.0F);
	            
	            if (f2 < 0.0F) {
	            	f2 = 0.0F;
	            }
	            
	            float f4 = Mth.lerp(partialTicks, entitypatch.getOriginal().oBob, entitypatch.getOriginal().bob);
	            f1 += Mth.sin(Mth.lerp(partialTicks, entitypatch.getOriginal().walkDistO, entitypatch.getOriginal().walkDist) * 6.0F) * 32.0F * f4;
	            
	            OpenMatrix4f transform = new OpenMatrix4f(colliderRootTransform)
						 .mulBack(poses[Armatures.BIPED.chest.getId()])
						 .mulBack(Armatures.BIPED.chest.getLocalTrasnform())
						 .translate(0.0F, 0.0F, 0.125F)
						 .rotateDeg(-(6.0F + f2 / 2.0F + f1), Vec3f.X_AXIS)
						 .rotateDeg(f3 / 2.0F, Vec3f.Z_AXIS)
						 .rotateDeg(f3 / 2.0F, Vec3f.Y_AXIS)
						 ;
	            
				clothObj.tick(transform, partialTicks, colliderRootTransform, poses, "default".equals(entitypatch.getOriginal().getModelName()) ? Meshes.BIPED : Meshes.ALEX);
				
				VertexConsumer vertexconsumer = buffer.getBuffer(EpicFightRenderTypes.getTriangulated(RenderType.entitySolid(entityliving.getCloakTextureLocation())));
				clothObj.draw(vertexconsumer, Mesh.DrawingFunction.ENTITY_TEXTURED, packedLight, 1.0F, 1.0F, 1.0F, 1.0F, OverlayTexture.NO_OVERLAY, partialTicks);
			});
		});
	}
}