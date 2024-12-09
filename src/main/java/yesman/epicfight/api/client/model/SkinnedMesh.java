package yesman.epicfight.api.client.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.SkinnedMesh.SkinnedMeshPart;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.model.JsonAssetLoader;
import yesman.epicfight.api.utils.GLConstants;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec4f;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.client.renderer.EpicFightVertexFormatElement;
import yesman.epicfight.client.renderer.shader.AnimationShaderInstance;
import yesman.epicfight.client.renderer.shader.ShaderParser;
import yesman.epicfight.main.EpicFightMod;

@OnlyIn(Dist.CLIENT)
public class SkinnedMesh extends StaticMesh<SkinnedMeshPart, SkinnedMeshVertexBuilder> {
	protected final float[] weights;
	protected final int[] affectingJointCounts;
	protected final int[][] affectingWeightIndices;
	protected final int[][] affectingJointIndices;
	
	private final int maxJointCount;
	private int arrayObjectId;
	
	private VertexBuffer<Float> positionsBuffer = new VertexBuffer<> (GLConstants.GL_FLOAT, 3, false, ByteBuffer::putFloat);
	private VertexBuffer<Float> uvsBuffer = new VertexBuffer<> (GLConstants.GL_FLOAT, 2, false, ByteBuffer::putFloat);
	private VertexBuffer<Byte> normalsBuffer = new VertexBuffer<> (GLConstants.GL_BYTE, 3, true, ByteBuffer::put);
	private VertexBuffer<Short> jointsBuffer = new VertexBuffer<> (GLConstants.GL_SHORT, 3, false, ByteBuffer::putShort);
	private VertexBuffer<Float> weightsBuffer = new VertexBuffer<> (GLConstants.GL_FLOAT, 3, false, ByteBuffer::putFloat);
	
	public SkinnedMesh(@Nullable Map<String, Number[]> arrayMap, @Nullable Map<MeshPartDefinition, List<SkinnedMeshVertexBuilder>> partBuilders, @Nullable SkinnedMesh parent, RenderProperties properties) {
		super(arrayMap, partBuilders, parent, properties);
		
		this.weights = parent == null ? ParseUtil.unwrapFloatWrapperArray(arrayMap.get("weights")) : parent.weights;
		this.affectingJointCounts = parent == null ? ParseUtil.unwrapIntWrapperArray(arrayMap.get("vcounts")) : parent.affectingJointCounts;
		
		if (parent != null) {
			this.affectingJointIndices = parent.affectingJointIndices;
			this.affectingWeightIndices = parent.affectingWeightIndices;
		} else {
			int[] vindices = ParseUtil.unwrapIntWrapperArray(arrayMap.get("vindices"));
			this.affectingJointIndices = new int[this.affectingJointCounts.length][];
			this.affectingWeightIndices = new int[this.affectingJointCounts.length][];
			int idx = 0;
			
			for (int i = 0; i < this.affectingJointCounts.length; i++) {
				int count = this.affectingJointCounts[i];
				int[] jointId = new int[count];
				int[] weights = new int[count];
				
				for (int j = 0; j < count; j++) {
					jointId[j] = vindices[idx * 2];
					weights[j] = vindices[idx * 2 + 1];
					idx++;
				}
				
				this.affectingJointIndices[i] = jointId;
				this.affectingWeightIndices[i] = weights;
			}
		}
		
		int maxJointId = 0;
		
		for (int[] i : this.affectingJointIndices) {
			for (int j : i) {
				if (maxJointId < j) {
					maxJointId = j;
				}
			}
		}
		
		this.maxJointCount = maxJointId;
		this.arrayObjectId = GlStateManager._glGenVertexArrays();
		
		List<Float> positionList = Lists.newArrayList();
		List<Float> uvList = Lists.newArrayList();
		List<Byte> normalList = Lists.newArrayList();
		List<Short> jointList = Lists.newArrayList();
		List<Float> weightList = Lists.newArrayList();
		Map<SkinnedMeshVertexBuilder, Integer> vertexBuilderMap = Maps.newHashMap();
		
		int currentBoundVao = GlStateManager._getInteger(GLConstants.GL_VERTEX_ARRAY_BINDING);
		int currentBoundVbo = GlStateManager._getInteger(GLConstants.GL_VERTEX_ARRAY_BUFFER_BINDING);
		
		GlStateManager._glBindVertexArray(this.arrayObjectId);
		
		for (SkinnedMeshPart part : this.parts.values()) {
			part.createVbo(vertexBuilderMap, this.positions, this.uvs, this.normals, this.weights, this.affectingJointCounts, this.affectingJointIndices, this.affectingWeightIndices, positionList, uvList, normalList, jointList, weightList);
		}
		
		this.positionsBuffer.bindVertexData(positionList);
		this.uvsBuffer.bindVertexData(uvList);
		this.normalsBuffer.bindVertexData(normalList);
		this.jointsBuffer.bindVertexData(jointList);
		this.weightsBuffer.bindVertexData(weightList);
		
		GlStateManager._glBindVertexArray(currentBoundVao);
		GlStateManager._glBindBuffer(GLConstants.GL_ARRAY_BUFFER, currentBoundVbo);
	}
	
