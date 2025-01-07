package yesman.epicfight.api.asset;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;

import io.netty.util.internal.StringUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import yesman.epicfight.api.animation.AnimationClip;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Keyframe;
import yesman.epicfight.api.animation.TransformSheet;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation.Phase;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.client.model.ClassicMesh;
import yesman.epicfight.api.client.model.ClassicMeshVertexBuilder;
import yesman.epicfight.api.client.model.CompositeMesh;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.MeshPartDefinition;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.model.Meshes.MeshContructor;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.SkinnedMeshVertexBuilder;
import yesman.epicfight.api.client.model.SoftBodyTranslatable;
import yesman.epicfight.api.client.model.StaticMesh;
import yesman.epicfight.api.client.model.transformer.VanillaModelTransformer.VanillaMeshPartDefinition;
import yesman.epicfight.api.exception.AssetLoadingException;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.api.utils.math.Vec4f;
import yesman.epicfight.gameasset.Armatures.ArmatureContructor;
import yesman.epicfight.main.EpicFightMod;

public class JsonAssetLoader {
	public static final OpenMatrix4f BLENDER_TO_MINECRAFT_COORD = OpenMatrix4f.createRotatorDeg(-90.0F, Vec3f.X_AXIS);
	public static final OpenMatrix4f MINECRAFT_TO_BLENDER_COORD = OpenMatrix4f.invert(BLENDER_TO_MINECRAFT_COORD, null);
	public static final String UNGROUPED_VERTICES_GROUP = "noGroups";
	
	private JsonObject rootJson;
	private ResourceLocation resourceLocation;
	private String filehash;
	
