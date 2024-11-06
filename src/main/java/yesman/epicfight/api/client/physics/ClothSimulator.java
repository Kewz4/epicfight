package yesman.epicfight.api.client.physics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.compress.utils.Lists;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.ClothMesh;
import yesman.epicfight.api.client.model.ClothMesh.ClothPart;
import yesman.epicfight.api.client.model.ClothMesh.ClothPartDefinition.ConstraintType;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.ModelPart;
import yesman.epicfight.api.client.model.VertexBuilder;
import yesman.epicfight.api.client.physics.ClothSimulator.ClothObjectBuilder;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.physics.SimulationObject;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;

/**
 * Inspired by Ten minuates physics tutorial 14, 15
 * 
 * https://matthias-research.github.io/pages/tenMinutePhysics/index.html
 * 
 * https://www.youtube.com/@TenMinutePhysics
 **/
@OnlyIn(Dist.CLIENT)
public class ClothSimulator extends AbstractSimulator<ClothObjectBuilder, ClothMesh, ClothSimulatable, ClothSimulator.ClothObject> {
	private static final float GRAVITY = 9.8F;
	private static final float PARTICLE_MASS = 0.01F;
	private static final float SPACING = 0.05F;
	private static final float SELF_COLLISION = 0.025F;
	
	@OnlyIn(Dist.CLIENT)
	public static class ClothObjectBuilder extends SimulationObject.SimulationBuilder {
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
	public static class ClothObject implements SimulationObject<ClothObjectBuilder, ClothMesh, ClothSimulatable> {
		private final ClothMesh provider;
		private final Map<String, Part> parts;
		private final List<Map<Vec3, Vec3f>> poseNormals;
		
		@Nullable
		protected List<Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider>> clothColliders;
		
		//Storage vectors
		private static final Vec3f TRASNFORMED = new Vec3f();
		
		public ClothObject(ClothObjectBuilder builder, ClothMesh provider, Map<String, ClothPart> parts, float[] positions) {
			this.provider = provider;
			this.clothColliders = builder.clothColliders;
			
			this.poseNormals = Lists.newArrayList();
			
			for (int i = 0; i < provider.positions().length / 3; i++) {
				this.poseNormals.add(Maps.newHashMap());
			}
			
			ImmutableMap.Builder<String, Part> partBuilder = ImmutableMap.builder();
			
			for (Map.Entry<String, ClothPart> entry : parts.entrySet()) {
				partBuilder.put(entry.getKey(), new Part(entry.getKey(), entry.getValue(), positions));
			}
			
			this.parts = partBuilder.build();
		}
		
		@Override
		public ClothMesh getProvider() {
			return this.provider;
		}
		
		@Override
		public void tick(ClothSimulatable simulatableObj) {
		}
		
		private static final int SUB_STEPS = 15;
		private static final float YROT_LIMIT = 5.0F;
		private static final double MAX_VELOCITY_FORCE = 6.0D;
		private static final Vec3f SCALED_FORCE = new Vec3f();
		private final Vec3f force = new Vec3f();
		
