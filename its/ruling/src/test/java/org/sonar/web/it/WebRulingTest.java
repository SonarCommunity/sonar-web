/*
 * SonarSource :: HTML :: ITs :: Ruling
 * Copyright (c) 2013-2023 SonarSource SA and Matthijs Galesloot
 * sonarqube@googlegroups.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sonar.web.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarsource.analyzer.commons.ProfileGenerator;
import org.sonarqube.ws.Qualityprofiles;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.qualityprofiles.ActivateRuleRequest;
import org.sonarqube.ws.client.qualityprofiles.SearchRequest;
import org.sonarqube.ws.client.rules.CreateRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class WebRulingTest {

  private static final String LANGUAGE = "web";
  private static final String REPOSITORY_KEY = "Web";

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .useDefaultAdminCredentialsForBuilds(true)
    .setSonarVersion(Optional.ofNullable(System.getProperty("sonar.runtimeVersion")).orElse("LATEST_RELEASE"))
    .addPlugin(FileLocation.byWildcardMavenFilename(new File("../../sonar-html-plugin/target"), "sonar-html-plugin-*.jar"))
    .addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", "0.10.0.2181"))
    .build();

  @BeforeClass
  public static void prepare_quality_profiles() {
    File profile = ProfileGenerator.generateProfile(orchestrator.getServer().getUrl(), LANGUAGE, REPOSITORY_KEY,
      new ProfileGenerator.RulesConfiguration(), Collections.emptySet());
    orchestrator.getServer().restoreProfile(FileLocation.of(profile));
    instantiateTemplateRule("IllegalAttributeCheck", "Template_DoNotUseNameProperty", "attributes=\"name\"");
  }

  @Test
  public void ruling() throws Exception {
    File litsDifferencesFile = FileLocation.of("target/differences").getFile();
    String projectKey = "project";
    orchestrator.getServer().provisionProject(projectKey, projectKey);
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, LANGUAGE, "rules");
    SonarScanner build = SonarScanner.create()
      .setProjectDir(FileLocation.of("../sources").getFile())
      .setProjectKey(projectKey)
      .setProjectName(projectKey)
      .setProjectVersion("1")
      .setSourceDirs(".")
      .setSourceEncoding("UTF-8")
      .setProperty("sonar.html.file.suffixes", "xhtml,html,php,erb")
      .setProperty("sonar.jsp.file.suffixes", "jspf,jsp")
      .setProperty("sonar.lits.dump.old", FileLocation.of("src/test/resources/expected").getFile().getAbsolutePath())
      .setProperty("sonar.lits.dump.new", FileLocation.of("target/actual").getFile().getAbsolutePath())
      .setProperty("sonar.lits.differences", litsDifferencesFile.getAbsolutePath())
      .setProperty("sonar.exclusions", "external_webkit-jb-mr1/LayoutTests/fast/encoding/*utf*")
      .setProperty("sonar.cpd.exclusions", "**/*")
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx1024m");

    // To prevent adding error or exception that may be unseen in the logs
    BuildResult result = orchestrator.executeBuild(build);
    List<String> errorList = result.getLogs().lines().filter(line -> line.startsWith("ERROR")).collect(Collectors.toList());
    assertThat(errorList).hasSize(0);

    String differences = Files.readString(litsDifferencesFile.toPath());
    assertThat(differences).isEmpty();
  }

  private static void instantiateTemplateRule(String ruleTemplateKey, String instantiationKey, String params) {
    newAdminWsClient(orchestrator)
      .rules()
      .create(
        new CreateRequest()
          .setName(instantiationKey)
          .setMarkdownDescription(instantiationKey)
          .setSeverity("INFO")
          .setStatus("READY")
          .setTemplateKey(REPOSITORY_KEY + ":" + ruleTemplateKey)
          .setCustomKey(instantiationKey)
          .setPreventReactivation("true")
          .setParams(
            Arrays.asList(
              (
                "name=\"" +
                instantiationKey +
                "\";key=\"" +
                instantiationKey +
                "\";markdown_description=\"" +
                instantiationKey +
                "\";" +
                params
              ).split(";", 0)
            )
          )
      );

    String profileKey = newAdminWsClient(orchestrator)
      .qualityprofiles()
      .search(new SearchRequest().setLanguage(LANGUAGE))
      .getProfilesList()
      .stream()
      .filter(qp -> "rules".equals(qp.getName()))
      .map(Qualityprofiles.SearchWsResponse.QualityProfile::getKey)
      .findFirst()
      .orElse(null);

    if (!StringUtils.isEmpty(profileKey)) {
      newAdminWsClient(orchestrator)
        .qualityprofiles()
        .activateRule(
          new ActivateRuleRequest()
            .setKey(profileKey)
            .setRule(REPOSITORY_KEY + ":" + instantiationKey)
            .setSeverity("INFO")
            .setParams(Collections.emptyList())
        );
    } else {
      throw new IllegalStateException(
        "Could not retrieve profile key : Template rule " +
        ruleTemplateKey +
        " has not been activated"
      );
    }
  }

  static WsClient newAdminWsClient(Orchestrator orchestrator) {
    return WsClientFactories
      .getDefault()
      .newClient(
        HttpConnector
          .newBuilder()
          .credentials(Server.ADMIN_LOGIN, Server.ADMIN_PASSWORD)
          .url(orchestrator.getServer().getUrl())
          .build()
      );
  }
}