	public void pointPositionsBuffer(int attrIndex) {
		this.positionsBuffer.vertexAttribPointer(attrIndex);
	}
	
	public void uvPositionsBuffer(int attrIndex) {
		this.uvsBuffer.vertexAttribPointer(attrIndex);
	}
	
	public void normalPositionsBuffer(int attrIndex) {
		this.normalsBuffer.vertexAttribPointer(attrIndex);
	}
	
	public void jointPositionsBuffer(int attrIndex) {
		this.jointsBuffer.vertexAttribPointer(attrIndex);
	}
	
	public void weightPositionsBuffer(int attrIndex) {
		this.weightsBuffer.vertexAttribPointer(attrIndex);
	}
	
	public void destroy() {
		this.positionsBuffer.destroy();
		this.uvsBuffer.destroy();
		this.normalsBuffer.destroy();
		this.jointsBuffer.destroy();
		this.weightsBuffer.destroy();
        this.parts.values().forEach(part -> RenderSystem.glDeleteBuffers(part.indexBufferId));
        
        RenderSystem.glDeleteVertexArrays(this.arrayObjectId);
        this.arrayObjectId = -1;
	}
	
	@Override
	protected Map<String, SkinnedMeshPart> createModelPart(Map<MeshPartDefinition, List<SkinnedMeshVertexBuilder>> partBuilders) {
		Map<String, SkinnedMeshPart> parts = Maps.newHashMap();
		
		partBuilders.forEach((partDefinition, vertexBuilder) -> {
			parts.put(partDefinition.partName(), new SkinnedMeshPart(vertexBuilder, partDefinition.getModelPartAnimationProvider(), partDefinition.clothInfo()));
		});
		
		return parts;
	}
	
	@Override
	protected SkinnedMeshPart getOrLogException(Map<String, SkinnedMeshPart> parts, String name) {
		if (!parts.containsKey(name)) {
			EpicFightMod.LOGGER.debug("Cannot find the mesh part named " + name + " in " + this.getClass().getCanonicalName());
			return null;
		}
		
		return parts.get(name);
	}
	
	/**
	 * Draws the model without applying animation
	 */
	@Override
	public void draw(PoseStack poseStack, VertexConsumer vertexConsumer, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
		for (SkinnedMeshPart part : this.parts.values()) {
			part.draw(poseStack, vertexConsumer, drawingFunction, packedLight, r, g, b, a, overlay);
		}
	}
	
	protected static final OpenMatrix4f[] FINAL_POSES = OpenMatrix4f.allocateMatrix(ShaderParser.MAX_JOINTS);
	protected static final OpenMatrix4f[] NORMAL_POSES = OpenMatrix4f.allocateMatrix(ShaderParser.MAX_JOINTS);
	
