package yesman.epicfight.api.animation.types;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.model.Armature;

public class DirectStaticAnimation extends StaticAnimation implements AnimationAccessor<DirectStaticAnimation> {
	public DirectStaticAnimation() {
		super();
	}
	
	public DirectStaticAnimation(float convertTime, boolean isRepeat, String path, Armature armature) {
		super(convertTime, isRepeat, path, armature);
	}
	
	/* Multilayer Constructor */
	public DirectStaticAnimation(ResourceLocation baseAnimPath, float transitionTime, boolean repeatPlay, String registryName, Armature armature) {
		super(transitionTime, repeatPlay, baseAnimPath.toString(), armature);
	}
	
	@Override
	public DirectStaticAnimation get() {
		return this;
	}

	@Override
	public ResourceLocation registryName() {
		return null;
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