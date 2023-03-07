package me.modmuss50.optifabric.patcher.fixes;

import com.google.common.collect.MoreCollectors;
import me.modmuss50.optifabric.util.RemappingUtils;
import org.apache.commons.lang3.Validate;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Arrays;

public class HeldItemRendererFix implements ClassFixer {
    /*
    Optifine changes multiple ItemStack#isOf calls to instanceof (and some other stuff)
    which don't affect vanilla behaviour, but mess up a redirect of the
    aforementioned method. As far as I checked, the only behavioural change
    Optifine does is introducing a Shader check at the start of the method.
     */
    private static final String stitchName = RemappingUtils.getMethodName("class_759", "method_3228", "(Lnet/minecraft/class_742;FFLnet/minecraft/class_1268;FLnet/minecraft/class_1799;FLnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V");

    @Override
    public void fix(ClassNode optifine, ClassNode minecraft) {

        //Remove the old method
        optifine.methods.removeIf(methodNode -> methodNode.name.equals(stitchName));

        //Find the vanilla method
        MethodNode methodNode = minecraft.methods.stream().filter(node -> node.name.equals(stitchName)).collect(MoreCollectors.onlyElement());
        Validate.notNull(methodNode, "old method null");

        //Add the check optifine does to the original method
        LabelNode label = (LabelNode) Arrays.stream(methodNode.instructions.toArray()).filter(insn -> insn instanceof LabelNode).findFirst().orElseThrow(() -> new IllegalStateException("No LabelNode?"));
        InsnList optifineCheck = new InsnList();
        optifineCheck.add(new LabelNode());
        optifineCheck.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/optifine/Config", "isShaders", "()Z"));
        optifineCheck.add(new JumpInsnNode(Opcodes.IFEQ, label));
        optifineCheck.add(new VarInsnNode(Opcodes.ALOAD, 4));
        String desc = "(L" + RemappingUtils.getClassName("class_1268") + ";)Z";
        optifineCheck.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/optifine/shaders/Shaders", "isSkipRenderHand", desc));
        optifineCheck.add(new JumpInsnNode(Opcodes.IFEQ, label));
        optifineCheck.add(new InsnNode(Opcodes.RETURN));
        methodNode.instructions.insert(optifineCheck);

        optifine.methods.add(methodNode);
    }
}