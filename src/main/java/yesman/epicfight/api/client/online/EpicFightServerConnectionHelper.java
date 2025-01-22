package yesman.epicfight.api.client.online;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.function.BiConsumer;

import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.main.EpicFightMod;

public class EpicFightServerConnectionHelper {
	public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(60000)).build();
	
	static {
		InputStream inputstream = EpicFightMod.class.getResourceAsStream("/ServerCommunicationHelper.dll");
		String javaLibPath = System.getProperty("java.library.path");
		List<String> paths = List.of(javaLibPath.split(";"));
		File file = new File(paths.get(0) + "/ServerCommunicationHelper.dll");
		boolean exist = file.exists();
		boolean created = false;
		
		if (!exist) {
			try {
				file.createNewFile();
				FileOutputStream fos = new FileOutputStream(file);
				byte[] bytes = inputstream.readAllBytes();
				fos.write(bytes, 0, bytes.length);
				created = true;
				fos.flush();
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		System.loadLibrary("ServerCommunicationHelper");
		
		if (created) {
			file.delete();
		}
	}
	
	public static native void autoLogin(String minecraftUuid, String accessToken, String refreshToken, String provider, BiConsumer<HttpResponse<String>, Exception> onResponse);
	
	public static native void signIn(String minecraftUuid, String authenticationCode, BiConsumer<HttpResponse<String>, Exception> onResponse);
	
	public static native void signOut(String minecraftUuid, String accessToken, String refreshToken, String provider, BiConsumer<HttpResponse<String>, Exception> onResponse);
	
	public static native void getAvailableCosmetics(String accessToken, String refreshToken, String provider, BiConsumer<HttpResponse<String>, Exception> onResponse);
	
	public static native void saveConfiguration(String postBody, BiConsumer<HttpResponse<String>, Exception> onResponse);
	
	public static native void getPlayerSkinInfo(String minecraftUuid, BiConsumer<HttpResponse<String>, Exception> onResponse);
	
	/* Warn: this function doesn't behave as async */
	public static native void loadRemoteMesh(String path, BiConsumer<Mesh, Exception> onResponse);
}
