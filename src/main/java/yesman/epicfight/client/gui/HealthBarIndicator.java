package yesman.epicfight.client.gui;

import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Nullable;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.renderer.EpicFightRenderTypes;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.effect.VisibleMobEffect;

@OnlyIn(Dist.CLIENT)
public class HealthBarIndicator extends EntityIndicator {
	@Override
	public boolean shouldDraw(LivingEntity entity, @Nullable LivingEntityPatch<?> entitypatch, LocalPlayerPatch playerpatch) {
		HealthBarType healthBarType = ClientConfig.healthBarType;
		Minecraft mc = Minecraft.getInstance();
		
		if (healthBarType == HealthBarType.NONE) {
			return false;
		} else if (!entity.canChangeDimensions() || entity.isInvisible() || entity == playerpatch.getOriginal().getVehicle()) {
			return false;
		} else if (entity.distanceToSqr(mc.getCameraEntity()) >= 400) {
			return false;
		} else if (entity instanceof Player playerIn) {
			if (playerIn == playerpatch.getOriginal() && playerpatch.getMaxStunShield() <= 0.0F) {
				return false;
			} else if (playerIn.isCreative() || playerIn.isSpectator()) {
				return false;
			}
		}
		
		if (healthBarType == HealthBarType.TARGET) {
			return playerpatch.getTarget() == entity;
		}

		return (!entity.getActiveEffects().isEmpty() || !(entity.getHealth() >= entity.getMaxHealth())) && entity.deathTime < 19;
	}
	
	@Override
	public void drawIndicator(LivingEntity entity, @Nullable LivingEntityPatch<?> entitypatch, LocalPlayerPatch playerpatch, PoseStack poseStack, MultiBufferSource multiBufferSource, float partialTicks) {
		Matrix4f mvMatrix = super.getMVMatrix(poseStack, entity, 0.0F, entity.getBbHeight() + 0.25F, 0.0F, true, partialTicks);
		Collection<MobEffectInstance> activeEffects = entity.getActiveEffects(); 
		
		if (!activeEffects.isEmpty() && !entity.is(playerpatch.getOriginal())) {
			Iterator<MobEffectInstance> iter = activeEffects.iterator();
			int acives = activeEffects.size();
			int row = acives > 1 ? 1 : 0;
			int column = ((acives-1) / 2);
			float startX = -0.8F + -0.3F * row;
			float startY = -0.15F + 0.15F * column;
			
			for (int i = 0; i <= column; i++) {
				for (int j = 0; j <= row; j++) {
					MobEffectInstance effectInstance = iter.next();
					MobEffect effect = effectInstance.getEffect();
					ResourceLocation rl;
					
					if (effect instanceof VisibleMobEffect visibleMobEffect) {
						rl = visibleMobEffect.getIcon(effectInstance);
					} else {
						rl = new ResourceLocation(ForgeRegistries.MOB_EFFECTS.getKey(effect).getNamespace(), "textures/mob_effect/" + ForgeRegistries.MOB_EFFECTS.getKey(effect).getPath() + ".png");
					}
					
					Minecraft.getInstance().getTextureManager().bindForSetup(rl);
					float x = startX + 0.3F * j;
					float y = startY + -0.3F * i;
					
					VertexConsumer vertexBuilder1 = multiBufferSource.getBuffer(EpicFightRenderTypes.entityIndicator(rl));
					
					this.drawTexturedModalRect2DPlane(mvMatrix, vertexBuilder1, x, y, x + 0.3F, y + 0.3F, 0, 0, 256, 256, 0.003921F);
					
					if (!iter.hasNext()) {
						break;
					}
				}
			}
		}
		
		VertexConsumer vertexBuilder = multiBufferSource.getBuffer(EpicFightRenderTypes.entityIndicator(BATTLE_ICON));
		//VertexConsumer vertexBuilder = multiBufferSource.getBuffer(EpicFightRenderTypes.entityIndicator(new ResourceLocation(EpicFightMod.MODID, "textures/gui/custom_health_bars.png")));
		
		float size = 0.003921F;
		float ratio = Mth.clamp(entity.getHealth() / entity.getMaxHealth(), 0.0F, 1.0F);
		float healthRatio = -0.5F + ratio;
		
		int textureRatio = (int) (62 * ratio);
		this.drawTexturedModalRect2DPlane(mvMatrix, vertexBuilder, -0.5F, -0.05F, healthRatio, 0.05F, 1, 15, textureRatio, 20, size);
		this.drawTexturedModalRect2DPlane(mvMatrix, vertexBuilder, healthRatio, -0.05F, 0.5F, 0.05F, textureRatio, 10, 62, 15, size);
		
		float absorption = entity.getAbsorptionAmount();
		
		if (absorption > 0.0D) {
			float absorptionRatio = Mth.clamp(absorption / entity.getMaxHealth(), 0.0F, 1.0F);
			int absTexRatio = (int) (62 * absorptionRatio);
			this.drawTexturedModalRect2DPlane(mvMatrix, vertexBuilder, -0.5F, -0.05F, absorptionRatio - 0.5F, 0.05F, 1, 20, absTexRatio, 25, size);
		}
		
		if (entitypatch != null) {
			this.renderStunShield(entitypatch, mvMatrix, vertexBuilder);
		}
	}
	
	private void renderStunShield(LivingEntityPatch<?> entitypatch, Matrix4f mvMatrix, VertexConsumer vertexConsumer) {
		if (entitypatch.getStunShield() == 0) {
			return;
		}
		
		float size = 0.003921F;
		float ratio = Mth.clamp(entitypatch.getStunShield() / entitypatch.getMaxStunShield(), 0.0F, 1.0F);
		float barRatio = -0.5F + ratio;
		int textureRatio = (int) (62 * ratio);
		
		this.drawTexturedModalRect2DPlane(mvMatrix, vertexConsumer, -0.5F, -0.1F, barRatio, -0.05F, 1, 5, textureRatio, 10, size);
		this.drawTexturedModalRect2DPlane(mvMatrix, vertexConsumer, barRatio, -0.1F, 0.5F, -0.05F, textureRatio, 0, 63, 5, size);
	}
	
	public enum HealthBarType {
		NONE, HURT, TARGET;
		
		@Override
		public String toString() {
			return ParseUtil.toLowerCase(this.name());
		}
		
		public HealthBarType nextEnum() {
			return HealthBarType.values()[(this.ordinal() + 1) % 3];
		}
	}
}