	/**
	 * Draws the model to vanilla buffer
	 */
	@Override
	public void drawPosed(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay, Armature armature, OpenMatrix4f[] poses) {
		Matrix4f matrix4f = poseStack.last().pose();
		Matrix3f matrix3f = poseStack.last().normal();
		
		for (SkinnedMeshPart part : this.parts.values()) {
			if (!part.isHidden()) {
				OpenMatrix4f transform = part.getVanillaPartTransform();
				
				for (int i = 0; i < poses.length; i++) {
					FINAL_POSES[i].load(poses[i]);
					
					if (armature != null) {
						FINAL_POSES[i].mulBack(armature.searchJointById(i).getToOrigin());
					}
					
					if (transform != null) {
						FINAL_POSES[i].mulBack(transform);
					}
					
					NORMAL_POSES[i] = FINAL_POSES[i].removeTranslation();
				}
				
				for (SkinnedMeshVertexBuilder vi : part.getVertices()) {
					vi.getVertexPosition(SkinnedMesh.this, POSITION, FINAL_POSES);
					vi.getVertexNormal(SkinnedMesh.this, NORMAL, NORMAL_POSES);
					
					POSITION.mul(matrix4f);
					NORMAL.mul(matrix3f);
					
					drawingFunction.draw(builder, POSITION.x, POSITION.y, POSITION.z, NORMAL.x, NORMAL.y, NORMAL.z, packedLight, r, g, b, a, this.uvs[vi.uv * 2], this.uvs[vi.uv * 2 + 1], overlay);
				}
			}
		}
	}
	
	/**
	 * Draws the model depending on animation shader option
	 * @param armature give this parameter as null if @param poses already bound origin translation
	 * @param poses
	 */
	public void draw(PoseStack poseStack, MultiBufferSource multiBufferSource, RenderType renderType, int packedLight, float r, float g, float b, float a, int overlay, Armature armature, OpenMatrix4f[] poses) {
		if (EpicFightMod.CLIENT_CONFIGS.useAnimationShader.getValue()) {
			renderType.setupRenderState();
			AnimationShaderInstance animationShader = EpicFightRenderTypes.getAnimationShader(renderType);
			this.drawWithShader(poseStack, animationShader, packedLight, 1.0F, 1.0F, 1.0F, 1.0F, overlay, armature, poses);
			renderType.clearRenderState();
		} else {
			VertexConsumer vertexConsumer = multiBufferSource.getBuffer(EpicFightRenderTypes.getTriangulated(renderType));
			this.drawPosed(poseStack, vertexConsumer, Mesh.DrawingFunction.ENTITY_TEXTURED, packedLight, r, g, b, a, overlay, armature, poses);
		}
	}
	
	/**
	 * Draw the model with shader optimization by shader and vertex format
	 */
	public void drawWithShader(PoseStack poseStack, ShaderInstance shader, int packedLight, float r, float g, float b, float a, int overlay, Armature armature, OpenMatrix4f[] poses) {
		AnimationShaderInstance animationShader = EpicFightRenderTypes.getAnimationShader(shader);
		this.drawWithShader(poseStack, animationShader, packedLight, 1.0F, 1.0F, 1.0F, 1.0F, OverlayTexture.NO_OVERLAY, armature, poses);
	}
	
