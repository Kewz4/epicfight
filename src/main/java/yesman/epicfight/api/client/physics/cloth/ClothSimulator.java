package yesman.epicfight.api.client.physics.cloth;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.ClassicMesh.ClassicMeshPart;
import yesman.epicfight.api.client.model.ClassicMeshVertexBuilder;
import yesman.epicfight.api.client.model.CompositeMesh;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.MeshPart;
import yesman.epicfight.api.client.model.SkinnedMesh.SkinnedMeshPart;
import yesman.epicfight.api.client.model.SkinnedMeshVertexBuilder;
import yesman.epicfight.api.client.model.SoftBodyTranslatable;
import yesman.epicfight.api.client.model.StaticMesh;
import yesman.epicfight.api.client.model.VertexBuilder;
import yesman.epicfight.api.client.physics.AbstractSimulator;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator.ClothObjectBuilder;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.physics.SimulationObject;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.renderer.shader.ShaderParser;
import yesman.epicfight.main.EpicFightMod;

/**
 * Referred to Matthias Müller's Ten minuates physics tutorial video number 14, 15
 * 
 * https://matthias-research.github.io/pages/tenMinutePhysics/index.html
 * 
 * https://www.youtube.com/@TenMinutePhysics
 **/
@OnlyIn(Dist.CLIENT)
public class ClothSimulator extends AbstractSimulator<ClothObjectBuilder, SoftBodyTranslatable, ClothSimulatable, ClothSimulator.ClothObject> {
	public static final ResourceLocation PLAYER_CLOAK = ResourceLocation.tryBuild(EpicFightMod.MODID, "ingame_cloak");
	public static final ResourceLocation MODELPREVIEWER_CLOAK = ResourceLocation.tryBuild(EpicFightMod.MODID, "previewer_cloak");
	
	private static final float PARTICLE_MASS = 0.01F;
	private static final float SPACING = 0.05F;
	private static float SELF_COLLISION = 0.025F;
	
	@OnlyIn(Dist.CLIENT)
	public static class ClothObjectBuilder extends SimulationObject.SimulationObjectBuilder {
		List<Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider>> clothColliders = Lists.newArrayList();
		
		public ClothObjectBuilder addEntry(Function<ClothSimulatable, OpenMatrix4f> obbTransformer, ClothOBBCollider clothOBBCollider) {
			this.clothColliders.add(Pair.of(obbTransformer, clothOBBCollider));
			return this;
		}
		
		public ClothObjectBuilder putAll(List<Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider>> clothOBBColliders) {
			this.clothColliders.addAll(clothOBBColliders);
			return this;
		}
		
