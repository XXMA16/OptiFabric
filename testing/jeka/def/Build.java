import java.util.jar.Attributes.Name;

import dev.jeka.core.api.depmanagement.JkQualifiedDependency;
import dev.jeka.core.api.depmanagement.JkQualifiedDependencySet;
import dev.jeka.core.api.depmanagement.JkRepo;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.api.project.JkIdeSupport.JkSupplier;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkCompileLayout.Concern;
import dev.jeka.core.tool.JkBean;

class Build extends JkBean implements JkSupplier {
	final JkProject runPreparer = JkProject.of().simpleFacade()
            .setJvmTargetVersion(JkJavaVersion.V8)
            .configureCompileDeps(deps -> deps
                    .and("org.jsoup:jsoup:1.15.4")
                    .and("com.google.code.gson:gson:2.9.0")
                    .and("org.tinylog:tinylog-api:2.4.1")
                    .and("org.tinylog:tinylog-impl:2.4.1")
                    .andFiles(JkLocator.getJekaJarPath())
            ).applyOnProject(project -> project.getConstruction().apply(construction -> {
            	construction.getCompilation().getLayout().setSources("prepare").mixResourcesAndSources();
            	construction.getTesting().getCompilation().getLayout().emptySources().emptyResources();
            	construction.getManifest().addMainAttribute(Name.MAIN_CLASS, "com.chocohead.wim.Main");
            	project.getArtifactProducer().putMainArtifact(construction::createFatJar);
            	project.includeJavadocAndSources(false, true);
            	project.getPublication().setModuleId("will-it-mix");
            })).skipTests(true).getProject();
	final JkProject runner = JkProject.of().simpleFacade()
            .setJvmTargetVersion(JkJavaVersion.V8)
            .configureCompileDeps(deps -> deps
                    .and("net.fabricmc:sponge-mixin:0.12.4+mixin.0.8.5")
                    .and("net.fabricmc:fabric-loader:0.14.17")
                    //Loader dependencies... which we can get away with skipping for the purposes of compiling
                    /*.and("org.ow2.asm:asm-tree:9.4")
                    .and("org.ow2.asm:asm-commons:9.4")
                    .and("org.ow2.asm:asm-util:9.4")
                    .and("net.fabricmc:tiny-mappings-parser:0.3.0+build.17")
            		.and("net.fabricmc:tiny-remapper:0.8.2")
    				.and("net.fabricmc:access-widener:2.1.0")*/
                    .and("org.apache.commons:commons-lang3:3.12.0") //Already a Minecraft dependency so doesn't hurt
            ).applyOnProject(project -> project.getConstruction().apply(construction -> {
            	construction.getDependencyResolver().addRepos(JkRepo.of("https://maven.fabricmc.net"));
            	construction.getCompilation().getLayout().setSourceSimpleStyle(Concern.PROD);
            	construction.getTesting().getCompilation().getLayout().emptySources().emptyResources();
            	project.includeJavadocAndSources(false, true);
            	project.getPublication().setModuleId("will-it-mix-provider");
            })).skipTests(true).getProject();

    public void pack() {
        clean();
        runPreparer.pack();
        JkPathTree.of(getOutputDir()).andMatching(false, "*.jar").deleteContent();
        runner.pack();
    }

	@Override
	public JkIdeSupport getJavaIdeSupport() {
		JkIdeSupport out = runPreparer.getJavaIdeSupport();
		JkIdeSupport runner = this.runner.getJavaIdeSupport();
		if (runner.getSourceVersion().compareTo(out.getSourceVersion()) > 0) {
			out.setSourceVersion(runner.getSourceVersion());
		}
		JkCompileLayout<?> outLayout = out.getProdLayout();
		JkCompileLayout<?> runnerLayout = runner.getProdLayout();
		out.setProdLayout(outLayout.addSource(runnerLayout.getSources()).setResources(outLayout.getResources().and(runnerLayout.getResources())));
        //Could do test layout too... but they're both empty
		JkQualifiedDependencySet dependencies = out.getDependencies();
		for (JkQualifiedDependency dependency : runner.getDependencies().getEntries()) {
			dependencies = dependencies.and(dependency);
		}
		out.setDependencies(dependencies);
        out.setDependencyResolver(out.getDependencyResolver().addRepos(runner.getDependencyResolver().getRepos()));
		return out;
	}
}