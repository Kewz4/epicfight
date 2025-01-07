package yesman.epicfight.api.animation.types;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;

public class DirectStaticAnimation extends StaticAnimation implements AnimationAccessor<DirectStaticAnimation> {
	private ResourceLocation registryName;
	
	public DirectStaticAnimation() {
		super();
		this.accessor = this;
	}
	
	public DirectStaticAnimation(float transitionTime, boolean isRepeat, AnimationAccessor<? extends StaticAnimation> owner, ResourceLocation registryName, AssetAccessor<? extends Armature> armature) {
		super(transitionTime, isRepeat, registryName.getPath(), armature);
		
		this.registryName = registryName;
		this.accessor = owner;
	}
	
	/* Multilayer Constructor */
	public DirectStaticAnimation(ResourceLocation baseAnimPath, float transitionTime, boolean repeatPlay, String registryName, AssetAccessor<? extends Armature> armature) {
		super(transitionTime, repeatPlay, baseAnimPath.toString(), armature);
		
		this.registryName = ResourceLocation.tryParse(registryName);
	}
	
	@Override
	public DirectStaticAnimation get() {
		return this;
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
}