		public static ClothObjectBuilder create() {
			return new ClothObjectBuilder();
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class ClothObject implements SimulationObject<ClothObjectBuilder, SoftBodyTranslatable, ClothSimulatable>, Mesh {
		private final SoftBodyTranslatable provider;
		private final Map<String, ClothPart> parts;
		private final List<Map<Integer, Vec3f>> poseNormals;
		
		@Nullable
		protected List<Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider>> clothColliders;
		
		//Storage vectors
		private static final Vec3f TRASNFORMED = new Vec3f();
		private static final Vector3f NORMAL = new Vector3f();
		
		public ClothObject(ClothObjectBuilder builder, SoftBodyTranslatable provider, Map<String, MeshPart<VertexBuilder<StaticMesh<?, ?>>>> parts, float[] positions) {
			this.provider = provider;
			this.clothColliders = builder.clothColliders;
			this.poseNormals = Lists.newArrayList();
			
			for (int i = 0; i < positions.length / 3; i++) {
				this.poseNormals.add(Maps.newHashMap());
			}
			
			for (MeshPart<VertexBuilder<StaticMesh<?, ?>>> meshPart : parts.values()) {
				for (VertexBuilder<StaticMesh<?, ?>> vb : meshPart.getVertices()) {
					Map <Integer, Vec3f> map = this.poseNormals.get(vb.position);
					
					if (!map.containsKey(vb.normal)) {
						vb.getVertexNormal(provider.getSoftBodyMesh(), NORMAL);
						map.put(vb.normal, new Vec3f(NORMAL.x, NORMAL.y, NORMAL.z));
					}
				}
			}
			
			ImmutableMap.Builder<String, ClothPart> partBuilder = ImmutableMap.builder();
			
			for (Map.Entry<String, MeshPart<VertexBuilder<StaticMesh<?, ?>>>> entry : parts.entrySet()) {
				partBuilder.put(entry.getKey(), new ClothPart(entry.getKey(), entry.getValue(), positions));
			}
			
			this.parts = partBuilder.build();
		}

		@Override
		public SoftBodyTranslatable getProvider() {
			return this.provider;
		}
		
		private static /*final*/ int SUB_STEPS = 3;
		private static final float YROT_LIMIT = 5.0F;
		private static final double MAX_VELOCITY_FORCE = 6.0D;
		private static final Vec3f SCALED_CENTRIFUGAL_FORCE = new Vec3f();
		private static final Vec3f OFFSET = new Vec3f();
		private static final OpenMatrix4f[] BOUND_ANIMATION_TRANSFORM = OpenMatrix4f.allocateMatrixArray(ShaderParser.MAX_JOINTS);
		private static final OpenMatrix4f COLLIDER_TRANSFORM = new OpenMatrix4f();
		private final Vec3f centrifugalForceDirection = new Vec3f();
		
		/**
		 * This method needs be called before drawing simulated cloth
		 */
		public void tick(ClothSimulatable simulatableObj, Function<Float, OpenMatrix4f> colliderTransform, float partialTick, @Nullable Armature armature, @Nullable OpenMatrix4f[] poses) {
			boolean skinned = poses != null && armature != null;
			
			for (int j = 0; j < ShaderParser.MAX_JOINTS; j++) {
				if (j >= armature.getJointNumber()) {
					break;
				}
				
				if (skinned) {
					BOUND_ANIMATION_TRANSFORM[j].load(poses[j]);
					BOUND_ANIMATION_TRANSFORM[j].mulBack(armature.searchJointById(j).getToOrigin());
				}
			}
			
			float deltaFrameTime = Minecraft.getInstance().getDeltaFrameTime();
			float yRotO = simulatableObj.getYRotO();
			float yRot = simulatableObj.getYRot();
			float deltaYRot = Mth.clamp(yRot - yRotO, -YROT_LIMIT, YROT_LIMIT) * 0.75F * deltaFrameTime;
			Vec3 velocity = simulatableObj.getObjectVelocity();
			double centrifugalForce = deltaYRot * deltaYRot * Mth.clamp(velocity.length() / 0.2D, 1.0D, MAX_VELOCITY_FORCE) * PARTICLE_MASS * (0.16F / deltaFrameTime);
			
			this.centrifugalForceDirection.add(Vec3f.fromDoubleVector(MathUtils.getVectorForRotation(0.0F, yRotO - 180.0F)
									                       .add(MathUtils.getVectorForRotation(0.0F, yRotO + (deltaYRot > 0.0F ? -90.0F : 90.0F)))
									                       .scale(centrifugalForce)
														   .add(velocity.scale(centrifugalForce * deltaFrameTime))
			));
			
			// Reset normal vectors
			this.poseNormals.forEach((poseNormals) -> poseNormals.values().forEach((vec3f) -> vec3f.set(0.0F, 0.0F, 0.0F)));
			
			SELF_COLLISION = 0.015F;
			SUB_STEPS = 3;
			
			float gravity = simulatableObj.getGravity();
			float subStebInvert = 1.0F / SUB_STEPS;
			float subSteppingDeltaTime = deltaFrameTime * subStebInvert;
			
			for (int i = 0; i < SUB_STEPS; i++) {
				float substepPartialTick = partialTick - deltaFrameTime + subSteppingDeltaTime * (i + 1);
				
				if (this.clothColliders != null) {
					OpenMatrix4f colliderMatrix = colliderTransform.apply(substepPartialTick);
					
					for (Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider> entry : this.clothColliders) {
						entry.getSecond().transform(OpenMatrix4f.mul(colliderMatrix, entry.getFirst().apply(simulatableObj), COLLIDER_TRANSFORM));
					}
				}
				
				Vec3 pos = simulatableObj.getAccuratePartialLocation(substepPartialTick);
				float yRotLerp = simulatableObj.getAccurateYRot(substepPartialTick);
				
				OpenMatrix4f clothRootMatrix = OpenMatrix4f.createTranslation((float)pos.x, (float)pos.y, (float)pos.z)
														   .rotateDeg(180.0F - yRotLerp, Vec3f.Y_AXIS);
				
				Vec3f.scale(this.centrifugalForceDirection, SCALED_CENTRIFUGAL_FORCE, subStebInvert);
				
				for (ClothPart part : this.parts.values()) {
					part.tick(SCALED_CENTRIFUGAL_FORCE, gravity, subSteppingDeltaTime, i + 1, clothRootMatrix, this.clothColliders, skinned ? BOUND_ANIMATION_TRANSFORM : null);
				}
			}
			
			this.centrifugalForceDirection.scale(1.0F - deltaFrameTime * 0.5F);
			
			// Update normals & offset particles
			for (ClothPart part : this.parts.values()) {
				part.updateNormal(false);
				
				if (!part.normalOffsetParticles.isEmpty()) {
					for (ClothPart.OffsetParticle offsetParticle : part.normalOffsetParticles.values()) {
						// Update offset positions
						ClothPart.Particle rootParticle = offsetParticle.rootParticle();
						Map<Integer, Vec3f> rootNormalMap = this.poseNormals.get(rootParticle.meshVertexId);
						OFFSET.set(0.0F, 0.0F, 0.0F);
						
						for (Integer normIdx : offsetParticle.positionNormalMembers()) {
							OFFSET.add(rootNormalMap.get(normIdx));
						}
						
						OFFSET.scale(offsetParticle.length / OFFSET.length());
						
						offsetParticle.position.set(
							  rootParticle.position.x - OFFSET.x
							, rootParticle.position.y - OFFSET.y
							, rootParticle.position.z - OFFSET.z
						);
					}
				}
				
				part.updateNormal(true);
			}
		}
		
		@Override
		public void draw(PoseStack poseStack, VertexConsumer vertexConsumer, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
			for (ClothPart part : this.parts.values()) {
				part.draw(poseStack, vertexConsumer, drawingFunction, packedLight, r, g, b, a, overlay);
			}
			
			if (this.provider instanceof CompositeMesh compositeMesh) {
				poseStack.popPose();
				compositeMesh.getStaticMesh().draw(poseStack, vertexConsumer, drawingFunction, packedLight, 1.0F, 1.0F, 1.0F, 1.0F, overlay);
				poseStack.pushPose();
			}
		}
		
		@Override
		public void drawPosed(PoseStack poseStack, VertexConsumer vertexConsumer, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay, Armature armature, OpenMatrix4f[] poses) {
			for (ClothPart part : this.parts.values()) {
				part.draw(poseStack, vertexConsumer, drawingFunction, packedLight, r, g, b, a, overlay);
			}
			
			/**
			Debug cloth collider
			if (this.clothColliders != null) {
				for (Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider> entry : this.clothColliders) {
					entry.getSecond().draw(poseStack, Minecraft.getInstance().renderBuffers().bufferSource(), 0xFFFFFFFF);
				}
			}
			**/
			
			// Remove entity pos translation
			poseStack.popPose();
			
			if (this.provider instanceof CompositeMesh compositeMesh) {
				//compositeMesh.getStaticMesh().drawPosed(poseStack, vertexConsumer, drawingFunction, packedLight, 1.0F, 1.0F, 1.0F, 1.0F, overlay, armature, poses);
			}
			
			poseStack.pushPose();
		}
		
		@Override
		public void initialize() {
		}
		
		@OnlyIn(Dist.CLIENT)
		public class ClothPart {
			final String name;
			final Map<Integer, Particle> particles;
			final Map<Integer, ClothPart.OffsetParticle> normalOffsetParticles;
			
			final List<ConstraintList> constraints;
			final Multimap<Integer, Particle> spatialHash;
			final int hashTableSize;
			
			// Storage vector
			private static final Vec3f VEC3F = new Vec3f();
			private static final Vector4f POSITION = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
			private static final Vector3f NORMAL = new Vector3f();
			private static final Vec3f AVERAGE = new Vec3f();
			
			ClothPart(String name, MeshPart<?> clothPart, float[] positions) {
				this.name = name;
				this.particles = Maps.newLinkedHashMap();
				//this.normalOffsetConnections = Maps.newHashMap();
				this.normalOffsetParticles = Maps.newHashMap();
				
				for (VertexBuilder<?> vb : clothPart.getVertices()) {
					Map<Integer, Vec3f> positionNormals = poseNormals.get(vb.position);
					
					if (!positionNormals.containsKey(vb.normal)) {
						positionNormals.put(vb.normal, new Vec3f());
					}
				}
				
				ImmutableList.Builder<ConstraintList> constraintsBuilder = ImmutableList.builder();
				SoftBodyTranslatable.ClothSimulationInfo clothInfo = clothPart.getClothInfo();
				
				/**
				 * Add particles
				 */
				for (int i = 0; i < clothInfo.particles().length / 2; i++) {
					int positionIndex = clothInfo.particles()[i * 2];
					int weightIndex = clothInfo.particles()[i * 2 + 1];
					float influence = clothInfo.weights()[weightIndex];
					float rootDistance = clothInfo.rootDistance()[i];
					
					VertexBuilder<?> vb = null;
					
					if (clothPart instanceof ClassicMeshPart) {
						vb = new ClassicMeshVertexBuilder(positionIndex, -1, -1);
					} else if (clothPart instanceof SkinnedMeshPart) {
						vb = new SkinnedMeshVertexBuilder(positionIndex, -1, -1);
					}
					
					float x = positions[positionIndex * 3];
					float y = positions[positionIndex * 3 + 1];
					float z = positions[positionIndex * 3 + 2];
					
					@SuppressWarnings("unchecked")
					Particle particle = new Particle((VertexBuilder<StaticMesh<?, ?>>)vb, new Vec3f(x, y, z), influence, rootDistance, positionIndex);
					this.particles.put(positionIndex, particle);
				}
				
				this.hashTableSize = this.particles.size() * 2;
				this.spatialHash = HashMultimap.create(this.hashTableSize, 2);
				int idx = 0;
				
				/**
				 * Add constraints
				 */
				for (int[] constraints : clothInfo.constraints()) {
					float compliance = clothInfo.compliances()[idx];
					SoftBodyTranslatable.ConstraintType constraintType = clothInfo.constraintTypes()[idx];
					List<Constraint> constraintList;
					idx++;
					
					switch(constraintType) {
					case STRETCHING -> {
						constraintList = new ArrayList<> (constraints.length / 2);
						
						for (int i = 0; i < constraints.length / 2; i++) {
							int idx1 = constraints[i * 2];
							int idx2 = constraints[i * 2 + 1];
							
							constraintList.add(new StretchConstraint(this.particles.get(idx1), this.particles.get(idx2)));
						}
						
						constraintsBuilder.add(new ConstraintList(compliance, constraintList));
					}
					case SHAPING -> {
						constraintList = new ArrayList<> (constraints.length / 2);
						
						for (int i = 0; i < constraints.length / 2; i++) {
							int idx1 = constraints[i * 2];
							int idx2 = constraints[i * 2 + 1];
							
							constraintList.add(new ShapingConstraint(this.particles.get(idx1), this.particles.get(idx2)));
						}
						
						constraintsBuilder.add(new ConstraintList(compliance, constraintList));
					}
					case BENDING -> {
						constraintList = new ArrayList<> (constraints.length / 4);
						
						for (int i = 0; i < constraints.length / 4; i++) {
							int idx1 = constraints[i * 4];
							int idx2 = constraints[i * 4 + 1];
							int idx3 = constraints[i * 4 + 2];
							int idx4 = constraints[i * 4 + 3];
							
							constraintList.add(new BendingConstraint(this.particles.get(idx1), this.particles.get(idx2), this.particles.get(idx3), this.particles.get(idx4)));
						}
						
						constraintsBuilder.add(new ConstraintList(compliance, constraintList));
					}
					case VOLUME -> {
						constraintList = new ArrayList<> (constraints.length / 4);
						
						for (int i = 0; i < constraints.length / 4; i++) {
							int idx1 = constraints[i * 4];
							int idx2 = constraints[i * 4 + 1];
							int idx3 = constraints[i * 4 + 2];
							int idx4 = constraints[i * 4 + 3];
							
							constraintList.add(new VolumeConstraint(this.particles.get(idx1), this.particles.get(idx2), this.particles.get(idx3), this.particles.get(idx4)));
						}
						
						constraintsBuilder.add(new ConstraintList(compliance, constraintList));
					}
					}
				}
				
				this.constraints = constraintsBuilder.build();
				
				/**
				 * Setup normal offsets
				 */
				if (clothInfo.normalOffsetMapping() != null) {
					for (int i = 0; i < clothInfo.normalOffsetMapping().length / 2; i++) {
						int rootParticle = clothInfo.normalOffsetMapping()[i * 2];
						int extendingParticle = clothInfo.normalOffsetMapping()[i * 2 + 1];
						Vec3f offsetDirection = new Vec3f( positions[extendingParticle * 3] - positions[rootParticle * 3]
														 , positions[extendingParticle * 3 + 1] - positions[rootParticle * 3 + 1]
														 , positions[extendingParticle * 3 + 2] - positions[rootParticle * 3 + 2]);
						
						List<Integer> positionNormalMembers = Lists.newArrayList();
						List<Integer> inverseNormals = Lists.newArrayList();
						OffsetParticle offsetParticle = new OffsetParticle(extendingParticle, offsetDirection.length(), this.particles.get(rootParticle), new Vec3f(), positionNormalMembers, inverseNormals);
						offsetDirection.normalize();
						
						Map<Integer, Vec3f> rootNormalMap = poseNormals.get(rootParticle);
						List<Vec3f> rootNormals = new ArrayList<> (rootNormalMap.values());
						List<Set<Integer>> normalSubsets = new ArrayList<> (MathUtils.getSubset(IntStream.rangeClosed(0, rootNormals.size() - 1).boxed().toList()));
						int candidate = -1;
						int loopIdx = 0;
						float maxDot = -10000.0F;
						
						for (Set<Integer> subset : normalSubsets) {
							Set<Vec3f> rootNormal = subset.stream().map((normIdx) -> rootNormals.get(normIdx)).collect(Collectors.toSet());
							Vec3f.average(rootNormal, AVERAGE);
							AVERAGE.scale(-1.0F);
							
							float dot = Vec3f.dot(offsetDirection, AVERAGE);
							
							if (maxDot < dot) {
								maxDot = dot;
								candidate = loopIdx;
							}
							
							loopIdx++;
						}
						
						normalSubsets.get(candidate).forEach((orderIdx) -> {
							int iterCount = 0;
							Iterator<Map.Entry<Integer, Vec3f>> iter = rootNormalMap.entrySet().iterator();
							
							while (iter.hasNext()) {
								Map.Entry<Integer, Vec3f> entry = iter.next();
								
								if (orderIdx == iterCount) {
									positionNormalMembers.add(entry.getKey());
									break;
								}
								
								iterCount++;
							}
						});
						
						this.normalOffsetParticles.put(extendingParticle, offsetParticle);
						
						for (Vec3f normal : poseNormals.get(extendingParticle).values()) {
							int leastDotIdx = MathUtils.getLeastAngleVectorIdx(normal, rootNormals.toArray(new Vec3f[0]));
							int iterCount = 0;
							Iterator<Map.Entry<Integer, Vec3f>> iter = rootNormalMap.entrySet().iterator();
							
							while (iter.hasNext()) {
								Map.Entry<Integer, Vec3f> entry = iter.next();
								
								if (leastDotIdx == iterCount) {
									inverseNormals.add(entry.getKey());
									break;
								}
								
								iterCount++;
							}
						}
					}
				}
			}
			
			public void buildSpatialHash() {
				this.spatialHash.clear();
				
				// Create spatial hash map
				for (Particle p : this.particles.values()) {
					int hash = this.getHash(p.position.x, p.position.y, p.position.z);
					this.spatialHash.put(hash, p);
				}
			}
			
			public void tick(Vec3f centrifugalForce, float gravity, float deltaTime, int stepCount, OpenMatrix4f clothRootTransform, List<Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider>> clothColliders, @Nullable OpenMatrix4f[] poses) {
				boolean paused = Minecraft.getInstance().isPaused();
				
				for (Particle p : this.particles.values()) {
					if (stepCount == 1) {
						p.collided = false;
					}
					
					float influenceInv = 1.0F - p.influence;
					
					// Apply animation transform
					if (influenceInv > 0.0) {
						p.vertexBuilder.getVertexPosition(ClothObject.this.provider.getSoftBodyMesh(), POSITION, poses);
						VEC3F.set(POSITION.x, POSITION.y, POSITION.z);
						OpenMatrix4f.transform3v(clothRootTransform, VEC3F, TRASNFORMED);
						Vec3f.interpolate(p.position, TRASNFORMED, influenceInv, TRASNFORMED);
						p.position.set(TRASNFORMED);
					}
					
					if (!paused) {
						p.position.y -= gravity * PARTICLE_MASS * p.influence * deltaTime;
						p.position.add(centrifugalForce.x * p.rootDistance * p.influence, centrifugalForce.y * p.rootDistance * p.influence, centrifugalForce.z * p.rootDistance * p.influence);
					}
				}
				
				if (paused) {
					return;
				}
				
				for (ConstraintList constraintsBundle : this.constraints) {
					for (Constraint c : constraintsBundle.constraints()) {
						float alpha = constraintsBundle.compliance() / (deltaTime * deltaTime);
						c.solve(alpha, stepCount);
					}
				}
				
				// Detect collision with mesh collider
				if (clothColliders != null) {
					for (Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider> entry : clothColliders) {
						for (Particle p : this.particles.values()) {
							// Exclude root particles
							if (p.influence == 0.0F) {
								continue;
							}
							
							Vec3 particlePos = p.position.toDoubleVector();
							Vec3 pushedPoint = entry.getSecond().getCollidePoint(particlePos, p.position.toDoubleVector());
							
							if (!particlePos.equals(pushedPoint)) {
								p.collided = true;
								p.position.set(pushedPoint);
							}
						}
					}
				}
				
				// Detect self collision
				for (Particle p1 : this.particles.values()) {
					int hash = this.getHash(p1.position.x, p1.position.y, p1.position.z);
					
					for (Particle p2 : this.spatialHash.get(hash)) {
						if (p1 == p2) {
							continue;
						}
						
						Vec3f.sub(p1.position, p2.position, VEC3F);
						float length = VEC3F.length();
						
						if (length < SELF_COLLISION) {
							float influenceSum = p1.influence + p2.influence;
							float scale = (SELF_COLLISION - length) / SELF_COLLISION;
							float p1Move = p1.influence / influenceSum;
							float p2Move = p2.influence / influenceSum;
							VEC3F.scale(scale);
							
							p1.position.add(VEC3F.x * p1Move, VEC3F.y * p1Move, VEC3F.z * p1Move);
							p2.position.sub(VEC3F.x * p2Move, VEC3F.y * p2Move, VEC3F.z * p2Move);
						}
					}
				}
			}
			
			private int getHash(double x, double y, double z) {
				int xi = (int)Math.floor(x / SPACING);
				int yi = (int)Math.floor(y / SPACING);
				int zi = (int)Math.floor(z / SPACING);
				int hash = (xi * 92837111) ^ (yi * 689287499) ^ (zi * 283923481);
				
				return Math.abs(hash) % this.hashTableSize;
			}
			
			public Vec3f getParticlePosition(int idx) {
				if (this.particles.containsKey(idx)) {
					return this.particles.get(idx).position;
				} else {
					return this.normalOffsetParticles.get(idx).position;
				}
			}
			
			private static final Vec3f TO_P2 = new Vec3f();
			private static final Vec3f TO_P3 = new Vec3f();
			private static final Vec3f CROSS = new Vec3f();
			
			// Calculate vertex normals
			private void updateNormal(boolean updateOffsetParticles) {
				SoftBodyTranslatable softBodyMesh = ClothObject.this.provider;
				MeshPart<?> modelPart = softBodyMesh.getSoftBodyMesh().getPart(this.name);
				
				for (int i = 0; i < modelPart.getVertices().size() / 3; i++) {
					VertexBuilder<?> triP1 = modelPart.getVertices().get(i * 3);
					VertexBuilder<?> triP2 = modelPart.getVertices().get(i * 3 + 1);
					VertexBuilder<?> triP3 = modelPart.getVertices().get(i * 3 + 2);
					
					if (!this.particles.containsKey(triP1.position) || !this.particles.containsKey(triP2.position) || !this.particles.containsKey(triP3.position)) {
						if (!updateOffsetParticles) {
							continue;
						}
					} else {
						if (updateOffsetParticles) {
							continue;
						}
					}
					
					Vec3f p1Pos = this.getParticlePosition(triP1.position);
					Vec3f p2Pos = this.getParticlePosition(triP2.position);
					Vec3f p3Pos = this.getParticlePosition(triP3.position);
					
					Vec3f.cross(Vec3f.sub(p2Pos, p1Pos, TO_P2), Vec3f.sub(p3Pos, p1Pos, TO_P3), CROSS);
					CROSS.normalize();
					
					Map<Integer, Vec3f> triP1Normals = poseNormals.get(triP1.position);
					Map<Integer, Vec3f> triP2Normals = poseNormals.get(triP2.position);
					Map<Integer, Vec3f> triP3Normals = poseNormals.get(triP3.position);
					
					triP1Normals.get(triP1.normal).add(CROSS);
					triP2Normals.get(triP2.normal).add(CROSS);
					triP3Normals.get(triP3.normal).add(CROSS);
				}
			}
			
			public void draw(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
				SoftBodyTranslatable softBodyMesh = ClothObject.this.provider;
				MeshPart<?> modelPart = softBodyMesh.getSoftBodyMesh().getPart(this.name);
				
				if (modelPart.isHidden()) {
					return;
				}
				
				Matrix4f matrix4f = poseStack.last().pose();
				Matrix3f matrix3f = poseStack.last().normal();
				float[] uvs = softBodyMesh.getSoftBodyMesh().uvs();
				
				for (int i = 0; i < modelPart.getVertices().size(); i++) {
					/**
					Enable when testing normal offset
					if (i % 3 == 0) {
						if (i + 1 == modelPart.getVertices().size() || i + 2 == modelPart.getVertices().size()) {
							
						} else {
							VertexBuilder<?> v1 = modelPart.getVertices().get(i);
							VertexBuilder<?> v2 = modelPart.getVertices().get(i + 1);
							VertexBuilder<?> v3 = modelPart.getVertices().get(i + 2);
							
							if (!Minecraft.getInstance().player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty() && (this.particles.get(v1.position) == null || this.particles.get(v2.position) == null || this.particles.get(v3.position) == null)) {
								i += 2;
								continue;
							}
						}
					}
					**/
					
					VertexBuilder<?> vb = modelPart.getVertices().get(i);
					Vec3f particlePosition = this.getParticlePosition(vb.position);
					Vec3f poseNormal = poseNormals.get(vb.position).get(vb.normal);
					poseNormal.normalize();
					
					POSITION.set(particlePosition.x, particlePosition.y, particlePosition.z);
					NORMAL.set(poseNormal.x, poseNormal.y, poseNormal.z);
					POSITION.mul(matrix4f);
					NORMAL.mul(matrix3f);
					
					drawingFunction.draw(builder, POSITION.x, POSITION.y, POSITION.z, NORMAL.x(), NORMAL.y(), NORMAL.z(), packedLight, r, g, b, a, uvs[vb.uv * 2], uvs[vb.uv * 2 + 1], overlay);
				}
				
				/**
				for (VertexBuilder<?> vi : modelPart.getVertices()) {
					Particle particle = this.particles.get(vi.position);
					
					if (particle == null) {
						OffsetParticle offsetParticle = this.normalOffsetParticles.get(vi.position);
						Particle rootParticle = offsetParticle.rootParticle();
						Map<Integer, Vec3f> rootNormalMap = poseNormals.get(rootParticle.meshVertexId);
						
						OFFSET.set(0.0F, 0.0F, 0.0F);
						
						for (Integer i : offsetParticle.positionNormalMembers()) {
							OFFSET.add(rootNormalMap.get(i));
						}
						
						OFFSET.normalize();
						OFFSET.scale(offsetParticle.length);
						
						POSITION.set(
							  rootParticle.position.x + OFFSET.x
							, rootParticle.position.y + OFFSET.y
							, rootParticle.position.z + OFFSET.z
						);
					} else {
						POSITION.set(particle.position.x, particle.position.y, particle.position.z);
					}
					
					Vec3f poseNormal = poseNormals.get(vi.position).get(vi.normal);
					poseNormal.normalize();
					
					NORMAL.set(poseNormal.x, poseNormal.y, poseNormal.z);
					POSITION.mul(matrix4f);
					NORMAL.mul(matrix3f);
					
					drawingFunction.draw(builder, POSITION.x, POSITION.y, POSITION.z, NORMAL.x(), NORMAL.y(), NORMAL.z(), packedLight, r, g, b, a, uvs[vi.uv * 2], uvs[vi.uv * 2 + 1], overlay);
				}**/
			}
			
			@OnlyIn(Dist.CLIENT)
			class Particle {
				final VertexBuilder<StaticMesh<?, ?>> vertexBuilder;
				final Vec3f position;
				final float influence;
				final float rootDistance;
				final int meshVertexId;
				
				boolean collided;
				
				Particle(VertexBuilder<StaticMesh<?, ?>> vertexBuilder, Vec3f position, float influence, float rootDistance, int meshVertexId) {
					this.vertexBuilder = vertexBuilder;
					this.position = position;
					this.influence = influence;
					this.rootDistance = rootDistance;
					this.meshVertexId = meshVertexId;
					
					this.collided = false;
				}
			}
			
			@OnlyIn(Dist.CLIENT)
			public static record ConstraintList(float compliance, List<Constraint> constraints) {
			}
			
			@OnlyIn(Dist.CLIENT)
			public static record OffsetParticle(int offsetVertexId, float length, Particle rootParticle, Vec3f position, List<Integer> positionNormalMembers, List<Integer> inverseNormal) {
			}
			
			@OnlyIn(Dist.CLIENT)
			abstract class Constraint {
				abstract void solve(float alpha, int stepcount);
			}
			
			/**
			 * A constraint that restricts stretching of two particles
			 */
			@OnlyIn(Dist.CLIENT)
			class StretchConstraint extends Constraint {
				final Particle p1;
				final Particle p2;
				final float restLength;
				
				// Storage vector
				static final Vec3f GRADIENT = new Vec3f();
				
				StretchConstraint(Particle p1, Particle p2) {
					this.p1 = p1;
					this.p2 = p2;
					this.restLength = p1.position.distance(p2.position);
				}
				
				@Override
				void solve(float alpha, int stepcount) {
				    float influenceSum = this.p1.influence + this.p2.influence;
				    
				    if (influenceSum < 1E-8) {
				        return;
				    }
				    
				    Vec3f.sub(this.p2.position, this.p1.position, GRADIENT);
				    float currentLength = GRADIENT.length();
				    
				    if (currentLength  < 1E-8) {
				        return;
				    }
				    
				    // Normalize
				    GRADIENT.scale(1.0F / currentLength);
				    
				    float constraint = currentLength - this.restLength;
				    float force = constraint / (influenceSum + alpha);
				    float p1Move = force * this.p1.influence;
				    float p2Move = -force * this.p2.influence;
				    
				    this.p1.position.add(GRADIENT.x * p1Move, GRADIENT.y * p1Move, GRADIENT.z * p1Move);
				    this.p2.position.add(GRADIENT.x * p2Move, GRADIENT.y * p2Move, GRADIENT.z * p2Move);
				}
			}
			
			/**
			 * A constraint that restricts stretching of two particles, and doesn't allow stretching over the rest length.
			 * 
			 * Be used to prevent streching too much in gravity direction in low fps
			 */
			@OnlyIn(Dist.CLIENT)
			class ShapingConstraint extends Constraint {
				final Particle p1;
				final Particle p2;
				final float restLength;
				
				// Storage vector
				static final Vec3f TOWARD = new Vec3f();
				
				ShapingConstraint(Particle p1, Particle p2) {
					this.p1 = p1;
					this.p2 = p2;
					this.restLength = p1.position.distance(p2.position);
				}
				
				@Override
				void solve(float alpha, int stepcount) {
					// remove the influence in the last substep
				    float p1Influence = stepcount == SUB_STEPS ? 0.0F : this.p1.influence;
				    float p2Influence = this.p2.influence;
				    float influenceSum = p1Influence + p2Influence;
				    
				    if (influenceSum < 1E-5) {
				        return;
				    }
				    
				    Vec3f.sub(this.p2.position, this.p1.position, TOWARD);
				    float distanceLength = TOWARD.length();
				    
				    if (distanceLength == 0.0F) {
				        return;
				    }
				    
				    //Normalize
				    TOWARD.scale(1.0F / distanceLength);
				    
				    float distanceGap = distanceLength - this.restLength;
				    float force = distanceGap / (influenceSum + alpha);
				    float p1Move = force * p1Influence * influenceSum;
				    float p2Move = -force * p2Influence * influenceSum;
				    
				    this.p1.position.add(TOWARD.x * p1Move, TOWARD.y * p1Move, TOWARD.z * p1Move);
				    this.p2.position.add(TOWARD.x * p2Move, TOWARD.y * p2Move, TOWARD.z * p2Move);
				}
			}
			
			/**
			 * A constraint that restricts bending of member particles. p2, p3 are adjacent edge particles, and p1, p4 are opponent each other
			 */
			@OnlyIn(Dist.CLIENT)
			class BendingConstraint extends Constraint {
				final Particle p1;
				final Particle p2;
				final Particle p3;
				final Particle p4;
				final float restAngle;
				final float oppositeDistance;
				
				// Storage vector
				static final Vec3f[] GRADIENTS = { new Vec3f(), new Vec3f(), new Vec3f(), new Vec3f() };
				static final Vec3f NORMAL_SUM = new Vec3f();
				
				BendingConstraint(Particle p1, Particle p2, Particle p3, Particle p4) {
					this.p1 = p1;
					this.p2 = p2;
					this.p3 = p3;
					this.p4 = p4;
					this.restAngle = this.getDihedralAngle();
					this.oppositeDistance = Vec3f.sub(this.p1.position, this.p4.position, null).lengthSqr(); 
				}
				
				@Override
				void solve(float alpha, int stepcount) {
				    float influenceSum = this.p1.influence + this.p2.influence + this.p3.influence + this.p4.influence;
				    
				    if (influenceSum < 1E-8) {
				        return;
				    }
				    
					float currentAngle = this.getDihedralAngle();
				    float constraint = (this.restAngle - currentAngle);
				    
				    while (constraint > Math.PI) {
				    	constraint -= Math.PI * 2;
				    }
				    
				    while (constraint < -Math.PI) {
				    	constraint += Math.PI * 2;
				    }
				    
				    // radian angle * diameter
				    constraint = this.oppositeDistance * constraint;
				    
				    float edgeLength = EDGE.length();
				    
				    CROSS1.scale(edgeLength);
				    CROSS2.scale(edgeLength);
				    GRADIENTS[0].set(CROSS1);
				    GRADIENTS[3].set(CROSS2);
				    
				    Vec3f.add(CROSS1, CROSS2, NORMAL_SUM);
				    NORMAL_SUM.scale(-0.5F);
				    GRADIENTS[1].set(NORMAL_SUM);
				    GRADIENTS[2].set(NORMAL_SUM);
				    
				    float weight = this.p1.influence * GRADIENTS[0].lengthSqr()
				    				+ this.p2.influence * GRADIENTS[1].lengthSqr()
				    				+ this.p3.influence * GRADIENTS[2].lengthSqr()
				    				+ this.p4.influence * GRADIENTS[3].lengthSqr();
				    
				    if (weight < 1E-8) {
						return;
					}
				    
				    float stiffness = 1.0F;
				    float force = (-constraint * stiffness) / (influenceSum + alpha);
				    
				    GRADIENTS[0].scale(force * this.p1.influence);
				    GRADIENTS[1].scale(force * this.p2.influence);
				    GRADIENTS[2].scale(force * this.p3.influence);
				    GRADIENTS[3].scale(force * this.p4.influence);
				    
				    Vec3f.add(this.p1.position, GRADIENTS[0], this.p1.position);
				    Vec3f.add(this.p2.position, GRADIENTS[1], this.p2.position);
				    Vec3f.add(this.p3.position, GRADIENTS[2], this.p3.position);
				    Vec3f.add(this.p4.position, GRADIENTS[3], this.p4.position);
				}
				
				static final Vec3f P2P1 = new Vec3f();
				static final Vec3f P3P1 = new Vec3f();
				static final Vec3f P4P2 = new Vec3f();
				static final Vec3f P4P3 = new Vec3f();
				static final Vec3f EDGE = new Vec3f();
				static final Vec3f EDGE_NORM = new Vec3f();
				
				static final Vec3f CROSS1 = new Vec3f();
				static final Vec3f CROSS2 = new Vec3f();
				static final Vec3f CROSS3 = new Vec3f();
				
				public float getDihedralAngle() {
					Vec3f.sub(this.p1.position, this.p2.position, P2P1);
					Vec3f.sub(this.p1.position, this.p3.position, P3P1);
					Vec3f.sub(this.p4.position, this.p2.position, P4P2);
					Vec3f.sub(this.p4.position, this.p3.position, P4P3);
					Vec3f.sub(this.p3.position, this.p2.position, EDGE);
					
					Vec3f.cross(P2P1, P3P1, CROSS1);
					Vec3f.cross(P4P3, P4P2, CROSS2);
					CROSS1.normalize();
					CROSS2.normalize();
					Vec3f.normalize(EDGE, EDGE_NORM);
					
					float cos = Vec3f.dot(CROSS1, CROSS2);
					float sin = Vec3f.dot(Vec3f.cross(CROSS1, CROSS2, CROSS3), EDGE_NORM);
					
					return (float)Math.atan2(sin, cos);
				}
			}
			
			/**
			 * A constraint that resists squashing of tetrahedral.
			 * 
			 * Note: This constraint is expensive. Consider using NormalMappedParticle instead.
			 */
			@OnlyIn(Dist.CLIENT)
			class VolumeConstraint extends Constraint {
				final Particle[] particles;
				final float restVolume;
				
				static final float SUBDIVISION = 1.0F / 6.0F;
				static final int[][] VOLUME_ORDER = { {1, 3, 2}, {0, 2, 3}, {0, 3, 1}, {0, 1, 2} };
				static final Vec3f[] SHRINK_DIRECTIONS = { new Vec3f(), new Vec3f(), new Vec3f(), new Vec3f() };
				
				// Storage vectors
				static final Vec3f P1_TO_P2 = new Vec3f();
				static final Vec3f P1_TO_P3 = new Vec3f();
				static final Vec3f P1_TO_P4 = new Vec3f();
				static final Vec3f TET_CROSS = new Vec3f();
				
				VolumeConstraint(Particle p1, Particle p2, Particle p3, Particle p4) {
					this.particles = new Particle[4];
					this.particles[0] = p1;
					this.particles[1] = p2;
					this.particles[2] = p3;
					this.particles[3] = p4;
					this.restVolume = this.getTetrahedralVolume();
				}

				@Override
				void solve(float alpha, int stepcount) {
					float weight = 0.0F;
					
					for (int i = 0; i < 4; i++) {
						Particle p1 = this.particles[VOLUME_ORDER[i][0]];
						Particle p2 = this.particles[VOLUME_ORDER[i][1]];
						Particle p3 = this.particles[VOLUME_ORDER[i][2]];
						
						Vec3f.sub(p2.position, p1.position, P1_TO_P2);
						Vec3f.sub(p3.position, p1.position, P1_TO_P3);
						Vec3f.cross(P1_TO_P2, P1_TO_P3, SHRINK_DIRECTIONS[i]);
						SHRINK_DIRECTIONS[i].scale(SUBDIVISION);
						
						weight += this.particles[i].influence * SHRINK_DIRECTIONS[i].lengthSqr();
					}
					
					if (weight < 1E-8) {
						return;
					}
					
					float constraint = this.restVolume - this.getTetrahedralVolume();
					float force = constraint / (weight + alpha);
					
					for (int i = 0; i < 4; i++) {
						SHRINK_DIRECTIONS[i].scale(force * this.particles[i].influence);
						Vec3f.add(this.particles[i].position, SHRINK_DIRECTIONS[i], this.particles[i].position);
					}
				}
				
				float getTetrahedralVolume() {
					Vec3f.sub(this.particles[1].position, this.particles[0].position, P1_TO_P2);
					Vec3f.sub(this.particles[2].position, this.particles[0].position, P1_TO_P3);
					Vec3f.sub(this.particles[3].position, this.particles[0].position, P1_TO_P4);
					Vec3f.cross(P1_TO_P2, P1_TO_P3, TET_CROSS);
					
					return Vec3f.dot(TET_CROSS, P1_TO_P4) / 6.0F;
				}
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class ClothOBBCollider extends OBBCollider {
		// 0: right, 1: top, 2: near, 3: left, 4: bottom, 5: far
		private final int[] allowedPlaneIds;
		
		public ClothOBBCollider(double vertexX, double vertexY, double vertexZ, double centerX, double centerY, double centerZ, int... allowedPlaneIds) {
			super(vertexX, vertexY, vertexZ, centerX, centerY, centerZ);
			
			this.allowedPlaneIds = allowedPlaneIds;
		}
		
		public Vec3 getCollidePoint(Vec3 point, Vec3 root) {
			Vec3 toOpponent = point.subtract(this.worldCenter);
			Vec3 projection1 = null;
			Vec3 projection2 = null;
			Vec3 projection3 = null;
			Vec3 toPlane1 = null;
			Vec3 toPlane2 = null;
			Vec3 toPlane3 = null;
			
			int order = 0;
			
			for (Vec3 seperateAxis : this.rotatedNormal) {
				Vec3 maxProj = null;
				double maxDot = Double.MIN_VALUE;
				
				if (seperateAxis.dot(toOpponent) < 0.0D) {
					seperateAxis = seperateAxis.scale(-1.0D);
				}
				
				for (Vec3 vertexVector : this.rotatedVertex) {
					Vec3 toVertex = seperateAxis.dot(vertexVector) > 0.0D ? vertexVector : vertexVector.scale(-1.0D);
					double dot = seperateAxis.dot(toVertex);
					
					if (dot > maxDot || maxProj == null) {
						maxDot = dot;
						maxProj = toVertex;
					}
				}
				
				Vec3 opponentProjection = MathUtils.projectVector(toOpponent, seperateAxis);
				Vec3 vertexProjection = MathUtils.projectVector(maxProj, seperateAxis);
				
				if (opponentProjection.length() > vertexProjection.length()) {
					return point;
				} else {
					switch (order) {
					case 0 -> {
						projection1 = opponentProjection;
						toPlane1 = vertexProjection;
					}
					case 1 -> {
						projection2 = opponentProjection;
						toPlane2 = vertexProjection;
					}
					case 2 -> {
						projection3 = opponentProjection;
						toPlane3 = vertexProjection;
					}
					}
				}
				
				order++;
			}
			
			Vec3 destCandidate1 = this.allowedPlaneIds[2] > 0 ? projection1.add(projection2).add(toPlane3) : null;
			Vec3 destCandidate2 = this.allowedPlaneIds[0] > 0 ? projection2.add(projection3).add(toPlane1) : null;
			Vec3 destCandidate3 = this.allowedPlaneIds[1] > 0 ? projection3.add(projection1).add(toPlane2) : null;
			Vec3 destCandidate4 = this.allowedPlaneIds[5] > 0 ? projection1.add(projection2).subtract(toPlane3) : null;
			Vec3 destCandidate5 = this.allowedPlaneIds[3] > 0 ? projection2.add(projection3).subtract(toPlane1) : null;
			Vec3 destCandidate6 = this.allowedPlaneIds[4] > 0 ? projection3.add(projection1).subtract(toPlane2) : null;
			
			return MathUtils.getNearestVector(root.subtract(this.worldCenter), destCandidate1, destCandidate2, destCandidate3, destCandidate4, destCandidate5, destCandidate6).add(this.worldCenter);
		}
	}
}
