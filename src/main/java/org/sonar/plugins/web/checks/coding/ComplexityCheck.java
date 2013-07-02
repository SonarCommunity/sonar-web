/*
 * Sonar Web Plugin
 * Copyright (C) 2010 SonarSource and Matthijs Galesloot
 * dev@sonar.codehaus.org
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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.web.checks.AbstractPageCheck;
import org.sonar.plugins.web.node.Attribute;
import org.sonar.plugins.web.node.Node;
import org.sonar.plugins.web.node.TagNode;

import java.util.List;

@Rule(
  key = "ComplexityCheck",
  priority = Priority.MINOR)
public final class ComplexityCheck extends AbstractPageCheck {

  private static final int DEFAULT_MAX_COMPLEXITY = 10;
  private static final String DEFAULT_OPERATORS = "&&,||,and,or";
  private static final String DEFAULT_TAGS = "catch,choose,if,forEach,forTokens,when";

  @RuleProperty(
    key = "max",
    defaultValue = "" + DEFAULT_MAX_COMPLEXITY)
  public int max = DEFAULT_MAX_COMPLEXITY;

  @RuleProperty(
    key = "operators",
    defaultValue = DEFAULT_OPERATORS)
  public String operators = DEFAULT_OPERATORS;

  @RuleProperty(
    key = "tags",
    defaultValue = DEFAULT_TAGS)
  public String tags = DEFAULT_TAGS;

  private int complexity;
  private String[] operatorsArray;
  private String[] tagsArray;

  @Override
  public void startDocument(List<Node> nodes) {
    complexity = 1;
    operatorsArray = trimSplitCommaSeparatedList(operators);
    tagsArray = trimSplitCommaSeparatedList(tags);
  }

  @Override
  public void endDocument() {
    super.endDocument();

    if (complexity > max) {
      String msg = String.format("Complexity is %d (max allowed is %d)", complexity, max);
      createViolation(0, msg);
    }

    getWebSourceCode().addMeasure(CoreMetrics.COMPLEXITY, complexity);
  }

  @Override
  public void startElement(TagNode node) {

    // count jstl tags
    if (ArrayUtils.contains(tagsArray, node.getLocalName()) || ArrayUtils.contains(tagsArray, node.getNodeName())) {
      complexity++;
    } else {
      // count complexity in expressions
      for (Attribute a : node.getAttributes()) {
        if (isUnifiedExpression(a.getValue())) {
          String[] tokens = StringUtils.split(a.getValue(), " \t\n");

          for (String token : tokens) {
            if (ArrayUtils.contains(operatorsArray, token)) {
              complexity++;
            }
          }
        }
      }
    }
  }

}
