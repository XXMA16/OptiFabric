package me.modmuss50.optifabric.mod;

import com.google.common.base.MoreObjects;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import net.fabricmc.loader.util.version.SemanticVersionImpl;
import net.fabricmc.loader.util.version.SemanticVersionPredicateParser;

import java.util.function.BiFunction;
import java.util.function.Predicate;

//Might need this class for other stuff too in the future
public class Loader {
    private static final boolean quilt;
    private static final BiFunction<String, ModMetadata, Boolean> versionCompareFunction;

    static {
        quilt = FabricLoader.getInstance().isModLoaded("quilt_loader");
        if (quilt) {
            versionCompareFunction = (versionRange, mod) -> {
                try {
                    return VersionPredicate.parse(versionRange).test(mod.getVersion());
                } catch (VersionParsingException e) {
                    System.err.println("Error comparing the version for ".concat(MoreObjects.firstNonNull(mod.getName(), mod.getId())));
                    e.printStackTrace();
                    return false; //Let's just gamble on the version not being valid also not being a problem
                }
            };
        } else {
            versionCompareFunction = (versionRange, mod) -> {
                try {
                    Predicate<SemanticVersionImpl> predicate = SemanticVersionPredicateParser.create(versionRange);
                    SemanticVersionImpl version = new SemanticVersionImpl(mod.getVersion().getFriendlyString(), false);
                    return predicate.test(version);
                } catch (@SuppressWarnings("deprecation") net.fabricmc.loader.util.version.VersionParsingException e) {
                    System.err.println("Error comparing the version for ".concat(MoreObjects.firstNonNull(mod.getName(), mod.getId())));
                    e.printStackTrace();
                    return false; //Let's just gamble on the version not being valid also not being a problem
                }
            };
        }
    }

    public static boolean isQuilt() {
        return quilt;
    }

    public static boolean compareVersions(String versionRange, ModMetadata mod) {
        return versionCompareFunction.apply(versionRange, mod);
    }
}

