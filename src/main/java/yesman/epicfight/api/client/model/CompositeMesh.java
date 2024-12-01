package yesman.epicfight.api.client.model;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

@OnlyIn(Dist.CLIENT)
public class CompositeMesh implements Mesh, MeshProvider<CompositeMesh> {
	private final Map<String, Mesh> meshes;
	
	public CompositeMesh(ImmutableMap.Builder<String, Mesh> meshBuilder) {
		this.meshes = meshBuilder.build();
	}
	
	@Override
	public CompositeMesh get() {
		return this;
	}
	
	@Override
	public void initialize() {
		this.meshes.values().forEach(Mesh::initialize);
	}
	
	@Override
	public void draw(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
		for (Mesh mesh : this.meshes.values()) {
			mesh.draw(poseStack, builder, drawingFunction, packedLight, r, g, b, a, overlay);
		}
	}
	
	@Override
	public void drawPosed(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay, Armature armature, OpenMatrix4f[] poses) {
		for (Mesh mesh : this.meshes.values()) {
			mesh.drawPosed(poseStack, builder, drawingFunction, packedLight, r, g, b, a, overlay, armature, poses);
		}
	}
}