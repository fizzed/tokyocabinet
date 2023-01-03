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

public class blaze {
    private final Logger log = Contexts.logger();

    private final List<Target> targets = asList(
        // Linux x64 (ubuntu 16.04, glibc 2.23+)
        new Target("linux", "x64", null, "amd64/ubuntu:16.04"),
        //new Target("linux", "x64", null, null), // fully local

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





    //
    // Project Build Farm
    //

    public void execute(ProjectExecute projectExecute) throws Exception {
        final Path relProjectDir = withBaseDir("..");
        final Path absProjectDir = relProjectDir.toRealPath();
        final String containerPrefix = Contexts.config().value("container-prefix").get();
        final String targetsFilter = Contexts.config().value("targets").orNull();

        // filtered targets?
        List<Target> _targets = targets;
        if (targetsFilter != null && !targetsFilter.trim().equals("")) {
            final String _targetsFilter = ","+targetsFilter+",";
            _targets = targets.stream()
                .filter(v -> _targetsFilter.contains(","+v.getOsArch()+","))
                .collect(toList());
        }

        log.info("=====================================================");
        log.info("Project info");
        log.info("  relativeDir: {}", relProjectDir);
        log.info("  absoluteDir: {}", absProjectDir);
        log.info("  containerPrefix: {}", containerPrefix);
        log.info("  targets:");
        for (Target target : _targets) {
            log.info("    {}", target);
        }
        log.info("");

        for (Target target : _targets) {
            log.info("=====================================================");
            log.info("Executing for");
            log.info("  os-arch: {}", target.getOsArch());
            log.info("  ssh: {}", target.getSshHost());
            log.info("  baseDockerImage: {}", target.getBaseDockerImage());

            final boolean container = target.getBaseDockerImage() != null;

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

                    final LogicalProject project = new LogicalProject(target, containerPrefix, absProjectDir, relProjectDir, remoteProjectDir, container, sshSession);

                    projectExecute.execute(target, project, (localExecute, remoteExecute) -> {
                        remoteExecute.execute(sshSession);
                    });
                }
            } else {
                final LogicalProject project = new LogicalProject(target, containerPrefix, absProjectDir, relProjectDir, null, container, null);

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
        private final Target target;
        private final String containerPrefix;
        private final Path absoluteDir;
        private final Path relativeDir;
        private final String remoteDir;
        private final boolean container;
        private final SshSession sshSession;

        public LogicalProject(Target target, String containerPrefix, Path absoluteDir, Path relativeDir, String remoteDir, boolean container, SshSession sshSession) {
            this.target = target;
            this.containerPrefix = containerPrefix;
            this.absoluteDir = absoluteDir;
            this.relativeDir = relativeDir;
            this.remoteDir = remoteDir;
            this.container = container;
            this.sshSession = sshSession;
        }

        public String getContainerName() {
            return this.containerPrefix + "-" + target.getOsArch();
        }

        public String getContainerPrefix() {
            return containerPrefix;
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

        public SshSession getSshSession() {
            return sshSession;
        }

        public boolean hasContainer() {
            return this.container;
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

        public String actionPath(String path) {
            // is in container?
            if (this.container) {
                return "/project/" + path;
            } else if (this.sshSession != null) {
                // a remote path then
                return this.remotePath(path);
            } else {
                // otherwise, local host path
                return this.relativePath(path);
            }
        }

        public Exec action(String path, Object... arguments) {
            final String actionScript = this.actionPath(path);

            // is remote?
            if (this.sshSession != null) {
                if (this.container) {
                    // in container too?
                    return sshExec(sshSession, "docker", "run", "-v", this.getRemoteDir()+":/project", this.getContainerName(), actionScript).args(arguments);
                } else {
                    // remote path
                    return sshExec(sshSession, actionScript).args(arguments);
                }
            } else {
                // on local machine
                if (this.container) {
                    // in container too?
                    return exec("docker", "run", "-v", this.getAbsoluteDir()+":/project", this.getContainerName(), actionScript).args(arguments);
                } else {
                    // fully local
                    return exec(actionScript).args(arguments);
                }
            }
        }

        public Exec exec(String path, Object... arguments) {
            final String actionScript = this.sshSession != null ? this.remotePath(path) : this.relativePath(path);
            // is remote?
            if (this.sshSession != null) {
                return sshExec(sshSession, actionScript).args(arguments);
            } else {
                return Systems.exec(actionScript).args(arguments);
            }
        }

        public Exec rsync(String sourcePath, String destPath) {
            // is remote?
            if (this.sshSession != null) {
                // rsync the project target/output to the target project
                return Systems.exec("rsync", "-avrt", "--delete", "--progress", this.target.getSshHost()+":"+this.remotePath(sourcePath), this.relativePath(destPath));
            } else {
                // local execute
                return Systems.exec("rsync", "-avrt", "--delete", "--progress", this.relativePath(sourcePath), this.relativePath(destPath));
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