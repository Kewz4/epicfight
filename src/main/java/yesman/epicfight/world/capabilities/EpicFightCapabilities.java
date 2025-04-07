package yesman.epicfight.world.capabilities;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.projectile.ProjectilePatch;
import yesman.epicfight.world.capabilities.skill.CapabilitySkill;

@SuppressWarnings("rawtypes")
public class EpicFightCapabilities {
	public static final Capability<EntityPatch> CAPABILITY_ENTITY = CapabilityManager.get(new CapabilityToken<>(){});
    public static final Capability<CapabilityItem> CAPABILITY_ITEM = CapabilityManager.get(new CapabilityToken<>(){});
    public static final Capability<ProjectilePatch> CAPABILITY_PROJECTILE = CapabilityManager.get(new CapabilityToken<>(){});
    public static final Capability<CapabilitySkill> CAPABILITY_SKILL = CapabilityManager.get(new CapabilityToken<>(){});
    
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.register(CapabilityItem.class);
		event.register(EntityPatch.class);
		event.register(ProjectilePatch.class);
		event.register(CapabilitySkill.class);
	}
	
	public static CapabilityItem getItemStackCapability(ItemStack stack) {
		return stack.isEmpty() ? CapabilityItem.EMPTY : stack.getCapability(CAPABILITY_ITEM).orElse(CapabilityItem.EMPTY);
	}
	
	public static CapabilityItem getItemStackCapabilityOr(ItemStack stack, @Nullable CapabilityItem defaultCap) {
		return stack.isEmpty() ? defaultCap : stack.getCapability(CAPABILITY_ITEM).orElse(defaultCap);
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends EntityPatch> T getEntityPatch(Entity entity, Class<T> type) {
		if (entity != null) {
			EntityPatch<?> entitypatch = entity.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY).orElse(null);
			
			if (entitypatch != null && type.isAssignableFrom(entitypatch.getClass())) {
				return (T)entitypatch;
			}
		}
		
		return null;
	}
	
	/**
	 * Return entity patch with unparameterized original entity
	 */
	@SuppressWarnings("unchecked")
	public static <T extends EntityPatch<?>> Optional<T> getEntityPatchUnparameterized(Entity entity, Class<T> type) {
		if (entity != null) {
			EntityPatch<?> entitypatch = entity.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY).orElse(null);
			
			if (entitypatch != null && type.isAssignableFrom(entitypatch.getClass())) {
				return Optional.of((T)entitypatch);
			}
		}
		
		return Optional.empty();
		
	}
	
	@SuppressWarnings("unchecked")
	public static <E extends Entity, T extends EntityPatch<E>> Optional<T> getEntityPatchParameterized(Entity entity, Class<E> entitytype, Class<?> patchtype) {
		if (entity != null && entitytype.isAssignableFrom(entity.getClass())) {
			EntityPatch<?> entitypatch = entity.getCapability(EpicFightCapabilities.CAPABILITY_ENTITY).orElse(null);
			
			if (entitypatch != null && patchtype.isAssignableFrom(entitypatch.getClass())) {
				return Optional.of((T)entitypatch);
			}
		}
		
		return Optional.empty();
	}
}