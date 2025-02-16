package yesman.epicfight.api.client.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.common.collect.ImmutableList;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.physics.cloth.ClothSimulatable;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator.ClothObject;
import yesman.epicfight.api.utils.ParseUtil;

@OnlyIn(Dist.CLIENT)
public abstract class StaticMesh<P extends MeshPart<V>, V extends VertexBuilder<?>> implements Mesh, SoftBodyTranslatable {
	protected static final Vector4f POSITION = new Vector4f();
	protected static final Vector3f NORMAL = new Vector3f();
	
	protected final float[] positions;
	protected final float[] normals;
	protected final float[] uvs;
	
	protected final int vertexCount;
	protected final RenderProperties renderProperties;
	protected final Map<String, P> parts;
	protected final List<Vec3> normalList;
	
	/**
	 * @param arrayMap Null if parent is not null
	 * @param partBuilders Null if parent is not null
	 * @param parent Null if arrayMap and parts are not null
	 * @param renderProperties
	 */
	public StaticMesh(@Nullable Map<String, Number[]> arrayMap, @Nullable Map<MeshPartDefinition, List<V>> partBuilders, @Nullable StaticMesh<P, V> parent, RenderProperties renderProperties) {
		this.positions = (parent == null) ? ParseUtil.unwrapFloatWrapperArray(arrayMap.get("positions")) : parent.positions;
		this.normals = (parent == null) ? ParseUtil.unwrapFloatWrapperArray(arrayMap.get("normals")) : parent.normals;
		this.uvs = (parent == null) ? ParseUtil.unwrapFloatWrapperArray(arrayMap.get("uvs")) : parent.uvs;
		this.parts = (parent == null) ? this.createModelPart(partBuilders) : parent.parts;
		this.renderProperties = renderProperties;
		
		int totalV = 0;
		
		for (MeshPart<V> modelpart : this.parts.values()) {
			totalV += modelpart.getVertices().size();
		}
		
		this.vertexCount = totalV;
		
		if (this.canStartSoftBodySimulation()) {
			ImmutableList.Builder<Vec3> normalBuilder = ImmutableList.builder();
			
			for (int i = 0; i < this.normals.length / 3; i++) {
				normalBuilder.add(new Vec3(this.normals[i * 3], this.normals[i * 3 + 1], this.normals[i * 3 + 2]));
			}
			
			this.normalList = normalBuilder.build();
		} else {
			this.normalList = null;
		}
	}
	
	protected abstract Map<String, P> createModelPart(Map<MeshPartDefinition, List<V>> partBuilders);
	protected abstract P getOrLogException(Map<String, P> parts, String name);
	
	public boolean hasPart(String part) {
		return this.parts.containsKey(part);
	}
	
	public MeshPart<V> getPart(String part) {
		return this.parts.get(part);
	}
	
	public Collection<P> getAllParts() {
		return this.parts.values();
	}
	
	public RenderProperties getRenderProperty() {
		return this.renderProperties;
	}
	
	public float[] positions() {
		return this.positions;
	}
	
	public float[] normals() {
		return this.normals;
	}
	
	public float[] uvs() {
		return this.uvs;
	}
	
	@Nullable
	public List<Vec3> normalList() {
		return this.normalList;
	}
	
	@Override
	public void initialize() {
		this.parts.values().forEach((part) -> part.setHidden(false));
	}
	
	@Override
	public boolean canStartSoftBodySimulation() {
		boolean hasSimulInfo = true;
		
		for (MeshPart<?> part : this.parts.values()) {
			hasSimulInfo &= part.clothInfo != null;
		}
		
		return hasSimulInfo;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ClothSimulator.ClothObject createSimulationData(@Nullable SoftBodyTranslatable provider, ClothSimulatable simObject, ClothSimulator.ClothObjectBuilder simBuilder) {
		return new ClothObject(simBuilder, provider == null ? this : provider, (Map<String, MeshPart<VertexBuilder<StaticMesh<?, ?>>>>)this.parts, this.positions);
	}
}