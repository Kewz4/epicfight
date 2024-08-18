package yesman.epicfight.api.client.model;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

@OnlyIn(Dist.CLIENT)
public class SpringMassModelPart extends ModelPart<VertexBuilder> {
	public SpringMassModelPart(List<VertexBuilder> vertices, Supplier<OpenMatrix4f> vanillaPartTracer) {
		super(vertices, vanillaPartTracer);
	}
	
	@Override
	public void draw(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
		
	}
}
