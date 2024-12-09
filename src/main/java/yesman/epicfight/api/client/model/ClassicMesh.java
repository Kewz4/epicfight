package yesman.epicfight.api.client.model;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.ClassicMesh.ClassicMeshPart;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.main.EpicFightMod;

@OnlyIn(Dist.CLIENT)
public class ClassicMesh extends StaticMesh<ClassicMeshPart, ClassicMeshVertexBuilder> implements MeshProvider<ClassicMesh> {
	public ClassicMesh(Map<String, Number[]> arrayMap, Map<MeshPartDefinition, List<ClassicMeshVertexBuilder>> partBuilders, ClassicMesh parent, RenderProperties properties) {
		super(arrayMap, partBuilders, parent, properties);
	}
	
	@Override
	protected Map<String, ClassicMeshPart> createModelPart(Map<MeshPartDefinition, List<ClassicMeshVertexBuilder>> partBuilders) {
		Map<String, ClassicMeshPart> parts = Maps.newHashMap();
		
		partBuilders.forEach((partDefinition, vertexBuilder) -> {
			parts.put(partDefinition.partName(), new ClassicMeshPart(vertexBuilder, partDefinition.getModelPartAnimationProvider(), partDefinition.clothInfo()));
		});
		
		return parts;
	}
	
	@Override
	protected ClassicMeshPart getOrLogException(Map<String, ClassicMeshPart> parts, String name) {
		if (!parts.containsKey(name)) {
			EpicFightMod.LOGGER.debug("Can not find the mesh part named " + name + " in " + this.getClass().getCanonicalName());
			return null;
		}
		
		return parts.get(name);
	}
	
	@Override
	public void draw(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
		for (ClassicMeshPart part : this.parts.values()) {
			part.draw(poseStack, builder, drawingFunction, packedLight, r, g, b, a, overlay);
		}
	}
	
	@Override
	public void drawPosed(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay, Armature armature, OpenMatrix4f[] poses) {
		this.draw(poseStack, builder, drawingFunction, packedLight, r, g, b, a, overlay);
	}
	
	@OnlyIn(Dist.CLIENT)
	public class ClassicMeshPart extends MeshPart<ClassicMeshVertexBuilder> {
		public ClassicMeshPart(List<ClassicMeshVertexBuilder> verticies, @Nullable Supplier<OpenMatrix4f> vanillaPartTracer, @Nullable SoftBodyMesh.ClothSimulationInfo clothInfo) {
			super(verticies, vanillaPartTracer, clothInfo);
		}
		
		@Override
		public void draw(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
			if (this.isHidden()) {
				return;
			}
			
			poseStack.pushPose();
			OpenMatrix4f transform = this.getVanillaPartTransform();
			
			if (transform != null) {
				poseStack.mulPoseMatrix(OpenMatrix4f.exportToMojangMatrix(transform));
			}
			
			Matrix4f matrix4f = poseStack.last().pose();
			Matrix3f matrix3f = poseStack.last().normal();
			
			for (ClassicMeshVertexBuilder vi : this.getVertices()) {
				vi.getVertexPosition(ClassicMesh.this, POSITION);
				vi.getVertexNormal(ClassicMesh.this, NORMAL);
				POSITION.mul(matrix4f);
				NORMAL.mul(matrix3f);
				
				drawingFunction.draw(builder, POSITION.x(), POSITION.y(), POSITION.z(), NORMAL.x(), NORMAL.y(), NORMAL.z(), packedLight, r, g, b, a, uvs[vi.uv * 2], uvs[vi.uv * 2 + 1], overlay);
			}
			
			poseStack.popPose();
		}
	}
	
	@Override
	public ClassicMesh get() {
		return this;
	}
}