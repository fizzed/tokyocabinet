import org.slf4j.Logger;
import java.util.List;
import static java.util.Arrays.asList;
import java.nio.file.Path;
import java.util.function.Supplier;
import static java.util.stream.Collectors.toList;
import com.fizzed.blaze.Contexts;
import com.fizzed.blaze.system.Exec;
import static com.fizzed.blaze.Contexts.withBaseDir;
import static com.fizzed.blaze.Contexts.fail;
import com.fizzed.blaze.Systems;
import static com.fizzed.blaze.Systems.exec;
import com.fizzed.blaze.ssh.SshSession;
import static com.fizzed.blaze.SecureShells.sshConnect;
import static com.fizzed.blaze.SecureShells.sshExec;
import com.fizzed.buildx.*;

public class blaze {

    private final List<Target> targets = asList(
        // Linux arm64 (ubuntu 16.04, glibc 2.23+)
        new Target("linux", "x64")
            .setTags("build")
            .setContainerImage("amd64/ubuntu:16.04"),

        // Linux arm64 (ubuntu 16.04, glibc 2.23+)
        new Target("linux", "arm64")
            .setTags("build")
            .setContainerImage("amd64/ubuntu:16.04"),

        // Linux armhf (ubuntu 16.04, glibc 2.23+)
        new Target("linux", "armhf")
            .setTags("build")
            .setContainerImage("amd64/ubuntu:16.04"),

        // Linux armhf (ubuntu 16.04, glibc 2.23+)
        new Target("linux", "armel")
            .setTags("build")
            .setContainerImage("amd64/ubuntu:16.04"),

        // Linux riscv64 (ubuntu 18.04, glibc 2.31+)
        new Target("linux", "riscv64")
            .setTags("build")
            .setContainerImage("amd64/ubuntu:18.04"),

        // Linux MUSL x64 (alpine 3.11)
        new Target("linux_musl", "x64")
            .setTags("build")
            .setContainerImage("amd64/ubuntu:16.04"),

        // Linux MUSL arm64 (alpine 3.11)
        new Target("linux_musl", "arm64")
            .setTags("build")
            .setContainerImage("amd64/ubuntu:16.04"),

        // MacOS x64 (10.13+)
        new Target("macos", "x64")
            .setTags("build", "test")
            .setHost("bmh-build-x64-macos1013-1"),

        // MacOS arm64 (12+)
        new Target("macos", "arm64")
            .setTags("build", "test")
            .setHost("bmh-build-arm64-macos12-1"),

        //
        // test-only containers
        //

        new Target("linux", "x64-test")
            .setTags("test")
            .setContainerImage("amd64/ubuntu:16.04"),

        new Target("linux", "arm64-test")
            .setTags("test")
            .setHost("bmh-build-arm64-ubuntu22-1")
            .setContainerImage("arm64v8/ubuntu:16.04"),

        new Target("linux", "armhf-test")
            .setTags("test")
            .setContainerImage("arm32v7/ubuntu:16.04"),

        new Target("linux", "armel-test")
            .setTags("test")
            .setContainerImage("arm32v5/debian"),

        new Target("linux", "riscv64-test")
            .setTags("test")
            .setContainerImage("riscv64/ubuntu:20.04"),

        new Target("linux_musl", "x64-test")
            .setTags("test")
            .setContainerImage("amd64/alpine:3.11"),

        new Target("linux_musl", "arm64-test")
            .setTags("test")
            .setHost("bmh-build-arm64-ubuntu22-1")
            .setContainerImage("arm64v8/alpine:3.11")

        /*new Target("linux", "x64")
            .setContainerImage("amd64/ubuntu:16.04")*/



            // Linux x64 (ubuntu 16.04, glibc 2.23+)
        /*new Target("linux", "x64").setContainerImage("amd64/ubuntu:16.04"),


        // Linux MUSL x64 (alpine 3.11)
        new Target("linux_musl", "x64").setContainerImage("amd64/alpine:3.11"),

        // Linux MUSL arm64 (alpine 3.11)
        new Target("linux_musl", "arm64").setHost("bmh-build-arm64-ubuntu22-1").setContainerImage("arm64v8/alpine:3.11"),

        // MacOS x64 (10.13+)
        new Target("macos", "x64").setHost("bmh-build-x64-macos1013-1"),

        // MacOS arm64 (12+)
        new Target("macos", "arm64").setHost("bmh-build-arm64-macos12-1"),
        */

        // potentially others could be built too
        // mips64le/debian linux-mips64le
        // s390x/debian linux-s390x
        // ppc64le/debian linux-ppc64le
    );

    public void build_containers() throws Exception {
        new Buildx(targets)
            //.setTags("build")
            .execute((target, project) -> {
                if (project.hasContainer()) {
                    project.exec("setup/build-docker-container-action.sh", target.getContainerImage(), project.getContainerName(), target.getOs(), target.getArch()).run();
                }
            });
    }

    public void build_native_libs() throws Exception {
        new Buildx(targets)
            .setTags("build")
            .execute((target, project) -> {
                String buildScript = "setup/build-native-lib-linux-action.sh";
                if (target.getOs().equals("macos")) {
                    buildScript = "setup/build-native-lib-macos-action.sh";
                }

                project.action(buildScript, target.getOs(), target.getArch()).run();

                final String artifactRelPath = "tokyocabinet-" + target.getOsArch() + "/src/main/resources/jne/" + target.getOs() + "/" + target.getArch() + "/";
                project.rsync(artifactRelPath, artifactRelPath).run();
            });
    }

    public void tests() throws Exception {
        new Buildx(targets)
            .setTags("test")
            .execute((target, project) -> {
                project.action("setup/test-project-action.sh").run();
            });
    }

}