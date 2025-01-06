package yesman.epicfight.api.client.epicskins;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.model.SoftBodyTranslatable;
import yesman.epicfight.api.client.physics.cloth.ClothColliderPresets;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator;
import yesman.epicfight.api.physics.SimulationTypes;
import yesman.epicfight.client.gui.widgets.ColorSlider;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

@OnlyIn(Dist.CLIENT)
public record EpicSkinsInformation(Supplier<ResourceLocation> cloakTexture, float r, float g, float b) {
	private static final String USER_COSMETICS_ENDPOINT = "/cosmetic/user_cosmetic";
	
	public static void initEpicSkins(AbstractClientPlayerPatch<?> playerpatch) {
		HttpRequest request = HttpRequest.newBuilder()
			     .GET()
			     .uri(URI.create(RemoteAssets.SERVER_URL + USER_COSMETICS_ENDPOINT + "?mc_uuid=" + playerpatch.getOriginal().getUUID().toString().replace("-", "")))
			     .build();
		
		RemoteAssets.getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((response, ex) -> {
			JsonReader jsonReader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(response.body().getBytes()), StandardCharsets.UTF_8));
			JsonObject json = Streams.parse(jsonReader).getAsJsonObject();
			
			if (json.entrySet().isEmpty()) {
				playerpatch.getSimulator(SimulationTypes.CLOTH).ifPresent((clothSimulator) -> {
					SoftBodyTranslatable.TRACKING_SIMULATION_SUBJECTS.add(playerpatch);
					
					clothSimulator.runWhenPermanent(
						  ClothSimulator.PLAYER_CLOAK
						, Meshes.CLOAK
						, ClothSimulator.ClothObjectBuilder.create().putAll("default".equals(playerpatch.getOriginal().getModelName()) ? ClothColliderPresets.BIPED : ClothColliderPresets.BIPED)
						, () -> {
							  return playerpatch.getOriginal().isCapeLoaded() && !playerpatch.getOriginal().isInvisible() && playerpatch.getOriginal().isModelPartShown(PlayerModelPart.CAPE)
									 && playerpatch.getOriginal().getItemBySlot(EquipmentSlot.CHEST).getItem() != Items.ELYTRA;
						  }
					);
					
					playerpatch.setEpicSkinsInformation(new EpicSkinsInformation(() -> playerpatch.getOriginal().getCloakTextureLocation(), 1.0F, 1.0F, 1.0F));
				});
			} else {
				int cloakType = GsonHelper.getAsInt(json, "cloakTypeId");
				int colorColor = GsonHelper.getAsInt(json, "cloakColor");
				boolean useVanillaTexture = GsonHelper.getAsBoolean(json, "cloakVanillaTexture");
				boolean colorConfigurable = GsonHelper.getAsBoolean(json, "colorConfigurable");
				boolean textureConfigurable = GsonHelper.getAsBoolean(json, "textureConfigurable");
				String cloakFile = GsonHelper.getAsString(json, "fileLocation");
				
				Supplier<ResourceLocation> cloakTextureProvider = null;
				
				if (useVanillaTexture && textureConfigurable) {
					cloakTextureProvider = () -> playerpatch.getOriginal().getCloakTextureLocation();
				} else {
					ResourceLocation remoteCloakTexture = RemoteAssets.getInstance().getRemoteTexture(GsonHelper.getAsString(json, "textureLocation"));
					cloakTextureProvider = () -> remoteCloakTexture;
				}
				
				final Supplier<ResourceLocation> fCloakTextureProvider = cloakTextureProvider;
				
				RemoteAssets.getInstance().getRemoteMesh(cloakType, cloakFile, (mesh) -> {
					playerpatch.getSimulator(SimulationTypes.CLOTH).ifPresent((clothSimulator) -> {
						SoftBodyTranslatable.TRACKING_SIMULATION_SUBJECTS.add(playerpatch);
						
						clothSimulator.runWhenPermanent(
							  ClothSimulator.PLAYER_CLOAK
							, (SoftBodyTranslatable)mesh
							, ClothSimulator.ClothObjectBuilder.create().putAll("default".equals(playerpatch.getOriginal().getModelName()) ? ClothColliderPresets.BIPED : ClothColliderPresets.BIPED)
							, () -> {
								  return playerpatch.getOriginal().isCapeLoaded() && !playerpatch.getOriginal().isInvisible() && playerpatch.getOriginal().isModelPartShown(PlayerModelPart.CAPE)
										 && playerpatch.getOriginal().getItemBySlot(EquipmentSlot.CHEST).getItem() != Items.ELYTRA;
							  }
						);
						
						if (colorConfigurable && !useVanillaTexture) {
							double brightness = (colorColor & 255) / 255.0F;
							double saturation = ((colorColor & 65280) >> 8) / 255.0F;
							double hue = ((colorColor & 16711680) >> 16) / 255.0F;
							int hueColor = ColorSlider.rgbColor(hue);
							int saturationApplied = ColorSlider.sliderPositionToColor(saturation, new int[] { hueColor, 0xFFFFFFFF } );
							int brightnessApplied = ColorSlider.sliderPositionToColor(brightness, new int[] { saturationApplied, 0xFF000000 } );
							float r = ((brightnessApplied & 16711680) >> 16) / 255.0F;
							float g = ((brightnessApplied & 65280) >> 8) / 255.0F;
							float b = (brightnessApplied & 255) / 255.0F;
							
							playerpatch.setEpicSkinsInformation(new EpicSkinsInformation(fCloakTextureProvider, r, g, b));
						} else {
							playerpatch.setEpicSkinsInformation(new EpicSkinsInformation(fCloakTextureProvider, 1.0F, 1.0F, 1.0F));
						}
					});
				});
			}
		});
	}
}