	public JsonAssetLoader(ResourceManager resourceManager, ResourceLocation resourceLocation) throws AssetLoadingException {
		JsonReader jsonReader = null;
		this.resourceLocation = resourceLocation;
		
		try {
			try {
				Resource resource = resourceManager.getResource(resourceLocation).orElseThrow();
				InputStream inputStream = resource.open();
				InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
				
				jsonReader = new JsonReader(isr);
				jsonReader.setLenient(true);
				this.rootJson = Streams.parse(jsonReader).getAsJsonObject();
			} catch (NoSuchElementException e) {
				// In this case, reads the animation data from mod.jar (Especially in a server)
				Class<?> modClass = ModList.get().getModObjectById(resourceLocation.getNamespace()).get().getClass();
				InputStream inputStream = modClass.getResourceAsStream("/assets/" + resourceLocation.getNamespace() + "/" + resourceLocation.getPath());
				
				if (inputStream == null) {
					modClass = ModList.get().getModObjectById(EpicFightMod.MODID).get().getClass();
					inputStream = modClass.getResourceAsStream("/assets/" + resourceLocation.getNamespace() + "/" + resourceLocation.getPath());
				}
				
				//Still null, throws exception.
				if (inputStream == null) {
					throw new AssetLoadingException("Can't find specified file in mod resource " + resourceLocation);
				}
				
				BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
				InputStreamReader reader = new InputStreamReader(bufferedInputStream, StandardCharsets.UTF_8);
				
				jsonReader = new JsonReader(reader);
				jsonReader.setLenient(true);
				this.rootJson = Streams.parse(jsonReader).getAsJsonObject();
			}
		} catch (IOException e) {
			throw new AssetLoadingException("Can't read " + resourceLocation.toString() + " because of " + e);
		} finally {
			if (jsonReader != null) {
				try {
					jsonReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		this.filehash = getSHA256Hash(this.rootJson.toString());
	}
	
	@OnlyIn(Dist.CLIENT)
	public JsonAssetLoader(InputStream inputstream, ResourceLocation resourceLocation) throws IOException {
		JsonReader jsonReader = null;
		this.resourceLocation = resourceLocation;
		
		jsonReader = new JsonReader(new InputStreamReader(inputstream, StandardCharsets.UTF_8));
		jsonReader.setLenient(true);
		this.rootJson = Streams.parse(jsonReader).getAsJsonObject();
		jsonReader.close();
		
		this.filehash = StringUtil.EMPTY_STRING;
	}
	
	@OnlyIn(Dist.CLIENT)
	public JsonAssetLoader(JsonObject rootJson, ResourceLocation rl) {
		this.rootJson = rootJson;
		this.resourceLocation = rl;
		this.filehash = StringUtil.EMPTY_STRING;
	}
	
	@OnlyIn(Dist.CLIENT)
	public SkinnedMesh.RenderProperties getRenderProperties() {
		if (!this.rootJson.has("render_properties")) {
			return null;
		}
		
		JsonObject properties = this.rootJson.getAsJsonObject("render_properties");
		SkinnedMesh.RenderProperties renderProperties = Mesh.RenderProperties.create();
		
		if (properties != null) {
			if (properties.has("transparent")) {
				renderProperties.transparency(properties.get("transparent").getAsBoolean());
			}
			
			if (properties.has("texture_path")) {
				renderProperties.customTexturePath(properties.get("texture_path").getAsString());
			}
			
			if (properties.has("parent_part_visualizer")) {
				JsonObject partVisualizer = properties.get("parent_part_visualizer").getAsJsonObject();
				
				partVisualizer.entrySet().forEach((entry) -> renderProperties.newPartVisualizer(entry.getKey(), entry.getValue().getAsBoolean()));
			}
			
			return renderProperties;
		}
		
		return renderProperties;
	}
	
	@OnlyIn(Dist.CLIENT)
	public ResourceLocation getParent() {
		return this.rootJson.has("parent") ? new ResourceLocation(this.rootJson.get("parent").getAsString()) : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	public Map<String, SoftBodyTranslatable.ClothSimulationInfo> loadClothInformation(Float[] positionArray) {
		JsonObject obj = this.rootJson.getAsJsonObject("vertices");
		JsonObject clothInfoObj = obj.getAsJsonObject("cloth_info");
		
		if (clothInfoObj == null) {
			return null;
		}
		
		Map<String, SoftBodyTranslatable.ClothSimulationInfo> clothInfo = Maps.newHashMap();
		
		for (Map.Entry<String, JsonElement> e : clothInfoObj.entrySet()) {
			JsonObject clothObject = e.getValue().getAsJsonObject();
			int[] particlesArray = ParseUtil.toIntArrayPrimitive(clothObject.get("particles").getAsJsonObject().get("array").getAsJsonArray());
			
			JsonArray constraintsArray = clothObject.get("constraints").getAsJsonArray();
			List<int[]> constraintsList = new ArrayList<> (constraintsArray.size());
			float[] compliances = new float[constraintsArray.size()];
			SoftBodyTranslatable.ConstraintType[] constraintType = new SoftBodyTranslatable.ConstraintType[constraintsArray.size()];
			float[] rootDistances = new float[particlesArray.length / 2];
			
			int i = 0;
			
			for (JsonElement element : constraintsArray) {
				JsonObject asJsonObject = element.getAsJsonObject();
				
				if (asJsonObject.has("unused") && GsonHelper.getAsBoolean(asJsonObject, "unused")) {
					continue;
				}
				
				constraintType[i] = SoftBodyTranslatable.ConstraintType.valueOf(GsonHelper.getAsString(asJsonObject, "type").toUpperCase(Locale.ROOT));
				compliances[i] = GsonHelper.getAsFloat(asJsonObject, "compliance");
				constraintsList.add(ParseUtil.toIntArrayPrimitive(asJsonObject.get("array").getAsJsonArray()));
				element.getAsJsonObject().get("compliance");
				i++;
			}
			
			List<Vec3> rootParticles = Lists.newArrayList();
			
			for (int j = 0; j < particlesArray.length / 2; j++) {
				if (particlesArray[j * 2 + 1] == 0) {
					int posId = particlesArray[j * 2];
					rootParticles.add(new Vec3(positionArray[posId * 3 + 0], positionArray[posId * 3 + 1], positionArray[posId * 3 + 2]));
				}
			}
			
			for (int j = 0; j < particlesArray.length / 2; j++) {
				int posId = particlesArray[j * 2];
				Vec3 position = new Vec3(positionArray[posId * 3 + 0], positionArray[posId * 3 + 1], positionArray[posId * 3 + 2]);
				Vec3 nearest = MathUtils.getNearestVector(position, rootParticles);
				rootDistances[j] = (float)position.distanceTo(nearest);
			}
			
			SoftBodyTranslatable.ClothSimulationInfo clothSimulInfo = new SoftBodyTranslatable.ClothSimulationInfo(constraintsList, constraintType, compliances, particlesArray, rootDistances);
			clothInfo.put(e.getKey(), clothSimulInfo);
		}
		
		return clothInfo;
	}
	
	@OnlyIn(Dist.CLIENT)
	public <T extends ClassicMesh> T loadClassicMesh(MeshContructor<ClassicMesh.ClassicMeshPart, ClassicMeshVertexBuilder, T> constructor) {
		ResourceLocation parent = this.getParent();
		
		if (parent != null) {
			T mesh = Meshes.getOrCreate(parent, (jsonLoader) -> jsonLoader.loadClassicMesh(constructor)).get();
			return constructor.invoke(null, null, mesh, this.getRenderProperties());
		} else {
			JsonObject obj = this.rootJson.getAsJsonObject("vertices");
			JsonObject positions = obj.getAsJsonObject("positions");
			JsonObject normals = obj.getAsJsonObject("normals");
			JsonObject uvs = obj.getAsJsonObject("uvs");
			JsonObject parts = obj.getAsJsonObject("parts");
			JsonObject indices = obj.getAsJsonObject("indices");
			
			Float[] positionArray = ParseUtil.toFloatArray(positions.get("array").getAsJsonArray());
			
			for (int i = 0; i < positionArray.length / 3; i++) {
				int k = i * 3;
				Vec4f posVector = new Vec4f(positionArray[k], positionArray[k+1], positionArray[k+2], 1.0F);
				OpenMatrix4f.transform(BLENDER_TO_MINECRAFT_COORD, posVector, posVector);
				positionArray[k] = posVector.x;
				positionArray[k+1] = posVector.y;
				positionArray[k+2] = posVector.z;
			}
			
			Float[] normalArray = ParseUtil.toFloatArray(normals.get("array").getAsJsonArray());
			
			for (int i = 0; i < normalArray.length / 3; i++) {
				int k = i * 3;
				Vec4f normVector = new Vec4f(normalArray[k], normalArray[k+1], normalArray[k+2], 1.0F);
				OpenMatrix4f.transform(BLENDER_TO_MINECRAFT_COORD, normVector, normVector);
				normalArray[k] = normVector.x;
				normalArray[k+1] = normVector.y;
				normalArray[k+2] = normVector.z;
			}
			
			Float[] uvArray = ParseUtil.toFloatArray(uvs.get("array").getAsJsonArray());
			
			Map<String, Number[]> arrayMap = Maps.newHashMap();
			Map<MeshPartDefinition, List<ClassicMeshVertexBuilder>> meshMap = Maps.newHashMap();
			Map<String, SoftBodyTranslatable.ClothSimulationInfo> clothInfoMap = this.loadClothInformation(positionArray);
			
			arrayMap.put("positions", positionArray);
			arrayMap.put("normals", normalArray);
			arrayMap.put("uvs", uvArray);
			
			if (parts != null) {
				for (Map.Entry<String, JsonElement> e : parts.entrySet()) {
					meshMap.put(VanillaMeshPartDefinition.of(e.getKey(), clothInfoMap != null ? clothInfoMap.get(e.getKey()) : null), ClassicMeshVertexBuilder.create(ParseUtil.toIntArrayPrimitive(e.getValue().getAsJsonObject().get("array").getAsJsonArray())));
				}
			}
			
			if (indices != null) {
				meshMap.put(VanillaMeshPartDefinition.of(UNGROUPED_VERTICES_GROUP, clothInfoMap != null ? clothInfoMap.get(UNGROUPED_VERTICES_GROUP) : null), ClassicMeshVertexBuilder.create(ParseUtil.toIntArrayPrimitive(indices.get("array").getAsJsonArray())));
			}
			
			return constructor.invoke(arrayMap, meshMap, null, this.getRenderProperties());
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public <T extends SkinnedMesh> T loadSkinnedMesh(MeshContructor<SkinnedMesh.SkinnedMeshPart, SkinnedMeshVertexBuilder, T> constructor) {
		ResourceLocation parent = this.getParent();
		
		if (parent != null) {
			T mesh = Meshes.getOrCreate(parent, (jsonLoader) -> jsonLoader.loadSkinnedMesh(constructor)).get();
			return constructor.invoke(null, null, mesh, this.getRenderProperties());
		} else {
			JsonObject obj = this.rootJson.getAsJsonObject("vertices");
			JsonObject positions = obj.getAsJsonObject("positions");
			JsonObject normals = obj.getAsJsonObject("normals");
			JsonObject uvs = obj.getAsJsonObject("uvs");
			JsonObject vdincies = obj.getAsJsonObject("vindices");
			JsonObject weights = obj.getAsJsonObject("weights");
			JsonObject vcounts = obj.getAsJsonObject("vcounts");
			JsonObject parts = obj.getAsJsonObject("parts");
			JsonObject indices = obj.getAsJsonObject("indices");
			
			Float[] positionArray = ParseUtil.toFloatArray(positions.get("array").getAsJsonArray());
			
			for (int i = 0; i < positionArray.length / 3; i++) {
				int k = i * 3;
				Vec4f posVector = new Vec4f(positionArray[k], positionArray[k+1], positionArray[k+2], 1.0F);
				OpenMatrix4f.transform(BLENDER_TO_MINECRAFT_COORD, posVector, posVector);
				positionArray[k] = posVector.x;
				positionArray[k+1] = posVector.y;
				positionArray[k+2] = posVector.z;
			}
			
			Float[] normalArray = ParseUtil.toFloatArray(normals.get("array").getAsJsonArray());
			
			for (int i = 0; i < normalArray.length / 3; i++) {
				int k = i * 3;
				Vec4f normVector = new Vec4f(normalArray[k], normalArray[k+1], normalArray[k+2], 1.0F);
				OpenMatrix4f.transform(BLENDER_TO_MINECRAFT_COORD, normVector, normVector);
				normalArray[k] = normVector.x;
				normalArray[k+1] = normVector.y;
				normalArray[k+2] = normVector.z;
			}
			
			Float[] uvArray = ParseUtil.toFloatArray(uvs.get("array").getAsJsonArray());
			Float[] weightArray = ParseUtil.toFloatArray(weights.get("array").getAsJsonArray());
			Integer[] affectingJointCounts = ParseUtil.toIntArray(vcounts.get("array").getAsJsonArray());
			Integer[] affectingJointIndices = ParseUtil.toIntArray(vdincies.get("array").getAsJsonArray());
			
			Map<String, Number[]> arrayMap = Maps.newHashMap();
			Map<MeshPartDefinition, List<SkinnedMeshVertexBuilder>> meshMap = Maps.newHashMap();
			Map<String, SoftBodyTranslatable.ClothSimulationInfo> clothInfoMap = this.loadClothInformation(positionArray);
			
			arrayMap.put("positions", positionArray);
			arrayMap.put("normals", normalArray);
			arrayMap.put("uvs", uvArray);
			arrayMap.put("weights", weightArray);
			arrayMap.put("vcounts", affectingJointCounts);
			arrayMap.put("vindices", affectingJointIndices);
			
			if (parts != null) {
				for (Map.Entry<String, JsonElement> e : parts.entrySet()) {
					meshMap.put(VanillaMeshPartDefinition.of(e.getKey(), clothInfoMap != null ? clothInfoMap.get(e.getKey()) : null), SkinnedMeshVertexBuilder.create(ParseUtil.toIntArrayPrimitive(e.getValue().getAsJsonObject().get("array").getAsJsonArray())));
				}
			}
			
			if (indices != null) {
				meshMap.put(VanillaMeshPartDefinition.of(UNGROUPED_VERTICES_GROUP, clothInfoMap != null ? clothInfoMap.get(UNGROUPED_VERTICES_GROUP) : null), SkinnedMeshVertexBuilder.create(ParseUtil.toIntArrayPrimitive(indices.get("array").getAsJsonArray())));
			}
			
			return constructor.invoke(arrayMap, meshMap, null, this.getRenderProperties());
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public CompositeMesh loadCompositeMesh() throws AssetLoadingException {
		if (!this.rootJson.has("meshes")) {
			throw new AssetLoadingException("Composite mesh loading exception: lower meshes undefined");
		}
		
		JsonAssetLoader clothLoader = new JsonAssetLoader(this.rootJson.get("meshes").getAsJsonObject().get("cloth").getAsJsonObject(), null);
		JsonAssetLoader staticLoader = new JsonAssetLoader(this.rootJson.get("meshes").getAsJsonObject().get("static").getAsJsonObject(), null);
		SoftBodyTranslatable softBodyMesh = (SoftBodyTranslatable)clothLoader.loadMesh(false);
		StaticMesh<?, ?> staticMesh = (StaticMesh<?, ?>)staticLoader.loadMesh(false);
		
		if (!softBodyMesh.canStartSoftBodySimulation()) {
			throw new AssetLoadingException("Composite mesh loading exception: soft mesh doesn't have cloth info");
		}
		
		return new CompositeMesh(staticMesh, softBodyMesh);
	}
	
	@OnlyIn(Dist.CLIENT)
	public Mesh loadMesh(boolean allowCompositeMesh) throws AssetLoadingException {
		if (!this.rootJson.has("mesh_loader")) {
			throw new AssetLoadingException("Mesh loading exception: No mesh loader provided!");
		}
		
		String loader = this.rootJson.get("mesh_loader").getAsString();
		
		switch (loader) {
		case "classic_mesh" -> {
			return this.loadClassicMesh(ClassicMesh::new);
		}
		case "skinned_mesh" -> {
			return this.loadSkinnedMesh(SkinnedMesh::new);
		}
		case "composite_mesh" -> {
			if (!allowCompositeMesh) {
				throw new AssetLoadingException("Can't have a composite mesh inside another composite mesh");
			}
			
			return this.loadCompositeMesh();
		}
		default -> {
			throw new AssetLoadingException("Mesh loading exception: Unsupported mesh loader: " + loader);
		}
		}
	}
	
	public <T extends Armature> T loadArmature(ArmatureContructor<T> constructor) {
		JsonObject obj = this.rootJson.getAsJsonObject("armature");
		JsonObject hierarchy = obj.get("hierarchy").getAsJsonArray().get(0).getAsJsonObject();
		JsonArray nameAsVertexGroups = obj.getAsJsonArray("joints");
		Map<String, Joint> jointMap = Maps.newHashMap();
		Joint joint = getJoint(hierarchy, nameAsVertexGroups, jointMap, true);
		joint.initOriginTransform(new OpenMatrix4f());
		String armatureName = this.resourceLocation.toString().replaceAll("(animmodels/|\\.json)", "");
		
		return constructor.invoke(armatureName, jointMap.size(), joint, jointMap);
	}
	
	public static Joint getJoint(JsonObject object, JsonArray nameAsVertexGroups, Map<String, Joint> jointMap, boolean start) {
		float[] floatArray = ParseUtil.toFloatArrayPrimitive(object.get("transform").getAsJsonArray());
		OpenMatrix4f localMatrix = OpenMatrix4f.load(null, floatArray);
		localMatrix.transpose();
		
		if (start) {
			localMatrix.mulFront(BLENDER_TO_MINECRAFT_COORD);
		}
		
		String name = object.get("name").getAsString();
		int index = -1;
		
		for (int i = 0; i < nameAsVertexGroups.size(); i++) {
			if (name.equals(nameAsVertexGroups.get(i).getAsString())) {
				index = i;
				break;
			}
		}
		
		if (index == -1) {
			throw new IllegalStateException("[ModelParsingError]: Joint name " + name + " doesn't exist!");
		}
		
		Joint joint = new Joint(name, index, localMatrix);
		jointMap.put(name, joint);
		
		if (object.has("children")) {
			for (JsonElement children : object.get("children").getAsJsonArray()) {
				joint.addSubJoints(getJoint(children.getAsJsonObject(), nameAsVertexGroups, jointMap, false));
			}
		}
		
		return joint;
	}
	
	public AnimationClip loadClipForAnimation(StaticAnimation animation) {
		if (this.rootJson == null) {
			throw new IllegalStateException("Can't find animation in path: " + animation);
		}
		
		JsonArray array = this.rootJson.get("animation").getAsJsonArray();
		boolean action = animation instanceof ActionAnimation;
		boolean attack = animation instanceof AttackAnimation;
		boolean noTransformData = !action && !attack && FMLEnvironment.dist == Dist.DEDICATED_SERVER;
		boolean root = true;
		Armature armature = animation.getArmature().get();
		
		Set<String> allowedJoints = Sets.newLinkedHashSet();
		
		if (attack) {
			for (Phase phase : ((AttackAnimation)animation).phases) {
				Joint joint = armature.rootJoint;
				
				for (AttackAnimation.JointColliderPair colliderInfo : phase.getColliders()) {
					int pathIndex = armature.searchPathIndex(colliderInfo.getFirst().getName());
					
					while (joint != null) {
						allowedJoints.add(joint.getName());
						int nextJoint = pathIndex % 10;
						
						if (nextJoint > 0) {
							pathIndex /= 10;
							joint = joint.getSubJoints().get(nextJoint - 1);
						} else {
							joint = null;
						}
					}
				}
			}
		} else if (action) {
			allowedJoints.add("Root");
		}
		
		AnimationClip clip = new AnimationClip();
		
		for (JsonElement element : array) {
			JsonObject keyObject = element.getAsJsonObject();
			String name = keyObject.get("name").getAsString();
			
			if (attack && FMLEnvironment.dist == Dist.DEDICATED_SERVER && !allowedJoints.contains(name)) {
				if (name.equals("Coord")) {
					root = false;
				}
				
				continue;
			}
			
			Joint joint = armature.searchJointByName(name);
			
			if (joint == null) {
				if (name.equals("Coord") && action) {
					JsonArray timeArray = keyObject.getAsJsonArray("time");
					JsonArray transformArray = keyObject.getAsJsonArray("transform");
					int timeNum = timeArray.size();
					int matrixNum = transformArray.size();
					float[] times = new float[timeNum];
					float[] transforms = new float[matrixNum * 16];
					
					for (int i = 0; i < timeNum; i++) {
						times[i] = timeArray.get(i).getAsFloat();
					}
					
					for (int i = 0; i < matrixNum; i++) {
						JsonArray matrixJson = transformArray.get(i).getAsJsonArray();
						
						for (int j = 0; j < 16; j++) {
							transforms[i * 16 + j] = matrixJson.get(j).getAsFloat();
						}
					}
					
					TransformSheet sheet = getTransformSheet(times, transforms, new OpenMatrix4f(), true);
					((ActionAnimation)animation).addProperty(ActionAnimationProperty.COORD, sheet);
					root = false;
					continue;
				} else {
					EpicFightMod.LOGGER.debug("[EpicFightMod] No joint named " + name + " in " + animation);
					continue;
				}
			}
			
			JsonArray timeArray = keyObject.getAsJsonArray("time");
			JsonArray transformArray = keyObject.getAsJsonArray("transform");
			int timeNum = timeArray.size();
			int matrixNum = transformArray.size();
			float[] times = new float[timeNum];
			float[] transforms = new float[matrixNum * 16];
			
			for (int i = 0; i < timeNum; i++) {
				times[i] = timeArray.get(i).getAsFloat();
			}
			
			for (int i = 0; i < matrixNum; i++) {
				JsonArray matrixJson = transformArray.get(i).getAsJsonArray();
				
				for (int j = 0; j < 16; j++) {
					transforms[i * 16 + j] = matrixJson.get(j).getAsFloat();
				}
			}
			
			TransformSheet sheet = getTransformSheet(times, transforms, OpenMatrix4f.invert(joint.getInitialLocalTransform(), null), root);
			
			if (!noTransformData) {
				clip.addJointTransform(name, sheet);
			}
			
			if (clip.getClipTime() < times[times.length - 1]) {
				clip.setClipTime(times[times.length - 1]);
			}
			
			root = false;
		}
		
		return clip;
	}
	
	public AnimationClip loadAllJointsClipForAnimation(StaticAnimation animation) {
		JsonArray array = this.rootJson.get("animation").getAsJsonArray();
		boolean root = true;
		Armature armature = animation.getArmature().get();
		AnimationClip clip = new AnimationClip();
		
		for (JsonElement element : array) {
			JsonObject keyObject = element.getAsJsonObject();
			String name = keyObject.get("name").getAsString();
			Joint joint = armature.searchJointByName(name);
			
			if (joint == null) {
				if (EpicFightMod.warnAssetExceptions()) {
					EpicFightMod.LOGGER.debug(animation.getRegistryName() + ": No joint named " + name + " in armature");
				}
				continue;
			}
			
			JsonArray timeArray = keyObject.getAsJsonArray("time");
			JsonArray transformArray = keyObject.getAsJsonArray("transform");
			int timeNum = timeArray.size();
			int matrixNum = transformArray.size();
			float[] times = new float[timeNum];
			float[] transforms = new float[matrixNum * 16];
			
			for (int i = 0; i < timeNum; i++) {
				times[i] = timeArray.get(i).getAsFloat();
			}
			
			for (int i = 0; i < matrixNum; i++) {
				JsonArray matrixJson = transformArray.get(i).getAsJsonArray();
				
				for (int j = 0; j < 16; j++) {
					transforms[i * 16 + j] = matrixJson.get(j).getAsFloat();
				}
			}
			
			TransformSheet sheet = getTransformSheet(times, transforms, OpenMatrix4f.invert(joint.getInitialLocalTransform(), null), root);
			clip.addJointTransform(name, sheet);
			
			if (clip.getClipTime() < times[times.length - 1]) {
				clip.setClipTime(times[times.length - 1]);
			}
			
			root = false;
		}
		
		return clip;
	}
	
	public JsonObject getRootJson() {
		return this.rootJson;
	}
	
	public String getFileHash() {
		return this.filehash;
	}
	
	public AnimationClip loadAnimationClip(Armature armature) {
		JsonArray array = this.rootJson.get("animation").getAsJsonArray();
		AnimationClip clip = new AnimationClip();
		boolean root = true;
		
		for (JsonElement element : array) {
			JsonObject keyObject = element.getAsJsonObject();
			String name = keyObject.get("name").getAsString();
			Joint joint = armature.searchJointByName(name);
			
			if (joint == null) {
				continue;
			}
			
			JsonArray timeArray = keyObject.getAsJsonArray("time");
			JsonArray transformArray = keyObject.getAsJsonArray("transform");
			int timeNum = timeArray.size();
			int matrixNum = transformArray.size();
			float[] times = new float[timeNum];
			float[] transforms = new float[matrixNum * 16];
			
			for (int i = 0; i < timeNum; i++) {
				times[i] = timeArray.get(i).getAsFloat();
			}
			
			for (int i = 0; i < matrixNum; i++) {
				JsonArray matrixJson = transformArray.get(i).getAsJsonArray();
				
				for (int j = 0; j < 16; j++) {
					transforms[i * 16 + j] = matrixJson.get(j).getAsFloat();
				}
			}
			
			TransformSheet sheet = getTransformSheet(times, transforms, OpenMatrix4f.invert(joint.getInitialLocalTransform(), null), root);
			clip.addJointTransform(name, sheet);
			
			if (clip.getClipTime() < times[times.length - 1]) {
				clip.setClipTime(times[times.length - 1]);
			}
			
			root = false;
		}
		
		return clip;
	}
	
	private static TransformSheet getTransformSheet(float[] times, float[] trasnformMatrix, OpenMatrix4f invLocalTransform, boolean correct) {
		List<Keyframe> keyframeList = Lists.newArrayList();
		
		for (int i = 0; i < times.length; i++) {
			float timeStamp = times[i];

			if (timeStamp < 0) {
				continue;
			}
			
			float[] matrixElements = new float[16];

			for (int j = 0; j < 16; j++) {
				matrixElements[j] = trasnformMatrix[i*16 + j];
			}
			
			OpenMatrix4f matrix = OpenMatrix4f.load(null, matrixElements);
			matrix.transpose();
			
			if (correct) {
				matrix.mulFront(BLENDER_TO_MINECRAFT_COORD);
			}
			
			matrix.mulFront(invLocalTransform);
			
			JointTransform transform = new JointTransform(matrix.toTranslationVector(), matrix.toQuaternion(), matrix.toScaleVector());
			keyframeList.add(new Keyframe(timeStamp, transform));
		}
		
		TransformSheet sheet = new TransformSheet(keyframeList);
		return sheet;
	}
	
	public static String getSHA256Hash(String str){
		String hashStream = "";
		
		try {
			MessageDigest sh = MessageDigest.getInstance("SHA-256");
			sh.update(str.getBytes());
			byte byteData[] = sh.digest();
			StringBuffer sb = new StringBuffer();
			
			for (int i = 0; i < byteData.length; i++) {
				sb.append(Integer.toString((byteData[i] & 0xFF) + 0x100, 16).substring(1));
			}
			
			hashStream = sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			hashStream = null;
		}
		
		return hashStream;
    }
}