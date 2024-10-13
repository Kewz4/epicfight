package yesman.epicfight.api.client.physics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles;
import yesman.epicfight.api.client.model.AnimatedMesh;
import yesman.epicfight.api.client.model.ClothMesh;
import yesman.epicfight.api.client.model.ClothMesh.ClothPart;
import yesman.epicfight.api.client.model.ClothMesh.ClothPartDefinition.ConstraintType;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.ModelPart;
import yesman.epicfight.api.client.model.VertexBuilder;
import yesman.epicfight.api.client.physics.ClothSimulator.ClothObject.Part.Particle;
import yesman.epicfight.api.physics.SimulationData;
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
public class ClothSimulator extends AbstractSimulator<ClothMesh, ClothSimulatable, ClothSimulator.ClothObject> {
	private static final int SUB_STEPS = 15;
	private static final float GRAVITY = 9.8F;
	private static final float PARTICLE_MASS = 0.01F;
	private static final float SPACING = 0.05F;
	private static final float SELF_COLLISION = 0.025F;
	private static final float TIME_STEP = 0.16F;
	
	@OnlyIn(Dist.CLIENT)
	public static class ClothObject implements SimulationData<ClothMesh, ClothSimulatable> {
		private final ClothMesh provider;
		private final Map<String, Part> parts;
		
		//Storage vectors
		private static final Vec3f TRASNFORMED = new Vec3f();
		
		public ClothObject(ClothMesh provider, Map<String, ClothPart> parts, float[] positions) {
			ImmutableMap.Builder<String, Part> partBuilder = ImmutableMap.builder();
			
			for (Map.Entry<String, ClothPart> entry : parts.entrySet()) {
				partBuilder.put(entry.getKey(), new Part(entry.getKey(), entry.getValue(), positions));
			}
			
			this.parts = partBuilder.build();
			this.provider = provider;
		}
		
		@Override
		public ClothMesh getProvider() {
			return this.provider;
		}
		
		@Override
		public void tick(ClothSimulatable simulatableObj) {
		}
		
		public void tick(OpenMatrix4f transform, float partialTick, @Nullable OpenMatrix4f[] poses, @Nullable AnimatedMesh colliderMesh) {
			for (Part part : this.parts.values()) {
				for (Particle p : part.particles.values()) {
					float influence = 1.0F - p.influence;
					
					if (influence > 0.0) {
						OpenMatrix4f.transform3v(transform, p.initPosition, TRASNFORMED);
						Vec3f.interpolate(p.position, TRASNFORMED, influence, TRASNFORMED);
						p.position.set(TRASNFORMED);
					}
				}
			}
			
			if (!Minecraft.getInstance().isPaused()) {
				for (Part part : this.parts.values()) {
					part.tick(TIME_STEP, poses, colliderMesh);
				}
			}
		}
		
		public void draw(VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay, float partialTick) {
			PoseStack poseStack$2 = new PoseStack();
			Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
			
			Minecraft mc = Minecraft.getInstance();
			Camera dummy = new Camera();
			dummy.setup(mc.level, mc.player, !mc.options.getCameraType().isFirstPerson(), mc.options.getCameraType().isMirrored(), partialTick);
			ComputeCameraAngles cameraSetup = ForgeHooksClient.onCameraSetup(Minecraft.getInstance().gameRenderer, dummy, partialTick);
			
			poseStack$2.mulPose(Axis.ZP.rotationDegrees(cameraSetup.getRoll()));
			poseStack$2.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
			poseStack$2.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
			poseStack$2.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
			
			for (Part part : this.parts.values()) {
				part.draw(poseStack$2, builder, drawingFunction, packedLight, r, g, b, a, overlay);
			}
		}
		
		@OnlyIn(Dist.CLIENT)
		public class Part {
			final String name;
			final Map<Integer, Particle> particles;
			final List<Pair<Float, List<Constraint>>> constraints;
			
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
				ImmutableList.Builder<Pair<Float, List<Constraint>>> constraintsBuilder = ImmutableList.builder();
				
				for (int i = 0; i < clothPart.particles().length / 2; i++) {
					int posIndex = clothPart.particles()[i * 2];
					int influence = clothPart.particles()[i * 2 + 1];
					
					float x = positions[posIndex * 3];
					float y = positions[posIndex * 3 + 1];
					float z = positions[posIndex * 3 + 2];
					
					Particle particle = new Particle(new Vec3f(x, y, z), influence, posIndex);
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
						
						constraintsBuilder.add(Pair.of(compliance, constraintList));
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
						
						constraintsBuilder.add(Pair.of(compliance, constraintList));
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
				
				constraintsBuilder.add(Pair.of(0.0F, constraintList));
				
				this.constraints = constraintsBuilder.build();
			}
			
			public void tick(float deltaTime, @Nullable OpenMatrix4f[] poses, @Nullable AnimatedMesh colliderMesh) {
				this.spatialHash.clear();
				
				// Create spatial hash map
				for (Particle p : this.particles.values()) {
					int hash = this.getHash(p.position.x, p.position.y, p.position.z);
					this.spatialHash.put(hash, p);
				}
				
				for (Particle p : particles.values()) {
					p.position.y -= GRAVITY * PARTICLE_MASS * p.influence * deltaTime;
				}
				
				float subSteppingDeltaTime = deltaTime / SUB_STEPS;
				
				for (int i = 0; i < SUB_STEPS; i++) {
					for (Pair<Float, List<Constraint>> pair : this.constraints) {
						for (Constraint c : pair.getSecond()) {
							c.solve(subSteppingDeltaTime, pair.getFirst());
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
			}
			
			private int getHash(double x, double y, double z) {
				int xi = (int)Math.floor(x / SPACING);
				int yi = (int)Math.floor(y / SPACING);
				int zi = (int)Math.floor(z / SPACING);
				int hash = (xi * 92837111) ^ (yi * 689287499) ^ (zi * 283923481);
				
				return Math.abs(hash) % this.hashTableSize;
			}
			
			public void draw(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
				ModelPart<?> modelPart = ClothObject.this.provider.getPart(this.name);
				
				if (modelPart.isHidden()) {
					return;
				}
				
				poseStack.pushPose();
				
				Matrix4f matrix4f = poseStack.last().pose();
				Matrix3f matrix3f = poseStack.last().normal();
				
				float[] normals = ClothObject.this.provider.normals();
				float[] uvs = ClothObject.this.provider.uvs();
				
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
				final int meshVertexId;
				
				Particle(Vec3f position, float influence, int meshVertexId) {
					this.position = position;
					this.initPosition = position.copy();
					this.influence = influence;
					this.meshVertexId = meshVertexId;
				}
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
				    float alpha = compliance / (deltaTime * deltaTime);
				    float influenceSum = this.p1.influence + this.p2.influence;
				    
				    if (influenceSum == 0.0F) {
				        return;
				    }
				    
				    Vec3f.sub(this.p2.position, this.p1.position, TOWARD);
				    float distanceLength = TOWARD.length();
				    
				    if (distanceLength == 0.0) {
				        return;
				    }
				    
				    TOWARD.scale(1.0F / distanceLength);
				    
				    float distanceGap = distanceLength - this.restLength;
				    float force = distanceGap / (this.p1.influence + this.p2.influence + alpha);
				    float p1Modifier = this.p1.influence / influenceSum;
				    float p2Modifier = this.p2.influence / influenceSum;
				    
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
}
