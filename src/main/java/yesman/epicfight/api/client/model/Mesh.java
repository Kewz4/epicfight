package yesman.epicfight.api.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;

@OnlyIn(Dist.CLIENT)
public interface Mesh {
	
	void initialize();
	
	/* Draw wihtout mesh deformation */
	void draw(PoseStack poseStack, VertexConsumer vertexConsumer, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay);
	
	/* Draw with mesh deformation */
	void drawPosed(PoseStack poseStack, VertexConsumer vertexConsumer, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay, Armature armature, OpenMatrix4f[] poses);
	
	@OnlyIn(Dist.CLIENT)
	public static record RenderProperties(ResourceLocation customTexturePath, Vec3f customColor, boolean isTransparent) {
		public static class Builder {
			protected String customTexturePath;
			protected Vec3f customColor = new Vec3f();
			protected boolean isTransparent;
			
			public RenderProperties.Builder customTexturePath(String path) {
				this.customTexturePath = path;
				return this;
			}
			
			public RenderProperties.Builder transparency(boolean isTransparent) {
				this.isTransparent = isTransparent;
				return this;
			}
			
			public RenderProperties.Builder customColor(float r, float g, float b) {
				this.customColor.x = r;
				this.customColor.y = g;
				this.customColor.z = b;
				return this;
			}
			
			public RenderProperties build() {
				return new RenderProperties(ResourceLocation.tryParse(this.customTexturePath), this.customColor, this.isTransparent);
			}
			
			public static RenderProperties.Builder create() {
				return new RenderProperties.Builder();
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	@FunctionalInterface
	public interface DrawingFunction {
		public static final DrawingFunction NEW_ENTITY = (builder, posX, posY, posZ, normX, normY, normZ, packedLight, r, g, b, a, u, v, overlay) -> {
			builder.vertex(posX, posY, posZ, r, g, b, a, u, v, overlay, packedLight, normX, normY, normZ);
		};
		
		public static final DrawingFunction POSITION_TEX_COLOR_NORMAL = (builder, posX, posY, posZ, normX, normY, normZ, packedLight, r, g, b, a, u, v, overlay) -> {
			builder.vertex(posX, posY, posZ);
			builder.uv(u, v);
			builder.color(r, g, b, a);
			builder.normal(normX, normY, normZ);
			builder.endVertex();
		};
		
		public static final DrawingFunction POSITION_COLOR_LIGHTMAP = (builder, posX, posY, posZ, normX, normY, normZ, packedLight, r, g, b, a, u, v, overlay) -> {
			builder.vertex(posX, posY, posZ);
			builder.color(r, g, b, a);
			builder.uv2(packedLight);
			builder.endVertex();
		};
		
		public static final DrawingFunction POSITION_COLOR_NORMAL = (builder, posX, posY, posZ, normX, normY, normZ, packedLight, r, g, b, a, u, v, overlay) -> {
			builder.vertex(posX, posY, posZ);
			builder.color(r, g, b, a);
			builder.normal(normX, normY, normZ);
			builder.endVertex();
		};
		
		public static final DrawingFunction POSITION_COLOR_TEX_LIGHTMAP = (builder, posX, posY, posZ, normX, normY, normZ, packedLight, r, g, b, a, u, v, overlay) -> {
			builder.vertex(posX, posY, posZ);
			builder.color(r, g, b, a);
			builder.uv(u, v);
			builder.uv2(packedLight);
			builder.endVertex();
		};
		
		public void draw(VertexConsumer builder, float posX, float posY, float posZ, float normX, float normY, float normZ, int packedLight, float r, float g, float b, float a, float u, float v, int overlay);
	}
}