	public void drawWithShader(PoseStack poseStack, AnimationShaderInstance animationShaderInstance, int packedLight, float r, float g, float b, float a, int overlay, Armature armature, OpenMatrix4f[] poses) {
		if (this.arrayObjectId < 0) {
			throw new IllegalStateException("Mesh destroyed");
		}
		
		if (animationShaderInstance == null) {
			return;
		}
		
		for (int i = 0; i < 12; ++i) {
			int j = RenderSystem.getShaderTexture(i);
			animationShaderInstance._setSampler("Sampler" + i, j);
		}
		
		if (animationShaderInstance.getModelViewMatrixUniform() != null) {
			animationShaderInstance.getModelViewMatrixUniform().set(poseStack.last().pose());
		}
		
		if (animationShaderInstance.getProjectionMatrixUniform() != null) {
			animationShaderInstance.getProjectionMatrixUniform().set(RenderSystem.getProjectionMatrix());
		}
		
		if (animationShaderInstance.getNormalMatrixUniform() != null) {
			animationShaderInstance.getNormalMatrixUniform().set(poseStack.last().normal());
		}
		
		if (animationShaderInstance.getInverseViewRotationMatrixUniform() != null) {
			animationShaderInstance.getInverseViewRotationMatrixUniform().set(RenderSystem.getInverseViewRotationMatrix());
		}
		
		if (animationShaderInstance.getColorModulatorUniform() != null) {
			animationShaderInstance.getColorModulatorUniform().set(RenderSystem.getShaderColor());
		}
		
		if (animationShaderInstance.getGlintAlphaUniform() != null) {
			animationShaderInstance.getGlintAlphaUniform().set(RenderSystem.getShaderGlintAlpha());
		}
		
		if (animationShaderInstance.getFogStartUniform() != null) {
			animationShaderInstance.getFogStartUniform().set(RenderSystem.getShaderFogStart());
		}
		
		if (animationShaderInstance.getFogEndUniform() != null) {
			animationShaderInstance.getFogEndUniform().set(RenderSystem.getShaderFogEnd());
		}
		
		if (animationShaderInstance.getFogColorUniform() != null) {
			animationShaderInstance.getFogColorUniform().set(RenderSystem.getShaderFogColor());
		}
		
		if (animationShaderInstance.getFogShapeUniform() != null) {
			animationShaderInstance.getFogShapeUniform().set(RenderSystem.getShaderFogShape().getIndex());
		}
		
		if (animationShaderInstance.getTextureMatrixUniform() != null) {
			animationShaderInstance.getTextureMatrixUniform().set(RenderSystem.getTextureMatrix());
		}
		
		if (animationShaderInstance.getGameTimeUniform() != null) {
			animationShaderInstance.getGameTimeUniform().set(RenderSystem.getShaderGameTime());
		}
		
		if (animationShaderInstance.getScreenSizeUniform() != null) {
			Window window = Minecraft.getInstance().getWindow();
			animationShaderInstance.getScreenSizeUniform().set((float) window.getWidth(), (float) window.getHeight());
		}
		
		if (animationShaderInstance.getColorUniform() != null) {
			animationShaderInstance.getColorUniform().set(r, g, b, a);
		}
		
		if (animationShaderInstance.getOverlayUniform() != null) {
			animationShaderInstance.getOverlayUniform().set(overlay & '\uffff', overlay >> 16 & '\uffff');
		}
		
		if (animationShaderInstance.getLightUniform() != null) {
			animationShaderInstance.getLightUniform().set(packedLight & '\uffff', packedLight >> 16 & '\uffff');
		}
		
		animationShaderInstance.setupShaderLights();
		
		int currentBoundVao = GlStateManager._getInteger(GLConstants.GL_VERTEX_ARRAY_BINDING);
		int currentBoundVbo = GlStateManager._getInteger(GLConstants.GL_VERTEX_ARRAY_BUFFER_BINDING);
		
		GlStateManager._glBindVertexArray(this.arrayObjectId);
		EpicFightVertexFormatElement.bindDrawing(this);
		
		for (SkinnedMeshPart part : this.parts.values()) {
			part.drawWithShader(animationShaderInstance, armature, poses);
		}
		
		EpicFightVertexFormatElement.unbindDrawing();
		
		GlStateManager._glBindVertexArray(currentBoundVao);
		GlStateManager._glBindBuffer(GLConstants.GL_ARRAY_BUFFER, currentBoundVbo);
	}
	
	public int getMaxJointCount() {
		return this.maxJointCount;
	}
	
	@OnlyIn(Dist.CLIENT)
	public class SkinnedMeshPart extends MeshPart<SkinnedMeshVertexBuilder> {
		private int indexBufferId;
		
