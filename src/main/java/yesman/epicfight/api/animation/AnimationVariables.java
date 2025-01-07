package yesman.epicfight.api.animation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.utils.datastruct.TypeFlexibleHashMap;
import yesman.epicfight.api.utils.datastruct.TypeFlexibleHashMap.TypeKey;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.network.common.AnimationVariablePacket;

public class AnimationVariables {
	protected final Animator animator;
	protected final TypeFlexibleHashMap<AnimationVariableKey<?>> animationVariables = new TypeFlexibleHashMap<> (false);
	
	public AnimationVariables(Animator animator) {
		this.animator = animator;
	}
	
	public <T> T getSharedVariable(SharedAnimationVariableKey<T> key) {
		return this.animationVariables.get(key);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(IndependentAnimationVariableKey<T> key, AssetAccessor<? extends StaticAnimation> animation) {
		if (animation == null) {
			return null;
		}
		
		Map<ResourceLocation, Object> subMap = this.animationVariables.get(key);
		
		if (subMap == null) {
			return key.defaultValue();
		} else {
			return (T)subMap.get(animation.registryName());
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getOrDefault(IndependentAnimationVariableKey<T> key, AssetAccessor<? extends StaticAnimation> animation) {
		if (animation == null) {
			return null;
		}
		
		Map<ResourceLocation, Object> subMap = this.animationVariables.get(key);
		
		if (subMap == null) {
			return key.defaultValue();
		} else {
			return (T)subMap.getOrDefault(animation.registryName(), key.defaultValue());
		}
	}
	
	public <T> void putSharedVariable(SharedAnimationVariableKey<T> key) {
		this.putSharedVariable(key, key.defaultValue());
	}
	
	public <T> void putSharedVariable(SharedAnimationVariableKey<T> key, T value) {
		this.putSharedVariable(key, value, true);
	}
	
	@SuppressWarnings("unchecked")
	@Deprecated // Avoid direct use
	public <T> void putSharedVariable(SharedAnimationVariableKey<T> key, T value, boolean synchronize) {
		if (this.animationVariables.containsKey(key) && !key.mutable()) {
			throw new UnsupportedOperationException("Can't modify a const variable");
		}
		
		this.animationVariables.put((AnimationVariableKey<?>)key, value);
		
		if (synchronize && key instanceof SynchedAnimationVariableKey) {
			SynchedAnimationVariableKey<T> synchedanimationvariablekey = (SynchedAnimationVariableKey<T>)key;
			synchedanimationvariablekey.sync(this.animator.entitypatch, (AssetAccessor<? extends StaticAnimation>)null, value, AnimationVariablePacket.Action.PUT);
		}
	}
	
	public <T> void put(IndependentAnimationVariableKey<T> key, AssetAccessor<? extends StaticAnimation> animation) {
		this.put(key, animation, key.defaultValue());
	}
	
	public <T> void put(IndependentAnimationVariableKey<T> key, AssetAccessor<? extends StaticAnimation> animation, T value) {
		this.put(key, animation, value, true);
	}
	
	@SuppressWarnings("unchecked")
	@Deprecated // Avoid direct use
	public <T> void put(IndependentAnimationVariableKey<T> key, AssetAccessor<? extends StaticAnimation> animation, T value, boolean synchronize) {
		if (animation == Animations.EMPTY_ANIMATION) {
			return;
		}
		
		this.animationVariables.computeIfPresent(key, (k, v) -> {
			Map<ResourceLocation, Object> variablesByAnimations = ((Map<ResourceLocation, Object>)v);
			
			if (!key.mutable() && variablesByAnimations.containsKey(animation.registryName())) {
				throw new UnsupportedOperationException("Can't modify a const variable");
			}
			
			variablesByAnimations.put(animation.registryName(), value);
			
			return v;
		});
		
		this.animationVariables.computeIfAbsent(key, (k) -> {
			return new HashMap<> (Map.of(animation.registryName(), value));
		});
		
		if (synchronize && key instanceof SynchedAnimationVariableKey) {
			SynchedAnimationVariableKey<T> synchedanimationvariablekey = (SynchedAnimationVariableKey<T>)key;
			synchedanimationvariablekey.sync(this.animator.entitypatch, animation, value, AnimationVariablePacket.Action.PUT);
		}
	}
	
	public <T> T removeSharedVariable(SharedAnimationVariableKey<T> key) {
		return this.removeSharedVariable(key, true);
	}
	
	@SuppressWarnings("unchecked")
	@Deprecated // Avoid direct use
	public <T> T removeSharedVariable(SharedAnimationVariableKey<T> key, boolean synchronize) {
		if (!key.mutable()) {
			throw new UnsupportedOperationException("Can't remove a const variable");
		}
		
		if (synchronize && key instanceof SynchedAnimationVariableKey) {
			SynchedAnimationVariableKey<T> synchedanimationvariablekey = (SynchedAnimationVariableKey<T>)key;
			synchedanimationvariablekey.sync(this.animator.entitypatch, null, null, AnimationVariablePacket.Action.REMOVE);
		}
		
		return (T)this.animationVariables.remove(key);
	}
	
	@SuppressWarnings("unchecked")
	public void removeAll(AnimationAccessor<? extends StaticAnimation> animation) {
		if (animation == Animations.EMPTY_ANIMATION) {
			return;
		}
		
		for (Map.Entry<AnimationVariableKey<?>, Object> entry : this.animationVariables.entrySet()) {
			if (entry.getKey().isSharedKey()) {
				continue;
			}
			
			Map<ResourceLocation, Object> map = (Map<ResourceLocation, Object>)entry.getValue();
			map.remove(animation.registryName());
		}
	}
	
	public void remove(IndependentAnimationVariableKey<?> key, AssetAccessor<? extends StaticAnimation> animation) {
		this.remove(key, animation, true);
	}
	
	@SuppressWarnings("unchecked")
	@Deprecated // Avoid direct use
	public void remove(IndependentAnimationVariableKey<?> key, AssetAccessor<? extends StaticAnimation> animation, boolean synchronize) {
		if (animation == Animations.EMPTY_ANIMATION) {
			return;
		}
		
		Map<ResourceLocation, Object> map = (Map<ResourceLocation, Object>)this.animationVariables.get(key);
		map.remove(animation.registryName());
		
		if (synchronize && key instanceof SynchedAnimationVariableKey) {
			SynchedAnimationVariableKey<?> synchedanimationvariablekey = (SynchedAnimationVariableKey<?>)key;
			synchedanimationvariablekey.sync(this.animator.entitypatch, null, null, AnimationVariablePacket.Action.REMOVE);
		}
	}
	
	public static <T> SharedAnimationVariableKey<T> shared(Supplier<T> initValueSupplier, boolean mutable) {
		return new SharedAnimationVariableKey<> (initValueSupplier, mutable);
	}
	
	public static <T> IndependentAnimationVariableKey<T> independent(Supplier<T> initValueSupplier, boolean mutable) {
		return new IndependentAnimationVariableKey<> (initValueSupplier, mutable);
	}
	
	protected abstract static class AnimationVariableKey<T> implements TypeKey<T> {
		private final Supplier<T> initValueSupplier;
		private final boolean mutable;
		
		protected AnimationVariableKey(Supplier<T> initValueSupplier, boolean mutable) {
			this.initValueSupplier = initValueSupplier;
			this.mutable = mutable;
		}
		
		@Override
		public T defaultValue() {
			return this.initValueSupplier.get();
		}
		
		public boolean mutable() {
			return this.mutable;
		}
		
		public abstract boolean isSharedKey();
		public abstract boolean isSynched(); 
	}
	
	public static class SharedAnimationVariableKey<T> extends AnimationVariableKey<T> {
		protected SharedAnimationVariableKey(Supplier<T> initValueSupplier, boolean mutable) {
			super(initValueSupplier, mutable);
		}
		
		@Override
		public boolean isSharedKey() {
			return true;
		}
		
		@Override
		public boolean isSynched() {
			return false;
		}
	}
	
	public static class IndependentAnimationVariableKey<T> extends AnimationVariableKey<T> {
		protected IndependentAnimationVariableKey(Supplier<T> initValueSupplier, boolean mutable) {
			super(initValueSupplier, mutable);
		}
		
		@Override
		public boolean isSharedKey() {
			return false;
		}
		
		@Override
		public boolean isSynched() {
			return false;
		}
	}
}
