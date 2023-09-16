package me.modmuss50.optifabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.modmuss50.optifabric.mod.OptifineResources;
import net.minecraft.resource.DefaultResourcePack;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.IOException;
import java.io.InputStream;

@Mixin(value = DefaultResourcePack.class, priority = 400)
abstract class DefaultResourcePackMixin {
	@Dynamic
	@WrapOperation(method = {"findInputStream", "getResourceOF"}, at = @At(value = "INVOKE", target = "Lnet/optifine/reflect/ReflectorForge;getOptiFineResourceStream(Ljava/lang/String;)Ljava/io/InputStream;"), require = 1, allow = 1)
	private InputStream doFindResource(String pathStr, Operation<InputStream> original) {
		try {
			InputStream stream = OptifineResources.INSTANCE.getResource(pathStr);
			if (stream != null) return stream;
		} catch (IOException e) {
			//Optifine does this if it goes wrong so we will too
			//It doesn't in later versions (should revisit this sometime in the future)
			e.printStackTrace();
		}
		return original.call(pathStr);
	}
}
