package me.modmuss50.optifabric.patcher.fixes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.MoreCollectors;

import org.apache.commons.lang3.Validate;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import me.modmuss50.optifabric.util.RemappingUtils;

import java.util.List;

public class KeyboardFix implements ClassFixer {
	//net/minecraft/client/Keyboard.onKey(JIIII)V
	private final String onKeyName = RemappingUtils.getMethodName("class_309", "method_1466", "(JIIII)V");
	private final String screenClass = RemappingUtils.getClassName("class_437");
	private final List<String> onKeyLambdasNames = ImmutableList.of(
			RemappingUtils.getMethodName("class_309", "method_1458", "(L" + screenClass + ";II)V"),
			RemappingUtils.getMethodName("class_309", "method_1473", "(L" + screenClass + ";CI)V"));

	@Override
	public void fix(ClassNode optifine, ClassNode minecraft) {
		Validate.notNull(onKeyName, "onKeyName null");

		//Needed for JEI, might as well remove the useless forge checks optifine does
		optifine.methods.removeIf(methodNode -> onKeyLambdasNames.contains(methodNode.name));
		minecraft.methods.stream()
				.filter(methodNode -> onKeyLambdasNames.contains(methodNode.name))
				.forEach(methodNode -> optifine.methods.add(methodNode));

		//Remove the old "broken" method
		optifine.methods.removeIf(methodNode -> methodNode.name.equals(onKeyName));

		//Find the vanilla method
		MethodNode methodNode = minecraft.methods.stream().filter(node -> node.name.equals(onKeyName)).collect(MoreCollectors.onlyElement());
		Validate.notNull(methodNode, "old method null");
		
		//Find the lambda inside the vanilla method (not matched as Optifine changes its descriptor)
		MethodNode lambdaNode = minecraft.methods.stream().filter(node -> "method_1454".equals(node.name)).collect(MoreCollectors.onlyElement());
		Validate.notNull(lambdaNode, "old method lambda null");

		//Add the vanilla methods back in
		optifine.methods.add(methodNode);
		optifine.methods.add(lambdaNode);

		//lambda$chatTyped(Screen,int,int)void
		String targetDescC = RemappingUtils.mapMethodDescriptor("(Lnet/minecraft/class_437;CI)V"); // method_1473
		String targetDescI = RemappingUtils.mapMethodDescriptor("(Lnet/minecraft/class_437;II)V"); // method_1458
		for (MethodNode method : optifine.methods) {
			if ((method.access | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC) == method.access && (method.desc.equals(targetDescC) || method.desc.equals(targetDescI))) { //Screen, int, int
				method.desc = method.desc.replace("L" + RemappingUtils.getClassName("class_437") + ";", "L" + RemappingUtils.getClassName("class_364") + ";");//Screen->Element
				for (AbstractInsnNode ain : method.instructions.toArray()) {
					if (ain.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) ain).var == 0) {
						method.instructions.insert(ain, new TypeInsnNode(Opcodes.CHECKCAST, RemappingUtils.getClassName("class_437")));
					}
				}
			} else {
				for (AbstractInsnNode ain : method.instructions.toArray()) {
					if (ain.getOpcode() == Opcodes.INVOKEDYNAMIC) {
						InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) ain;
						if (idin.bsmArgs.length == 3 && idin.bsmArgs[1] instanceof Handle) {
							Handle handle = (Handle) idin.bsmArgs[1];
							if (handle.getTag() == Opcodes.H_INVOKESTATIC && handle.getOwner().equals(RemappingUtils.getClassName("class_309")) && (handle.getDesc().equals(targetDescC) || handle.getDesc().equals(targetDescI))) {
								idin.desc = idin.desc.replace("L" + RemappingUtils.getClassName("class_437") + ";", "L" + RemappingUtils.getClassName("class_364") + ";");
								idin.bsmArgs[1] = new Handle(handle.getTag(), handle.getOwner(), handle.getName(), handle.getDesc().replace("L" + RemappingUtils.getClassName("class_437") + ";", "L" + RemappingUtils.getClassName("class_364") + ";"), handle.isInterface());
							}
						}
					}
				}
			}
		}
	}
}