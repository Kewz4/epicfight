package yesman.epicfight.api.client.model;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.api.client.model.ClothMesh.ClothPart;
import yesman.epicfight.api.client.physics.ClothSimulatable;
import yesman.epicfight.api.client.physics.ClothSimulator;
import yesman.epicfight.api.client.physics.ClothSimulator.ClothObject;
import yesman.epicfight.api.forgeevent.ModelBuildEvent;
import yesman.epicfight.api.physics.SimulatableObject;
import yesman.epicfight.api.physics.SimulationProvider;
import yesman.epicfight.api.physics.SimulationTypes;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.main.EpicFightMod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EpicFightMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClothMesh extends Mesh<ClothPart, VertexBuilder> implements SimulationProvider<ClothSimulatable, ClothSimulator.ClothObject, ClothSimulator.ClothObjectBuilder> {
	private static final List<Pair<ClothSimulatable, ClothMesh>> TRACKING_SIMULATORS = Lists.newArrayList();
	
	@SubscribeEvent
	public static void onReload(ModelBuildEvent.MeshBuild event) {
		for (Pair<ClothSimulatable, ClothMesh> pair : TRACKING_SIMULATORS) {
			if (!pair.getFirst().valid()) {
				continue;
			}
			
			((SimulatableObject)pair.getFirst()).getSimulator(SimulationTypes.CLOTH).ifPresent((simulator) -> {
				ClothMesh newMesh = (ClothMesh)Meshes.getMeshOrNull(event.getOldRegistry().inverse().get(pair.getSecond()));
				
				simulator.restart(pair.getSecond(), newMesh);
			});
		}
		
		TRACKING_SIMULATORS.clear();
	}
	
	public final List<Vec3> positionList;
	public final List<Vec3> normalList;
	
	public ClothMesh(Map<String, float[]> arrayMap, Map<MeshPartDefinition, List<VertexBuilder>> partBuilders, @Nullable ClothMesh parent, RenderProperties properties) {
		super(arrayMap, partBuilders, null, properties);
		
		ImmutableList.Builder<Vec3> positionsBuilder = ImmutableList.builder();
		
		for (int i = 0; i < this.positions.length / 3; i++) {
			positionsBuilder.add(new Vec3(this.positions[i * 3], this.positions[i * 3 + 1], this.positions[i * 3 + 2]));
		}
		
		this.positionList = positionsBuilder.build();
		
		ImmutableList.Builder<Vec3> normalBuilder = ImmutableList.builder();
		
		for (int i = 0; i < this.normals.length / 3; i++) {
			normalBuilder.add(new Vec3(this.normals[i * 3], this.normals[i * 3 + 1], this.normals[i * 3 + 2]));
		}
		
		this.normalList = normalBuilder.build();
	}
	
	@Override
	protected Map<String, ClothPart> createModelPart(Map<MeshPartDefinition, List<VertexBuilder>> partBuilders) {
		Map<String, ClothPart> parts = Maps.newHashMap();
		
		partBuilders.forEach((partDefinition, vertexBuilder) -> {
			ClothPartDefinition clothDefinition = (ClothPartDefinition)partDefinition;
			
			parts.put(partDefinition.partName(), new ClothPart(vertexBuilder, clothDefinition.constraints(), clothDefinition.constraintTypes(), clothDefinition.compliances(), clothDefinition.particles(), clothDefinition.rootDistance()));
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
		TRACKING_SIMULATORS.add(Pair.of(simObject, this));
		return new ClothObject(builder, this, this.parts, this.positions);
	}
	
	@OnlyIn(Dist.CLIENT)
	public record ClothPartDefinition(String partName, List<int[]> constraints, ConstraintType[] constraintTypes, float[] compliances, int[] particles, float[] rootDistance) implements MeshPartDefinition {
		public static ClothPartDefinition of(String partName, List<int[]> constraints, ConstraintType[] constraintTypes, float[] compliances, int[] particles, float[] rootDistance) {
			return new ClothPartDefinition(partName, constraints, constraintTypes, compliances, particles, rootDistance);
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
	
	public float[] positions() {
		return this.positions;
	}
	
	public float[] normals() {
		return super.normals;
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
		
		public ClothPart(List<VertexBuilder> verticies, List<int[]> constraints, ClothPartDefinition.ConstraintType[] constraintTypes, float[] compliances, int[] particles, float[] rootDistance) {
			super(verticies, null);
			
			this.constraints = constraints;
			this.constraintTypes = constraintTypes;
			this.compliances = compliances;
			this.particles = particles;
			this.rootDistance = rootDistance;
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
