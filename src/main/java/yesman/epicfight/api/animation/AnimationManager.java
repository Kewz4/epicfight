package yesman.epicfight.api.animation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableInt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.event.IModBusEvent;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.animation.types.datapack.ClipHoldingAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.api.client.animation.ClientAnimationDataReader;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.api.utils.InstantiateInvoker;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.client.CPCheckAnimationRegistrySync;
import yesman.epicfight.network.server.SPDatapackSync;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@SuppressWarnings("unchecked")
public class AnimationManager extends SimpleJsonResourceReloadListener {
	private static final AnimationManager INSTANCE = new AnimationManager();
	private static ResourceManager serverResourceManager = null;
	
	public static AnimationManager getInstance() {
		return INSTANCE;
	}
	
	private final Map<Integer, AnimationAccessor<? extends StaticAnimation>> animationById = Maps.newHashMap();
	private final Map<ResourceLocation, AnimationAccessor<? extends StaticAnimation>> animationByName = Maps.newHashMap();
	private final Map<AnimationAccessor<? extends StaticAnimation>, StaticAnimation> animationRegistry = Maps.newHashMap();
	
	public AnimationManager() {
		super(new GsonBuilder().create(), "animmodels/animations");
	}
	
	public static <T extends StaticAnimation> AnimationAccessor<T> byKey(String registryName) {
		return byKey(ResourceLocation.tryParse(registryName));
	}
	
	public static <T extends StaticAnimation> AnimationAccessor<T> byKey(ResourceLocation registryName) {
		return (AnimationAccessor<T>)getInstance().animationByName.get(registryName);
	}
	
	public static <T extends StaticAnimation> AnimationAccessor<T> byId(int animationId) {
		return (AnimationAccessor<T>)getInstance().animationById.get(animationId);
	}
	
	public Map<ResourceLocation, AnimationAccessor<? extends StaticAnimation>> getAnimations(Predicate<AnimationAccessor<? extends StaticAnimation>> filter) {
		Map<ResourceLocation, AnimationAccessor<? extends StaticAnimation>> filteredItems = this.animationRegistry.entrySet().stream()
			.filter((entry) -> !entry.getKey().userAsset() && filter.test(entry.getKey()))
			.reduce(Maps.<ResourceLocation, AnimationAccessor<? extends StaticAnimation>>newHashMap(),
					(map, entry) -> {
						map.put(entry.getKey().registryName(), entry.getKey());
						return map;
					},
					(map1, map2) -> {
						map1.putAll(map2);
						return map1;
					}
			);
		
		return ImmutableMap.copyOf(filteredItems);
	}
	
	/**
	 * Registers animations created by datapack edit screen
	 */
	public void registerUserAnimation(ClipHoldingAnimation animation) {
		AnimationAccessor<StaticAnimation> userAnimationAccessor = new AnimationAccessor<StaticAnimation> (animation.getCreator().getRegistryName(), -1, true, (Class<StaticAnimation>)animation.getClass(), (accessor) -> {
			return animation.cast();
		});
		
		this.animationRegistry.put(userAnimationAccessor, animation.cast());
	}
	
	/**
	 * Remove user animations created by datapack edit screen
	 */
	public void removeUserAnimation(ClipHoldingAnimation animation) {
		this.animationRegistry.remove(ReferenceByName.create(animation.getCreator().getRegistryName()));
	}
	
	public void loadAnimationClip(StaticAnimation animation, BiConsumer<JsonAssetLoader, StaticAnimation> clipLoader) {
		if (getAnimationResourceManager() == null) {
			return;
		}
		
		JsonAssetLoader modelLoader = new JsonAssetLoader(getAnimationResourceManager(), animation.getLocation());
		clipLoader.accept(modelLoader, animation);
	}
	
