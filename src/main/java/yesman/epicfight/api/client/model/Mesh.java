package yesman.epicfight.api.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

@OnlyIn(Dist.CLIENT)
public interface Mesh {
	/* Draw classic mesh */
	void draw(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay);
	/* Draw mesh with animation */
	void drawPosed(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay, Armature armature, OpenMatrix4f[] poses);
	
	void initialize();
	
	@OnlyIn(Dist.CLIENT)
	public static class RenderProperties {
		protected String customTexturePath;
		protected boolean isTransparent;
		protected Object2BooleanMap<String> parentPartVisualizer;
		
		public String getCustomTexturePath() {
			return this.customTexturePath;
		}
		
		public boolean isTransparent() {
			return this.isTransparent;
		}
		
		public Object2BooleanMap<String> getParentPartVisualizer() {
			return this.parentPartVisualizer;
		}
		
		public RenderProperties customTexturePath(String path) {
			this.customTexturePath = path;
			return this;
		}
		
		public RenderProperties transparency(boolean isTransparent) {
			this.isTransparent = isTransparent;
			return this;
		}
		
		public RenderProperties newPartVisualizer(String partName, boolean setVisible) {
			if (this.parentPartVisualizer == null) {
				this.parentPartVisualizer = new Object2BooleanOpenHashMap<>();
			}
			
			this.parentPartVisualizer.put(partName, setVisible);
			
			return this;
		}
		
		public static RenderProperties create() {
			return new RenderProperties();
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
		
		public static final DrawingFunction POSITION_COLOR_TEXTURE_LIGHTMAP = (builder, posX, posY, posZ, normX, normY, normZ, packedLight, r, g, b, a, u, v, overlay) -> {
			builder.vertex(posX, posY, posZ);
			builder.color(r, g, b, a);
			builder.uv(u, v);
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