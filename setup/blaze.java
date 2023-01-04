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
import com.fizzed.blaze.buildx.*;

public class blaze extends BlazeBuildx {

    @Override
    protected List<Target> targets() {
        return asList(
            // Linux x64 (ubuntu 16.04, glibc 2.23+)
            new Target("linux", "x64", null, "amd64/ubuntu:16.04"),
            //new Target("linux", "x64", null, null), // fully local

            // Linux arm64 (ubuntu 16.04, glibc 2.23+)
            new Target("linux", "arm64", "bmh-build-arm64-ubuntu22-1", "arm64v8/ubuntu:16.04"),

            // Linux armhf (ubuntu 16.04, glibc 2.23+)
            new Target("linux", "armhf", null, "arm32v7/ubuntu:16.04"),

            // Linux MUSL x64 (alpine 3.11)
            new Target("linux_musl", "x64", null, "amd64/alpine:3.11"),

            // Linux MUSL arm64 (alpine 3.11)
            new Target("linux_musl", "arm64", "bmh-build-arm64-ubuntu22-1", "arm64v8/alpine:3.11"),

            // MacOS x64 (10.13+)
            new Target("macos", "x64", "bmh-build-x64-macos1013-1", null),

            // MacOS arm64 (12+)
            new Target("macos", "arm64", "bmh-build-arm64-macos12-1", null),

            // Linux riscv64 (ubuntu 20.04, glibc 2.31+)
            new Target("linux", "riscv64", null, "riscv64/ubuntu:20.04")

            // potentially others could be built too
            // arm32v7/debian linux-armhf
            // arm32v5/debian linux-armel
            // mips64le/debian linux-mips64le
            // s390x/debian linux-s390x
            // ppc64le/debian linux-ppc64le
        );
    }

    public void build_containers() throws Exception {
        this.execute((target, project, executor) -> {
            if (project.hasContainer()) {
                project.exec("setup/build-docker-container-action.sh", target.getBaseDockerImage(), project.getContainerName()).run();
            }
        });
    }

    public void build_native_libs() throws Exception {
        this.execute((target, project, executor) -> {
            final String artifactRelPath = "tokyocabinet-" + target.getOsArch() + "/src/main/resources/jne/" + target.getOs() + "/" + target.getArch() + "/";

            String buildScript = "setup/build-native-lib-linux-action.sh";
            if (target.getOs().equals("macos")) {
                buildScript = "setup/build-native-lib-macos-action.sh";
            }

            project.action(buildScript).run();
            project.rsync("target/output/", artifactRelPath).run();
        });
    }

    public void test_containers() throws Exception {
        this.execute((target, project, executor) -> {
            project.action("setup/test-project-action.sh").run();
        });
    }

}