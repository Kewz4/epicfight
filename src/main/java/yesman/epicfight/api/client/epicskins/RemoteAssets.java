package yesman.epicfight.api.client.epicskins;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.online.texture.RemoteTexture;
import yesman.epicfight.main.EpicFightMod;

@OnlyIn(Dist.CLIENT)
public class RemoteAssets {
	public static final String SERVER_URL = "https://epic-fight.com";
	
	private static final RemoteAssets INSTANCE = new RemoteAssets();
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
															.connectTimeout(Duration.ofMillis(60000))
															.build();
	
	private static final TextureManager TEXTURE_MANAGER = Minecraft.getInstance().getTextureManager();
	
	public static HttpClient getHttpClient() {
		return HTTP_CLIENT;
	}
	
	public static RemoteAssets getInstance() {
		return INSTANCE;
	}
	
	private final Map<Integer, CachedMeshProvider> cachedMeshes = Maps.newConcurrentMap();
	
	public synchronized AssetAccessor<Mesh> getRemoteMesh(int seq, String path, @Nullable Consumer<Mesh> callback) {
		if (this.cachedMeshes.containsKey(seq)) {
			CachedMeshProvider cachedMesh = this.cachedMeshes.get(seq);
			
			if (callback != null) {
				if (cachedMesh.get() == null) {
					cachedMesh.addWork(callback);
				} else {
					callback.accept(cachedMesh.get());
				}
			}
			
			return cachedMesh;
		}
		
		HttpRequest request = HttpRequest.newBuilder()
									     .GET()
									     .uri(URI.create(SERVER_URL + "/models/" + path))
									     .build();
		
		CachedMeshProvider meshProvider = new CachedMeshProvider();
		meshProvider.addWork(callback);
		this.cachedMeshes.put(seq, meshProvider);
		
		HTTP_CLIENT.sendAsync(request, BodyHandlers.ofString()).whenCompleteAsync((response, ex) -> {
			if (ex != null) {
				EpicFightMod.LOGGER.error("Failed at loading remote mesh " + seq + ": " + ex.getMessage());
			} else {
				if (response.statusCode() == 200) {
					Minecraft.getInstance().execute(() -> {
						try {
							JsonAssetLoader jsonLoader = new JsonAssetLoader(new ByteArrayInputStream(response.body().getBytes()), null);
							meshProvider.loadMesh(jsonLoader.loadMesh(true));
						} catch (Exception e) {
							e.printStackTrace();
						}
					});
				} else {
					EpicFightMod.LOGGER.error("Failed at loading remote mesh " + path + ": status: " + response.statusCode() + ", " + response.body());
				}
			}
		});
		
		return this.cachedMeshes.get(seq);
	}
	
	public synchronized ResourceLocation getRemoteTexture(String fileName) {
		ResourceLocation textureLocation = new ResourceLocation(EpicFightMod.EPICSKINS_MODID, "textures/remote/" + fileName);
		AbstractTexture texture = TEXTURE_MANAGER.getTexture(textureLocation, MissingTextureAtlasSprite.getTexture());
		
		if (texture == MissingTextureAtlasSprite.getTexture()) {
			AbstractTexture httptexture = new RemoteTexture(SERVER_URL + "/textures/" + fileName, MissingTextureAtlasSprite.getLocation());
			TEXTURE_MANAGER.register(textureLocation, httptexture);
		}
		
		return textureLocation;
	}
	
	@OnlyIn(Dist.CLIENT)
	private class CachedMeshProvider implements AssetAccessor<Mesh> {
		private Queue<Consumer<Mesh>> callback = Queues.newArrayDeque();
		private Mesh mesh;
		
		public void addWork(Consumer<Mesh> callback) {
			this.callback.add(callback);
		}
		
		public void loadMesh(Mesh mesh) {
			this.mesh = mesh;
			this.callback.forEach((callback) -> callback.accept(mesh));
			this.callback.clear();
			this.callback = null;
		}
		
		@Override
		public Mesh get() {
			return this.mesh;
		}
		
		@Override
		public ResourceLocation registryName() {
			return null;
		}
		
		@Override
		public boolean isPresent() {
			return this.mesh != null;
		}

		@Override
		public boolean inRegistry() {
			return false;
		}
	}
}
