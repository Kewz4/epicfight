package yesman.epicfight.api.animation;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;
import yesman.epicfight.api.animation.SynchedAnimationVariableKey.SynchedIndependentAnimationVariableKey;
import yesman.epicfight.api.utils.PacketBufferCodec;
import yesman.epicfight.main.EpicFightMod;

public class SynchedAnimationVariableKeys {
	private static final Supplier<RegistryBuilder<SynchedAnimationVariableKey<?>>> BUILDER = () -> new RegistryBuilder<SynchedAnimationVariableKey<?>>().addCallback(SynchedAnimationVariableKey.getRegistryCallback());
	
	public static final DeferredRegister<SynchedAnimationVariableKey<?>> SYNCHED_ANIMATION_VARIABLE_KEYS = DeferredRegister.create(new ResourceLocation(EpicFightMod.MODID, "synched_animation_variable_keys"), EpicFightMod.MODID);
	public static final Supplier<IForgeRegistry<SynchedAnimationVariableKey<?>>> REGISTRY = SYNCHED_ANIMATION_VARIABLE_KEYS.makeRegistry(BUILDER);
	
	public static final RegistryObject<SynchedIndependentAnimationVariableKey<Vec3>> DESTINATION = SYNCHED_ANIMATION_VARIABLE_KEYS.register("destination", () -> SynchedAnimationVariableKey.independent(() -> (Vec3)null, true, PacketBufferCodec.VEC3));
	
	public static final RegistryObject<SynchedIndependentAnimationVariableKey<Integer>> TARGET_ENTITY = SYNCHED_ANIMATION_VARIABLE_KEYS.register("target_entity", () -> SynchedAnimationVariableKey.independent(() -> (Integer)null, true, PacketBufferCodec.INTEGER));
}
