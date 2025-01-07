package yesman.epicfight.api.animation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
import net.minecraftforge.fml.event.IModBusEvent;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
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

@SuppressWarnings("unchecked")
public class AnimationManager extends SimpleJsonResourceReloadListener {
	private static final AnimationManager INSTANCE = new AnimationManager();
	private static ResourceManager serverResourceManager = null;
	
	public static AnimationManager getInstance() {
		return INSTANCE;
	}
	
	private final Map<Integer, AnimationAccessor<? extends StaticAnimation>> animationById = Maps.newHashMap();
	private final Map<ResourceLocation, AnimationAccessor<? extends StaticAnimation>> animationByName = Maps.newHashMap();
	private final Map<AnimationAccessor<? extends StaticAnimation>, StaticAnimation> animations = Maps.newHashMap();
	
	private final Map<ResourceLocation, String> resourcepackAnimationCommands = Maps.newHashMap();
	
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
	
	public Map<ResourceLocation, AnimationAccessor<? extends StaticAnimation>> getAnimations(Predicate<AssetAccessor<? extends StaticAnimation>> filter) {
		Map<ResourceLocation, AnimationAccessor<? extends StaticAnimation>> filteredItems = this.animations.entrySet().stream()
			.filter((entry) -> filter.test(entry.getKey()))
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
	
	public AnimationClip loadAnimationClip(StaticAnimation animation, BiFunction<JsonAssetLoader, StaticAnimation, AnimationClip> clipLoader) {
		if (getAnimationResourceManager() == null) {
			return null;
		}
		
		JsonAssetLoader modelLoader = new JsonAssetLoader(getAnimationResourceManager(), animation.getLocation());
		AnimationClip loadedClip = clipLoader.apply(modelLoader, animation);
		
		return loadedClip;
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
		
		this.animations.clear();
		this.animationById.entrySet().removeIf((entry) -> entry.getValue().resourcepackAnimation());
		this.animationByName.entrySet().removeIf((entry) -> entry.getValue().resourcepackAnimation());
		this.resourcepackAnimationCommands.clear();
		
		return super.prepare(resourceManager, profilerIn);
	}
	
	@Override
	protected void apply(Map<ResourceLocation, JsonElement> objectIn, ResourceManager resourceManager, ProfilerFiller profilerIn) {
		Armatures.reload(resourceManager);
		
		Set<ResourceLocation> registeredAnimation = this.animationById.values().stream().reduce(Sets.newHashSet(), (set, accessor) -> {
			set.add(accessor.registryName());
			
			for (AssetAccessor<? extends StaticAnimation> subAnimAccessor : accessor.get().getSubAnimations()) {
				set.add(subAnimAccessor.registryName());
			}
			
			return set;
		}, (set1, set2) -> {
			set1.addAll(set2);
			return set1;
		});
		
		/**
		 * Load animations that are not registered by {@link AnimationRegistryEvent}
		 * Reads from /assets folder in physical client, /datapack in physical server.
		 */
		objectIn.entrySet().stream().filter((entry) -> !registeredAnimation.contains(entry.getKey()) && !entry.getKey().getPath().contains("/data/"))
									.sorted((e1, e2) -> e1.getKey().toString().compareTo(e2.getKey().toString()))
									.forEach((entry) -> {
										try {
											this.readResourcepackAnimation(entry.getKey(), entry.getValue().getAsJsonObject());
										} catch (Exception e) {
											EpicFightMod.LOGGER.error("Failed to load User animation " + entry.getKey() + " because of " + e + ". Skipped.");
											e.printStackTrace();
										}
									});
		
		SkillManager.reloadAllSkillsAnimations();
		
		this.animations.values().stream().reduce(Lists.<AssetAccessor<? extends StaticAnimation>>newArrayList(), (list, anim) -> {
			list.addAll(anim.getSubAnimations());
			return list;
		}, (list1, list2) -> {
			list1.addAll(list2);
			return list1;
		}).forEach((animation) -> {
			animation.get().postInit();
			
			if (EpicFightMod.isPhysicalClient()) {
				AnimationManager.readAnimationProperties(animation.get());
			}
		});
	}
	
	public static ResourceLocation getAnimationDataFileLocation(ResourceLocation location) {
		int splitIdx = location.getPath().lastIndexOf('/');
		
		if (splitIdx < 0) {
			splitIdx = 0;
		}
		
		return ResourceLocation.tryBuild(location.getNamespace(), String.format("%s/data%s", location.getPath().substring(0, splitIdx), location.getPath().substring(splitIdx)));
	}
	
	public static void setServerResourceManager(ResourceManager pResourceManager) {
		serverResourceManager = pResourceManager;
	}
	
	public static ResourceManager getAnimationResourceManager() {
		return EpicFightMod.isPhysicalClient() ? Minecraft.getInstance().getResourceManager() : serverResourceManager;
	}
	
	public int getResourcepackAnimationCount() {
		return this.resourcepackAnimationCommands.size();
	}
	
	public Stream<CompoundTag> getResourcepackAnimationStream() {
		return this.resourcepackAnimationCommands.entrySet().stream().map((entry) -> {
			CompoundTag compTag = new CompoundTag();
			compTag.putString("registry_name", entry.getKey().toString());
			compTag.putString("invoke_command", entry.getValue());
			
			return compTag;
		});
	}
	
	/**
	 * @param mandatoryPack : creates dummy animations for server side animations without animation clips when the server has mandatory resource pack.
	 *                        custom weapon types & mob capabilities won't be created because they won't be able to find the animations from the server
	 *                        dummy animations will be automatically removed right after reloading resourced as the server forces using resource pack
	 */
	@OnlyIn(Dist.CLIENT)
	public void processServerPacket(SPDatapackSync packet, boolean mandatoryPack) {
		if (mandatoryPack) {
			for (CompoundTag tag : packet.getTags()) {
				String invocationCommand = tag.getString("invoke_command");
				ResourceLocation registryName = new ResourceLocation(tag.getString("registry_name"));
				
				if (this.animations.containsKey(registryName)) {
					continue;
				}
				
				try {
					//StaticAnimation animation = InstantiateInvoker.invoke(invocationCommand, StaticAnimation.class).getResult();
					//this.animationById.put(null, animation);
					//this.animationByName.put(registryName, null);
					//this.animations.put(null, animation);
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
		int animationCount = this.animations.size();
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
		
		for (String registryName : this.animations.keySet().stream().map((rl) -> rl.toString()).toList()) {
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
	private void readResourcepackAnimation(ResourceLocation rl, JsonObject json) throws Exception {
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
		this.resourcepackAnimationCommands.put(rl, invocationCommand);
		
		AnimationAccessor<StaticAnimation> resourcepackAnimationAccessor = AnimationAccessorImpl.create(rl, this.animations.size() + 1, true, (accessor) -> {
			StaticAnimation animation = null;
			
			try {
				animation = InstantiateInvoker.invoke(invocationCommand, StaticAnimation.class).getResult();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (!animation.getRegistryName().equals(rl)) {
				throw new IllegalStateException("The animation registry name in invocation command is not matching with its path!");
			}
			
			animation.setAccessor(accessor);
			JsonElement propertiesElement = json.getAsJsonObject().get("properties");
			
			if (propertiesElement != null) {
				JsonObject propertiesObject = propertiesElement.getAsJsonObject();
				
				for (Map.Entry<String, JsonElement> entry : propertiesObject.entrySet()) {
					AnimationProperty<?> propertyKey = AnimationProperty.getSerializableProperty(entry.getKey());
					Object value = propertyKey.parseFrom(entry.getValue());
					animation.addPropertyUnsafe(propertyKey, value);
				}
			}
			
			return animation;
		});
		
		this.animationById.put(resourcepackAnimationAccessor.id(), resourcepackAnimationAccessor);
		this.animationByName.put(resourcepackAnimationAccessor.registryName(), resourcepackAnimationAccessor);
		this.animations.put(resourcepackAnimationAccessor, null);
	}
	
	public interface AnimationAccessor<A extends DynamicAnimation> extends AssetAccessor<A> {
		int id();
		
		default boolean resourcepackAnimation() {
			return false;
		}
		
		default boolean idBetween(AnimationAccessor<? extends StaticAnimation> a1, AnimationAccessor<? extends StaticAnimation> a2) {
			return a1.id() <= this.id() && a2.id() >= this.id();
		}
	}
	
	public static record AnimationAccessorImpl<A extends StaticAnimation> (ResourceLocation registryName, int id, boolean resourcepackAnimation, Function<AnimationAccessor<A>, A> onLoad) implements AnimationAccessor<A> {
		public static <A extends StaticAnimation> AnimationAccessor<A> create(ResourceLocation registryName, int id, boolean resourcepackAnimation, Function<AnimationAccessor<A>, A> onLoad) {
			return new AnimationAccessorImpl<A> (registryName, id, resourcepackAnimation, onLoad);
		}
		
		@Override
		public A get() {
			if (INSTANCE.animations.get(this) == null) {
				INSTANCE.animations.put(this, this.onLoad.apply(this));
			}
			
			return (A)INSTANCE.animations.get(this);
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
			} else if (obj instanceof AnimationAccessor armatureAccessor) {
				return this.registryName.equals(armatureAccessor.registryName());
			} else if (obj instanceof ResourceLocation rl) {
				return this.registryName.equals(rl);
			} else if (obj instanceof String name) {
				return this.registryName.toString().equals(name);
			} else {
				return false;
			}
		}
	}
	
	public static class AnimationRegistryEvent extends Event implements IModBusEvent {
		private String namespace;
		
		public void startsWith(String namespace) {
			this.namespace = namespace;
		}
		
		public <T extends StaticAnimation> AnimationManager.AnimationAccessor<T> nextAccessor(String id, Function<AnimationManager.AnimationAccessor<T>, T> onLoad) {
			AnimationAccessor<T> accessor = AnimationAccessorImpl.create(ResourceLocation.tryBuild(this.namespace, id), INSTANCE.animations.size() + 1, false, onLoad);
			INSTANCE.animationById.put(accessor.id(), accessor);
			INSTANCE.animationByName.put(accessor.registryName(), accessor);
			INSTANCE.animations.put(accessor, null);
			
			return accessor; 
		}
	}
}
