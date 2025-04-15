package yesman.epicfight.api.utils;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.utils.math.Vec3f;

public interface PacketBufferCodec<T> {
	void encode(T obj, FriendlyByteBuf buffer);
	
	T decode(FriendlyByteBuf buffer);
	
	public static final PacketBufferCodec<Boolean> BOOLEAN = new PacketBufferCodec<> () {
		@Override
		public void encode(Boolean obj, FriendlyByteBuf buffer) {
			buffer.writeBoolean(obj);
		}
		
		@Override
		public Boolean decode(FriendlyByteBuf buffer) {
			return buffer.readBoolean();
		}
	};
	
	public static final PacketBufferCodec<Integer> INTEGER = new PacketBufferCodec<> () {
		@Override
		public void encode(Integer obj, FriendlyByteBuf buffer) {
			buffer.writeInt(obj);
		}
		
		@Override
		public Integer decode(FriendlyByteBuf buffer) {
			return buffer.readInt();
		}
	};
	
	public static final PacketBufferCodec<Float> FLOAT = new PacketBufferCodec<> () {
		@Override
		public void encode(Float obj, FriendlyByteBuf buffer) {
			buffer.writeFloat(obj);
		}
		
		@Override
		public Float decode(FriendlyByteBuf buffer) {
			return buffer.readFloat();
		}
	};
	
	public static final PacketBufferCodec<Double> DOUBLE = new PacketBufferCodec<> () {
		@Override
		public void encode(Double obj, FriendlyByteBuf buffer) {
			buffer.writeDouble(obj);
		}
		
		@Override
		public Double decode(FriendlyByteBuf buffer) {
			return buffer.readDouble();
		}
	};
	
	public static final PacketBufferCodec<Vec3> VEC3 = new PacketBufferCodec<> () {
		@Override
		public void encode(Vec3 obj, FriendlyByteBuf buffer) {
			buffer.writeDouble(obj.x);
			buffer.writeDouble(obj.y);
			buffer.writeDouble(obj.z);
		}
		
		@Override
		public Vec3 decode(FriendlyByteBuf buffer) {
			return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
		}
	};
	
	public static final PacketBufferCodec<Vec3f> VEC3F = new PacketBufferCodec<> () {
		@Override
		public void encode(Vec3f obj, FriendlyByteBuf buffer) {
			buffer.writeFloat(obj.x);
			buffer.writeFloat(obj.y);
			buffer.writeFloat(obj.z);
		}
		
		@Override
		public Vec3f decode(FriendlyByteBuf buffer) {
			return new Vec3f(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
		}
	};
}
