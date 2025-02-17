/*
 * Copyright 2000-2023 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.jmix.flowui.devserver.frontend;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.Platform;
import com.vaadin.flow.server.frontend.FallibleCommand;
import com.vaadin.flow.server.frontend.FrontendToolsSettings;
import com.vaadin.flow.server.frontend.FrontendVersion;
import com.vaadin.flow.shared.util.SharedUtil;
import elemental.json.JsonObject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.jmix.flowui.devserver.frontend.FrontendUtils.commandToString;
import static io.jmix.flowui.devserver.frontend.NodeUpdater.HASH_KEY;
import static io.jmix.flowui.devserver.frontend.NodeUpdater.PROJECT_FOLDER;
import static io.jmix.flowui.devserver.frontend.NodeUpdater.VAADIN_DEP_KEY;
import static io.jmix.flowui.devserver.frontend.NodeUpdater.VAADIN_VERSION;

/**
 * Run <code>npm install</code> after dependencies have been updated.
 */
public class TaskRunNpmInstall implements FallibleCommand {

    /** Container for npm installation statistics. */
    public static class Stats {
        private long installTimeMs = 0;
        private long cleanupTimeMs = 0;
        private String packageManager = "";

        /** Create an instance. */
        private Stats() {
        }

        /**
         * Gets the time spent running {@code npm install}.
         *
         * @return the time in milliseconds
         */
        public long getInstallTimeMs() {
            return installTimeMs;
        }

        /**
         * Gets the time spent doing cleanup before {@code npm install}.
         *
         * @return the time in milliseconds
         */
        public long getCleanupTimeMs() {
            return cleanupTimeMs;
        }

        /**
         * Gets the package manager used for installation.
         *
         * @return the name of the package manager
         */
        public String getPackageManager() {
            return packageManager;
        }

    }

    private static final String MODULES_YAML = ".modules.yaml";

    private static final String NPM_VALIDATION_FAIL_MESSAGE = "%n%n======================================================================================================"
            + "%nThe path to npm cache contains whitespaces, and the currently installed npm version doesn't accept this."
            + "%nMost likely your Windows user home path contains whitespaces."
            + "%nTo workaround it, please change the npm cache path by using the following command:"
            + "%n    npm config set cache [path-to-npm-cache] --global"
            + "%n(you may also want to exclude the whitespaces with 'dir /x' to use the same dir),"
            + "%nor upgrade the npm version to 7 (or newer) by:"
            + "%n 1) Running 'npm-windows-upgrade' tool with Windows PowerShell:"
            + "%n        Set-ExecutionPolicy Unrestricted -Scope CurrentUser -Force"
            + "%n        npm install -g npm-windows-upgrade"
            + "%n        npm-windows-upgrade"
            + "%n 2) Manually installing a newer version of npx: npm install -g npx"
            + "%n 3) Manually installing a newer version of pnpm: npm install -g pnpm"
            + "%n 4) Deleting the following files from your Vaadin project's folder (if present):"
            + "%n        node_modules, package-lock.json, webpack.generated.js, pnpm-lock.yaml, pnpmfile.js"
            + "%n======================================================================================================%n";

    private static Stats lastInstallStats = new Stats();

    private final NodeUpdater packageUpdater;

    private final List<String> ignoredNodeFolders = Arrays.asList(".bin",
            "pnpm", ".ignored_pnpm", ".pnpm", ".staging", ".vaadin",
            MODULES_YAML);

    private final Options options;

