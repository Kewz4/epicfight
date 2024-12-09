package yesman.epicfight.api.client.epicskins;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

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
import yesman.epicfight.api.client.model.SoftBodyMesh;
import yesman.epicfight.api.client.physics.cloth.ClothColliderPresets;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator;
import yesman.epicfight.api.physics.SimulationTypes;
import yesman.epicfight.client.gui.widgets.ColorSlider;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;

@OnlyIn(Dist.CLIENT)
public class EpicSkinsInformation {
	private static final String USER_COSMETICS_ENDPOINT = "https://127.0.0.1:8080/cosmetic/user_cosmetic";
	
	public static void initEpicSkins(AbstractClientPlayerPatch<?> playerpatch) {
		HttpRequest request = HttpRequest.newBuilder()
			     .GET()
			     .uri(URI.create(USER_COSMETICS_ENDPOINT + "?mc_uuid=" + playerpatch.getOriginal().getStringUUID()))
			     .build();

		RemoteAssets.getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((response, ex) -> {
			JsonReader jsonReader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(response.body().getBytes()), StandardCharsets.UTF_8));
			JsonObject json = Streams.parse(jsonReader).getAsJsonObject();
			
			if (json.entrySet().isEmpty()) {
				playerpatch.getSimulator(SimulationTypes.CLOTH).ifPresent((clothSimulator) -> {
					SoftBodyMesh.TRACKING_SIMULATABLE_OBJECTS.add(playerpatch);
					
					clothSimulator.runWhenPermanent(
						  ClothSimulator.PLAYER_CLOAK
						, Meshes.CLOAK
						, ClothSimulator.ClothObjectBuilder.create().putAll("default".equals(playerpatch.getOriginal().getModelName()) ? ClothColliderPresets.BIPED : ClothColliderPresets.BIPED)
						, () -> {
							  return playerpatch.getOriginal().isCapeLoaded() && !playerpatch.getOriginal().isInvisible() && playerpatch.getOriginal().isModelPartShown(PlayerModelPart.CAPE)
									 && playerpatch.getOriginal().getItemBySlot(EquipmentSlot.CHEST).getItem() != Items.ELYTRA;
						  }
					);
					
					playerpatch.setEpicSkinsInformation(new EpicSkinsInformation(playerpatch.getOriginal().getCloakTextureLocation(), 1.0F, 1.0F, 1.0F));
				});
			} else {
				int cloakType = GsonHelper.getAsInt(json, "cloakTypeId");
				int colorColor = GsonHelper.getAsInt(json, "cloakColor");
				boolean useVanillaTexture = GsonHelper.getAsBoolean(json, "cloakVanillaTexture");
				boolean colorConfigurable = GsonHelper.getAsBoolean(json, "colorConfigurable");
				String cloakFile = GsonHelper.getAsString(json, "fileLocation");
				ResourceLocation cloakTexture = useVanillaTexture ? playerpatch.getOriginal().getCloakTextureLocation() : RemoteAssets.getInstance().getRemoteTexture(GsonHelper.getAsString(json, "textureLocation"));
				
				RemoteAssets.getInstance().getRemoteMesh(cloakType, cloakFile, (mesh) -> {
					playerpatch.getSimulator(SimulationTypes.CLOTH).ifPresent((clothSimulator) -> {
						SoftBodyMesh.TRACKING_SIMULATABLE_OBJECTS.add(playerpatch);
						
						clothSimulator.runWhenPermanent(
							  ClothSimulator.PLAYER_CLOAK
							, (SoftBodyMesh)mesh
							, ClothSimulator.ClothObjectBuilder.create().putAll("default".equals(playerpatch.getOriginal().getModelName()) ? ClothColliderPresets.BIPED : ClothColliderPresets.BIPED)
							, () -> {
								  return playerpatch.getOriginal().isCapeLoaded() && !playerpatch.getOriginal().isInvisible() && playerpatch.getOriginal().isModelPartShown(PlayerModelPart.CAPE)
										 && playerpatch.getOriginal().getItemBySlot(EquipmentSlot.CHEST).getItem() != Items.ELYTRA;
							  }
						);
						
						if (colorConfigurable) {
							double brightness = (colorColor & 255) / 255.0F;
							double saturation = ((colorColor & 65280) >> 8) / 255.0F;
							double hue = ((colorColor & 16711680) >> 16) / 255.0F;
							int hueColor = ColorSlider.rgbColor(hue);
							int saturationApplied = ColorSlider.sliderPositionToColor(saturation, new int[] { hueColor, 0xFFFFFFFF } );
							int brightnessApplied = ColorSlider.sliderPositionToColor(brightness, new int[] { saturationApplied, 0xFFFFFFFF } );
							float r = ((brightnessApplied & 16711680) >> 16) / 255.0F;
							float g = ((brightnessApplied & 65280) >> 8) / 255.0F;
							float b = (brightnessApplied & 255) / 255.0F;
							
							playerpatch.setEpicSkinsInformation(new EpicSkinsInformation(cloakTexture, r, g, b));
						} else {
							playerpatch.setEpicSkinsInformation(new EpicSkinsInformation(cloakTexture, 1.0F, 1.0F, 1.0F));
						}
					});
				});
			}
		});
	}
	
	private final ResourceLocation cloakTexture;
	private final float r;
	private final float g;
	private final float b;
	
	private EpicSkinsInformation(ResourceLocation cloakTexture, float r, float g, float b) {
		this.cloakTexture = cloakTexture;
		this.r = r;
		this.g = g;
		this.b = b;
	}
	
	public ResourceLocation cloakTexture() {
		return this.cloakTexture;
	}
	
	public float r() {
		return this.r;
	}
	
	public float g() {
		return this.g;
	}
	
	public float b() {
		return this.b;
	}
}