		public SkinnedMeshPart(List<SkinnedMeshVertexBuilder> animatedMeshPartList, @Nullable Supplier<OpenMatrix4f> vanillaPartTracer, @Nullable SoftBodyMesh.ClothSimulationInfo clothInfo) {
			super(animatedMeshPartList, vanillaPartTracer, clothInfo);
		}
		
		private void createVbo(
				Map<SkinnedMeshVertexBuilder, Integer> vertexBuilderMap
			  , float[] positions
			  , float[] uvs
			  , float[] normals
			  , float[] weights
			  , int[] affectingJointCounts
			  , int[][] affectingJointIndices
			  , int[][] affectingWeightsIndices
			  , List<Float> position
			  , List<Float> uv
			  , List<Byte> normal
			  , List<Short> joint
			  , List<Float> weight
		) {
			ByteBuffer indicesBuffer = ByteBuffer.allocateDirect(this.getVertices().size() * 4).order(ByteOrder.nativeOrder());
			
			for (SkinnedMeshVertexBuilder vb : this.getVertices()) {
				if (vertexBuilderMap.containsKey(vb)) {
					indicesBuffer.putInt(vertexBuilderMap.get(vb));
				} else {
					int next = vertexBuilderMap.size();
					indicesBuffer.putInt(next);
					vertexBuilderMap.put(vb, next);
					position.add(positions[vb.position * 3]);
					position.add(positions[vb.position * 3 + 1]);
					position.add(positions[vb.position * 3 + 2]);
					uv.add(uvs[vb.uv * 2]);
					uv.add(uvs[vb.uv * 2 + 1]);
					normal.add(packNormal(normals[vb.normal * 3]));
					normal.add(packNormal(normals[vb.normal * 3 + 1]));
					normal.add(packNormal(normals[vb.normal * 3 + 2]));
					joint.add(affectingJointCounts[vb.position] > 0 ? (short)affectingJointIndices[vb.position][0] : -1);
					joint.add(affectingJointCounts[vb.position] > 1 ? (short)affectingJointIndices[vb.position][1] : -1);
					joint.add(affectingJointCounts[vb.position] > 2 ? (short)affectingJointIndices[vb.position][2] : -1);
					weight.add(affectingJointCounts[vb.position] > 0 ? weights[affectingWeightsIndices[vb.position][0]] : 0.0F);
					weight.add(affectingJointCounts[vb.position] > 1 ? weights[affectingWeightsIndices[vb.position][1]] : 0.0F);
					weight.add(affectingJointCounts[vb.position] > 2 ? weights[affectingWeightsIndices[vb.position][2]] : 0.0F);
				}
			}
			
			indicesBuffer.flip();
			
			this.indexBufferId = GlStateManager._glGenBuffers();
			GlStateManager._glBindBuffer(GLConstants.GL_ELEMENT_ARRAY_BUFFER, this.indexBufferId);
			GlStateManager._glBufferData(GLConstants.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GLConstants.GL_STATIC_DRAW);
			GlStateManager._glBindBuffer(GLConstants.GL_ELEMENT_ARRAY_BUFFER, 0);
		}
		
		@Override
		public void draw(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
			if (this.isHidden()) {
				return;
			}
			
			Matrix4f matrix4f = poseStack.last().pose();
			Matrix3f matrix3f = poseStack.last().normal();
			
			for (SkinnedMeshVertexBuilder vi : this.getVertices()) {
				vi.getVertexPosition(SkinnedMesh.this, POSITION);
				vi.getVertexNormal(SkinnedMesh.this, NORMAL);
				POSITION.mul(matrix4f);
				NORMAL.mul(matrix3f);
				
				drawingFunction.draw(builder, POSITION.x(), POSITION.y(), POSITION.z(), NORMAL.x(), NORMAL.y(), NORMAL.z(), packedLight, r, g, b, a, uvs[vi.uv * 2], uvs[vi.uv * 2 + 1], overlay);
			}
		}
		
