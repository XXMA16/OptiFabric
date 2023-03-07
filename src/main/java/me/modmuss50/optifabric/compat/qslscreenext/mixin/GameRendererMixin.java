package me.modmuss50.optifabric.compat.qslscreenext.mixin;

import me.modmuss50.optifabric.compat.InterceptingMixin;
import me.modmuss50.optifabric.compat.PlacatingSurrogate;
import me.modmuss50.optifabric.compat.Shim;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GameRenderer.class)
@InterceptingMixin("org/quiltmc/qsl/screen/mixin/client/GameRendererMixin")
abstract class GameRendererMixin {
	@Shim
	private native void onBeforeRenderScreen(float tickDelta, long startTime, boolean tick, CallbackInfo call, int mouseX, int mouseY, Window window, Matrix4f projection, MatrixStack modelView, MatrixStack matrices);

	@PlacatingSurrogate
	private void onBeforeRenderScreen(float tickDelta, long startTime, boolean tick, CallbackInfo call, int mouseX, int mouseY, Window window, float guiFarPlane, Matrix4f projection, MatrixStack matrices) {
	}

	@Inject(method = "render(FJZ)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/util/math/MatrixStack;IIF)V", ordinal = 0),
			locals = LocalCapture.CAPTURE_FAILHARD,
			cancellable = true)
	private void onBeforeRenderScreen(float tickDelta, long startTime, boolean tick, CallbackInfo call, int mouseX, int mouseY, Window window, float guiFarPlane, Matrix4f projection, MatrixStack modelView, MatrixStack matrices) {
		onBeforeRenderScreen(tickDelta, startTime, tick, call, mouseX, mouseY, window, projection, modelView, matrices);
	}

	@Shim
	private native void onAfterRenderScreen(float tickDelta, long startTime, boolean tick, CallbackInfo call, int mouseX, int mouseY, Window window, Matrix4f projection, MatrixStack modelView, MatrixStack matrices);

	@PlacatingSurrogate
	private void onAfterRenderScreen(float tickDelta, long startTime, boolean tick, CallbackInfo call, int mouseX, int mouseY, Window window, float guiFarPlane, Matrix4f projection, MatrixStack matrices) {
	}

	@Inject(method = "render(FJZ)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/util/math/MatrixStack;IIF)V", shift = Shift.AFTER, ordinal = 0),
			locals = LocalCapture.CAPTURE_FAILHARD)
	private void onAfterRenderScreen(float tickDelta, long startTime, boolean tick, CallbackInfo call, int mouseX, int mouseY, Window window, float guiFarPlane, Matrix4f projection, MatrixStack modelView, MatrixStack matrices) {
		onAfterRenderScreen(tickDelta, startTime, tick, call, mouseX, mouseY, window, projection, modelView, matrices);
	}
}