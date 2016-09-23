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
package org.sonar.plugins.web.checks.coding;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.web.checks.AbstractPageCheck;
import org.sonar.plugins.web.checks.RuleTags;
import org.sonar.plugins.web.node.Node;
import org.sonar.squidbridge.annotations.SqaleLinearWithOffsetRemediation;

import java.util.List;

@Rule(
  key = "ComplexityCheck",
  name = "Files should not be too complex",
  priority = Priority.MAJOR,
  tags = {RuleTags.BRAIN_OVERLOADED})
@SqaleLinearWithOffsetRemediation(coeff = "1min", offset = "30min", effortToFixDescription = "per complexity point above the threshold")
public final class ComplexityCheck extends AbstractPageCheck {

  private static final int DEFAULT_MAX_COMPLEXITY = 10;

  @RuleProperty(
    key = "max",
    description = "Maximum allowed complexity",
    defaultValue = "" + DEFAULT_MAX_COMPLEXITY)
  public int max = DEFAULT_MAX_COMPLEXITY;

  @Override
  public void startDocument(List<Node> nodes) {
    int complexity = getWebSourceCode().getMeasure(CoreMetrics.COMPLEXITY);

    if (complexity > max) {
      String msg = String.format("Split this file to reduce complexity per file from %d to no more than the %d authorized.", complexity, max);
      createViolation(0, msg, (double) (complexity - max));
    }
  }

}
