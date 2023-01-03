import org.slf4j.Logger;
import java.util.List;
import static java.util.Arrays.asList;
import java.nio.file.Path;
import com.fizzed.blaze.Contexts;
import static com.fizzed.blaze.Contexts.withBaseDir;
import static com.fizzed.blaze.Contexts.fail;
import static com.fizzed.blaze.Systems.exec;
import com.fizzed.blaze.ssh.SshSession;
import static com.fizzed.blaze.SecureShells.sshConnect;
import static com.fizzed.blaze.SecureShells.sshExec;

public class blaze {
    private final Logger log = Contexts.logger();

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
        this.execute((target, project, executor) -> {
            executor.execute(
                // local execute
                () -> {
                    exec(project.relativePath("setup/helper-build-docker-container.sh"), target.getBaseDockerImage(), target.getOsArch()).run();
                },
                // remote execute
                (sshSession) -> {
                    // remote in docker execute
                    if (target.getBaseDockerImage() != null) {
                        sshExec(sshSession, project.remotePath("setup/helper-build-docker-container.sh"), target.getBaseDockerImage(), target.getOsArch()).run();
                    } else {
                        // remote (non docker) execute
                        // nothing to do, project already synced
                    }
                }
            );
        });
    }

    private String resolveBuildScriptFunc(Target target) {
        switch (target.getOs()) {
            case "macos":
                return "setup/helper-build-native-libs-macos.sh";
            case "linux":
            case "linux_musl":
                return "setup/helper-build-native-libs-linux.sh";
            default:
                throw new RuntimeException("Unsupported os " + target.getOs());
        }
    }

    public void build_native_libs() throws Exception {
        this.execute((target, project, executor) -> {
            final String buildScript = this.resolveBuildScriptFunc(target);
            final String artifactRelPath = "tokyocabinet-" + target.getOsArch() + "/src/main/resources/jne/" + target.getOs() + "/" + target.getArch() + "/";

            executor.execute(
                // local execute
                () -> {
                    exec("docker", "run", "-v", project.getAbsoluteDir() + ":/project", "tokyocabinet-" + target.getOsArch(), "/project/" + buildScript).run();

                    // rsync the project target/output to the target project
                    exec("rsync", "-avrt", "--delete", "--progress", project.relativePath("target/output/"), project.relativePath(artifactRelPath)).run();
                },
                // remote execute
                (sshSession) -> {
                    // remote in docker execute
                    if (target.getBaseDockerImage() != null) {
                        sshExec(sshSession, "docker", "run", "-v", project.getRemoteDir() + ":/project", "tokyocabinet-" + target.getOsArch(), "/project/" + buildScript).run();
                    } else {
                        // remote (non docker) execute
                        sshExec(sshSession, project.remotePath(buildScript)).run();
                    }

                    // rsync the project target/output to the target project
                    exec("rsync", "-avrt", "--delete", "--progress", target.getSshHost() + ":" + project.remotePath("target/output/"), project.relativePath(artifactRelPath)).run();
                }
            );
        });
    }

    public void execute(ProjectExecute projectExecute) throws Exception {
        final Path relProjectDir = withBaseDir("..");
        final Path absProjectDir = relProjectDir.toRealPath();

        log.info("=====================================================");
        log.info("Project info");
        log.info("  relativeDir: {}", relProjectDir);
        log.info("  absoluteDir: {}", absProjectDir);
        log.info("  targets:");
        for (Target target : targets) {
            log.info("    {}", target);
        }
        log.info("");

        for (Target target : targets) {
            log.info("=====================================================");
            log.info("Executing for");
            log.info("  os-arch: {}", target.getOsArch());
            log.info("  ssh: {}", target.getSshHost());
            log.info("  baseDockerImage: {}", target.getBaseDockerImage());

            if (target.getSshHost() != null) {
                final String remoteProjectDir = "~/remote-build/" + absProjectDir.getFileName().toString();
                log.info("  remoteDir: {}", remoteProjectDir);

                try (SshSession sshSession = sshConnect("ssh://" + target.getSshHost()).run()) {
                    log.info("Connected with...");

                    sshExec(sshSession, "uname", "-a").run();

                    log.info("Will make sure remote host has project dir {}", remoteProjectDir);

                    sshExec(sshSession, "mkdir", "-p", remoteProjectDir).run();

                    log.info("Will rsync current project to remote host...");

                    // sync our project directory to the remote host
                    exec("rsync", "-avrt", "--delete", "--progress", "--exclude=.git/", "--exclude=target/", absProjectDir+"/", target.getSshHost()+":"+remoteProjectDir+"/").run();

                    final LogicalProject project = new LogicalProject(absProjectDir, relProjectDir, remoteProjectDir);

                    projectExecute.execute(target, project, (localExecute, remoteExecute) -> {
                        remoteExecute.execute(sshSession);
                    });
                }
            } else {
                final LogicalProject project = new LogicalProject(absProjectDir, relProjectDir, null);

                projectExecute.execute(target, project, (localExecute, remoteExecute) -> {
                    localExecute.execute();
                });
            }
        }
    }

    public interface LocalExecute {
        void execute() throws Exception;
    }

    public interface RemoteExecute {
        void execute(SshSession sshSession) throws Exception;
    }

    public interface LocalRemoteExecute {
        void execute(LocalExecute localExecute, RemoteExecute remoteExecute) throws Exception;
    }

    public interface ProjectExecute {
        void execute(Target target, LogicalProject project, LocalRemoteExecute localRemoteExecute) throws Exception;
    }

    static public class LogicalProject {
        private final Path absoluteDir;
        private final Path relativeDir;
        private final String remoteDir;

        public LogicalProject(Path absoluteDir, Path relativeDir, String remoteDir) {
            this.absoluteDir = absoluteDir;
            this.relativeDir = relativeDir;
            this.remoteDir = remoteDir;
        }

        public Path getAbsoluteDir() {
            return absoluteDir;
        }

        public Path getRelativeDir() {
            return relativeDir;
        }

        public String getRemoteDir() {
            return remoteDir;
        }

        // helpers

        public String relativePath(String path) {
            return this.relativeDir.resolve(".").toString() + "/" + path;
        }

        public String remotePath(String path) {
            if (this.remoteDir == null) {
                throw new RuntimeException("Project is NOT remote (no remoteDir)");
            }
            return this.remoteDir + "/" + path;
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

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getOsArch());
            if (this.baseDockerImage != null) {
                sb.append(" with container ");
                sb.append(this.baseDockerImage);
            }
            if (this.sshHost != null) {
                sb.append(" on host ");
                sb.append(this.sshHost);
            }
            return sb.toString();
        }

    }

}