    /**
     * Create an instance of the command.
     *
     * @param packageUpdater
     *            package-updater instance used for checking if previous
     *                       execution modified the package.json file
     * @param options
     *            the options for the task
     */
    TaskRunNpmInstall(NodeUpdater packageUpdater, Options options) {
        this.packageUpdater = packageUpdater;
        this.options = options;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        String toolName = options.isEnablePnpm() ? "pnpm" : "npm";
        String command = "install";
        if (options.isCiBuild()) {
            if (options.isEnablePnpm()) {
                command += " --frozen-lockfile";
            } else {
                command = "ci";
            }
        }
        if (packageUpdater.modified || shouldRunNpmInstall()) {
            String logMessage = "Running `" + toolName + " " + command
                    + "` to "
                    + "resolve and optionally download frontend dependencies. "
                    + "This may take a moment, please stand by...";
            packageUpdater.log().info(logMessage);
            FrontendUtils.logInFile(logMessage);
            runNpmInstall();
            updateLocalHash();
        } else {
            String logMessage = String.format("Skipping `%s install` because the frontend packages are already "
                            + "installed in the folder '%s' and the hash in the file '%s' is the same as in '%s'",
                    toolName,
                    options.getNodeModulesFolder().getAbsolutePath(),
                    packageUpdater.getVaadinJsonFile().getAbsolutePath(),
                    Constants.PACKAGE_JSON);
            packageUpdater.log().info(logMessage);
            FrontendUtils.logInFile(logMessage);
        }
    }

    /**
     * Updates
     *
     * <pre>
     * node_modules/.vaadin/vaadin.json
     * </pre>
     * <p>
     * with package.json hash, project folder and the platform version.
     * <p>
     * This is for handling updated package to the code repository by another
     * developer as then the hash is updated and we may just be missing one
     * module, as well as for detecting that the platform version has changed
     * which may require a deeper cleanup.
     */
    private void updateLocalHash() {
        try {
            final JsonObject vaadin = packageUpdater.getPackageJson(packageUpdater.getStudioJsonFile())
                    .getObject(VAADIN_DEP_KEY);
            if (vaadin == null) {
                String message = "No vaadin object in package.json";
                packageUpdater.log().warn(message);
                FrontendUtils.logInFile(message);
                return;
            }
            final String hash = vaadin.getString(HASH_KEY);

            final Map<String, String> updates = new HashMap<>();
            updates.put(HASH_KEY, hash);
            TaskUpdatePackages.getVaadinVersion(packageUpdater.finder)
                    .ifPresent(s -> updates.put(VAADIN_VERSION, s));
            updates.put(PROJECT_FOLDER,
                    options.getStudioFolder().getAbsolutePath());
            packageUpdater.updateVaadinJsonContents(updates);
        } catch (IOException e) {
            String message = "Failed to update node_modules hash.";
            packageUpdater.log().warn(message, e);
            FrontendUtils.logInFile(message + "\n" + Arrays.toString(e.getStackTrace()));
        }
    }

    private boolean shouldRunNpmInstall() {
        if (!options.getNodeModulesFolder().isDirectory()) {
            return true;
        }
        // Ignore .bin and pnpm folders as those are always installed for
        // pnpm execution
        File[] installedPackages = options.getNodeModulesFolder()
                .listFiles((dir, name) -> !ignoredNodeFolders.contains(name));
        assert installedPackages != null;
        if (installedPackages.length == 0) {
            // Nothing installed
            return true;
        }
        return isVaadinHashOrProjectFolderUpdated();
    }

    boolean isVaadinHashOrProjectFolderUpdated() {
        try {
            JsonObject nodeModulesVaadinJson = packageUpdater
                    .getVaadinJsonContents();
            if (nodeModulesVaadinJson.hasKey(HASH_KEY)) {
                final JsonObject packageJson = packageUpdater.getPackageJson(packageUpdater.getStudioJsonFile());
                if (!nodeModulesVaadinJson.getString(HASH_KEY)
                        .equals(packageJson.getObject(VAADIN_DEP_KEY)
                                .getString(HASH_KEY))) {
                    return true;
                }

                if (nodeModulesVaadinJson.hasKey(PROJECT_FOLDER)
                        && !options.getStudioFolder().getAbsolutePath()
                        .equals(nodeModulesVaadinJson
                                .getString(PROJECT_FOLDER))) {
                    return true;
                }

                return false;
            }
        } catch (IOException e) {
            String message = "Failed to load hashes forcing npm execution";
            packageUpdater.log().warn(message, e);
            FrontendUtils.logInFile(message+ "\n" + Arrays.toString(e.getStackTrace()));
        }
        return true;
    }