		public void tick(ClothSimulatable simulatableObj, Function<Float, OpenMatrix4f> clothRootTransform, Function<Float, OpenMatrix4f> colliderTransform, @Nullable OpenMatrix4f[] poses, float partialTick) {
			float deltaFrameTime = Minecraft.getInstance().getDeltaFrameTime();
			float yRotO = simulatableObj.getYRotO();
			float yRot = simulatableObj.getYRot();
			float deltaYRot = Mth.clamp(yRot - yRotO, -YROT_LIMIT, YROT_LIMIT) * 0.75F * deltaFrameTime;
			Vec3 velocity = simulatableObj.getObjectVelocity();
			double centrifugalForce = deltaYRot * deltaYRot * Mth.clamp(velocity.length() / 0.2D, 1.0D, MAX_VELOCITY_FORCE) * PARTICLE_MASS * (0.16F / deltaFrameTime);
			
			this.force.add(Vec3f.fromDoubleVector(MathUtils.getVectorForRotation(0.0F, yRotO - 180.0F)
									                       .add(MathUtils.getVectorForRotation(0.0F, yRotO + (deltaYRot > 0.0F ? -90.0F : 90.0F)))
									                       .scale(centrifugalForce)
														   .add(velocity.scale(centrifugalForce * deltaFrameTime))
			));
			
			float subStebInvert = 1.0F / SUB_STEPS;
			float subSteppingDeltaTime = deltaFrameTime * subStebInvert;
			
			for (int i = 0; i < SUB_STEPS; i++) {
				float substepPartialTick = partialTick - deltaFrameTime + subSteppingDeltaTime * (i + 1);
				
				if (this.clothColliders != null) {
					OpenMatrix4f colliderMatrix = colliderTransform.apply(substepPartialTick);
					
					for (Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider> entry : this.clothColliders) {
						entry.getSecond().transform(OpenMatrix4f.mul(colliderMatrix, entry.getFirst().apply(simulatableObj), null));
					}
				}
				
				OpenMatrix4f clothRootMatrix = clothRootTransform.apply(substepPartialTick);
				Vec3f.scale(this.force, SCALED_FORCE, subStebInvert);
				
				for (Part part : this.parts.values()) {
					part.tick(SCALED_FORCE, subSteppingDeltaTime, clothRootMatrix, this.clothColliders);
				}
			}
			
			this.force.scale(1.0F - deltaFrameTime * 0.5F);
		}
		
		public void draw(ClothSimulatable simulatableObj, PoseStack poseStack, MultiBufferSource bufferSource, VertexConsumer vertexConsumer, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay, float partialTick) {
			this.poseNormals.forEach((poseNormals) -> poseNormals.values().forEach((vec3f) -> vec3f.set(0.0F, 0.0F, 0.0F)));
			
			for (Part part : this.parts.values()) {
				part.draw(poseStack, vertexConsumer, drawingFunction, packedLight, r, g, b, a, overlay);
			}
		}
		
		@OnlyIn(Dist.CLIENT)
		public class Part {
			final String name;
			final Map<Integer, Particle> particles;
			final List<ConstraintsBundle> constraints;
			final Multimap<Integer, Particle> spatialHash;
			final int hashTableSize;
			
			// Storage vector
			private static final Vec3f P2_TO_P1 = new Vec3f();
			private static final Vector4f POSITION = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
			private static final Vector3f NORMAL = new Vector3f();
			
			Part(String name, ClothPart clothPart, float[] positions) {
				this.name = name;
				this.particles = Maps.newLinkedHashMap();
				
				for (VertexBuilder vb : clothPart.getVertices()) {
					Vec3 normal = provider.normalList.get(vb.normal);
					Map<Vec3, Vec3f> positionNormals = poseNormals.get(vb.position);
					
					if (!positionNormals.containsKey(normal)) {
						positionNormals.put(normal, new Vec3f());
					}
				}
				
				List<Particle> particlesList = Lists.newArrayList();
				ImmutableList.Builder<ConstraintsBundle> constraintsBuilder = ImmutableList.builder();
				
				for (int i = 0; i < clothPart.particles().length / 2; i++) {
					int posIndex = clothPart.particles()[i * 2];
					int influence = clothPart.particles()[i * 2 + 1];
					float rootDistance = clothPart.rootDistance()[i];
					float x = positions[posIndex * 3];
					float y = positions[posIndex * 3 + 1];
					float z = positions[posIndex * 3 + 2];
					
					Particle particle = new Particle(new Vec3f(x, y, z), influence, rootDistance, posIndex);
					this.particles.put(posIndex, particle);
					particlesList.add(particle);
				}
				
				this.hashTableSize = this.particles.size() * 2;
				this.spatialHash = HashMultimap.create(this.hashTableSize, 2);
				int idx = 0;
				
				for (int[] constraints : clothPart.constraints()) {
					float compliance = clothPart.compliances()[idx];
					ConstraintType constraintType = clothPart.constraintTypes()[idx];
					List<Constraint> constraintList;
					idx++;
					
					switch(constraintType) {
					case DISTANCE -> {
						constraintList = new ArrayList<> (constraints.length / 2);
						
						for (int i = 0; i < constraints.length / 2; i++) {
							int idx1 = constraints[i * 2];
							int idx2 = constraints[i * 2 + 1];
							
							constraintList.add(new DistanceConstraint(particlesList.get(idx1), particlesList.get(idx2)));
						}
						
						constraintsBuilder.add(new ConstraintsBundle(compliance, constraintList));
					}
					case VOLUME -> {
						constraintList = new ArrayList<> (constraints.length / 4);
						
						for (int i = 0; i < constraints.length / 4; i++) {
							int idx1 = constraints[i * 4];
							int idx2 = constraints[i * 4 + 1];
							int idx3 = constraints[i * 4 + 2];
							int idx4 = constraints[i * 4 + 3];
							
							constraintList.add(new VolumeConstraint(particlesList.get(idx1), particlesList.get(idx2), particlesList.get(idx3), particlesList.get(idx4)));
						}
						
						constraintsBuilder.add(new ConstraintsBundle(compliance, constraintList));
					}
					}
				}
				
				this.constraints = constraintsBuilder.build();
			}
			
