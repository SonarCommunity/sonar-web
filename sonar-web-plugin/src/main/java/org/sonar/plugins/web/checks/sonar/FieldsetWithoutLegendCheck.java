/*
 * SonarSource :: Web :: Sonar Plugin
 * Copyright (c) 2010-2016 SonarSource SA and Matthijs Galesloot
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
package org.sonar.plugins.web.checks.sonar;

import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.web.checks.AbstractPageCheck;
import org.sonar.plugins.web.checks.RuleTags;
import org.sonar.plugins.web.node.TagNode;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;

@Rule(
  key = "FieldsetWithoutLegendCheck",
  name = "\"fieldset\" tags should contain a \"legend\"",
  priority = Priority.MAJOR,
  tags = {RuleTags.ACCESSIBILITY})
@SqaleConstantRemediation("5min")
public class FieldsetWithoutLegendCheck extends AbstractPageCheck {

  private int fieldsetLine = 0;
  private boolean foundLegend;

  @Override
  public void startElement(TagNode node) {
    if (isFieldSet(node)) {
      foundLegend = false;
      fieldsetLine = node.getStartLinePosition();
    } else if (isLegend(node)) {
      foundLegend = true;
    }
  }

  @Override
  public void endElement(TagNode node) {
    if (isFieldSet(node)) {
      if (!foundLegend && fieldsetLine != 0) {
        createViolation(fieldsetLine, "Add a <legend> tag to this fieldset.");
      }

      foundLegend = false;
      fieldsetLine = 0;
    }
  }

  private static boolean isFieldSet(TagNode node) {
    return "FIELDSET".equalsIgnoreCase(node.getNodeName());
  }

  private static boolean isLegend(TagNode node) {
    return "LEGEND".equalsIgnoreCase(node.getNodeName());
  }

}
