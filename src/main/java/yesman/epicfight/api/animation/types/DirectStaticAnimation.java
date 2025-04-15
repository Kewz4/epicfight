package yesman.epicfight.api.animation.types;

import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.property.JointMaskEntry;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class DirectStaticAnimation extends StaticAnimation implements AnimationAccessor<DirectStaticAnimation> {
	private ResourceLocation registryName;
	
	public DirectStaticAnimation() {
		this.accessor = this;
	}
	
	public DirectStaticAnimation(float transitionTime, boolean isRepeat, ResourceLocation registryName, AssetAccessor<? extends Armature> armature) {
		super(transitionTime, isRepeat, registryName.toString(), armature);
		
		this.registryName = registryName;
		this.accessor = this;
	}
	
	/* Multilayer Constructor */
	public DirectStaticAnimation(ResourceLocation baseAnimPath, float transitionTime, boolean repeatPlay, String registryName, AssetAccessor<? extends Armature> armature) {
		super(baseAnimPath, transitionTime, repeatPlay, registryName, armature);
		
		this.registryName = ResourceLocation.parse(registryName);
	}
	
	@Override
	public Optional<JointMaskEntry> getJointMaskEntry(LivingEntityPatch<?> entitypatch, boolean useCurrentMotion) {
		return super.getJointMaskEntry(entitypatch, useCurrentMotion);
	}
	
	@Override
	public DirectStaticAnimation get() {
		return this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <A extends DynamicAnimation> AnimationAccessor<A> getAccessor() {
		return (AnimationAccessor<A>)this;
	}
	
	@Override
	public ResourceLocation registryName() {
		return this.registryName;
	}

	@Override
	public boolean isPresent() {
		return true;
	}

	@Override
	public int id() {
		return -1;
	}
	
	@Override
	public int getId() {
		return -1;
	}
	
	@Override
	public boolean inRegistry() {
		return false;
	}
}