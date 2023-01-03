import org.slf4j.Logger;
import java.util.List;
import static java.util.Arrays.asList;
import java.nio.file.Path;
import com.fizzed.blaze.Contexts;
import static com.fizzed.blaze.Contexts.withBaseDir;
import static com.fizzed.blaze.Contexts.fail;
import static com.fizzed.blaze.Systems.exec;
import static com.fizzed.blaze.Systems.exec;
import com.fizzed.blaze.ssh.SshSession;
import static com.fizzed.blaze.SecureShells.sshConnect;
import static com.fizzed.blaze.SecureShells.sshExec;

public class blaze {

    private final Logger log = Contexts.logger();
    private final Path relProjectDir = withBaseDir("..");
    private final List<Target> targets = asList(
        // Linux x64 (ubuntu 16.04, glibc 2.23+)
        new Target("linux", "x64", null, "amd64/ubuntu:16.04"),

        // Linux arm64 (ubuntu 16.04, glibc 2.23+)
        new Target("linux", "arm64", "bmh-build-arm64-ubuntu22-1", "arm64v8/ubuntu:16.04"),

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

    public void build_containers() throws Exception {
        final Path absProjectDir = relProjectDir.toRealPath();

        log.info("Project dir: {}", absProjectDir);

        for (Target target : targets) {
            if (target.getSshHost() != null) {
                final String remoteProjectDir = "~/remote-build/" + absProjectDir.getFileName().toString();
                log.info("Remote project dir: {}", remoteProjectDir);

                try (SshSession session = sshConnect("ssh://" + target.getSshHost()).run()) {
                    log.info("Connected with...");

                    sshExec(session, "uname", "-a").run();

                    log.info("Will make sure remote host has project dir {}", remoteProjectDir);

                    sshExec(session, "mkdir", "-p", remoteProjectDir).run();

                    log.info("Will rsync current project to remote host...");

                    // sync our project directory to the remote host
                    exec("rsync", "-avrt", "--delete", "--progress", "--exclude=.git/", "--exclude=target/", absProjectDir+"/", target.getSshHost()+":"+remoteProjectDir+"/").run();

                    if (target.getBaseDockerImage() != null) {
                        sshExec(session, remoteProjectDir + "/setup/helper-build-docker-container.sh", target.getBaseDockerImage(), target.getOsArch()).run();
                    } else {
                        // remote environment just needed our projec directory
                    }
                }
            } else {
                // local container
                exec(relProjectDir.resolve("setup/helper-build-docker-container.sh"), target.getBaseDockerImage(), target.getOsArch()).run();
            }
        }
    }

    public void build_native_libs() throws Exception {
        final Path absProjectDir = relProjectDir.toRealPath();

        log.info("Project dir: {}", absProjectDir);

        for (Target target : targets) {
            // what build script will we use?
            String buildScript = "helper-build-native-libs-linux.sh";
            if (target.getOs().contains("macos")) {
                buildScript = "helper-build-native-libs-macos.sh";
            }

            final String artifactRelPath = "tokyocabinet-"+target.getOsArch()+"/src/main/resources/jne/"+target.getOs()+"/"+target.getArch()+"/";

            if (target.getSshHost() != null) {
                final String remoteProjectDir = "~/remote-build/" + absProjectDir.getFileName().toString();
                log.info("Remote project dir: {}", remoteProjectDir);

                try (SshSession session = sshConnect("ssh://" + target.getSshHost()).run()) {
                    log.info("Connected with...");

                    sshExec(session, "uname", "-a").run();

                    log.info("Will rsync current project to remote host...");

                    // sync our project directory to the remote host
                    exec("rsync", "-avrt", "--delete", "--exclude=.git/", "--exclude=target/", "--progress", absProjectDir+"/", target.getSshHost()+":"+remoteProjectDir+"/").run();

                    if (target.getBaseDockerImage() != null) {
                        sshExec(session, "docker", "run", "-v", remoteProjectDir+":/project", "tokyocabinet-"+target.getOsArch(), "/project/setup/"+buildScript).run();
                    } else {
                        sshExec(session, remoteProjectDir+"/setup/"+buildScript).run();
                    }

                    // rsync the project target/output to the target project
                    exec("rsync", "-avrt", "--delete", "--progress", target.getSshHost()+":"+remoteProjectDir+"/target/output/", absProjectDir+"/"+artifactRelPath).run();
                }
            } else {
                // local container
                exec("docker", "run", "-v", absProjectDir+":/project", "tokyocabinet-"+target.getOsArch(), "/project/setup/"+buildScript).run();

                // rsync the project target/output to the target project
                exec("rsync", "-avrt", "--delete", "--progress", absProjectDir+"/target/output/", absProjectDir+"/"+artifactRelPath).run();
            }
        }
    }

    static public class Target {

        private final String os;
        private final String arch;
        private final String sshHost;
        private final String baseDockerImage;

        public Target(String os, String arch, String sshHost, String baseDockerImage) {
            this.os = os;
            this.arch = arch;
            this.sshHost = sshHost;
            this.baseDockerImage = baseDockerImage;
        }

        public String getSshHost() {
            return this.sshHost;
        }

        public String getOs() {
            return this.os;
        }

        public String getArch() {
            return this.arch;
        }

        public String getOsArch() {
            return this.os + "-" + this.arch;
        }

        public String getBaseDockerImage() {
            return this.baseDockerImage;
        }

    }

}