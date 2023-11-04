import com.fizzed.blaze.Config;
import com.fizzed.blaze.Contexts;
import com.fizzed.blaze.Task;
import com.fizzed.buildx.Buildx;
import com.fizzed.buildx.ContainerBuilder;
import com.fizzed.buildx.Target;
import com.fizzed.jne.*;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.fizzed.blaze.Contexts.withBaseDir;
import static com.fizzed.blaze.Systems.*;
import static com.fizzed.blaze.util.Globber.globber;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;

public class blaze {

    private final Logger log = Contexts.logger();
    private final Config config = Contexts.config();
    private final Path projectDir = withBaseDir("../").toAbsolutePath();
    private final NativeTarget localNativeTarget = NativeTarget.detect();
    private final Path nativeDir = projectDir.resolve("native");
    private final Path targetDir = projectDir.resolve("target");

    @Task(order = 1)
    public void build_natives() throws Exception {
        final String targetStr = Contexts.config().value("target").orNull();
        final NativeTarget nativeTarget = targetStr != null ? NativeTarget.fromJneTarget(targetStr) : NativeTarget.detect();

        log.info("Building natives for target {}", nativeTarget.toJneTarget());

        log.info("Copying native code to (cleaned) {} directory...", targetDir);
        rm(targetDir).recursive().force().run();
        mkdir(targetDir).parents().run();
        cp(globber(nativeDir, "*")).target(targetDir).recursive().debug().run();

        final String buildScript;
        final String autoConfTarget = nativeTarget.toAutoConfTarget();
        if (nativeTarget.getOperatingSystem() == OperatingSystem.MACOS) {
            buildScript = "setup/build-native-lib-macos-action.sh";
        } else {
            buildScript = "setup/build-native-lib-linux-action.sh";
        }

        exec(buildScript, nativeTarget.toJneOsAbi(), nativeTarget.toJneArch(), autoConfTarget)
            .workingDir(this.projectDir)
            .verbose()
            .run();
    }

    @Task(order = 2)
    public void test() throws Exception {
        final Integer jdkVersion = this.config.value("jdk.version", Integer.class).orNull();
        final HardwareArchitecture jdkArch = ofNullable(this.config.value("jdk.arch").orNull())
            .map(HardwareArchitecture::resolve)
            .orElse(null);

        final long start = System.currentTimeMillis();
        final JavaHome jdkHome = new JavaHomeFinder()
            .jdk()
            .version(jdkVersion)
            .hardwareArchitecture(jdkArch)
            .preferredDistributions()
            .sorted(jdkVersion != null || jdkArch != null)  // sort if any criteria provided
            .find();

        log.info("");
        log.info("Detected {} (in {} ms)", jdkHome, (System.currentTimeMillis()-start));
        log.info("");

        exec("mvn", "clean", "test")
            .workingDir(this.projectDir)
            .env("JAVA_HOME", jdkHome.getDirectory().toString())
            .verbose()
            .run();
    }

    @Task(order = 3)
    public void clean() throws Exception {
        rm(this.targetDir)
            .recursive()
            .force()
            .verbose()
            .run();
    }

