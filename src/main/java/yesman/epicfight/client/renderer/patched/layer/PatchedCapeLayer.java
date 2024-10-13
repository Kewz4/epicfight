package yesman.epicfight.client.renderer.patched.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

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
				
				float f = Mth.rotLerp(partialTicks, entitypatch.getYRot(), entitypatch.getYRotO());
				
				OpenMatrix4f transform = OpenMatrix4f.createRotatorDeg(180.0F - f, new Vec3f(0.0F, 1.0F, 0.0F))
													 .mulBack(poses[Armatures.BIPED.chest.getId()])
													 .mulBack(Armatures.BIPED.chest.getLocalTrasnform())
													 .translate(0.0F, 0.0F, 0.125F)
													 .mulFront(OpenMatrix4f.createTranslation(xPos, yPos, zPos));
				
				clothObj.tick(transform, partialTicks, poses, "default".equals(entitypatch.getOriginal().getModelName()) ? Meshes.BIPED : Meshes.ALEX);
				
				VertexConsumer vertexconsumer = buffer.getBuffer(EpicFightRenderTypes.getTriangulated(RenderType.entitySolid(entityliving.getCloakTextureLocation())));
				clothObj.draw(vertexconsumer, Mesh.DrawingFunction.ENTITY_TEXTURED, packedLight, 1.0F, 1.0F, 1.0F, 1.0F, OverlayTexture.NO_OVERLAY, partialTicks);
			});
		});
	}
}