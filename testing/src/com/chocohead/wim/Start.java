package com.chocohead.wim;

import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;

public class Start {
	public static void main(String[] args) throws InterruptedException {
		try {
			MixinEnvironment.getCurrentEnvironment().audit();
		} finally {
			if (MixinEnvironment.getCurrentEnvironment().getOption(Option.DEBUG_EXPORT_DECOMPILE_THREADED)) {
				Thread.sleep(5000); //Give time for the decompiler to finish
			}
		}

		System.exit(0);
	}
}