    /**
     * Installs frontend resources (using either pnpm or npm) after
     * `package.json` has been updated.
     */
    private void runNpmInstall() throws ExecutionFailedException {
        // Do possible cleaning before generating any new files.
        cleanUp();
        long startTime = System.currentTimeMillis();

        Logger logger = packageUpdater.log();
        String baseDir = options.getStudioFolder().getAbsolutePath();

        FrontendToolsSettings settings = new FrontendToolsSettings(baseDir,
                () -> FrontendUtils.getVaadinHomeDirectory().getAbsolutePath());
        settings.setNodeDownloadRoot(options.getNodeDownloadRoot());
        settings.setForceAlternativeNode(options.isRequireHomeNodeExec());
        settings.setUseGlobalPnpm(options.isUseGlobalPnpm());
        settings.setAutoUpdate(options.isNodeAutoUpdate());
        settings.setNodeVersion(options.getNodeVersion());
        FrontendTools tools = new FrontendTools(settings);
        tools.validateNodeAndNpmVersion();
        if (options.isEnablePnpm()) {
            try {
                createPnpmFile(packageUpdater.versionsJson, tools);
            } catch (IOException exception) {
                throw new ExecutionFailedException(
                        "Failed to read frontend version data from vaadin-core "
                                + "and make it available to pnpm for locking transitive dependencies.\n"
                                + "Please report an issue, as a workaround try running project "
                                + "with npm by setting system variable -Dvaadin.pnpm.enable=false",
                        exception);
            }
            try {
                createNpmRcFile();
            } catch (IOException exception) {
                String message = ".npmrc generation failed; pnpm "
                        + "package installation may require manaually passing "
                        + "the --shamefully-hoist flag";
                logger.warn(message, exception);
                FrontendUtils.logInFile(message);
            }
        }

        List<String> npmExecutable;
        List<String> npmInstallCommand;
        List<String> postinstallCommand;

        try {
            if (options.isRequireHomeNodeExec()) {
                tools.forceAlternativeNodeExecutable();
            }
            if (options.isEnablePnpm()) {
                validateInstalledNpm(tools);
                npmExecutable = tools.getPnpmExecutable();
            } else {
                npmExecutable = tools.getNpmExecutable();
            }
            npmInstallCommand = new ArrayList<>(npmExecutable);
            postinstallCommand = new ArrayList<>(npmExecutable);
            // This only works together with "install"
            postinstallCommand.remove("--shamefully-hoist=true");

        } catch (IllegalStateException exception) {
            throw new ExecutionFailedException(exception.getMessage(),
                    exception);
        }

        npmInstallCommand.add("--ignore-scripts");

        if (options.isCiBuild()) {
            if (options.isEnablePnpm()) {
        npmInstallCommand.add("install");
                npmInstallCommand.add("--frozen-lockfile");
            } else {
                npmInstallCommand.add("ci");
            }
        } else {
            npmInstallCommand.add("install");
        }

        postinstallCommand.add("run");
        postinstallCommand.add("postinstall");

        logger.debug(
                commandToString(options.getStudioFolder().getAbsolutePath(),
                        npmInstallCommand));

        String toolName = options.isEnablePnpm() ? "pnpm" : "npm";

        String commandString = npmInstallCommand.stream()
                .collect(Collectors.joining(" "));

        String logMessage = String.format(
                "using '%s' for frontend package installation",
                String.join(" ", npmInstallCommand)
        );
        logger.info(logMessage);
        FrontendUtils.logInFile(logMessage);

        // Log a stronger request for patience if package-lock.json is
        // missing as "npm install" in this case can take minutes
        // https://github.com/vaadin/flow/issues/12825
        File packageLockFile = packageUpdater.getStudioPackageLockFile();
        if (!options.isEnablePnpm() && !packageLockFile.exists()) {
            String packageLockFileNotFoundWarnMessage = "package-lock.json is missing from this "
                    + "project. This may cause the npm package installation to "
                    + "take several minutes. It is recommended to keep the "
                    + "package-lock.json file persistently in your project. "
                    + "Please stand by...";
            packageUpdater.log().warn(packageLockFileNotFoundWarnMessage);
            FrontendUtils.logInFile(packageLockFileNotFoundWarnMessage);
        }

        Process process = null;
        try {
            process = runNpmCommand(npmInstallCommand, options.getStudioFolder());

            logger.debug("Output of `{}`:", commandString);
            StringBuilder toolOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String stdoutLine;
                while ((stdoutLine = reader.readLine()) != null) {
                    logger.debug(stdoutLine);
                    FrontendUtils.logInFile(stdoutLine);
                    toolOutput.append(stdoutLine).append(System.lineSeparator());
                }
            }

            int errorCode = process.waitFor();

            if (errorCode != 0) {
                // Echo the stdout from pnpm/npm to error level log
                logger.error("Command `{}` failed:\n{}", commandString, toolOutput);
                logger.error(">>> Dependency ERROR. Check that all required dependencies are deployed in {} repositories.", toolName);
                FrontendUtils.logInFile("ERROR for command: " + commandString + "\n" + toolOutput);
                throw new ExecutionFailedException(
                        SharedUtil.capitalize(toolName)
                                + " install has exited with non zero status. "
                                + "Some dependencies are not installed. Check "
                                + toolName + " command output");
            } else {
                String successfullyResolvedMessage = "Frontend dependencies resolved successfully.";
                logger.info(successfullyResolvedMessage);
                FrontendUtils.logInFile(successfullyResolvedMessage);
            }
        } catch (InterruptedException | IOException e) {
            logger.error("Error when running `{} install`", toolName, e);
            FrontendUtils.logInFile(String.format("Error when running `%s install`", toolName) + "\n" + e);
            if (e instanceof InterruptedException) {
                // Restore interrupted state
                Thread.currentThread().interrupt();
            }
            throw new ExecutionFailedException(
                    "Command '" + toolName + " install' failed to finish", e);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }

