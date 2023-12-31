/*
 * Copyright (c) 2015, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.dataloader.install;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.salesforce.dataloader.config.Messages;
import com.salesforce.dataloader.util.AppUtil;

public class Installer extends Thread {
    private static final String USERHOME=System.getProperty("user.home");
    private static final String PATH_SEPARATOR = System.getProperty("file.separator");
    private static final String CREATE_DEKSTOP_SHORTCUT_ON_WINDOWS = ":createDesktopShortcut";
    private static final String CREATE_START_MENU_SHORTCUT_ON_WINDOWS = ":createStartMenuShortcut";

    private static Logger logger;
    
    public void run() {
        System.out.println(Messages.getMessage(Installer.class, "exitMessage"));
    }

    public static void install(String[] args) {
        String installationDir = ".";
        try {
            Runtime.getRuntime().addShutdownHook(new Installer());
            setLogger();
            boolean hideBanner = false;
            boolean skipCopyArtifacts = false;
            boolean skipCreateDesktopShortcut = false;
            boolean skipCreateStartMenuShortcut = false;
            boolean skipCreateAppsDirShortcut = false;
            
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-b":
                        hideBanner = true;
                        continue;
                    case "-c":
                        skipCopyArtifacts = true;
                        continue;
                    case "-d":
                        skipCreateDesktopShortcut = true;
                        continue;
                    case "-s":
                        skipCreateStartMenuShortcut = true;
                        continue;
                    case "-a":
                        skipCreateAppsDirShortcut = true;
                        continue;
                    default:
                        continue;
                }
            }
            if (!hideBanner) {
                logger.debug("going to show banner");
                AppUtil.showBanner();
            }
            if (!skipCopyArtifacts) {
                logger.debug("going to select installation directory");
                installationDir = selectInstallationDir();
                logger.debug("going to copy artifacts");
                copyArtifacts(installationDir);
                extractInstallationArtifactsFromJar(installationDir);
            }
            if (!skipCreateDesktopShortcut) {
                logger.debug("going to create desktop shortcut");
                createDesktopShortcut(installationDir);
            }
            if (!skipCreateStartMenuShortcut && AppUtil.isRunningOnWindows()) {
                logger.debug("going to create start menu shortcut");
                createStartMenuShortcut(installationDir);
            }
            if (!skipCreateAppsDirShortcut && AppUtil.isRunningOnMacOS()) {
                logger.debug("going to create Applications directory shortcut");
                createAppsDirShortcut(installationDir);
            }
        } catch (Exception ex) {
            handleException(ex, Level.FATAL);
            System.exit(-1);            
        }
    }
        
    private static String selectInstallationDir() throws IOException {
        String installationDir = "";
        System.out.println(Messages.getMessage(Installer.class, "initialMessage", USERHOME + PATH_SEPARATOR));
        String installationDirRoot = promptAndGetUserInput("Provide the installation directory [default: dataloader] : ");
        if (installationDirRoot.isBlank()) {
            installationDirRoot = "dataloader";
        }
        logger.debug("installation directory: " + installationDirRoot);
        String installationPathSuffix = installationDirRoot + PATH_SEPARATOR + "v" + AppUtil.DATALOADER_VERSION;
        if (installationDirRoot.startsWith(PATH_SEPARATOR) 
             || (AppUtil.isRunningOnWindows() && installationDirRoot.indexOf(':') == 1 && installationDirRoot.indexOf(PATH_SEPARATOR) == 2)) {
            // Absolute path specified. 
            // Absolute path on Mac and Linux start with PATH_SEPARATOR
            // Absolute path on Windows starts with <Single character drive letter>:\. For example, "C:\"
            installationDir = installationPathSuffix;
        } else {
            installationDir = USERHOME + PATH_SEPARATOR + installationPathSuffix;
        }
        logger.debug("installation directory absolute path: " + installationDir);
        System.out.println(Messages.getMessage(Installer.class, "installationDirConfirmation", AppUtil.DATALOADER_VERSION, installationDir));
        return installationDir;
    }
    
    private static void copyArtifacts(String installationDir) throws Exception {
        Path installationDirPath = Paths.get(installationDir);
        if (Files.exists(installationDirPath)) {
            for (;;) {
                System.out.println("");
                final String prompt = Messages.getMessage(Installer.class, "overwriteInstallationDirPrompt", AppUtil.DATALOADER_VERSION, installationDir);
                String input = promptAndGetUserInput(prompt);
                if (Messages.getMessage(Installer.class, "promptAnswerYes").toLowerCase().startsWith(input.toLowerCase())) {
                    System.out.println(Messages.getMessage(Installer.class, "deletionInProgressMessage", AppUtil.DATALOADER_VERSION));
                    Messages.getMessage(Installer.class, "initialMessage");
                    logger.debug("going to delete " + installationDir);
                    FileUtils.deleteDirectory(new File(installationDir));
                    break;
                } else if (Messages.getMessage(Installer.class, "promptAnswerNo").toLowerCase().startsWith(input.toLowerCase())) {
                    System.exit(0);
                } else {
                    System.out.println(Messages.getMessage(Installer.class, "reprompt"));
                }
            }
        }
        String installationSourceDir = ".";
        installationSourceDir = new File(Installer.class.getProtectionDomain().getCodeSource().getLocation()
                .toURI()).getParent();
        logger.debug("going to create " + installationDir);
        createDir(installationDir);
        logger.debug("going to copy contents of " + installationSourceDir + " to " + installationDir);
        
        FileUtils.copyDirectory(new File(installationSourceDir), new File(installationDir));
        
        logger.debug("going to delete \\.* files from " + installationDir);
        deleteFilesFromDir(installationDir, "\\.*");
        logger.debug("going to delete install.* files from " + installationDir);
        deleteFilesFromDir(installationDir, "install.(.*)");
        logger.debug("going to delete META-INF from " + installationDir);
        deleteFilesFromDir(installationDir, "META-INF");
        logger.debug("going to delete zip files from " + installationDir);
        deleteFilesFromDir(installationDir, ".*.zip");
    }
    
    private static String promptAndGetUserInput(String prompt) throws IOException {
        if (prompt == null || prompt.isBlank()) {
            prompt = "Provide input: ";
        }
        System.out.print(prompt);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input = "";
        // Reading data using readLine
        input = reader.readLine();
        return input;
    }
    
    private static void deleteFilesFromDir(String directoryName, String filePattern) throws IOException {
        File directory = new File(directoryName);
        if (!directory.exists()) {
            return;
        }
        final File[] files = directory.listFiles( new FilenameFilter() {
            @Override
            public boolean accept( final File dir,
                                   final String name ) {
                boolean match = name.matches(filePattern);
                return match;
            }
        } );
        for ( final File file : files ) {
            if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            } else if ( !file.delete() ) {
                logger.error("Can't remove " + file.getAbsolutePath());
            }
        }
    }
    
    interface ShortcutCreatorInterface {
        public void create() throws Exception;
    }
    
    private static void createShortcut(String prompt, ShortcutCreatorInterface shortcutCreator) {
        for (;;) {
            System.out.println("");
            String input = "";
            try {
                input = promptAndGetUserInput(prompt);
            } catch (IOException e) {
                System.err.println(Messages.getMessage(Installer.class, "responseReadError"));
                handleException(e, Level.ERROR);
            }
            if (Messages.getMessage(Installer.class, "promptAnswerYes").toLowerCase().startsWith(input.toLowerCase())) {
                try {
                    shortcutCreator.create();
                } catch (Exception ex) {
                    System.err.println(Messages.getMessage(Installer.class, "shortcutCreateError"));
                    handleException(ex, Level.ERROR);
                }
                break;
            } else if (Messages.getMessage(Installer.class, "promptAnswerNo").toLowerCase().startsWith(input.toLowerCase())) {
                return;                  
            } else {
                System.out.println(Messages.getMessage(Installer.class, "reprompt"));
            }
        }
    }
    
    private static void createDesktopShortcut(String installationDir) {
        final String PROMPT = Messages.getMessage(Installer.class, "createDesktopShortcutPrompt");
        if (AppUtil.isRunningOnWindows()) {
            createShortcut(PROMPT,
                    new ShortcutCreatorInterface() {
                        public void create() throws Exception {
                            createShortcutOnWindows(CREATE_DEKSTOP_SHORTCUT_ON_WINDOWS, installationDir);
                        }
            });
        } else if (AppUtil.isRunningOnMacOS()) {
            createShortcut(PROMPT,
                    new ShortcutCreatorInterface() {
                        public void create()  throws Exception {
                                createSymLink(USERHOME + "/Desktop/DataLoader " + AppUtil.DATALOADER_VERSION,
                                        installationDir + "/dataloader.app", true);
                        }
            });
        }
    }
    
    private static void createAppsDirShortcut(String installationDir) {
        final String PROMPT =  Messages.getMessage(Installer.class, "createApplicationsDirShortcutPrompt");

        if (AppUtil.isRunningOnMacOS()) {
            createShortcut(PROMPT,
                    new ShortcutCreatorInterface() {
                        public void create() throws Exception {
                            createSymLink("/Applications/DataLoader " + AppUtil.DATALOADER_VERSION,
                                    installationDir + "/dataloader.app", true);
                        }
            });
        }
    }
    
    private static void createStartMenuShortcut(String installationDir) {
        final String PROMPT = Messages.getMessage(Installer.class, "createStartMenuShortcutPrompt");

        if (AppUtil.isRunningOnWindows()) {
            createShortcut(PROMPT,
                    new ShortcutCreatorInterface() {
                        public void create() throws Exception {
                            String APPDATA = System.getenv("APPDATA");
                            String SALESFORCE_START_MENU_DIR = APPDATA + "\\Microsoft\\Windows\\Start Menu\\Programs\\Salesforce\\" ;
                            createDir(SALESFORCE_START_MENU_DIR);
                            createShortcutOnWindows(CREATE_START_MENU_SHORTCUT_ON_WINDOWS, installationDir);
                        }
            });
        }
    }
    
    private static void createSymLink(String symlink, String target, boolean deleteExisting) throws IOException {
        Path symlinkPath = Paths.get(symlink);
        if (Files.exists(symlinkPath)) {
            if (deleteExisting) {
                logger.debug("Deleting existing symlink " + symlink);
                Files.delete(symlinkPath);
            } else {
                logger.debug("Symlink " + symlink + " exists. Skipping linking it to " + target);
                return;
            }
        }
        logger.debug("going to create symlink: " + symlink + " pointing to " + target);
        Files.createSymbolicLink(symlinkPath, Paths.get(target));
    }
    
    private static void createShortcutOnWindows(final String shortcutCommand, String installationDir) throws IOException, InterruptedException {
        String redirectWinCmdOutputStr = "";
        if (logger.getLevel() == Level.DEBUG) {
            redirectWinCmdOutputStr = " > debug.txt 2>&1";
        }
        String command = "cmd /c call \"" + installationDir + "\\util\\util.bat\"" 
                + " " + shortcutCommand + " \"" + installationDir + "\"" + redirectWinCmdOutputStr;
        logger.debug("going to execute windows command: ");
        logger.debug(command);
        Process p = Runtime.getRuntime().exec(command);
        int exitVal = p.waitFor();
        logger.debug("windows command exited with exit code: " + exitVal);
    }
    
    private static void configureOSSpecificInstallationArtifactsPostCopy(String installationDir) throws IOException {
        if (AppUtil.isRunningOnWindows()) {
            configureWindowsArtifactsPostCopy(installationDir);
        } else if (AppUtil.isRunningOnMacOS()) {
            configureMacOSArtifactsPostCopy(installationDir);
        } else if (AppUtil.isRunningOnLinux()) {
            configureLinuxArtifactsPostCopy(installationDir);
        }
    }
    
    private static void configureMacOSArtifactsPostCopy(String installationDir) throws IOException {
        final String MACOS_PACKAGE_BASE = installationDir + "/dataloader.app/Contents";
        final String PATH_TO_DL_EXECUTABLE_ON_MAC = MACOS_PACKAGE_BASE + "/MacOS/dataloader";
 
        // delete unnecessary artifacts
        logger.debug("going to delete dataloader.ico from " + installationDir);
        deleteFilesFromDir(installationDir + "/util", "(.*).bat");

        // create a soft link from <INSTALLATION_ABSOLUTE_PATH>/dataloader.app/Contents/MacOS/dataloader to 
        // <INSTALLATION_ABSOLUTE_PATH>/dataloader_console
        logger.debug("going to create symlink from " 
                    + 
                    PATH_TO_DL_EXECUTABLE_ON_MAC
                    + " to "
                    + installationDir + "/dataloader_console");
        logger.debug("going to create " + MACOS_PACKAGE_BASE + "/MacOS");
        createDir(MACOS_PACKAGE_BASE + "/MacOS");
        createSymLink(PATH_TO_DL_EXECUTABLE_ON_MAC,
                installationDir + "/dataloader_console", true);
    }
    
    private static void configureWindowsArtifactsPostCopy(String installationDir) throws IOException {
        deleteFilesFromDir(installationDir + "/util", "(.*).sh");
    }
    
    private static void configureLinuxArtifactsPostCopy(String installationDir) throws IOException {
        try {
            if (Files.exists(Paths.get(installationDir + "/dataloader_console"))) {
                Files.move(Paths.get(installationDir + "/dataloader_console"),
                    Paths.get(installationDir + "/dataloader.sh"));
            }
        } catch (InvalidPathException ex) {
            // do nothing - dataloader_console not found in the path
        }
        deleteFilesFromDir(installationDir + "/util", "(.*).bat");
    }

    private static void createDir(String dirPath) throws IOException {
        Files.createDirectories(Paths.get(dirPath));
    }

    public static void extractInstallationArtifactsFromJar(String installationDir) throws URISyntaxException, IOException {
        setLogger();
        AppUtil.extractDirFromJar("samples", installationDir, false);
        AppUtil.extractDirFromJar("configs", installationDir, false);
        String osSpecificExtractionPrefix = "mac/";
        if (AppUtil.isRunningOnWindows()) {
            osSpecificExtractionPrefix = "win/";
        } else if (AppUtil.isRunningOnLinux()) {
            osSpecificExtractionPrefix = "linux/";
        }
        AppUtil.extractDirFromJar(osSpecificExtractionPrefix, installationDir, true);
        configureOSSpecificInstallationArtifactsPostCopy(installationDir);
    }
    
    private static void handleException(Throwable ex, Level level) {
        if (logger != null) {
            logger.log(level, "Installer :", ex);
        } else {
            ex.printStackTrace();
        }
    }

    private static void setLogger() {
        if (logger == null) {
            logger = LogManager.getLogger(Installer.class);
        }
    }
}