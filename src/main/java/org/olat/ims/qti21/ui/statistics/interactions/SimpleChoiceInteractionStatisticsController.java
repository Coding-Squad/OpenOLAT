/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.ims.qti21.ui.statistics.interactions;

import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.ims.qti.statistics.model.StatisticsItem;
import org.olat.ims.qti21.model.statistics.ChoiceStatistics;
import org.olat.ims.qti21.model.xml.AssessmentHtmlBuilder;
import org.olat.ims.qti21.ui.statistics.QTI21StatisticResourceResult;

import uk.ac.ed.ph.jqtiplus.node.item.AssessmentItem;
import uk.ac.ed.ph.jqtiplus.node.item.interaction.ChoiceInteraction;
import uk.ac.ed.ph.jqtiplus.node.item.interaction.choice.Choice;
import uk.ac.ed.ph.jqtiplus.node.item.interaction.choice.SimpleChoice;
import uk.ac.ed.ph.jqtiplus.node.test.AssessmentItemRef;

/**
 * 
 * Initial date: 22 mars 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class SimpleChoiceInteractionStatisticsController extends ChoiceInteractionStatisticsController {
	
	public SimpleChoiceInteractionStatisticsController(UserRequest ureq, WindowControl wControl,
			AssessmentItemRef itemRef, AssessmentItem assessmentItem, ChoiceInteraction interaction,
			StatisticsItem itemStats, QTI21StatisticResourceResult resourceResult) {
		super(ureq, wControl, itemRef, assessmentItem, interaction, itemStats, resourceResult);
	}


	@Override
	protected List<ChoiceStatistics> getChoiceInteractionStatistics() {
		return qtiStatisticsManager
				.getChoiceInteractionStatistics(itemRef.getIdentifier().toString(), assessmentItem, (ChoiceInteraction)interaction, resourceResult.getSearchParams());
	}

	@Override
	protected String getAnswerText(Choice choice) {
		String text = choice.getLabel();
		if(!StringHelper.containsNonWhitespace(text)) {
			text = new AssessmentHtmlBuilder().flowStaticString(((SimpleChoice)choice).getFlowStatics());
			text = Formatter.formatLatexFormulas(text);
		}
		return text;
	}
}
