package yesman.epicfight.api.client.model;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

@OnlyIn(Dist.CLIENT)
public abstract class MeshPart<T extends VertexBuilder> {
	protected final List<T> verticies;
	protected final Supplier<OpenMatrix4f> vanillaPartTracer;
	protected boolean isHidden;
	
	public MeshPart(List<T> vertices, @Nullable Supplier<OpenMatrix4f> vanillaPartTracer) {
		this.verticies = vertices;
		this.vanillaPartTracer = vanillaPartTracer;
	}
	
	public abstract void draw(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay);
	
	public void setHidden(boolean hidden) {
		this.isHidden = hidden;
	}
	
	public boolean isHidden() {
		return this.isHidden;
	}
	
	public List<T> getVertices() {
		return this.verticies;
	}
	
	public OpenMatrix4f getVanillaPartTransform() {
		if (this.vanillaPartTracer == null) {
			return null;
		}
		
		return this.vanillaPartTracer.get();
	}
}