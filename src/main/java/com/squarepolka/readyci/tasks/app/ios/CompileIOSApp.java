package com.squarepolka.readyci.tasks.app.ios;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;
import com.squarepolka.readyci.taskrunner.BuildEnvironment;
import com.squarepolka.readyci.tasks.Task;
import com.squarepolka.readyci.tasks.TaskExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


@Component
public class CompileIOSApp extends Task {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompileIOSApp.class);

    public String taskIdentifier() {
        return "ios_compile_app";
    }

    public String description() {
        return "Compile iOS app";
    }

    public void performTask(BuildEnvironment buildEnvironment) {
        try {
            String relativeProfilePath = parameters.get("profilePath");
            String profilePath = String.format("%s/%s", buildEnvironment.buildPath, relativeProfilePath);
            String exportOptionsPath = String.format("%s/exportOptions.plist", buildEnvironment.buildPath);

            IOSProvisioningProperties profileProperties = readProvisioningProfile(buildEnvironment, profilePath);
            LOGGER.info(String.format("BUILDING %s for %s in team %s with identifier %s and profile %s",
                    profileProperties.appName,
                    profileProperties.organisationName,
                    profileProperties.devTeam,
                    profileProperties.bundleId,
                    relativeProfilePath));


            LOGGER.info(String.format("Installing the provisioning profile %s", profilePath));
            executeCommand(String.format("/usr/bin/open %s", profilePath));

            createExportOptionsFile(profileProperties.devTeam, profileProperties.bundleId, profileProperties.appName, exportOptionsPath);

        } catch (Exception e) {
            TaskExecuteException taskExecuteException = new TaskExecuteException(String.format("Exception while performing task %s %s", taskIdentifier(), e.toString()));
            taskExecuteException.setStackTrace(e.getStackTrace());
            throw taskExecuteException;
        }
    }

    private IOSProvisioningProperties readProvisioningProfile(BuildEnvironment buildEnvironment, String profilePath) throws Exception {
        InputStream provisioningFileInputStream = decryptProvisioningFile(profilePath);
        return readProvisioningInputStream(provisioningFileInputStream);
    }

    private InputStream decryptProvisioningFile(String profilePath) {
        LOGGER.debug(String.format("Parsing the provisioning profile %s", profilePath));
        return executeCommandWithOutput(String.format("/usr/bin/security cms -D -i %s", profilePath));
    }

    private IOSProvisioningProperties readProvisioningInputStream(InputStream processInputSteam) throws Exception {
        IOSProvisioningProperties iosProvisioningProperties = new IOSProvisioningProperties();
        NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(processInputSteam);
        iosProvisioningProperties.appName = rootDict.objectForKey("Name").toString();
        iosProvisioningProperties.organisationName = rootDict.objectForKey("TeamName").toString();
        NSArray appIdPrefixs = (NSArray) rootDict.objectForKey("ApplicationIdentifierPrefix");
        iosProvisioningProperties.devTeam = appIdPrefixs.lastObject().toString();
        iosProvisioningProperties.provisioningProfile = rootDict.objectForKey("UUID").toString();
        NSDictionary entitlementsDict = (NSDictionary) rootDict.objectForKey("Entitlements");
        String fullBundleId = entitlementsDict.objectForKey("application-identifier").toString();
        iosProvisioningProperties.bundleId = removeTeamFromBundleId(fullBundleId, iosProvisioningProperties.devTeam);
        return iosProvisioningProperties;
    }

    private String removeTeamFromBundleId(String bundleId, String teamId) {
        return bundleId.replace(String.format("%s.", teamId), "");
    }

    private void createExportOptionsFile(String devTeam, String bundleId, String appName, String exportOptionsPath) throws IOException {
        NSDictionary rootDict = new NSDictionary();
        rootDict.put("compileBitcode", true);
        rootDict.put("stripSwiftSymbols", true);
        rootDict.put("method","ad-hoc");
        rootDict.put("signingCertificate","manual");
        rootDict.put("thinning","<none>");
        rootDict.put("teamID",devTeam);
        NSDictionary provisioningProfilesDict = new NSDictionary();
        provisioningProfilesDict.put(bundleId, appName);
        rootDict.put("provisioningProfiles", provisioningProfilesDict);

        File exportOptionsFile = new File(exportOptionsPath);
        PropertyListParser.saveAsXML(rootDict, exportOptionsFile);
    }
}