			public void buildSpatialHash() {
				this.spatialHash.clear();
				
				// Create spatial hash map
				for (Particle p : this.particles.values()) {
					int hash = this.getHash(p.position.x, p.position.y, p.position.z);
					this.spatialHash.put(hash, p);
				}
			}
			
			public void tick(Vec3f force, float deltaTime, OpenMatrix4f clothRootTransform, List<Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider>> clothColliders) {
				boolean paused = Minecraft.getInstance().isPaused();
				
				for (Particle p : this.particles.values()) {
					float influenceInv = 1.0F - p.influence;
					
					// Apply animation transform
					if (influenceInv > 0.0) {
						OpenMatrix4f.transform3v(clothRootTransform, p.initPosition, TRASNFORMED);
						Vec3f.interpolate(p.position, TRASNFORMED, influenceInv, TRASNFORMED);
						p.position.set(TRASNFORMED);
					}
					
					if (!paused) {
						p.position.y -= GRAVITY * PARTICLE_MASS * p.influence * deltaTime;
						p.position.add(force.x * p.rootDistance * p.influence, force.y * p.rootDistance * p.influence, force.z * p.rootDistance * p.influence);
					}
				}
				
				if (paused) {
					return;
				}
				
				for (ConstraintsBundle constraintsBundle : this.constraints) {
					for (Constraint c : constraintsBundle.constraints()) {
						c.solve(deltaTime, constraintsBundle.compliance());
					}
				}
				
				// Detect collision with mesh collider
				if (clothColliders != null) {
					for (Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider> entry : clothColliders) {
						for (Particle p : this.particles.values()) {
							if (p.influence == 0.0F) {
								continue;
							}
							
							Vec3 particlePos = p.position.toDoubleVector();
							Vec3 pushedPoint = entry.getSecond().getCollidePoint(particlePos, p.position.toDoubleVector());
							p.position.set(pushedPoint);
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
						
						Vec3f.sub(p1.position, p2.position, P2_TO_P1);
						float length = P2_TO_P1.length();
						
						if (length < SELF_COLLISION) {
							float influenceSum = p1.influence + p2.influence;
							float scale = (SELF_COLLISION - length) / SELF_COLLISION;
							float p1Move = p1.influence / influenceSum;
							float p2Move = p2.influence / influenceSum;
							P2_TO_P1.scale(scale);
							
							p1.position.add(P2_TO_P1.x * p1Move, P2_TO_P1.y * p1Move, P2_TO_P1.z * p1Move);
							p2.position.sub(P2_TO_P1.x * p2Move, P2_TO_P1.y * p2Move, P2_TO_P1.z * p2Move);
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
			
			private static final Vec3f TO_P2 = new Vec3f();
			private static final Vec3f TO_P3 = new Vec3f();
			private static final Vec3f CROSS = new Vec3f();
			
			public void draw(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
				ClothMesh originalMesh = ClothObject.this.provider;
				ModelPart<?> modelPart = originalMesh.getPart(this.name);
				
				if (modelPart.isHidden()) {
					return;
				}
				
				poseStack.pushPose();
				
				Matrix4f matrix4f = poseStack.last().pose();
				Matrix3f matrix3f = poseStack.last().normal();
				
				float[] uvs = originalMesh.uvs();
				
				// Calculate pose normals
				for (int i = 0; i < modelPart.getVertices().size() / 3; i++) {
					VertexBuilder triP1 = modelPart.getVertices().get(i * 3);
					VertexBuilder triP2 = modelPart.getVertices().get(i * 3 + 1);
					VertexBuilder triP3 = modelPart.getVertices().get(i * 3 + 2);
					Vec3f p1Pos = this.particles.get(triP1.position).position;
					Vec3f p2Pos = this.particles.get(triP2.position).position;
					Vec3f p3Pos = this.particles.get(triP3.position).position;
					
					Vec3f.cross(Vec3f.sub(p2Pos, p1Pos, TO_P2), Vec3f.sub(p3Pos, p1Pos, TO_P3), CROSS);
					CROSS.normalise();
					
					Map<Vec3, Vec3f> triP1Normals = poseNormals.get(triP1.position);
					Map<Vec3, Vec3f> triP2Normals = poseNormals.get(triP2.position);
					Map<Vec3, Vec3f> triP3Normals = poseNormals.get(triP3.position);
					
					triP1Normals.get(provider.normalList.get(triP1.normal)).add(CROSS);
					triP2Normals.get(provider.normalList.get(triP2.normal)).add(CROSS);
					triP3Normals.get(provider.normalList.get(triP3.normal)).add(CROSS);
				}
				
				for (VertexBuilder vi : modelPart.getVertices()) {
					int uv = vi.uv * 2;
					Particle particle = this.particles.get(vi.position);
					Vec3f poseNormal = poseNormals.get(vi.position).get(provider.normalList.get(vi.normal));
					poseNormal.normalise();
					
					POSITION.x = particle.position.x;
					POSITION.y = particle.position.y;
					POSITION.z = particle.position.z;
					NORMAL.x = poseNormal.x;
					NORMAL.y = poseNormal.y;
					NORMAL.z = poseNormal.z;
					
					Vector4f posVec = matrix4f.transform(POSITION);
					Vector3f normVec = matrix3f.transform(NORMAL);
					
					drawingFunction.draw(builder, posVec.x, posVec.y, posVec.z, normVec.x(), normVec.y(), normVec.z(), packedLight, r, g, b, a, uvs[uv], uvs[uv + 1], overlay);
				}
				
				poseStack.popPose();
			}
			
			@OnlyIn(Dist.CLIENT)
			class Particle {
				final Vec3f position;
				final Vec3f initPosition;
				final float influence;
				final float rootDistance;
				final int meshVertexId;
				
				Particle(Vec3f position, float influence, float rootDistance, int meshVertexId) {
					this.position = position;
					this.initPosition = position.copy();
					this.influence = influence;
					this.rootDistance = rootDistance;
					this.meshVertexId = meshVertexId;
				}
			}
			
			@OnlyIn(Dist.CLIENT)
			public static record ConstraintsBundle(float compliance, List<Constraint> constraints) {
			}
			
			@OnlyIn(Dist.CLIENT)
			abstract class Constraint {
				abstract void solve(float deltaTime, float compliance);
			}
			
			@OnlyIn(Dist.CLIENT)
			class DistanceConstraint extends Constraint {
				private static final float MIN_STRETCH = 1.1F;
				private static final float STRETCH_RESTRICTION = 5.0F;
				
				final Particle p1;
				final Particle p2;
				final float restLength;
				
				// Storage vector
				static final Vec3f TOWARD = new Vec3f();
				
				DistanceConstraint(Particle p1, Particle p2) {
					this.p1 = p1;
					this.p2 = p2;
					this.restLength = p1.position.distance(p2.position);
				}
				
				void solve(float deltaTime, float compliance) {
				    float p1Influence = this.p1.influence;
				    float p2Influence = this.p2.influence;
				    float influenceSum = p1Influence + p2Influence;
				    
				    if (influenceSum < 1E-5) {
				        return;
				    }
				    
				    Vec3f.sub(this.p2.position, this.p1.position, TOWARD);
				    float distanceLength = TOWARD.length();
				    
				    if (distanceLength == 0.0) {
				        return;
				    }
				    
				    TOWARD.scale(1.0F / distanceLength);
				    
				    float alpha = compliance / (deltaTime * deltaTime);
				    float distanceGap = distanceLength - this.restLength;
				    float force = distanceGap / (influenceSum + alpha);
				    float p1Modifier = p1Influence / influenceSum;
				    float p2Modifier = p2Influence / influenceSum;
				    float stretchMultiplier = distanceLength / this.restLength;
				    
				    if (stretchMultiplier > MIN_STRETCH && p1Modifier > 0.0F) {
				    	float modifier = Math.min((stretchMultiplier - MIN_STRETCH) / STRETCH_RESTRICTION, 1.0F);
				    	p2Modifier = Math.min(p2Modifier + p1Modifier * modifier, 1.0F);
				    	p1Modifier = Math.max(p1Modifier - p1Modifier * modifier, 0.0F);
				    }
				    
				    float p1Move = force * p1Modifier * influenceSum;
				    float p2Move = -force * p2Modifier * influenceSum;
				    
				    this.p1.position.add(TOWARD.x * p1Move, TOWARD.y * p1Move, TOWARD.z * p1Move);
				    this.p2.position.add(TOWARD.x * p2Move, TOWARD.y * p2Move, TOWARD.z * p2Move);
				}
			}
			
			@OnlyIn(Dist.CLIENT)
			class VolumeConstraint extends Constraint {
				final Particle[] particles;
				final float restVolume;
				
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
					this.restVolume = getTetrahedralVolume(this.particles[0], this.particles[1], this.particles[2], this.particles[3]);
				}

				@Override
				void solve(float deltaTime, float compliance) {
					float alpha = compliance / (deltaTime * deltaTime);
					float weight = 0.0F;
					
					for (int i = 0; i < 4; i++) {
						Particle p1 = this.particles[VOLUME_ORDER[i][0]];
						Particle p2 = this.particles[VOLUME_ORDER[i][1]];
						Particle p3 = this.particles[VOLUME_ORDER[i][2]];
						
						Vec3f.sub(p2.position, p1.position, P1_TO_P2);
						Vec3f.sub(p3.position, p1.position, P1_TO_P3);
						Vec3f.cross(P1_TO_P2, P1_TO_P3, SHRINK_DIRECTIONS[i]);
						SHRINK_DIRECTIONS[i].scale(1.0F / 6.0F);
						
						weight += this.particles[i].influence * SHRINK_DIRECTIONS[i].lengthSqr();
					}
					
					if (weight == 0.0F) {
						return;
					}
					
					float currentVolume = getTetrahedralVolume(this.particles[0], this.particles[1], this.particles[2], this.particles[3]);
					float volumeGap = currentVolume - this.restVolume;
					float force = -volumeGap / (weight + alpha);
					
					for (int i = 0; i < 4; i++) {
						SHRINK_DIRECTIONS[i].scale(force * this.particles[i].influence);
						Vec3f.add(this.particles[i].position, SHRINK_DIRECTIONS[i], this.particles[i].position);
					}
				}
				
				static float getTetrahedralVolume(Particle p1, Particle p2, Particle p3, Particle p4) {
					Vec3f.sub(p2.position, p1.position, P1_TO_P2);
					Vec3f.sub(p3.position, p1.position, P1_TO_P3);
					Vec3f.sub(p4.position, p1.position, P1_TO_P4);
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
