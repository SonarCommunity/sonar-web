/*
 * SonarSource HTML analyzer :: Sonar Plugin
 * Copyright (c) 2010-2024 SonarSource SA and Matthijs Galesloot
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
package org.sonar.plugins.html.checks.accessibility;

import java.util.Locale;
import org.sonar.check.Rule;
import org.sonar.plugins.html.checks.AbstractPageCheck;
import org.sonar.plugins.html.node.TagNode;

import static org.sonar.plugins.html.api.HtmlConstants.INTERACTIVE_ELEMENTS;
import static org.sonar.plugins.html.api.HtmlConstants.KNOWN_HTML_TAGS;
import static org.sonar.plugins.html.api.HtmlConstants.NON_INTERACTIVE_ROLES;
import static org.sonar.plugins.html.api.HtmlConstants.PRESENTATION_ROLES;

@Rule(key = "S6843")
public class NoInteractiveElementToNoninteractiveRoleCheck extends AbstractPageCheck {

  private static final String MESSAGE = "Interactive elements should not be assigned non-interactive roles.";

  @Override
  public void startElement(TagNode node) {
    var role = node.getPropertyValue("role");
    var tagName = node.getNodeName().toLowerCase(Locale.ROOT);
    if (
      role != null &&
      KNOWN_HTML_TAGS.contains(tagName) &&
      INTERACTIVE_ELEMENTS.contains(tagName) &&
      (NON_INTERACTIVE_ROLES.contains(role) || PRESENTATION_ROLES.contains(role))
    ) {
      createViolation(node, MESSAGE);
    }
  }

}