package yesman.epicfight.world.capabilities.entitypatch;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.main.EpicFightMod;

public enum Factions implements Faction {
	NEUTRAL(ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID, "textures/gui/healthbars1.png"), MathUtils.packColor(255, 255, 0, 100), 0),
	UNDEAD(ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID, "textures/gui/healthbars1.png"), MathUtils.packColor(255, 0, 0, 100), 1),
	BLAZE(ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID, "textures/gui/healthbars1.png"), MathUtils.packColor(183, 227, 255, 255), 2),
	ENDERMAN(ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID, "textures/gui/healthbars1.png"), MathUtils.packColor(255, 0, 0, 100), 3),
	ILLAGER(ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID, "textures/gui/healthbars1.png"), MathUtils.packColor(255, 0, 0, 100), 4),
	PIGLINS(ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID, "textures/gui/healthbars1.png"), MathUtils.packColor(255, 0, 0, 100), 5),
	WITHER(ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID, "textures/gui/healthbars2.png"), MathUtils.packColor(255, 0, 0, 100), 1),
	VILLAGER(ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID, "textures/gui/healthbars2.png"), MathUtils.packColor(255, 0, 0, 100), 0),
	ZOMBIFIED_PIGLIN(ResourceLocation.fromNamespaceAndPath(EpicFightMod.MODID, "textures/gui/healthbars2.png"), MathUtils.packColor(255, 0, 0, 100), 2)
	;
	
	final ResourceLocation healthBar;
	final int healthBarIndex;
	final int damageColor;
	final int id;
	
	Factions(ResourceLocation healthBar, int damageColor, int healthBarIndex) { 
		this.id = Faction.ENUM_MANAGER.assign(this);
		this.healthBar = healthBar;
		this.damageColor = damageColor;
		this.healthBarIndex = healthBarIndex;
	}
	
	@Override
	public int universalOrdinal() {
		return this.id;
	}
	
	@Override
	public ResourceLocation healthBarTexture() {
		return this.healthBar;
	}
	
	@Override
	public int damageColor() {
		return this.damageColor;
	}
	
	@Override
	public int healthBarIndex() {
		return this.healthBarIndex;
	}
}