		public void drawWithShader(AnimationShaderInstance animationShaderInstance, Armature armature, OpenMatrix4f[] poses) {
			if (this.isHidden()) {
				return;
			}
			
			OpenMatrix4f transform = this.getVanillaPartTransform();
			
			for (int i = 0; i < poses.length; i++) {
				FINAL_POSES[i].load(poses[i]);
				
				if (armature != null) {
					FINAL_POSES[i].mulBack(armature.searchJointById(i).getToOrigin());
				}
				
				if (transform != null) {
					FINAL_POSES[i].mulBack(transform);
				}
			}
			
			for (int i = 0; i < FINAL_POSES.length; i++) {
				if (animationShaderInstance.getPoses(i) != null) {
					animationShaderInstance.getPoses(i).set(OpenMatrix4f.exportToMojangMatrix(FINAL_POSES[i]));
				}
			}
			
			animationShaderInstance._getVertexFormat().setupBufferState();
			animationShaderInstance._apply();
			
			GlStateManager._glBindBuffer(GLConstants.GL_ELEMENT_ARRAY_BUFFER, this.indexBufferId);
			RenderSystem.drawElements(VertexFormat.Mode.TRIANGLES.asGLMode, this.getVertices().size(), VertexFormat.IndexType.INT.asGLType);
			GlStateManager._glBindBuffer(GLConstants.GL_ELEMENT_ARRAY_BUFFER, 0);
			
			animationShaderInstance._clear();
			animationShaderInstance._getVertexFormat().clearBufferState();
		}
		
