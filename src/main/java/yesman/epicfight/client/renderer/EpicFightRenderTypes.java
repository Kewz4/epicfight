package yesman.epicfight.client.renderer;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import yesman.epicfight.api.exception.ShaderParsingException;
import yesman.epicfight.client.renderer.EpicFightVertexFormat.AnimationVertexFormat;
import yesman.epicfight.client.renderer.shader.AnimationShaderInstance;
import yesman.epicfight.client.renderer.shader.ShaderParser;
import yesman.epicfight.client.renderer.shader.VanillaAnimationShader;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.main.EpicFightMod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EpicFightMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public abstract class EpicFightRenderTypes extends RenderType {
	private static final RenderType ENTITY_UI_COLORED = 
		create(
			  EpicFightMod.MODID + ":ui_color"
			, DefaultVertexFormat.POSITION_COLOR
			, VertexFormat.Mode.QUADS
			, 256
			, true
			, false
			, RenderType.CompositeState.builder()
				.setShaderState(POSITION_COLOR_SHADER)
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
				.setLightmapState(NO_LIGHTMAP)
				.setOverlayState(NO_OVERLAY)
				.createCompositeState(true)
		);
	
	private static final Function<ResourceLocation, RenderType> ENTITY_UI_TEXTURE = Util.memoize(
		(textureLocation) -> create( 
			  EpicFightMod.MODID + ":ui_texture"
			, DefaultVertexFormat.POSITION_TEX
			, VertexFormat.Mode.QUADS
			, 256
			, true
			, false
			, RenderType.CompositeState.builder()
				.setShaderState(POSITION_TEX_SHADER)
				.setTextureState(new RenderStateShard.TextureStateShard(textureLocation, false, false))
				.setTransparencyState(NO_TRANSPARENCY)
				.setLightmapState(NO_LIGHTMAP)
				.setOverlayState(NO_OVERLAY)
				.createCompositeState(true)
		)
	);
	
	private static final RenderType OBB = create(
		  EpicFightMod.MODID + ":debug_collider"
		, DefaultVertexFormat.POSITION_COLOR_NORMAL
		, VertexFormat.Mode.LINE_STRIP
		, 256
		, false
		, false
		, RenderType.CompositeState.builder()
			.setShaderState(POSITION_COLOR_SHADER)
			.setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
			.setLayeringState(VIEW_OFFSET_Z_LAYERING)
			.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
			.setOutputState(ITEM_ENTITY_TARGET)
			.setWriteMaskState(COLOR_DEPTH_WRITE)
			.setCullState(NO_CULL)
			.createCompositeState(false)
	);
	
	private static final RenderType DEBUG_QUADS = create(
		  EpicFightMod.MODID + ":debug_quad"
		, DefaultVertexFormat.POSITION_COLOR
		, VertexFormat.Mode.QUADS
		, 256
		, false
		, false
		, RenderType.CompositeState.builder()
			.setShaderState(POSITION_COLOR_SHADER)
			.setLayeringState(VIEW_OFFSET_Z_LAYERING)
			.setTransparencyState(NO_TRANSPARENCY)
			.setWriteMaskState(COLOR_DEPTH_WRITE)
			.setCullState(NO_CULL)
			.createCompositeState(false)
	);
	
	private static final RenderType GUI_TRIANGLE = create(
		  EpicFightMod.MODID + ":gui_triangle"
		, DefaultVertexFormat.POSITION_COLOR
		, VertexFormat.Mode.TRIANGLES
		, 256
		, false
		, false
		, RenderType.CompositeState.builder()
			.setShaderState(RENDERTYPE_GUI_SHADER)
			.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
			.setDepthTestState(LEQUAL_DEPTH_TEST)
			.createCompositeState(false)
	);
	
	private static final Map<String, Map<ResourceLocation, RenderType>> TRIANGLED_RENDERTYPES_BY_NAME_TEXTURE = Maps.newHashMap();
	
	private static final Function<RenderType, RenderType> TRIANGULATED_RENDER_TYPES = Util.memoize((renderType$1) -> {
		if (renderType$1 instanceof CompositeRenderType compositeRenderType) {
			if (TRIANGLED_RENDERTYPES_BY_NAME_TEXTURE.containsKey(renderType$1.name)) {
				Map<ResourceLocation, RenderType> renderTypesByTexture = TRIANGLED_RENDERTYPES_BY_NAME_TEXTURE.get(renderType$1.name);
				
				if (compositeRenderType.state.textureState instanceof TextureStateShard texStateShard) {
					ResourceLocation texLocation = texStateShard.texture.orElse(null);
					
					if (renderTypesByTexture.containsKey(texLocation)) {
						return renderTypesByTexture.get(texLocation);
					}
				}
			}
			
			return new CompositeRenderType(renderType$1.name, renderType$1.format, VertexFormat.Mode.TRIANGLES, renderType$1.bufferSize(), renderType$1.affectsCrumbling(), renderType$1.sortOnUpload, compositeRenderType.state);
		} else {
			return renderType$1;
		}
	});
	
	public static RenderType getTriangulated(RenderType renderType) {
		return TRIANGULATED_RENDER_TYPES.apply(renderType);
	}
	
	/**
	 * Cache all Texture - RenderType entries to replace texture by MeshPart
	 */
	public static void addRenderType(String name, ResourceLocation textureLocation, RenderType renderType) {
		Map<ResourceLocation, RenderType> renderTypesByTexture = TRIANGLED_RENDERTYPES_BY_NAME_TEXTURE.computeIfAbsent(name, (k) -> Maps.newHashMap());
		renderTypesByTexture.put(textureLocation, renderType);
	}
	
	private static RenderType createTextureShardReplaced(ResourceLocation texToReplace, RenderType renderType) {
		if (renderType instanceof CompositeRenderType compositeRenderType && compositeRenderType.state.textureState instanceof TextureStateShard texStateShard) {
			CompositeState textureReplacedState = new CompositeState(
				  new RenderStateShard.TextureStateShard(texToReplace, texStateShard.blur, texStateShard.mipmap)
				, compositeRenderType.state.shaderState
				, compositeRenderType.state.transparencyState
				, compositeRenderType.state.depthTestState
				, compositeRenderType.state.cullState
				, compositeRenderType.state.lightmapState
				, compositeRenderType.state.overlayState
				, compositeRenderType.state.layeringState
				, compositeRenderType.state.outputState
				, compositeRenderType.state.texturingState
				, compositeRenderType.state.writeMaskState
				, compositeRenderType.state.lineState
				, compositeRenderType.state.colorLogicState
				, compositeRenderType.state.outlineProperty
			);
			
			return new CompositeRenderType(renderType.name, renderType.format, compositeRenderType.mode(), renderType.bufferSize(), renderType.affectsCrumbling(), renderType.sortOnUpload, textureReplacedState);
		} else {
			return null;
		}
	}
	
	public static RenderType replaceTexture(ResourceLocation texLocation, RenderType renderType) {
		if (TRIANGLED_RENDERTYPES_BY_NAME_TEXTURE.containsKey(renderType.name)) {
			Map<ResourceLocation, RenderType> renderTypesByTexture = TRIANGLED_RENDERTYPES_BY_NAME_TEXTURE.get(renderType.name);
			
			if (renderTypesByTexture.containsKey(texLocation)) {
				return renderTypesByTexture.get(texLocation);
			}
		}
		
		RenderType textureReplacedRenderType = createTextureShardReplaced(texLocation, renderType);
		
		if (textureReplacedRenderType == null) {
			return renderType;
		}
		
		Map<ResourceLocation, RenderType> renderTypesByTexture = TRIANGLED_RENDERTYPES_BY_NAME_TEXTURE.computeIfAbsent(textureReplacedRenderType.name, k -> Maps.newHashMap());
		renderTypesByTexture.put(texLocation, textureReplacedRenderType);
		
		return textureReplacedRenderType;
	}
	
	public static RenderType entityUIColor() {
		return ENTITY_UI_COLORED;
	}
	
	public static RenderType entityUITexture(ResourceLocation resourcelocation) {
		return ENTITY_UI_TEXTURE.apply(resourcelocation);
	}
	
	public static RenderType debugCollider() {
		return OBB;
	}
	
	public static RenderType debugQuads() {
		return DEBUG_QUADS;
	}
	
	public static RenderType guiTriangle() {
		return GUI_TRIANGLE;
	}
	
	private static Map<ResourceLocation, Resource> SHADER_LIBS;
	private static final List<ShaderTransformer> ANIMATION_SHADERS_TRANSFORMERS = Lists.newArrayList();
	private static final Map<String, AnimationShaderInstance> ANIMATION_SHADERS = Maps.newConcurrentMap();
	private static final Function<VertexFormat, VertexFormat> ANIMATION_VERTEX_FORMATS = Util.memoize((vertexFormat) -> {
		if (vertexFormat instanceof AnimationVertexFormat) {
			return vertexFormat;
		}
		
		ImmutableMap.Builder<String, VertexFormatElement> vertexFormatElements = ImmutableMap.builder();
		
		vertexFormat.getElementMapping().entrySet().stream().filter((entry) -> EpicFightVertexFormat.keep(entry.getValue()))
															.map((entry) -> Pair.of(entry.getKey(), EpicFightVertexFormat.convert(entry.getValue())))
															.forEach((pair) -> vertexFormatElements.put(pair.getFirst(), pair.getSecond()));
		
		vertexFormatElements.put("Joints", EpicFightVertexFormat.ELEMENT_JOINTS);
		vertexFormatElements.put("Weights", EpicFightVertexFormat.ELEMENT_WEIGHTS);
		
		VertexFormat animationVertexFormat = new AnimationVertexFormat(vertexFormatElements.build());
		
		return animationVertexFormat;
	});
	
	public static AnimationShaderInstance getAnimationShader(ShaderInstance shaderInstance) {
		if (shaderInstance instanceof AnimationShaderInstance animationShaderInstance) {
			return animationShaderInstance;
		}
		
		try {
			if (!ANIMATION_SHADERS.containsKey(shaderInstance.getName())) {
				AnimationShaderInstance animationShaderInstance = null;
				
				for (ShaderTransformer shaderTransformer : ANIMATION_SHADERS_TRANSFORMERS) {
					if (shaderTransformer.predicate().test(shaderInstance)) {
						animationShaderInstance = shaderTransformer.transformer().apply(shaderInstance);
						break;
					}
				}
				
				if (animationShaderInstance == null) {
					animationShaderInstance = ShaderTransformer.VANILLA_TRANSFORMER.transformer.apply(shaderInstance);
				}
				
				if (animationShaderInstance != null) {
					ANIMATION_SHADERS.put(shaderInstance.getName(), animationShaderInstance);
				}
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			EpicFightMod.LOGGER.warn("Failed to create shader with " + e.getMessage() + ". Automatically switches animation shader mode off.");
			Minecraft.getInstance().levelRenderer.allChanged();
			Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("epicfight.messages.shader_transform_fail", shaderInstance.getName()).withStyle(ChatFormatting.RED));
			
			ClientConfig.animationShaderLockedByException = true;
			ClientConfig.activateAnimationShader = false;
			ClientConfig.saveChanges();
		}
		
		return ANIMATION_SHADERS.get(shaderInstance.getName());
	}
	
	public static AnimationShaderInstance getAnimationShader(RenderType renderType) {
		if (renderType instanceof CompositeRenderType compositeRenderType) {
			Optional<Supplier<ShaderInstance>> shaderInstanceOptional = compositeRenderType.state.shaderState.shader;
			
			if (shaderInstanceOptional.isPresent()) {
				return getAnimationShader(shaderInstanceOptional.get().get());
			}
		}
		
		return null;
	}
	
	public static VertexFormat getAnimationVertexFormat(VertexFormat vertexFormat) {
		if (vertexFormat instanceof AnimationVertexFormat) {
			return vertexFormat;
		}
		
		return ANIMATION_VERTEX_FORMATS.apply(vertexFormat);
	}
	
	public static void registerShaderTransformer(Predicate<ShaderInstance> predicate, Function<ShaderInstance, AnimationShaderInstance> transformer) {
		ANIMATION_SHADERS_TRANSFORMERS.add(new ShaderTransformer(predicate, transformer));
	}
	
	@SubscribeEvent
	public static void registerShadersEvent(RegisterShadersEvent event) throws IOException {
		ANIMATION_SHADERS.clear();
		
		Map<ResourceLocation, Resource> shaderLibs = ((ResourceManager)((GameRenderer.ResourceCache) event.getResourceProvider()).original()).listResources("shaders/include", (rl) -> {
			String s = rl.getPath();
			return s.endsWith(".glsl");
		});
		
		SHADER_LIBS = ImmutableMap.copyOf(shaderLibs); 
		ClientConfig.animationShaderLockedByException = false;
	}
	
	public static void clearAnimationShaderInstance(String shaderName) {
		if (!ANIMATION_SHADERS.containsKey(shaderName)) {
			return;
		}
		
		AnimationShaderInstance animationShaderInstance = ANIMATION_SHADERS.get(shaderName);
		animationShaderInstance._clear();
		animationShaderInstance._close();
		ANIMATION_SHADERS.remove(shaderName);
	}
	
	@OnlyIn(Dist.CLIENT)
	private record ShaderTransformer(Predicate<ShaderInstance> predicate, Function<ShaderInstance, AnimationShaderInstance> transformer) {
		public static final ShaderTransformer VANILLA_TRANSFORMER = new ShaderTransformer((shaderInstance) -> true, (shaderInstance) -> {
			ShaderParser shaderParser = null;
			
			try {
				ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
				ResourceLocation shaderLocation = ResourceLocation.parse(shaderInstance.getName());
				shaderParser = new ShaderParser(resourceManager, shaderInstance.getName());
				boolean hasNormalAttribute = shaderParser.hasAttribute("Normal");
				boolean isEyesShader = "rendertype_eyes".equals(shaderLocation.getPath());
				
				if (shaderParser.hasAttribute("Color")) {
					shaderParser.addUniform("Color", ShaderParser.GLSLType.VEC4, "in .* Color;", ShaderParser.InsertPosition.FOLLOWING, Integer.MAX_VALUE, ShaderParser.ExceptionHandler.THROW, new Double[] {1.0D, 1.0D, 1.0D, 1.0D});
				}
				
				if (shaderParser.hasAttribute("UV1") && !isEyesShader) {
					shaderParser.addUniform("UV1", ShaderParser.GLSLType.IVEC2, "in .* UV1;", ShaderParser.InsertPosition.FOLLOWING, Integer.MAX_VALUE, ShaderParser.ExceptionHandler.THROW, new Integer[] {0, 0});
				}
				
				if (shaderParser.hasAttribute("UV2") && !isEyesShader) {
					shaderParser.addUniform("UV2", ShaderParser.GLSLType.IVEC2, "in .* UV2;", ShaderParser.InsertPosition.FOLLOWING, Integer.MAX_VALUE, ShaderParser.ExceptionHandler.THROW, new Integer[] {0, 0});
				}
				
				shaderParser.remove("Color", ShaderParser.Usage.ATTRIBUTE, ShaderParser.ExceptionHandler.IGNORE);
				shaderParser.remove("UV1", ShaderParser.Usage.ATTRIBUTE, ShaderParser.ExceptionHandler.IGNORE);
				shaderParser.remove("UV2", ShaderParser.Usage.ATTRIBUTE, ShaderParser.ExceptionHandler.IGNORE);
				shaderParser.addAttribute("Joints", ShaderParser.ExceptionHandler.THROW, ShaderParser.GLSLType.IVEC3);
				shaderParser.addAttribute("Weights", ShaderParser.ExceptionHandler.THROW, ShaderParser.GLSLType.VEC3);
				
				if (hasNormalAttribute && !isEyesShader) {
					shaderParser.addUniform("Normal_Mv_Matrix", ShaderParser.GLSLType.MATRIX3F, ShaderParser.ExceptionHandler.THROW, null);
				}
				
				shaderParser.addUniformArray("Poses", ShaderParser.GLSLType.MATRIX4F, ShaderParser.ExceptionHandler.THROW, null, ShaderParser.SHADER_ARRAY_LIMIT);
				shaderParser.replaceScript("Position", "Position_a", -1, ShaderParser.ExceptionHandler.THROW, "gl_Position", "in vec3 Position;");
				
				if (hasNormalAttribute && !isEyesShader) {
					shaderParser.replaceScript("Normal", "Normal_a", -1, ShaderParser.ExceptionHandler.THROW, "uniform mat3 Normal_Mv_Matrix;", "in vec3 Normal;");
				}
				
				shaderParser.insertToScript("in vec3 Position;", "\nvec3 Position_a = vec3(0.0);", 0, ShaderParser.InsertPosition.FOLLOWING);
				
				if (hasNormalAttribute && !isEyesShader) {
					shaderParser.insertToScript("in vec3 Normal;", "\nvec3 Normal_a = vec3(0.0);", 0, ShaderParser.InsertPosition.FOLLOWING);
				}
				
				shaderParser.insertToScript("void main\\(\\) \\{",
										    "void setAnimationPosition() {\n"
										  + "    for(int i=0;i<3;i++)\n"
										  + "    {\n"
										  + "        mat4 jointTransform = Poses[Joints[i]];\n"
										  + "        vec4 posePosition = jointTransform * vec4(Position, 1.0);\n"
										  + "        Position_a += vec3(posePosition.xyz) * Weights[i];\n"
										  + "    }\n"
										  + "}\n"
										  + "\n", 0, ShaderParser.InsertPosition.PRECEDING);
				
				if (hasNormalAttribute && !isEyesShader) {
					shaderParser.insertToScript("void main\\(\\) \\{",
											    "void setAnimationNormal() {\n"
											  + "    \n"
											  + "    for(int i=0;i<3;i++)\n"
											  + "    {\n"
											  + "        mat4 jointTransform = Poses[Joints[i]];\n"
											  + "        vec4 poseNormal = jointTransform * vec4(Normal, 1.0);\n"
											  + "        Normal_a += vec3(poseNormal.xyz) * Weights[i];\n"
											  + "    }\n"
											  + "    \n"
											  + "    Normal_a = Normal_Mv_Matrix * Normal_a;\n"
											  + "}\n", 0, ShaderParser.InsertPosition.PRECEDING);
					
					shaderParser.insertToScript("void main\\(\\) \\{", "\n    setAnimationNormal();", 0, ShaderParser.InsertPosition.FOLLOWING);
				}
				
				shaderParser.insertToScript("void main\\(\\) \\{", "\n    setAnimationPosition();", 0, ShaderParser.InsertPosition.FOLLOWING);
				
				Map<ResourceLocation, Resource> cache = Maps.newHashMap();
				cache.putAll(SHADER_LIBS);
				shaderParser.addToResourceCache(cache);
				GameRenderer.ResourceCache resourceProvider = new GameRenderer.ResourceCache(resourceManager, cache);
				
				return new VanillaAnimationShader(resourceProvider, ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID, shaderLocation.getPath()), EpicFightRenderTypes.getAnimationVertexFormat(shaderInstance.getVertexFormat()));
			} catch (IOException | ShaderParsingException e) {
				e.printStackTrace();
				
				if (shaderParser != null) {
					EpicFightMod.LOGGER.warn("Shader Script\n " + shaderParser.getOriginalScript());
				}
				
				throw new RuntimeException("Can't create animation shader", e);
			}
		});
	}
	
	//Util class
	private EpicFightRenderTypes() {
		super(null, null, null, -1, false, false, null, null);
	}
}