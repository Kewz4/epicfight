package yesman.epicfight.api.client.online;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.MeshProvider;
import yesman.epicfight.api.client.online.texture.RemoteTexture;
import yesman.epicfight.api.model.JsonAssetLoader;
import yesman.epicfight.main.EpicFightMod;

@OnlyIn(Dist.CLIENT)
public class OnlineAssets {
	private static final String MODEL_ENDPOINT = "https://127.0.0.1:8080/models/";
	private static final String TEXTURE_ENDPOINT = "https://127.0.0.1:8080/textures/";
	
	private static final OnlineAssets INSTANCE = new OnlineAssets();
	
	public static OnlineAssets getInstance() {
		return INSTANCE;
	}
	
	private final HttpClient httpClient;
	private final Map<Integer, CachedMeshProvider> cachedCapes = Maps.newHashMap();
	
	private OnlineAssets() {
		this.httpClient = HttpClient.newBuilder()
									.connectTimeout(Duration.ofMillis(60000))
									.build();
	}
	
	public MeshProvider<Mesh> getCosmeticMesh(int seq, String path, @Nullable Consumer<Mesh> loadCallback) {
		if (this.cachedCapes.containsKey(seq)) {
			return this.cachedCapes.get(seq);
		}
		
		HttpRequest request = HttpRequest.newBuilder()
									     .GET()
									     .uri(URI.create(MODEL_ENDPOINT + path))
									     .build();
		
		this.httpClient.sendAsync(request, BodyHandlers.ofString()).whenComplete((response, ex) -> {
			if (ex != null) {
				EpicFightMod.LOGGER.error("Failed at loading cape cosmetic " + seq + ": " + ex.getMessage());
			} else {
				if (response.statusCode() == 200) {
					try {
						JsonAssetLoader jsonLoader = new JsonAssetLoader(new ByteArrayInputStream(response.body().getBytes()), null);
						Mesh mesh = jsonLoader.loadMesh();
						this.cachedCapes.get(seq).setMesh(mesh);
						
						if (loadCallback != null) {
							loadCallback.accept(mesh);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					EpicFightMod.LOGGER.error("Failed at loading cape cosmetic " + seq + ": http response " + response.statusCode());
				}
			}
		});
		
		this.cachedCapes.put(seq, new CachedMeshProvider());
		
		return this.cachedCapes.get(seq);
	}
	
	public ResourceLocation registerRemoteTexture(String fileName) {
		ResourceLocation textureLocation = new ResourceLocation(EpicFightMod.EPICSKINS_MODID, "textures/remote/" + fileName);
		TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
		AbstractTexture texture = texturemanager.getTexture(textureLocation, MissingTextureAtlasSprite.getTexture());
		
		if (texture == MissingTextureAtlasSprite.getTexture()) {
			AbstractTexture httptexture = new RemoteTexture(TEXTURE_ENDPOINT + fileName, MissingTextureAtlasSprite.getLocation());
			texturemanager.register(textureLocation, httptexture);
		}
		
		return textureLocation;
	}
	
	@OnlyIn(Dist.CLIENT)
	private class CachedMeshProvider implements MeshProvider<Mesh> {
		private Mesh mesh;
		
		private void setMesh(Mesh mesh) {
			this.mesh = mesh;
		}
		
		@Override
		public Mesh get() {
			return this.mesh;
		}
	}
}