		static byte packNormal(float f) {
			return (byte)((int)(Mth.clamp(f, -1.0F, 1.0F) * 127.0F) & 255);
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	private class VertexBuffer<T extends Number> {
		private int vertexBufferIds;
		private final int glType;
		private final int size;
		private final boolean normalize;
		private final BiConsumer<ByteBuffer, T> bufferUploader;
		
		public VertexBuffer(int glType, int size, boolean normalize, BiConsumer<ByteBuffer, T> bufferUploader) {
			this.vertexBufferIds = GlStateManager._glGenBuffers();
			this.glType = glType;
			this.size = size;
			this.normalize = normalize;
			this.bufferUploader = bufferUploader;
		}
		
		public void bindVertexData(List<T> data) {
			if (this.vertexBufferIds < 0) {
				throw new RuntimeException("vertex buffer is already destroyed");
			}
			
			ByteBuffer buf = ByteBuffer.allocateDirect(data.size() * 4).order(ByteOrder.nativeOrder());
			
			for (T f : data) {
				this.bufferUploader.accept(buf, f);
			}
			
			buf.flip();
			
			GlStateManager._glBindBuffer(GLConstants.GL_ARRAY_BUFFER, this.vertexBufferIds);
			GlStateManager._glBufferData(GLConstants.GL_ARRAY_BUFFER, buf, GLConstants.GL_STATIC_DRAW);
			GlStateManager._glBindBuffer(GLConstants.GL_ARRAY_BUFFER, 0);
		}
		
		public void vertexAttribPointer(int attrIndex) {
			if (this.vertexBufferIds < 0) {
				throw new RuntimeException("vertex buffer is already destroyed");
			}
			
			GlStateManager._glBindBuffer(GLConstants.GL_ARRAY_BUFFER, this.vertexBufferIds);
			
			switch (this.glType) {
			case GLConstants.GL_DOUBLE, GLConstants.GL_FLOAT -> {
				GlStateManager._vertexAttribPointer(attrIndex, this.size, this.glType, this.normalize, 0, 0);
			}
			case GLConstants.GL_BYTE, GLConstants.GL_SHORT, GLConstants.GL_INT -> {
				if (this.normalize) {
					GlStateManager._vertexAttribPointer(attrIndex, this.size, this.glType, true, 0, 0);
				} else {
					GlStateManager._vertexAttribIPointer(attrIndex, this.size, this.glType, 0, 0);
				}
			}
			}
		}
		
		public void destroy() {
			RenderSystem.glDeleteBuffers(this.vertexBufferIds);
			this.vertexBufferIds = -1;
		}
	}
	
	/**
	 * Export this model as Json format
	 */
	public JsonObject toJsonObject() {
		JsonObject root = new JsonObject();
		JsonObject vertices = new JsonObject();
		float[] positions = this.positions.clone();
		float[] normals = this.normals.clone();
		
		for (int i = 0; i < positions.length / 3; i++) {
			int k = i * 3;
			Vec4f posVector = new Vec4f(positions[k], positions[k+1], positions[k+2], 1.0F);
			posVector.transform(JsonAssetLoader.MINECRAFT_TO_BLENDER_COORD);
			positions[k] = posVector.x;
			positions[k+1] = posVector.y;
			positions[k+2] = posVector.z;
		}
		
		for (int i = 0; i < normals.length / 3; i++) {
			int k = i * 3;
			Vec4f normVector = new Vec4f(normals[k], normals[k+1], normals[k+2], 1.0F);
			normVector.transform(JsonAssetLoader.MINECRAFT_TO_BLENDER_COORD);
			normals[k] = normVector.x;
			normals[k+1] = normVector.y;
			normals[k+2] = normVector.z;
		}
		
		IntList affectingJointAndWeightIndices = new IntArrayList();
		
		for (int i = 0; i < this.affectingJointCounts.length; i++) {
			for (int j = 0; j < this.affectingJointCounts[j]; j++) {
				affectingJointAndWeightIndices.add(this.affectingJointIndices[i][j]);
				affectingJointAndWeightIndices.add(this.affectingWeightIndices[i][j]);
			}
		}
		
		vertices.add("positions", ParseUtil.farrayToJsonObject(positions, 3));
		vertices.add("uvs", ParseUtil.farrayToJsonObject(this.uvs, 2));
		vertices.add("normals", ParseUtil.farrayToJsonObject(normals, 3));
		vertices.add("vcounts", ParseUtil.iarrayToJsonObject(this.affectingJointCounts, 1));
		vertices.add("weights", ParseUtil.farrayToJsonObject(this.weights, 1));
		vertices.add("vindices", ParseUtil.iarrayToJsonObject(affectingJointAndWeightIndices.toIntArray(), 1));
		
		if (!this.parts.isEmpty()) {
			JsonObject parts = new JsonObject();
			
			for (Map.Entry<String, SkinnedMeshPart> partEntry : this.parts.entrySet()) {
				IntList indicesArray = new IntArrayList();
				
				for (SkinnedMeshVertexBuilder vertexIndicator : partEntry.getValue().getVertices()) {
					indicesArray.add(vertexIndicator.position);
					indicesArray.add(vertexIndicator.uv);
					indicesArray.add(vertexIndicator.normal);
				}
				
				parts.add(partEntry.getKey(), ParseUtil.iarrayToJsonObject(indicesArray.toIntArray(), 3));
			}
			
			vertices.add("parts", parts);
		} else {
			int i = 0;
			int[] indices = new int[this.vertexCount * 3];
			
			for (SkinnedMeshPart part : this.parts.values()) {
				for (SkinnedMeshVertexBuilder vertexIndicator : part.getVertices()) {
					indices[i * 3] = vertexIndicator.position;
					indices[i * 3 + 1] = vertexIndicator.uv;
					indices[i * 3 + 2] = vertexIndicator.normal;
					i++;
				}
			}
			
			vertices.add("indices", ParseUtil.iarrayToJsonObject(indices, 3));
		}
		
		root.add("vertices", vertices);
		
		if (this.renderProperties != null) {
			JsonObject renderProperties = new JsonObject();
			renderProperties.addProperty("texture_path", this.renderProperties.getCustomTexturePath());
			renderProperties.addProperty("transparent", this.renderProperties.isTransparent());
			root.add("render_properties", renderProperties);
		}
		
		return root;
	}
}