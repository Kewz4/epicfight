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
import com.mojang.math.Axis;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles;
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
	
	private static final int BASIS_SUBSTEPS = 15;
	private static final int MAX_SUBSTEPS = 50;
	
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
		@Nullable
		protected List<Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider>> clothColliders;
		
		//Storage vectors
		private static final Vec3f TRASNFORMED = new Vec3f();
		
		public ClothObject(ClothObjectBuilder builder, ClothMesh provider, Map<String, ClothPart> parts, float[] positions) {
			ImmutableMap.Builder<String, Part> partBuilder = ImmutableMap.builder();
			
			for (Map.Entry<String, ClothPart> entry : parts.entrySet()) {
				partBuilder.put(entry.getKey(), new Part(entry.getKey(), entry.getValue(), positions));
			}
			
			this.parts = partBuilder.build();
			this.provider = provider;
			this.clothColliders = builder.clothColliders;
		}
		
		@Override
		public ClothMesh getProvider() {
			return this.provider;
		}
		
		@Override
		public void tick(ClothSimulatable simulatableObj) {
		}
		
		static final Vec3f FORCE = new Vec3f();
		
		public void tick(ClothSimulatable simulatableObj, Function<Float, OpenMatrix4f> clothRootTransform, Function<Float, OpenMatrix4f> colliderTransform, @Nullable OpenMatrix4f[] poses, float partialTick) {
			if (!Minecraft.getInstance().isPaused()) {
				float deltaFrameTime = Minecraft.getInstance().getDeltaFrameTime();
				int subStebs = Mth.clamp((int)(BASIS_SUBSTEPS * (deltaFrameTime / 0.15F)), BASIS_SUBSTEPS, MAX_SUBSTEPS);
				float subSteppingDeltaTime = deltaFrameTime / subStebs;
				
				for (int i = 0; i < subStebs; i++) {
					float substepPartialTick = partialTick - deltaFrameTime + subSteppingDeltaTime * i;
					
					if (this.clothColliders != null) {
						OpenMatrix4f colliderMatrix = colliderTransform.apply(substepPartialTick);
						
						for (Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider> entry : this.clothColliders) {
							entry.getSecond().transform(OpenMatrix4f.mul(colliderMatrix, entry.getFirst().apply(simulatableObj), null));
						}
					}
					
					OpenMatrix4f clothRootMatrix = clothRootTransform.apply(substepPartialTick);
					
					float yRot = Mth.wrapDegrees(simulatableObj.getAccurateYRot(substepPartialTick - subSteppingDeltaTime));
					float yRotDelta = Mth.clamp(Mth.wrapDegrees(simulatableObj.getAccurateYRot(substepPartialTick) - yRot), -0.35F, 0.35F);
					
					Vec3 velocity = simulatableObj.getAccuratePartialLocation(substepPartialTick).subtract(simulatableObj.getAccuratePartialLocation(substepPartialTick - subSteppingDeltaTime));
					double yDeltaMod = yRotDelta / 0.45D * Math.max(velocity.length() / 0.0075D, 0.75D);
					
					
					FORCE.add(Vec3f.fromDoubleVector(MathUtils.getVectorForRotation(0.0F, yRot - 180.0F)
							                                       .add(MathUtils.getVectorForRotation(0.0F, yRot + (yRotDelta > 0.0F ? -90.0F : 90.0F)))
							                                       .scale(Math.abs(yDeltaMod * yDeltaMod * yDeltaMod) * PARTICLE_MASS * (1.0D / subStebs))
																   .add(velocity.scale(Math.abs(yRotDelta) * (1.0D / subStebs)) )
														 ));
					
					FORCE.scale(0.98F);
					
					for (Part part : this.parts.values()) {
						part.tick(FORCE, subSteppingDeltaTime, clothRootMatrix, this.clothColliders);
					}
				}
			}
		}
		
		public void draw(MultiBufferSource bufferSource, VertexConsumer vertexConsumer, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay, float partialTick) {
			Minecraft mc = Minecraft.getInstance();
			PoseStack poseStack$2 = new PoseStack();
			Camera camera = mc.gameRenderer.getMainCamera();
			Camera dummy = new Camera();
			dummy.setup(mc.level, mc.player, !mc.options.getCameraType().isFirstPerson(), mc.options.getCameraType().isMirrored(), partialTick);
			ComputeCameraAngles cameraSetup = ForgeHooksClient.onCameraSetup(mc.gameRenderer, dummy, partialTick);
			
			poseStack$2.mulPose(Axis.ZP.rotationDegrees(cameraSetup.getRoll()));
			poseStack$2.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
			poseStack$2.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
			poseStack$2.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
			
			for (Part part : this.parts.values()) {
				part.draw(poseStack$2, vertexConsumer, drawingFunction, packedLight, r, g, b, a, overlay);
			}
			
			if (mc.getEntityRenderDispatcher().shouldRenderHitBoxes() && this.clothColliders != null) {
				for (Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider> entry : this.clothColliders) {
					entry.getSecond().draw(poseStack$2, bufferSource, -1);
				}
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
					boolean collider = clothPart.collider()[idx];
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
						
						constraintsBuilder.add(new ConstraintsBundle(compliance, collider, constraintList));
					}
					case VOLUME -> {
						constraintList = new ArrayList<> (constraints.length / 4);

						for (int i = 0; i < constraints.length / 4; i++) {
							int idx1 = constraints[i * 2];
							int idx2 = constraints[i * 2 + 1];
							int idx3 = constraints[i * 2 + 2];
							int idx4 = constraints[i * 2 + 3];
							
							constraintList.add(new VolumeConstraint(particlesList.get(idx1), particlesList.get(idx2), particlesList.get(idx3), particlesList.get(idx4)));
						}
						
						constraintsBuilder.add(new ConstraintsBundle(compliance, false, constraintList));
					}
					}
				}
				
				//Temporary code for creating volume constraints (11x17)
				List<Particle> particlesCollection = List.copyOf(this.particles.values());
				List<Constraint> constraintList = new ArrayList<> ();
				
				for (int i = 0; i < this.particles.size() / 2; i++) {
					if ((i != 0 && i % 11 == 10) || (i / 11 == 16)) {
						continue;
					}
					
					Particle p1 = particlesCollection.get(i);
					Particle p2 = particlesCollection.get(i + 1);
					Particle p3 = particlesCollection.get(i + 11);
					Particle p4 = particlesCollection.get(i + 12);
					
					Particle p5 = particlesCollection.get(i + 187);
					Particle p6 = particlesCollection.get(i + 1 + 187);
					Particle p7 = particlesCollection.get(i + 11 + 187);
					Particle p8 = particlesCollection.get(i + 12 + 187);
					
					VolumeConstraint vol1 = new VolumeConstraint(p1, p2, p3, p5);
					VolumeConstraint vol2 = new VolumeConstraint(p6, p2, p5, p8);
					VolumeConstraint vol3 = new VolumeConstraint(p4, p2, p3, p8);
					VolumeConstraint vol4 = new VolumeConstraint(p7, p3, p5, p8);
					VolumeConstraint vol5 = new VolumeConstraint(p2, p3, p5, p8);
					
					constraintList.add(vol1);
					constraintList.add(vol2);
					constraintList.add(vol3);
					constraintList.add(vol4);
					constraintList.add(vol5);
				}
				
				constraintsBuilder.add(new ConstraintsBundle(0.0F, false, constraintList));
				// Temp code ends
				
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
				// Apply animation transform
				for (Particle p : this.particles.values()) {
					float influenceInv = 1.0F - p.influence;
					
					if (influenceInv > 0.0) {
						OpenMatrix4f.transform3v(clothRootTransform, p.initPosition, TRASNFORMED);
						Vec3f.interpolate(p.position, TRASNFORMED, influenceInv, TRASNFORMED);
						p.position.set(TRASNFORMED);
					}
					
					p.position.y -= GRAVITY * PARTICLE_MASS * p.influence * deltaTime;
					p.position.add(force.x * p.rootDistance, force.y * p.rootDistance, force.z * p.rootDistance);
				}
				
				for (ConstraintsBundle constraintsBundle : this.constraints) {
					for (Constraint c : constraintsBundle.constraints()) {
						c.solve(deltaTime, constraintsBundle.compliance());
						
						if (constraintsBundle.useAsCollider()) {
							if (clothColliders != null) {
								for (Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider> entry : clothColliders) {
									Particle p = ((DistanceConstraint)c).p2;
									Particle rootP = ((DistanceConstraint)c).p1;
									
									if (p.influence == 0.0F) {
										continue;
									}
									
									Vec3 particlePos = p.position.toDoubleVector();
									Vec3 pushedPoint = entry.getSecond().getCollidePoint(particlePos, rootP.position.toDoubleVector());
									p.position.set(pushedPoint);
								}
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
			
			public void draw(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
				ClothMesh originalMesh = ClothObject.this.provider;
				ModelPart<?> modelPart = originalMesh.getPart(this.name);
				
				if (modelPart.isHidden()) {
					return;
				}
				
				poseStack.pushPose();
				
				Matrix4f matrix4f = poseStack.last().pose();
				Matrix3f matrix3f = poseStack.last().normal();
				
				float[] normals = originalMesh.normals();
				float[] uvs = originalMesh.uvs();
				
				for (VertexBuilder vi : modelPart.getVertices()) {
					int norm = vi.normal * 3;
					int uv = vi.uv * 2;
					Particle particle = this.particles.get(vi.position);
					
					POSITION.x = particle.position.x;
					POSITION.y = particle.position.y;
					POSITION.z = particle.position.z;
					NORMAL.x = normals[norm];
					NORMAL.y = normals[norm + 1];
					NORMAL.z = normals[norm + 2];
					
					Vector4f posVec = matrix4f.transform(POSITION);
					Vector3f normVec = matrix3f.transform(NORMAL);
					
					drawingFunction.draw(builder, posVec.x, posVec.y, posVec.z, normVec.x(), normVec.y(), normVec.z(), packedLight, r, g, b, a, uvs[uv], uvs[uv + 1], overlay);
				}
				
				poseStack.popPose();
			}
			
			@OnlyIn(Dist.CLIENT)
			class Particle {
				Vec3f position;
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
			public static record ConstraintsBundle(float compliance, boolean useAsCollider, List<Constraint> constraints) {
			}
			
			@OnlyIn(Dist.CLIENT)
			abstract class Constraint {
				abstract void solve(float deltaTime, float compliance);
			}
			
			@OnlyIn(Dist.CLIENT)
			class DistanceConstraint extends Constraint {
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
				    float force = distanceGap / (p1Influence + p2Influence + alpha);
				    float p1Modifier = p1Influence / influenceSum;
				    float p2Modifier = p2Influence / influenceSum;
				    
				    if (distanceGap > 1.6F) {
				    	float mod = Math.min(distanceGap / 3.0F, 0.47F);
				    	p1Modifier = Math.max(p1Modifier - mod, 0.0F);
				    	p2Modifier = Math.min(p2Modifier + mod, 1.0F);
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
