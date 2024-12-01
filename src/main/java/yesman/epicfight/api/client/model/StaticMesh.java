package yesman.epicfight.api.client.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class StaticMesh<P extends MeshPart<V>, V extends VertexBuilder> implements Mesh {
	protected final float[] positions;
	protected final float[] normals;
	protected final float[] uvs;
	
	protected final int vertexCount;
	protected final RenderProperties renderProperties;
	protected final Map<String, P> parts;
	
	/**
	 * @param arrayMap Null if parent is not null
	 * @param partBuilders Null if parent is not null
	 * @param parent Null if arrayMap and parts are not null
	 * @param renderProperties
	 */
	public StaticMesh(@Nullable Map<String, float[]> arrayMap, @Nullable Map<MeshPartDefinition, List<V>> partBuilders, @Nullable StaticMesh<P, V> parent, RenderProperties renderProperties) {
		this.positions = (parent == null) ? arrayMap.get("positions") : parent.positions;
		this.normals = (parent == null) ? arrayMap.get("normals") : parent.normals;
		this.uvs = (parent == null) ? arrayMap.get("uvs") : parent.uvs;
		this.renderProperties = renderProperties;
		this.parts = (parent == null) ? this.createModelPart(partBuilders) : parent.parts;
		
		int totalV = 0;
		
		for (MeshPart<V> modelpart : this.parts.values()) {
			totalV += modelpart.getVertices().size();
		}
		
		this.vertexCount = totalV;
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
	
	public void initialize() {
		this.parts.values().forEach((part) -> part.setHidden(false));
	}
}