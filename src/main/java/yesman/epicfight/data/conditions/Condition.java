package yesman.epicfight.data.conditions;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.utils.ExtendableEnum;
import yesman.epicfight.api.utils.ExtendableEnumManager;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public interface Condition<T> {
	default Condition<T> read(JsonElement json) throws CommandSyntaxException {
		return this.read(TagParser.parseTag(json.toString()));
	}
	
	public Condition<T> read(CompoundTag tag) throws IllegalArgumentException;
	public CompoundTag serializePredicate();
	public boolean predicate(T target);
	
	default <O> O assertTag(String key, String tagFormatMessage, CompoundTag tag, int tagType, BiFunction<CompoundTag, String, O> getter) throws IllegalArgumentException {
		if (!tag.contains(key)) {
			throw new IllegalArgumentException(MessageFormat.format("{0} condition error: {1} not specified!", this.getClass().getSimpleName(), key));
		}
		
		if (!tag.contains(key, tagType)) {
			throw new IllegalArgumentException(MessageFormat.format("{0} condition error: the {1} value must be a {2} format", this.getClass().getSimpleName(), key, tagFormatMessage));
		}
		
		return getter.apply(tag, key);
	}
	
	default <E extends Enum<E>> E assertEnumTag(String key, Class<E> enumCls, CompoundTag tag) throws IllegalArgumentException {
		if (!tag.contains(key)) {
			throw new IllegalArgumentException(MessageFormat.format("{0} condition error: {1} not specified!", this.getClass().getSimpleName(), key));
		}
		
		String enumString = this.assertTag(key, "string", tag, Tag.TAG_STRING, CompoundTag::getString).toUpperCase(Locale.ROOT);
		
		try {
			return Enum.valueOf(enumCls, enumString);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(MessageFormat.format("{0} condition error: invalid enum for {1}: {2}", this.getClass().getSimpleName(), key, enumString));
		}
	}
	
	default <E extends ExtendableEnum> E assertExtendableEnumTag(String key, ExtendableEnumManager<E> extendableEnumManager, CompoundTag tag) throws IllegalArgumentException, NoSuchElementException {
		if (!tag.contains(key)) {
			throw new IllegalArgumentException(MessageFormat.format("{0} condition error: {1} not specified!", this.getClass().getSimpleName(), key));
		}
		
		String enumString = this.assertTag(key, "string", tag, Tag.TAG_STRING, CompoundTag::getString).toLowerCase(Locale.ROOT);
		
		try {
			return extendableEnumManager.getOrThrow(enumString);
		} catch (NoSuchElementException ex) {
			throw new NoSuchElementException(MessageFormat.format("{0} condition error: {1}", this.getClass().getSimpleName(), ex.getMessage()));
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public List<ParameterEditor> getAcceptingParameters(Screen screen);
	
	public static abstract class EntityPatchCondition implements Condition<LivingEntityPatch<?>> {
	}
	
	public static abstract class EntityCondition implements Condition<Entity> {
	}
	
	public static abstract class ItemStackCondition implements Condition<ItemStack> {
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class ParameterEditor {
		public static ParameterEditor of(Function<Object, Tag> toTag, Function<Tag, Object> fromTag, AbstractWidget editWidget) {
			return new ParameterEditor(toTag, fromTag, editWidget);
		}
		
		public final Function<Object, Tag> toTag;
		public final Function<Tag, Object> fromTag;
		public final AbstractWidget editWidget;
		
		private ParameterEditor(Function<Object, Tag> toTag, Function<Tag, Object> fromTag, AbstractWidget editWidget) {
			this.toTag = toTag;
			this.fromTag = fromTag;
			this.editWidget = editWidget;
		}
	}
}