	public static void readAnimationProperties(StaticAnimation animation) {
		ResourceLocation dataLocation = getAnimationDataFileLocation(animation.getLocation());
		
		getAnimationResourceManager().getResource(dataLocation).ifPresent((rs) -> {
			ClientAnimationDataReader.readAndApply(animation, rs);
		});
	}
	
	@Override
	protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager, ProfilerFiller profilerIn) {
		if (!EpicFightMod.isPhysicalClient() && serverResourceManager == null) {
			serverResourceManager = resourceManager;
		}
		
		this.animationRegistry.clear();
		this.animationById.clear();
		this.animationByName.clear();
		
		return super.prepare(resourceManager, profilerIn);
	}
	
	@Override
	protected void apply(Map<ResourceLocation, JsonElement> objectIn, ResourceManager resourceManager, ProfilerFiller profilerIn) {
		Armatures.reload(resourceManager);
		ModLoader.get().postEvent(new AnimationRegistryEvent());
		
		final Map<ResourceLocation, StaticAnimation> registeredAnimation = Maps.newHashMap();
		this.animationRegistry.values().forEach(a1 -> a1.getSubAnimations().forEach((a2) -> registeredAnimation.put(a2.getRegistryName(), a2)));
		
		final MutableInt idCounter = new MutableInt(registeredAnimation.size());
		
		/**
		 * Load animations that are not registered from {@link AnimationRegistryEvent}
		 * Reads from Resource Pack in physical client, Datapack in physical server.
		 */
		objectIn.entrySet().stream().filter((entry) -> !registeredAnimation.containsKey(entry.getKey()) && !entry.getKey().getPath().contains("/data/"))
									.sorted((e1, e2) -> e1.getKey().toString().compareTo(e2.getKey().toString()))
									.forEach((entry) -> {
										try {
											this.readAnimationFromJson(entry.getKey(), entry.getValue().getAsJsonObject(), idCounter);
										} catch (Exception e) {
											EpicFightMod.LOGGER.error("Failed to load User animation " + entry.getKey() + " because of " + e + ". Skipped.");
											e.printStackTrace();
										}
									});
		
		SkillManager.reloadAllSkillsAnimations();
		
		this.animationRegistry.values().stream().reduce(Lists.<StaticAnimation>newArrayList(), (list, anim) -> {
			list.addAll(anim.getSubAnimations());
			return list;
		}, (list1, list2) -> {
			list1.addAll(list2);
			return list1;
		}).forEach((animation) -> {
			animation.postInit();
			
			if (EpicFightMod.isPhysicalClient()) {
				AnimationManager.readAnimationProperties(animation);
			}
		});
	}
	
	public static ResourceLocation getAnimationDataFileLocation(ResourceLocation location) {
		int splitIdx = location.getPath().lastIndexOf('/');
		
		if (splitIdx < 0) {
			splitIdx = 0;
		}
		
		return new ResourceLocation(location.getNamespace(), String.format("%s/data%s", location.getPath().substring(0, splitIdx), location.getPath().substring(splitIdx)));
	}
	
	public static void setServerResourceManager(ResourceManager pResourceManager) {
		serverResourceManager = pResourceManager;
	}
	
	public static ResourceManager getAnimationResourceManager() {
		return EpicFightMod.isPhysicalClient() ? Minecraft.getInstance().getResourceManager() : serverResourceManager;
	}
	
	public int getUserAnimationsCount() {
		return this.userAnimations.size();
	}
	
	public Stream<CompoundTag> getUserAnimationStream() {
		return this.userAnimations.values().stream().sorted((a1, a2) -> a1.getRegistryName().toString().compareTo(a2.getRegistryName().toString())).map((animation) -> {
			CompoundTag compTag = new CompoundTag();
			
			compTag.putString("registry_name", animation.getRegistryName().toString());
			compTag.putString("invoke_command", this.userAnimationInvocationCommands.get(animation.getRegistryName()));
			
			return compTag;
		});
	}
	
	/**
	 * @param createDummyAnimations : creates dummy animations for server side animations without animation clips when the server has mandatory resource pack.
	 *                                custom weapon types & mob capabilities won't be created because they won't be able to find the animations from the server
	 *                                dummy animations will be automatically removed right after reloading resourced as the server forces using resource pack
	 */
	@OnlyIn(Dist.CLIENT)
	public void processServerPacket(SPDatapackSync packet, boolean createDummyAnimations) {
		if (createDummyAnimations) {
			for (CompoundTag tag : packet.getTags()) {
				String invocationCommand = tag.getString("invoke_command");
				ResourceLocation registryName = new ResourceLocation(tag.getString("registry_name"));
				
				if (this.animationRegistry.containsKey(registryName)) {
					continue;
				}
				
				try {
					//this.currentWorkingModid = registryName.getNamespace();
					StaticAnimation animation = InstantiateInvoker.invoke(invocationCommand, StaticAnimation.class).getResult();
					this.animationById.put(null, animation);
					this.animationRegistry.put(null, animation);
					
					//this.userAnimations.put(registryName, animation);
					//this.currentWorkingModid = null;
				} catch (Exception e) {
					EpicFightMod.LOGGER.warn("Failed at creating animation from server resource pack");
					e.printStackTrace();
				}
			}
		}
		
		this.sendAnimationRegistrySyncCheck();
	}
	
	@OnlyIn(Dist.CLIENT)
	private void sendAnimationRegistrySyncCheck() {
		int animationCount = this.animationRegistry.size();
		String[] registryNames = new String[animationCount];
		
		for (int i = 0; i < animationCount; i++) {
			String registryName = this.animationById.get(i + 1).registryName().toString();
			registryNames[i] = registryName;
		}
		
		CPCheckAnimationRegistrySync packet = new CPCheckAnimationRegistrySync(animationCount, registryNames);
		EpicFightNetworkManager.sendToServer(packet);
	}
	
	public void validateClientAnimationRegistry(CPCheckAnimationRegistrySync msg, ServerGamePacketListenerImpl connection) {
		StringBuilder builder = new StringBuilder();
		int count = 0;
		
		Set<String> clientAnimationRegistry = new HashSet<> (Set.of(msg.registryNames));
		
		for (String registryName : this.animationRegistry.keySet().stream().map((rl) -> rl.toString()).toList()) {
			if (!clientAnimationRegistry.contains(registryName)) {
				// Animations that don't exist in client
				if (count < 10) {
					builder.append(registryName);
					builder.append("\n");
				}
				
				count++;
			} else {
				clientAnimationRegistry.remove(registryName);
			}
		}
		
		// Animations that don't exist in server
		for (String registryName : clientAnimationRegistry) {
			if (registryName.equals("empty")) {
				continue;
			}
			
			if (count < 10) {
				builder.append(registryName);
				builder.append("\n");
			}
			
			count++;
		}
		
		if (count >= 10) {
			builder.append(Component.translatable("gui.epicfight.warn.animation_unsync.etc", (count - 9)).getString());
			builder.append("\n");
		}
		
		if (!builder.isEmpty()) {
			connection.disconnect(Component.translatable("gui.epicfight.warn.animation_unsync", builder.toString()));
		}
	}
	
	
	private static final Set<String> NO_WARNING_MODID = Sets.newHashSet();
	
	public static void addNoWarningModId(String modid) {
		NO_WARNING_MODID.add(modid);
	}
	
	/**************************************************
	 * User-animation loader
	 **************************************************/
	@SuppressWarnings({ "deprecation" })
	private void readAnimationFromJson(ResourceLocation rl, JsonObject json, MutableInt idCounter) throws Exception {
		JsonElement constructorElement = json.get("constructor");
		
		if (constructorElement == null) {
			if (NO_WARNING_MODID.contains(rl.getNamespace())) {
				return;
			} else {
				throw new IllegalStateException("No constructor information has provided in User animation " + rl);
			}
		}
		
		JsonObject constructorObject = constructorElement.getAsJsonObject();
		String invocationCommand = constructorObject.get("invocation_command").getAsString();
		StaticAnimation animation = InstantiateInvoker.invoke(invocationCommand, StaticAnimation.class).getResult();
		
		AnimationAccessor<StaticAnimation> userAnimationAccessor = new AnimationAccessorImpl<> (animation.getRegistryName(), idCounter.getValue(), (accessor) -> {
			StaticAnimation animation$2 = null;
			
			// This never happen, just to solve compile error
			try {
				animation$2 = InstantiateInvoker.invoke(invocationCommand, StaticAnimation.class).getResult();
			} catch (Exception e) {
			}
			
			JsonElement propertiesElement = json.getAsJsonObject().get("properties");
			
			if (propertiesElement != null) {
				JsonObject propertiesObject = propertiesElement.getAsJsonObject();
				
				for (Map.Entry<String, JsonElement> entry : propertiesObject.entrySet()) {
					AnimationProperty<?> propertyKey = AnimationProperty.getSerializableProperty(entry.getKey());
					Object value = propertyKey.parseFrom(entry.getValue());
					animation.addPropertyUnsafe(propertyKey, value);
				}
			}
			
			return animation$2;
		});
		
		this.animationById.put(userAnimationAccessor.id(), userAnimationAccessor);
		this.animationRegistry.put(userAnimationAccessor, null);
		
		idCounter.add(1);
	}
	
	public interface AnimationAccessor<A extends DynamicAnimation> extends AssetAccessor<A> {
		int id();
		
		default boolean idBetween(AnimationAccessor<? extends StaticAnimation> a1, AnimationAccessor<? extends StaticAnimation> a2) {
			return a1.id() <= this.id() && a2.id() >= this.id();
		}
		
		default void putOnPlayer(AnimationPlayer animationPlayer, LivingEntityPatch<?> entitypatch) {
			animationPlayer.setPlayAnimation(this);
			animationPlayer.tick(entitypatch);
			animationPlayer.begin(this, entitypatch);
		}
	}
	
	public static record AnimationAccessorImpl<A extends StaticAnimation> (ResourceLocation registryName, int id, Function<AnimationAccessor<A>, A> onLoad) implements AnimationAccessor<A> {
		public static <A extends StaticAnimation> AnimationAccessor<A> create(ResourceLocation registryName, int id, Function<AnimationAccessor<A>, A> onLoad) {
			return new AnimationAccessorImpl<A> (registryName, id, onLoad);
		}
		
		@Override
		public A get() {
			if (INSTANCE.animationRegistry.get(this) == null) {
				INSTANCE.animationRegistry.put(this, this.onLoad.apply(this));
			}
			
			return (A)INSTANCE.animationRegistry.get(this);
		}
		
		public boolean isPresent() {
			return true;
		}
		
		public String toString() {
			return this.registryName.toString();
		}
		
		public int hashCode() {
			return this.registryName.hashCode();
		}
		
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof AnimationAccessor animationAccessor) {
				return this.registryName.equals(animationAccessor.registryName());
			} else {
				return false;
			}
		}
	}
	
	public static class AnimationRegistryEvent extends Event implements IModBusEvent {
		private String namespace;
		private int lastId;
		
		public void withNamespace(String namespace) {
			this.namespace = namespace;
		}
		
		public <T extends StaticAnimation> AnimationManager.AnimationAccessor<T> nextAccessor(String id, Function<AnimationManager.AnimationAccessor<T>, T> onLoad) {
			AnimationAccessor<T> accessor = AnimationAccessorImpl.create(new ResourceLocation(this.namespace, id), this.lastId++, onLoad);
			INSTANCE.animationById.put(accessor.id(), accessor);
			INSTANCE.animationByName.put(accessor.registryName(), accessor);
			INSTANCE.animationRegistry.put(accessor, null);
			
			return accessor; 
		}
	}
}
