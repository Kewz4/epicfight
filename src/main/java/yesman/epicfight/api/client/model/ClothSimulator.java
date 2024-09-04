package yesman.epicfight.api.client.model;

import java.util.List;
import java.util.function.BooleanSupplier;

import org.apache.commons.compress.utils.Lists;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import yesman.epicfight.api.utils.math.Vec3f;

@OnlyIn(Dist.CLIENT)
public class ClothSimulator {
	public static final Capability<ClothSimulator> CAPABILITY_KEY = CapabilityManager.get(new CapabilityToken<>(){});
	
	private final List<ClothPart> clothParts = Lists.newArrayList();
	
	public void addPart(BooleanSupplier validator, ClothModelPart clothPart) {
		
	}
	
	public void tick() {
		this.clothParts.removeIf((clothPart) -> !clothPart.validator.getAsBoolean());
	}
	
	public void draw(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
		
	}
	
	@OnlyIn(Dist.CLIENT)
	public class ClothPart {
		private final BooleanSupplier validator;
		private final List<Vertex> vertices = Lists.newArrayList();
		private final List<Link> link = Lists.newArrayList();
		
		private ClothPart(BooleanSupplier validator) {
			this.validator = validator;
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public class Vertex {
		private final Vec3f oPosition;
		private final Vec3f position;
		private boolean isFixed;
		
		private Vertex(Vec3f position, boolean isFixed) {
			this.oPosition = position.copy();
			this.position = position;
			this.isFixed = isFixed;
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public class Link {
		private final Vertex v1;
		private final Vertex v2;
		
		public Link(Vertex v1, Vertex v2) {
			this.v1 = v1;
			this.v2 = v2;
		}
	}
}