        List<String> postinstallPackages = new ArrayList<>();
        postinstallPackages.add(".");
        postinstallPackages.add("esbuild");
        postinstallPackages.add("@vaadin/vaadin-usage-statistics");
        postinstallPackages.addAll(options.getPostinstallPackages());

        for (String postinstallPackage : postinstallPackages) {
            File packageJsonFile = getPackageJsonForModule(postinstallPackage);
            if (packageJsonFile == null || !packageJsonFile.exists()) {
                continue;
            }
            File packageFolder = packageJsonFile.getParentFile();
            try {
                JsonObject packageJson = TaskGeneratePackageJson
                        .getJsonFileContent(packageJsonFile);
                if (!containsPostinstallScript(packageJson)) {
                    logger.debug(
                            "Skipping postinstall for '{}' as no postinstall script was found in the package.json",
                            postinstallPackage);
                    continue;
                }
            } catch (IOException ioe) {
                String errorMessage = String.format(
                        "Couldn't read package.json for %s. Skipping postinstall", postinstallPackage
                );
                logger.error(errorMessage, ioe);
                FrontendUtils.logInFile(errorMessage + ":\n" + ioe);
                continue;
            }

            logger.debug("Running postinstall for '{}'", postinstallPackage);
            try {
                process = runNpmCommand(postinstallCommand, packageFolder);
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    // Restore interrupted state
                    Thread.currentThread().interrupt();
                }
                throw new ExecutionFailedException(
                        "Error when running postinstall script for '"
                                + postinstallPackage + "'",
                        e);
            }
        }
        lastInstallStats.installTimeMs = System.currentTimeMillis() - startTime;
        lastInstallStats.packageManager = options.isEnablePnpm() ? "pnpm" : "npm";
    }

    private File getPackageJsonForModule(String module) {
        if (module.trim().equals("")) {
            return null;
        }
        if (module.equals(".")) {
            // The location of the project package.json
            return new File(options.getStudioFolder(), "package.json");
        }

        return new File(new File(options.getNodeModulesFolder(), module),
                "package.json");

    }

    private boolean containsPostinstallScript(JsonObject packageJson) {
        return packageJson != null && packageJson.hasKey("scripts")
                && packageJson.getObject("scripts").hasKey("postinstall");
    }

    private Process runNpmCommand(List<String> command, File workingDirectory)
            throws IOException {
        ProcessBuilder builder = FrontendUtils.createProcessBuilder(command);
        builder.environment().put("ADBLOCK", "1");
        builder.environment().put("NO_UPDATE_NOTIFIER", "1");
        builder.directory(workingDirectory);

        File logFile = FrontendUtils.getLogFile();
        if (logFile != null) {
            ProcessBuilder.Redirect logFileRedirect = ProcessBuilder.Redirect.appendTo(logFile);
            builder.redirectError(logFileRedirect);
            builder.redirectOutput(logFileRedirect);
        } else {
            FrontendUtils.console(FrontendUtils.RED, "Log file is null, redirect is unavailable");
            packageUpdater.log().warn("Log file is null, redirect is unavailable");
        }

        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        Process process = builder.start();

        // This will allow to destroy the process which does IO regardless
        // whether it's executed in the same thread or another (may be
        // daemon) thread
        Runtime.getRuntime()
                .addShutdownHook(new Thread(process::destroyForcibly));

        return process;
    }

    /*
     * The pnpmfile.js file is recreated from scratch every time when `pnpm
     * install` is executed. It doesn't take much time to recreate it and it's
     * not supposed that it can be modified by the user. This is done in the
     * same way as for webpack.generated.js.
     */
    private void createPnpmFile(JsonObject versionsJson, FrontendTools tools)
            throws IOException {
        if (versionsJson == null) {
            return;
        }

        String pnpmFileName = ".pnpmfile.cjs";
        final List<String> pnpmExecutable = tools.getSuitablePnpm();
        pnpmExecutable.add("--version");
        try {
            final FrontendVersion pnpmVersion = FrontendUtils.getVersion("pnpm",
                    pnpmExecutable);
            if (pnpmVersion.isOlderThan(new FrontendVersion("6.0"))) {
                pnpmFileName = "pnpmfile.js";
            }
        } catch (FrontendUtils.UnknownVersionException e) {
            packageUpdater.log().error("Failed to determine pnpm version", e);
        }

        File pnpmFile = new File(options.getStudioFolder().getAbsolutePath(),
                pnpmFileName);
        try (InputStream content = FrontendUtils.getResourceAsStream("pnpmfile.js")) {
            if (content == null) {
                throw new IOException("Couldn't find template pnpmfile.js in the classpath");
            }

            FileUtils.copyInputStreamToFile(content, pnpmFile);
            packageUpdater.log().debug("Generated pnpmfile hook file: '{}'",
                    pnpmFile);

            FileUtils.writeStringToFile(pnpmFile,
                    modifyPnpmFile(pnpmFile, versionsJson),
                    StandardCharsets.UTF_8);
        }
    }

    /*
     * Create an .npmrc file the project directory if there is none.
     */
    private void createNpmRcFile() throws IOException {
        File npmrcFile = new File(options.getStudioFolder().getAbsolutePath(),
                ".npmrc");
        boolean shouldWrite;
        if (npmrcFile.exists()) {
            List<String> lines = FileUtils.readLines(npmrcFile, StandardCharsets.UTF_8);
            if (lines.stream().anyMatch(line -> line.contains("NOTICE: this is an auto-generated file"))) {
                shouldWrite = true;
            } else {
                // Looks like this file was not generated by Vaadin
                if (lines.stream()
                        .noneMatch(line -> line.contains("shamefully-hoist"))) {
                    String message = "Custom .npmrc file ({}) found in "
                            + "project; pnpm package installation may "
                            + "require passing the --shamefully-hoist flag";
                    FrontendUtils.logInFile(message);
                    packageUpdater.log().info(message, npmrcFile);
                }
                shouldWrite = false;
            }
        } else {
            shouldWrite = true;
        }
        if (shouldWrite) {
            try (InputStream content = FrontendUtils.getResourceAsStream("npmrc")) {
                if (content == null) {
                    FrontendUtils.logInFile("Couldn't find template npmrc in the classpath");
                    throw new IOException("Couldn't find template npmrc in the classpath");
                }
                FileUtils.copyInputStreamToFile(content, npmrcFile);
                packageUpdater.log().debug("Generated pnpm configuration: '{}'", npmrcFile);
                FrontendUtils.logInFile("Generated pnpm configuration: " + npmrcFile);
            }
        }
    }

    private String modifyPnpmFile(File generatedFile, JsonObject versionsJson)
            throws IOException {
        String content = FileUtils.readFileToString(generatedFile,
                StandardCharsets.UTF_8);
        content = content.replace("versionsinfojson", versionsJson.toJson());
        return content;
    }

    private void cleanUp() throws ExecutionFailedException {
        if (!options.getNodeModulesFolder().exists()) {
            lastInstallStats.cleanupTimeMs = 0;
            return;
        }
        long startTime = System.currentTimeMillis();

        if (options.isCiBuild()) {
            deleteNodeModules(options.getNodeModulesFolder());
        } else {
        File modulesYaml = new File(options.getNodeModulesFolder(),
                MODULES_YAML);
        boolean hasModulesYaml = modulesYaml.exists() && modulesYaml.isFile();
        if (!options.isEnablePnpm() && hasModulesYaml) {
            deleteNodeModules(options.getNodeModulesFolder());
        } else if (options.isEnablePnpm() && !hasModulesYaml) {
                // presence of .staging dir with a "pnpm-*" folder means that
                // pnpm download is in progress, don't remove anything in this
                // case
            File staging = new File(options.getNodeModulesFolder(), ".staging");
            if (!staging.isDirectory() || staging.listFiles(
                    (dir, name) -> name.startsWith("pnpm-")).length == 0) {
                deleteNodeModules(options.getNodeModulesFolder());
            }
        }
        }
        lastInstallStats.cleanupTimeMs = System.currentTimeMillis() - startTime;
    }

    private void deleteNodeModules(File nodeModulesFolder)
            throws ExecutionFailedException {
        try {
            FrontendUtils.deleteNodeModules(nodeModulesFolder);
        } catch (IOException exception) {
            Logger log = packageUpdater.log();
            log.debug("Exception removing node_modules", exception);
            String failedToRemoveNodeModulesErrorMessage = "Failed to remove '"
                    + options.getStudioFolder().getAbsolutePath()
                    + "'. Please remove it manually.";
            log.error(failedToRemoveNodeModulesErrorMessage);
            FrontendUtils.logInFile(failedToRemoveNodeModulesErrorMessage);
            throw new ExecutionFailedException("Exception removing node_modules. Please remove it manually.");
        }
    }

    private void validateInstalledNpm(FrontendTools tools)
            throws IllegalStateException {
        File npmCacheDir = null;
        try {
            npmCacheDir = tools.getNpmCacheDir();
        } catch (FrontendUtils.CommandExecutionException
                 | IllegalStateException e) {
            String message = "Failed to get npm cache directory";
            packageUpdater.log().warn(message, e);
            FrontendUtils.logInFile(message);
        }

        if (npmCacheDir != null
                && !tools.folderIsAcceptableByNpm(npmCacheDir)) {
            FrontendUtils.console(String.format(NPM_VALIDATION_FAIL_MESSAGE));
            throw new IllegalStateException(
                    String.format(NPM_VALIDATION_FAIL_MESSAGE));
        }
    }

    /**
     * Returns timing information for the last operation.
     *
     * @return timing information
     */
    public static Stats getLastInstallStats() {
        return lastInstallStats;
    }

}

