package yesman.epicfight.api.asset;

import java.util.function.Function;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;

public record SelfAccessor<A>(ResourceLocation registryName, A asset) implements AssetAccessor<A> {
	public static <A> SelfAccessor<A> create(ResourceLocation registryName, A asset) {
		return new SelfAccessor<> (registryName, asset);
	}
	
	@Override
	public A get() {
		return this.asset;
	}

	@Override
	public boolean isPresent() {
		return true;
	}

	@Override
	public boolean inRegistry() {
		return false;
	}
	
	public static class SelfAnimationAccessor<A extends StaticAnimation> implements AnimationAccessor<A> {
		public static <A extends StaticAnimation> SelfAnimationAccessor<A> createAnimation(ResourceLocation registryName, Function<AnimationAccessor<A>, A> onLoad) {
			return new SelfAnimationAccessor<> (registryName, onLoad);
		}
		
		private final ResourceLocation registryName;
		private final Function<AnimationAccessor<A>, A> onLoad;
		A asset;
		
		private SelfAnimationAccessor(ResourceLocation registryName, Function<AnimationAccessor<A>, A> onLoad) {
			this.registryName = registryName;
			this.onLoad = onLoad;
		}
		
		@Override
		public A get() {
			if (this.asset == null) {
				this.asset = this.onLoad.apply(this);
			}
			
			return this.asset;
		}

		@Override
		public boolean isPresent() {
			return true;
		}
		
		@Override
		public boolean inRegistry() {
			return false;
		}
		
		@Override
		public int id() {
			return -1;
		}

		@Override
		public ResourceLocation registryName() {
			return this.registryName;
		}
	}
}
