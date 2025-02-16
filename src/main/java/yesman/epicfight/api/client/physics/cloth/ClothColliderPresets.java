package yesman.epicfight.api.client.physics.cloth;

import java.util.List;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

@OnlyIn(Dist.CLIENT)
public class ClothColliderPresets {
	public static final List<Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider>> BIPED_SLIM = ImmutableList.<Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider>>builder()
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[1], new ClothSimulator.ClothOBBCollider(0.125D, 0.24D, 0.125D, 0.0D, 0.22D, 0.0D, 0, 0, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[2], new ClothSimulator.ClothOBBCollider(0.125D, 0.1875D, 0.125D, 0.0D, 0.1875D, 0.0D, 0, 1, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[4], new ClothSimulator.ClothOBBCollider(0.125D, 0.24D, 0.125D, 0.0D, 0.22D, 0.0D, 0, 0, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[5], new ClothSimulator.ClothOBBCollider(0.125D, 0.1875D, 0.125D, 0.0D, 0.1875D, 0.0D, 0, 1, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[7], new ClothSimulator.ClothOBBCollider(0.25D, 0.1875D, 0.13D, 0.0D, 0.14D, 0.0D, 0, 0, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[8], new ClothSimulator.ClothOBBCollider(0.25D, 0.1875D, 0.13D, 0.0D, 0.1875D, 0.0D, 0, 0, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[9], new ClothSimulator.ClothOBBCollider(0.25D, 0.25D, 0.25D, 0.0D, 0.25D, 0.0D, 0, 0, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[11], new ClothSimulator.ClothOBBCollider(0.12D, 0.24D, 0.125D, -0.05D, 0.14D, 0.0D, 1, 0, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[12], new ClothSimulator.ClothOBBCollider(0.12D, 0.1875D, 0.125D, -0.05D, 0.14D, 0.0D, 1, 0, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[16], new ClothSimulator.ClothOBBCollider(0.12D, 0.24D, 0.125D, 0.05D, 0.14D, 0.0D, 1, 0, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[17], new ClothSimulator.ClothOBBCollider(0.12D, 0.1875D, 0.125D, 0.05D, 0.14D, 0.0D, 1, 0, 1, 0, 0, 0)))
			.build();
	
	public static final List<Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider>> BIPED = ImmutableList.<Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider>>builder()
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[1], new ClothSimulator.ClothOBBCollider(0.125D, 0.24D, 0.125D, 0.0D, 0.22D, 0.0D, 0, 0, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[2], new ClothSimulator.ClothOBBCollider(0.125D, 0.1875D, 0.125D, 0.0D, 0.1875D, 0.0D, 0, 1, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[4], new ClothSimulator.ClothOBBCollider(0.125D, 0.24D, 0.125D, 0.0D, 0.22D, 0.0D, 0, 0, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[5], new ClothSimulator.ClothOBBCollider(0.125D, 0.1875D, 0.125D, 0.0D, 0.1875D, 0.0D, 0, 1, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[7], new ClothSimulator.ClothOBBCollider(0.25D, 0.1875D, 0.13D, 0.0D, 0.14D, 0.0D, 0, 0, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[8], new ClothSimulator.ClothOBBCollider(0.25D, 0.1875D, 0.13D, 0.0D, 0.1875D, 0.0D, 0, 0, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[9], new ClothSimulator.ClothOBBCollider(0.25D, 0.25D, 0.25D, 0.0D, 0.25D, 0.0D, 0, 0, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[11], new ClothSimulator.ClothOBBCollider(0.13D, 0.24D, 0.13D, -0.0D, 0.14D, 0.0D, 1, 0, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[12], new ClothSimulator.ClothOBBCollider(0.13D, 0.1875D, 0.13D, -0.0D, 0.14D, 0.0D, 1, 0, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[16], new ClothSimulator.ClothOBBCollider(0.13D, 0.24D, 0.13D, 0.0D, 0.14D, 0.0D, 1, 0, 1, 0, 0, 0)))
			.add(Pair.of((simObject) -> simObject.getArmature().getPoseMatrices()[17], new ClothSimulator.ClothOBBCollider(0.13D, 0.1875D, 0.13D, 0.0D, 0.14D, 0.0D, 1, 0, 1, 0, 0, 0)))
			.build();
}
