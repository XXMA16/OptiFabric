package com.chocohead.wim.minecraft;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.gson.annotations.SerializedName;

import com.chocohead.wim.minecraft.MinecraftVersion.Library.Rule.Action;

public class MinecraftVersion {
	public static class Library {
		public static class Rule {
			public enum Action {
				@SerializedName("allow")
				ALLOW,
				@SerializedName("disallow")
				DISALLOW;
			}

			public static class OS {
				public OperatingSystem name;
				public String version;
				public String arch;

				public boolean doesMatch() {
					if (name != null && name != OperatingSystem.ACTIVE) {
						return false;
					}

					if (version != null && !Pattern.matches(version, System.getProperty("os.version"))) {
						return false;
					}

					if (arch != null && !Pattern.matches(arch, System.getProperty("os.arch"))) {
						return false;
					}

					return true;
				}
			}

			public Action action = Action.ALLOW;
			public OS os;

			public boolean doesRuleApply() {
				return os == null || os.doesMatch();
			}
		}

		public static class Downloads {
			public Download artifact;
			public Map<String, Download> classifiers = Collections.emptyMap();

			public Download getDownload() {
				return getDownload(null);
			}

			public boolean isSpecial() {
				return !classifiers.isEmpty();
			}

			public Download getDownload(String classifier) {
				return classifier == null ? artifact : classifiers.get(classifier);
			}
		}

		public String name;
		public Rule[] rules;
		public Map<OperatingSystem, String> natives;
		public Downloads downloads;

		public boolean shouldUse() {
			if (rules == null || rules.length == 0) return true;

			for (int i = rules.length - 1; i >= 0; i--) {
				if (rules[i].doesRuleApply()) {
					return rules[i].action == Action.ALLOW;
				}
			}

			return false;
		}

		public boolean hasNative() {
			return natives != null && natives.containsKey(OperatingSystem.ACTIVE);
		}

		public String getNative() {
			return natives.get(OperatingSystem.ACTIVE);
		}
	}

	public static class Download {
		public String url;
		@SerializedName("sha1")
		public String hash;
		public int size;
	}

	public String id;
	public List<Library> libraries;
	public Map<String, Download> downloads;
}