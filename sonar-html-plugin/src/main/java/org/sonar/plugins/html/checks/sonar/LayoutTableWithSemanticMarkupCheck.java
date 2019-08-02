/*
 * SonarHTML :: SonarQube Plugin
 * Copyright (c) 2010-2019 SonarSource SA and Matthijs Galesloot
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
package org.sonar.plugins.html.checks.sonar;

import static java.lang.String.format;

import java.util.Optional;

import org.sonar.check.Rule;
import org.sonar.plugins.html.checks.AbstractPageCheck;
import org.sonar.plugins.html.node.Attribute;
import org.sonar.plugins.html.node.TagNode;

@Rule(key = "S5258")
public class LayoutTableWithSemanticMarkupCheck extends AbstractPageCheck {

  private boolean isWithinLayoutTable = false;

  @Override
  public void startElement(TagNode node) {
    if (isTable(node) && isLayout(node)) {
      findAttribute(node, "SUMMARY").ifPresent(a -> createViolation(a.getLine(), format("Remove this \"%s\" attribute", a.getName())));
      isWithinLayoutTable = true;
    }
    if (isWithinLayoutTable) {
      if (isCaption(node) || isTableHeader(node)) {
        createViolation(node.getStartLinePosition(), format("Remove this \"%s\" element", node.getNodeName()));
      }
      if (isTableColumn(node)) {
        findAttribute(node, "HEADERS").ifPresent(a -> createViolation(a.getLine(), format("Remove this \"%s\" attribute", a.getName())));
        findAttribute(node, "SCOPE").ifPresent(a -> createViolation(a.getLine(), format("Remove this \"%s\" attribute", a.getName())));
      }
    }
  }

  @Override
  public void endElement(TagNode node) {
    if (isTable(node) && isWithinLayoutTable) {
      isWithinLayoutTable = false;
    }
  }

  private static boolean isTable(TagNode node) {
    return "TABLE".equalsIgnoreCase(node.getNodeName());
  }

  private static boolean isCaption(TagNode node) {
    return "CAPTION".equalsIgnoreCase(node.getNodeName());
  }

  private static boolean isTableHeader(TagNode node) {
    return "TH".equalsIgnoreCase(node.getNodeName());
  }

  private static boolean isTableColumn(TagNode node) {
    return "TD".equalsIgnoreCase(node.getNodeName());
  }

  private static boolean isLayout(TagNode node) {
    String role = node.getAttribute("role");
    return role != null && ("PRESENTATION".equalsIgnoreCase(role) || "NONE".equalsIgnoreCase(role));
  }

  private static Optional<Attribute> findAttribute(TagNode node, String attributeName) {
    return node.getAttributes().stream().filter(a -> attributeName.equalsIgnoreCase(a.getName())).findAny();
  }
}