    private final List<Target> crossTargets = asList(

        //
        // Linux
        //

        new Target("linux", "x64", "x64 Ubuntu 16.04, JDK 11 cross compiler")
            .setTags("build")
            .setContainerImage("fizzed/buildx:x64-ubuntu16-jdk11-buildx-linux-x64"),

        new Target("linux", "arm64", "x64 Ubuntu 16.04, JDK 11 cross compiler")
            .setTags("build")
            .setContainerImage("fizzed/buildx:x64-ubuntu16-jdk11-buildx-linux-arm64"),

        new Target("linux", "armhf", "x64 Ubuntu 16.04, JDK 11 cross compiler")
            .setTags("build")
            .setContainerImage("fizzed/buildx:x64-ubuntu16-jdk11-buildx-linux-armhf"),

        new Target("linux", "armel", "x64 Ubuntu 16.04, JDK 11 cross compiler")
            .setTags("build")
            .setContainerImage("fizzed/buildx:x64-ubuntu16-jdk11-buildx-linux-armel"),

        new Target("linux", "riscv64", "x64 Ubuntu 18.04, JDK 11 cross compiler")
            .setTags("build")
            .setContainerImage("fizzed/buildx:x64-ubuntu18-jdk11-buildx-linux-riscv64"),

        //
        // Linux (w/ MUSL)
        //

        new Target("linux_musl", "x64", "x64 Ubuntu 16.04, JDK 11 cross compiler")
            .setTags("build")
            .setContainerImage("fizzed/buildx:x64-ubuntu16-jdk11-buildx-linux_musl-x64"),

        new Target("linux_musl", "arm64", "x64 Ubuntu 16.04, JDK 11 cross compiler")
            .setTags("build")
            .setContainerImage("fizzed/buildx:x64-ubuntu16-jdk11-buildx-linux_musl-arm64"),

        //
        // FreeBSD (will not easily compile on freebsd)
        //

        /*new Target("freebsd", "x64")
            .setTags("build", "test")
            .setHost("bmh-build-x64-freebsd12-1"),

        *new Target("freebsd", "arm64")
            .setTags("build", "test")
            .setHost("bmh-build-arm64-freebsd13-1"),*/

        //
        // OpenBSD (will not easily compile on openbsd)
        //

        /*new Target("openbsd", "x64")
            .setTags("build", "test")
            .setHost("bmh-build-x64-openbsd72-1"),

        new Target("openbsd", "arm64")
            .setTags("build", "test")
            .setHost("bmh-build-arm64-openbsd72-1"),*/


        //
        // MacOS
        //

        new Target("macos", "x64", "MacOS 10.13")
            .setTags("build", "test")
            .setHost("bmh-build-x64-macos1013-1"),

        new Target("macos", "arm64", "MacOS 12")
            .setTags("build", "test")
            .setHost("bmh-build-arm64-macos12-1"),

        //
        // Windows
        //

        /*new Target("windows", "x64", "Windows 11")
            .setTags("build", "test")
            .setHost("bmh-build-x64-win11-1"),

        new Target("windows", "arm64", "Windows 11")
            .setTags("build")
            .setHost("bmh-build-x64-win11-1"),*/

        //
        // CI/Test Local Machine
        //

        new Target(localNativeTarget.toJneOsAbi(), localNativeTarget.toJneArch(), "local machine")
            .setTags("test"),

        //
        // CI/Test Linux
        //

        new Target("linux", "x64", "Ubuntu 16.04, JDK 11")
            .setTags("test")
            .setContainerImage("fizzed/buildx:x64-ubuntu16-jdk11"),

        new Target("linux", "x64", "Ubuntu 22.04, JDK 8")
            .setTags("test")
            .setContainerImage("fizzed/buildx:x64-ubuntu22-jdk8"),

        new Target("linux", "x64", "Ubuntu 22.04, JDK 11")
            .setTags("test")
            .setContainerImage("fizzed/buildx:x64-ubuntu22-jdk11"),

        new Target("linux", "x64", "Ubuntu 22.04, JDK 17")
            .setTags("test")
            .setContainerImage("fizzed/buildx:x64-ubuntu22-jdk17"),

        new Target("linux", "x64", "Ubuntu 22.04, JDK 21")
            .setTags("test")
            .setContainerImage("fizzed/buildx:x64-ubuntu22-jdk21"),

        new Target("linux", "arm64", "Ubuntu 16.04, JDK 11")
            .setTags("test")
            .setHost("bmh-hv-6")
            .setContainerImage("fizzed/buildx:arm64-ubuntu16-jdk11"),

        new Target("linux", "armhf", "Ubuntu 16.04, JDK 11")
            .setTags("test")
            .setHost("bmh-hv-6")
            .setContainerImage("fizzed/buildx:armhf-ubuntu16-jdk11"),

        new Target("linux", "armel", "Ubuntu 16.04, JDK 11")
            .setTags("test")
            .setHost("bmh-hv-6")
            .setContainerImage("fizzed/buildx:armel-debian11-jdk11"),

        new Target("linux", "riscv64", "Debian 11, JDK 21")
            .setTags("test")
            .setHost("bmh-build-riscv64-debian11-1"),

        //
        // CI/Test Linux w/ MUSL
        //

        new Target("linux_musl", "x64", "Alpine 3.11, JDK 11")
            .setTags("test")
            .setContainerImage("fizzed/buildx:x64-alpine3.11-jdk11"),

        new Target("linux_musl", "arm64", "Alpine 3.11, JDK 11")
            .setTags("test")
            .setHost("bmh-hv-6")
            .setContainerImage("fizzed/buildx:arm64-alpine3.11-jdk11")

        //
        // CI/Test Windows
        //

        /*new Target("windows", "x64", "Windows 10")
            .setTags("test")
            .setHost("bmh-build-x64-win10-1"),

        new Target("windows", "arm64", "Windows 11")
            .setTags("test")
            .setHost("bmh-build-arm64-win11-1")*/
    );

    @Task(order = 50)
    public void cross_build_containers() throws Exception {
        new Buildx(crossTargets)
            .containersOnly()
            .execute((target, project) -> {
                /*
                // no customization needed
                Path installScript = Paths.get("setup/install-linux.sh");
                if (target.getOs().equals("linux_musl")) {
                    installScript = Paths.get("setup/install-alpine.sh");
                }*/

                project.buildContainer(new ContainerBuilder()
                    //.setInstallScript(installScript)
                );
            });
    }

    @Task(order = 51)
    public void cross_build_natives() throws Exception {
        new Buildx(crossTargets)
            .tags("build")
            .execute((target, project) -> {
                // delegate to blaze
                project.action("java", "-jar", "blaze.jar", "build_natives", "--target", target.getOsArch())
                    .run();

                // we know that the only modified file will be in the artifact dir
                final String artifactRelPath = "tokyocabinet-" + target.getOsArch() + "/src/main/resources/jne/" + target.getOs() + "/" + target.getArch() + "/";
                project.rsync(artifactRelPath, artifactRelPath)
                    .run();
            });
    }

    @Task(order = 53)
    public void cross_tests() throws Exception {
        new Buildx(crossTargets)
            .tags("test")
            .execute((target, project) -> {
                project.action("java", "-jar", "blaze.jar", "test")
                    .run();
            });
    }

}
