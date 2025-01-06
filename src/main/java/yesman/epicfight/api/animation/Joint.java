package yesman.epicfight.api.animation;

import java.util.List;

import com.google.common.collect.Lists;

import yesman.epicfight.api.utils.math.OpenMatrix4f;

public class Joint {
	public static final Joint EMPTY = new Joint("empty", -1, new OpenMatrix4f());
	
	private final List<Joint> subJoints = Lists.newArrayList();
	private final int jointId;
	private final String jointName;
	private final OpenMatrix4f initialLocalTransform;
	private final OpenMatrix4f localTransform = new OpenMatrix4f();
	private final OpenMatrix4f toOrigin = new OpenMatrix4f();
	
	public Joint(String name, int jointId, OpenMatrix4f initialLocalTransform) {
		this.jointId = jointId;
		this.jointName = name;
		this.initialLocalTransform = initialLocalTransform.unmodifiable();
		this.localTransform.load(initialLocalTransform);
	}

	public void addSubJoints(Joint... joints) {
		for (Joint joint : joints) {
			if (!this.subJoints.contains(joint)) {
				this.subJoints.add(joint);
			}
		}
	}
	
	public void removeSubJoints(Joint... joints) {
		for (Joint joint : joints) {
			this.subJoints.remove(joint);
		}
	}
	
	public List<Joint> getAllJoints() {
		List<Joint> list = Lists.newArrayList();
		this.getSubJoints(list);
		
		return list;
	}
	
	private void getSubJoints(List<Joint> list) {
		list.add(this);
		
		for (Joint joint : this.subJoints) {
			joint.getSubJoints(list);
		}
	}
	
	public void initOriginTransform(OpenMatrix4f parentTransform) {
		OpenMatrix4f modelTransform = OpenMatrix4f.mul(parentTransform, this.localTransform, null);
		OpenMatrix4f.invert(modelTransform, this.toOrigin);
		
		for (Joint joint : this.subJoints) {
			joint.initOriginTransform(modelTransform);
		}
	}
	
	public void revertLocalTransform() {
		this.localTransform.load(this.initialLocalTransform);
	}
	
	public OpenMatrix4f getInitialLocalTransform() {
		return this.initialLocalTransform;
	}
	
	public OpenMatrix4f getLocalTransform() {
		return this.localTransform;
	}
	
	public OpenMatrix4f getToOrigin() {
		return this.toOrigin;
	}
	
	public List<Joint> getSubJoints() {
		return this.subJoints;
	}
	
	public String getName() {
		return this.jointName;
	}
	
	public int getId() {
		return this.jointId;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Joint joint) {
			return this.jointName.equals(joint.jointName) && this.jointId == joint.jointId;
		} else {
			return super.equals(o);
		}
	}
	
	@Override
	public int hashCode() {
		return this.jointName.hashCode() + this.jointId;
	}
	
	public String searchPath(String path, String joint) {
		if (joint.equals(this.getName())) {
			return path;
		} else {
			int i = 1;
			for (Joint subJoint : this.subJoints) {
				String str = subJoint.searchPath(i + path, joint);
				i++;
				if (str != null) {
					return str;
				}
			}
			return null;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("\nid: " + this.jointId);
		sb.append("\nname: " + this.jointName);
		sb.append("\nlocal transform: " + this.localTransform);
		sb.append("\nto origin: " + this.toOrigin);
		sb.append("\nchildren: [");
		
		int idx = 0;
		
		for (Joint joint : this.subJoints) {
			idx++;
			sb.append(joint.jointName);
			
			if (idx != this.subJoints.size()) {
				sb.append(", ");
			}
		}
		
		sb.append("]\n");
		
		for (Joint joint : this.subJoints) {
			sb.append(joint.toString());
		}
		
		return sb.toString();
	}
}
