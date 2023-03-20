package com.chocohead.wim;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.FieldUtils;

import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.throwables.ClassAlreadyLoadedException;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.MixinService;

public class Start {
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws InterruptedException {
		List<Set<String>> configs = new ArrayList<>();
		try {
			Object transformer = MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
			if (transformer == null) throw new IllegalStateException("Not running with a transformer?");

			Object processor = FieldUtils.readDeclaredField(transformer, "processor", true);
			if (processor == null) throw new IllegalStateException("Unable to find processor in " + transformer);

			List<?> mixinConfigs = (List<?>) FieldUtils.readDeclaredField(processor, "configs", true);
			if (mixinConfigs == null) throw new IllegalStateException("Unable to get configs from processor");

			Field unhandledTargets = null;
			for (Object config : mixinConfigs) {
				if (unhandledTargets == null) {
					unhandledTargets = FieldUtils.getDeclaredField(config.getClass(), "unhandledTargets", true);
				}
				configs.add((Set<String>) FieldUtils.readField(unhandledTargets, config));
			}
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to get list of mixins to apply", e);
		}

		try {
			Set<String> unhandled = configs.stream().flatMap(Set::stream).collect(Collectors.toSet());
			IClassProvider provider = MixinService.getService().getClassProvider();

			for (String target : unhandled) {
				try {
					provider.findClass(target, false);
				} catch (ClassNotFoundException e) {
					System.err.println("Could not load " + target);
				}
			}

			if (!configs.stream().allMatch(Set::isEmpty)) {
				throw new ClassAlreadyLoadedException("Could not test " + configs.stream().flatMap(Set::stream).collect(Collectors.joining(", ")));
			}
		} finally {
			if (MixinEnvironment.getCurrentEnvironment().getOption(Option.DEBUG_EXPORT_DECOMPILE_THREADED)) {
				Thread.sleep(5000); //Give time for the decompiler to finish
			}
		}

		System.exit(0);
	}
}