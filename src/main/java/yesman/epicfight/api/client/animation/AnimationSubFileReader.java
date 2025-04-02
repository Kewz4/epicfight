package yesman.epicfight.api.client.animation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.ibm.icu.impl.Pair;

import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.DirectStaticAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.client.animation.property.JointMaskEntry;
import yesman.epicfight.api.client.animation.property.JointMaskReloadListener;
import yesman.epicfight.api.client.animation.property.LayerInfo;
import yesman.epicfight.api.client.animation.property.TrailInfo;
import yesman.epicfight.main.EpicFightMod;

@OnlyIn(Dist.CLIENT)
public class AnimationSubFileReader {
	public static final SubFileType<ClientProperty> SUBFILE_CLIENT_PROPERTY = new ClientPropertyType();
	
	public static final SubFileType<PovAnimation> SUBFILE_POV_ANIMATION = new PovAnimationType();
	
	public static void readAndApply(StaticAnimation animation, Resource iresource, SubFileType<?> subFileType) {
		InputStream inputstream = null;
		
		try {
			inputstream = iresource.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		assert inputstream != null : "Input stream is null";
		
		subFileType.apply(inputstream, animation);
	}
	
	@OnlyIn(Dist.CLIENT)
	public static abstract class SubFileType<T> {
		private final String directory;
		private final Gson gson;
		private final TypeToken<T> typeToken;
		private final JsonDeserializer<T> deserializer;
		
		private SubFileType(String directory, Class<T> type, TypeToken<T> typeToken, JsonDeserializer<T> deserializer) {
			this.directory = directory;
			this.gson = new GsonBuilder().registerTypeAdapter(type, deserializer).create();
			this.typeToken = typeToken;
			this.deserializer = deserializer;
		}
		
		// Deserialize from input stream
		public void apply(InputStream inputstream, StaticAnimation animation) {
			Reader reader = new InputStreamReader(inputstream, StandardCharsets.UTF_8);
			T deserialized = GsonHelper.fromJson(this.gson, reader, this.typeToken);
			this.applySubFileInfo(deserialized, animation);
		}
		
		// Deserialize from json object
		public void apply(JsonElement jsonElement, StaticAnimation animation) {
			T deserialized = this.deserializer.deserialize(jsonElement, null, null);
			this.applySubFileInfo(deserialized, animation);
		}
		
		protected abstract void applySubFileInfo(T deserialized, StaticAnimation animation);
		
		public String getDirectory() {
			return this.directory;
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	private record ClientProperty(LayerInfo layerInfo, LayerInfo multilayerInfo, List<TrailInfo> trailInfo) {
	}
	
	@OnlyIn(Dist.CLIENT)
	private static class ClientPropertyType extends SubFileType<ClientProperty> {
		private ClientPropertyType() {
			super(
				  "data"
				,  ClientProperty.class
				, new TypeToken<ClientProperty>() {}
				, new AnimationSubFileReader.ClientAnimationPropertyDeserializer()
			);
		}
		
		@Override
		public void applySubFileInfo(ClientProperty deserialized, StaticAnimation animation) {
			if (deserialized.layerInfo() != null) {
				if (deserialized.layerInfo().jointMaskEntry.isValid()) {
					animation.addProperty(ClientAnimationProperties.JOINT_MASK, deserialized.layerInfo().jointMaskEntry);
				}
				
	        	animation.addProperty(ClientAnimationProperties.LAYER_TYPE, deserialized.layerInfo().layerType);
	        	animation.addProperty(ClientAnimationProperties.PRIORITY, deserialized.layerInfo().priority);
	        }
			
			if (deserialized.multilayerInfo() != null) {
				DirectStaticAnimation multilayerAnimation = new DirectStaticAnimation(animation.getLocation(), animation.getTransitionTime(), animation.isRepeat(), animation.getRegistryName().toString() + "_multilayer", animation.getArmature());
				
				if (deserialized.multilayerInfo().jointMaskEntry.isValid()) {
					multilayerAnimation.addProperty(ClientAnimationProperties.JOINT_MASK, deserialized.multilayerInfo().jointMaskEntry);
				}
				
				multilayerAnimation.addProperty(ClientAnimationProperties.LAYER_TYPE, deserialized.multilayerInfo().layerType);
				multilayerAnimation.addProperty(ClientAnimationProperties.PRIORITY, deserialized.multilayerInfo().priority);
				multilayerAnimation.addProperty(StaticAnimationProperty.ELAPSED_TIME_MODIFIER, (self, entitypatch, speed, prevElapsedTime, elapsedTime) -> {
					Layer baseLayer = entitypatch.getClientAnimator().baseLayer;
					
					if (baseLayer.animationPlayer.getAnimation().get().getRealAnimation().get() != animation) {
						return Pair.of(prevElapsedTime, elapsedTime);
					}
					
					if (!self.isStaticAnimation() && baseLayer.animationPlayer.getAnimation().get().isStaticAnimation()) {
						return Pair.of(prevElapsedTime + speed, elapsedTime + speed);
					}
					
					return Pair.of(baseLayer.animationPlayer.getPrevElapsedTime(), baseLayer.animationPlayer.getElapsedTime());
				});
				
				animation.addProperty(ClientAnimationProperties.MULTILAYER_ANIMATION, multilayerAnimation);
			}
			
			if (deserialized.trailInfo().size() > 0) {
				animation.addProperty(ClientAnimationProperties.TRAIL_EFFECT, deserialized.trailInfo());
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class ClientAnimationPropertyDeserializer implements JsonDeserializer<ClientProperty> {
		private static LayerInfo deserializeLayerInfo(JsonObject jsonObject) {
			return deserializeLayerInfo(jsonObject, null);
		}
		
		private static LayerInfo deserializeLayerInfo(JsonObject jsonObject, @Nullable Layer.LayerType defaultLayerType) {
			JointMaskEntry.Builder builder = JointMaskEntry.builder();
			Layer.Priority priority = jsonObject.has("priority") ? Layer.Priority.valueOf(GsonHelper.getAsString(jsonObject, "priority")) : null;
			Layer.LayerType layerType = jsonObject.has("layer") ? Layer.LayerType.valueOf(GsonHelper.getAsString(jsonObject, "layer")) : Layer.LayerType.BASE_LAYER;
			
			if (jsonObject.has("masks")) {
				JsonArray maskArray = jsonObject.get("masks").getAsJsonArray();
				
				if (!maskArray.isEmpty()) {
					builder.defaultMask(JointMaskReloadListener.getNoneMask());
					
					maskArray.forEach(element -> {
						JsonObject jointMaskEntry = element.getAsJsonObject();
						String livingMotionName = GsonHelper.getAsString(jointMaskEntry, "livingmotion");
						String type = GsonHelper.getAsString(jointMaskEntry, "type");
						
						if (!type.contains(":")) {
							type = (new StringBuilder(EpicFightMod.MODID)).append(":").append(type).toString();
						}
						
						if (livingMotionName.equals("ALL")) {
							builder.defaultMask(JointMaskReloadListener.getJointMaskEntry(type));
						} else {
							builder.mask((LivingMotion) LivingMotion.ENUM_MANAGER.getOrThrow(livingMotionName), JointMaskReloadListener.getJointMaskEntry(type));
						}
					});
				}
			}
			
			return new LayerInfo(builder.create(), priority, (defaultLayerType == null) ? layerType : defaultLayerType);
		}
		
		@Override
		public ClientProperty deserialize(JsonElement json, java.lang.reflect.Type type, JsonDeserializationContext context) throws JsonParseException {
			JsonObject jsonObject = json.getAsJsonObject();
			LayerInfo layerInfo = null;
			LayerInfo multilayerInfo = null;
			
			if (jsonObject.has("multilayer")) {
				JsonObject multiplayerJson = jsonObject.get("multilayer").getAsJsonObject();
				layerInfo = deserializeLayerInfo(multiplayerJson.get("base").getAsJsonObject());
				multilayerInfo = deserializeLayerInfo(multiplayerJson.get("composite").getAsJsonObject(), Layer.LayerType.COMPOSITE_LAYER);
			} else {
				layerInfo = deserializeLayerInfo(jsonObject);
			}
			
			List<TrailInfo> trailInfos = Lists.newArrayList();
			
			if (jsonObject.has("trail_effects")) {
				JsonArray trailArray = jsonObject.get("trail_effects").getAsJsonArray();
				trailArray.forEach(element -> trailInfos.add(TrailInfo.deserialize(element)));
			}
			
			return new ClientProperty(multilayerInfo, layerInfo, trailInfos);
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	private record PovAnimation() {
		
	}
	
	@OnlyIn(Dist.CLIENT)
	private static class PovAnimationType extends SubFileType<PovAnimation> {
		private PovAnimationType() {
			super(
				  "pov"
				, PovAnimation.class
				, new TypeToken<PovAnimation>() {}
				, new AnimationSubFileReader.PovAnimationDeserializer()
			);
		}
		
		@Override
		public void applySubFileInfo(PovAnimation deserialized, StaticAnimation animation) {
			
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class PovAnimationDeserializer implements JsonDeserializer<PovAnimation> {
		@Override
		public PovAnimation deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return null;
		}
	}
}
