package yesman.epicfight.api.client.model;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.ClothMesh.ClothPart;
import yesman.epicfight.api.client.physics.ClothSimulatable;
import yesman.epicfight.api.client.physics.ClothSimulator;
import yesman.epicfight.api.client.physics.ClothSimulator.ClothObject;
import yesman.epicfight.api.physics.SimulationProvider;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.main.EpicFightMod;

@OnlyIn(Dist.CLIENT)
public class ClothMesh extends Mesh<ClothPart, VertexBuilder> implements SimulationProvider<ClothSimulatable, ClothSimulator.ClothObject, ClothSimulator.ClothObjectBuilder> {
	public ClothMesh(Map<String, float[]> arrayMap, Map<MeshPartDefinition, List<VertexBuilder>> partBuilders, @Nullable ClothMesh parent, RenderProperties properties) {
		super(arrayMap, partBuilders, null, properties);
	}
	
	@Override
	protected Map<String, ClothPart> createModelPart(Map<MeshPartDefinition, List<VertexBuilder>> partBuilders) {
		Map<String, ClothPart> parts = Maps.newHashMap();
		
		partBuilders.forEach((partDefinition, vertexBuilder) -> {
			ClothPartDefinition clothDefinition = (ClothPartDefinition)partDefinition;
			
			parts.put(partDefinition.partName(), new ClothPart(vertexBuilder, clothDefinition.constraints(), clothDefinition.constraintTypes(), clothDefinition.compliances(), clothDefinition.particles(), clothDefinition.rootDistance(), clothDefinition.collider()));
		});
		
		return parts;
	}
	
	@Override
	public void draw(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
		for (ClothPart part : this.parts.values()) {
			part.draw(poseStack, builder, drawingFunction, packedLight, r, g, b, a, overlay);
		}
	}
	
	@Override
	protected ClothPart getOrLogException(Map<String, ClothPart> parts, String name) {
		if (!parts.containsKey(name)) {
			EpicFightMod.LOGGER.debug("Can not find the mesh part named " + name + " in " + this.getClass().getCanonicalName());
			return null;
		}
		
		return parts.get(name);
	}
	
	@Override
	public ClothSimulator.ClothObject createSimulationData(ClothSimulatable simObject, ClothSimulator.ClothObjectBuilder builder) {
		return new ClothObject(builder, this, this.parts, this.positions);
	}
	
	@OnlyIn(Dist.CLIENT)
	public record ClothPartDefinition(String partName, List<int[]> constraints, ConstraintType[] constraintTypes, float[] compliances, int[] particles, float[] rootDistance, boolean[] collider) implements MeshPartDefinition {
		public static ClothPartDefinition of(String partName, List<int[]> constraints, ConstraintType[] constraintTypes, float[] compliances, int[] particles, float[] rootDistance, boolean[] collider) {
			return new ClothPartDefinition(partName, constraints, constraintTypes, compliances, particles, rootDistance, collider);
		}
		
		@Override
		public Supplier<OpenMatrix4f> getModelPartAnimationProvider() {
			return null;
		}
		
		@OnlyIn(Dist.CLIENT)
		public enum ConstraintType {
			DISTANCE, VOLUME
		}
	}
	
	public float[] normals() {
		return this.normals;
	}
	
	public float[] uvs() {
		return this.uvs;
	}
	
	@OnlyIn(Dist.CLIENT)
	public class ClothPart extends ModelPart<VertexBuilder> {
		final List<int[]> constraints;
		final ClothPartDefinition.ConstraintType[] constraintTypes;
		final float[] compliances;
		final int[] particles;
		final float[] rootDistance;
		
		final boolean[] collider;
		
		public ClothPart(List<VertexBuilder> verticies, List<int[]> constraints, ClothPartDefinition.ConstraintType[] constraintTypes, float[] compliances, int[] particles, float[] rootDistance, boolean[] collider) {
			super(verticies, null);
			
			this.constraints = constraints;
			this.constraintTypes = constraintTypes;
			this.compliances = compliances;
			this.particles = particles;
			this.rootDistance = rootDistance;
			this.collider = collider;
		}
		
		public List<int[]> constraints() {
			return this.constraints;
		}
		
		public ClothPartDefinition.ConstraintType[] constraintTypes() {
			return this.constraintTypes;
		}
		
		public float[] compliances() {
			return this.compliances;
		}
		
		public int[] particles() {
			return this.particles;
		}
		
		public float[] rootDistance() {
			return this.rootDistance;
		}
		
		public boolean[] collider() {
			return this.collider;
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
			
			for (VertexBuilder vi : this.getVertices()) {
				int pos = vi.position * 3;
				int norm = vi.normal * 3;
				int uv = vi.uv * 2;
				Vector4f posVec = matrix4f.transform(new Vector4f(positions[pos], positions[pos + 1], positions[pos + 2], 1.0F));
				Vector3f normVec = matrix3f.transform(new Vector3f(normals[norm], normals[norm + 1], normals[norm + 2]));
				
				drawingFunction.draw(builder, posVec.x(), posVec.x(), posVec.z(), normVec.x(), normVec.y(), normVec.z(), packedLight, r, g, b, a, uvs[uv], uvs[uv + 1], overlay);
			}
			
			poseStack.popPose();
		}
	}
}
