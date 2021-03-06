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
package org.olat.modules.curriculum.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableElement;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelFactory;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableSearchEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SelectionEvent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.stack.TooledController;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.components.stack.TooledStackedPanel.Align;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableCalloutWindowController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.util.resource.OresHelper;
import org.olat.modules.curriculum.Curriculum;
import org.olat.modules.curriculum.CurriculumManagedFlag;
import org.olat.modules.curriculum.CurriculumSecurityCallback;
import org.olat.modules.curriculum.CurriculumService;
import org.olat.modules.curriculum.model.CurriculumInfos;
import org.olat.modules.curriculum.model.CurriculumSearchParameters;
import org.olat.modules.curriculum.ui.CurriculumManagerDataModel.CurriculumCols;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 12 févr. 2018<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CurriculumListManagerController extends FormBasicController implements TooledController {
	
	private FlexiTableElement tableEl;
	private Link newCurriculumButton;
	private CurriculumManagerDataModel tableModel;
	private final TooledStackedPanel toolbarPanel;
	
	private ToolsController toolsCtrl;
	private CloseableModalController cmc;
	private EditCurriculumController newCurriculumCtrl;
	private EditCurriculumOverviewController editCurriculumCtrl;
	private CloseableCalloutWindowController toolsCalloutCtrl;
	
	private int counter = 0;
	private final CurriculumSecurityCallback secCallback;

	@Autowired
	private CurriculumService curriculumService;
	
	public CurriculumListManagerController(UserRequest ureq, WindowControl wControl, TooledStackedPanel toolbarPanel,
			CurriculumSecurityCallback secCallback) {
		super(ureq, wControl, "manage_curriculum");
		this.toolbarPanel = toolbarPanel;
		this.secCallback = secCallback;

		initForm(ureq);
		loadModel(null, true);
	}
	
	@Override
	public void initTools() {
		if(secCallback.canNewCurriculum()) {
			newCurriculumButton = LinkFactory.createToolLink("add.curriculum", translate("add.curriculum"), this, "o_icon_add");
			newCurriculumButton.setElementCssClass("o_sel_add_curriculum");
			toolbarPanel.addTool(newCurriculumButton, Align.left);
		}
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		FlexiTableColumnModel columnsModel = FlexiTableDataModelFactory.createFlexiTableColumnModel();
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, CurriculumCols.key));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(CurriculumCols.displayName, "select"));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(CurriculumCols.identifier, "select"));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, CurriculumCols.externalId, "select"));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(CurriculumCols.numOfElements));
		DefaultFlexiColumnModel editCol = new DefaultFlexiColumnModel("edit.icon", translate("edit.icon"), "edit");
		editCol.setExportable(false);
		columnsModel.addFlexiColumnModel(editCol);
		if(secCallback.canEditCurriculum()) {
			DefaultFlexiColumnModel toolsCol = new DefaultFlexiColumnModel(CurriculumCols.tools);
			toolsCol.setExportable(false);
			toolsCol.setAlwaysVisible(true);
			columnsModel.addFlexiColumnModel(toolsCol);
		}
		
		tableModel = new CurriculumManagerDataModel(columnsModel, getLocale());
		tableEl = uifactory.addTableElement(getWindowControl(), "table", tableModel, 20, false, getTranslator(), formLayout);
		tableEl.setCustomizeColumns(true);
		tableEl.setSearchEnabled(true);
		tableEl.setEmtpyTableMessageKey("table.curriculum.empty");
		tableEl.setAndLoadPersistedPreferences(ureq, "cur-curriculum-manage");
	}
	
	private void loadModel(String searchString, boolean reset) {
		CurriculumSearchParameters params = new CurriculumSearchParameters();
		params.setSearchString(searchString);
		params.setManagerIdentity(getIdentity());
		List<CurriculumInfos> curriculums = curriculumService.getCurriculumsWithInfos(params);
		List<CurriculumRow> rows = curriculums.stream()
				.map(this::forgeRow).collect(Collectors.toList());
		tableModel.setObjects(rows);
		tableEl.reset(reset, reset, true);
	}
	
	private CurriculumRow forgeRow(CurriculumInfos curriculum) {
		FormLink toolsLink = uifactory.addFormLink("tools_" + (++counter), "tools", "", null, null, Link.NONTRANSLATED);
		toolsLink.setIconLeftCSS("o_icon o_icon_actions o_icon-lg");
		CurriculumRow row = new CurriculumRow(curriculum, toolsLink);
		toolsLink.setUserObject(row);
		return row;
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void formOK(UserRequest ureq) {
		//
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(newCurriculumCtrl == source) {
			if(event == Event.DONE_EVENT || event == Event.CHANGED_EVENT) {
				loadModel(tableEl.getQuickSearchString(), true);
			}
			cmc.deactivate();
			cleanUp();
		} else if(editCurriculumCtrl == source) {
			if(event == Event.DONE_EVENT || event == Event.CHANGED_EVENT) {
				loadModel(tableEl.getQuickSearchString(), false);
			}
			cmc.deactivate();
			cleanUp();
		} else if(cmc == source) {
			cleanUp();
		}
		super.event(ureq, source, event);
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(newCurriculumCtrl);
		removeAsListenerAndDispose(cmc);
		newCurriculumCtrl = null;
		cmc = null;
	}

	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if(newCurriculumButton == source) {
			doNewCurriculum(ureq);
		}
		super.event(ureq, source, event);
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(tableEl == source) {
			if(event instanceof SelectionEvent) {
				SelectionEvent se = (SelectionEvent)event;
				String cmd = se.getCommand();
				if("select".equals(cmd)) {
					CurriculumRow row = tableModel.getObject(se.getIndex());
					doSelectCurriculum(ureq, row);
				} else if("edit".equals(cmd)) {
					CurriculumRow row = tableModel.getObject(se.getIndex());
					doEditCurriculum(ureq, row);
				}
			} else if(event instanceof FlexiTableSearchEvent) {
				doSearch((FlexiTableSearchEvent)event);
			}
		} else if (source instanceof FormLink) {
			FormLink link = (FormLink)source;
			String cmd = link.getCmd();
			if("tools".equals(cmd)) {
				doOpenTools(ureq, (CurriculumRow)link.getUserObject(), link);
			} 
		}
		super.formInnerEvent(ureq, source, event);
	}
	
	private void doSearch(FlexiTableSearchEvent event) {
		loadModel(event.getSearch(), true);
	}
	
	private void doNewCurriculum(UserRequest ureq) {
		if(newCurriculumCtrl != null) return;

		newCurriculumCtrl = new EditCurriculumController(ureq, getWindowControl(), secCallback);
		listenTo(newCurriculumCtrl);
		
		cmc = new CloseableModalController(getWindowControl(), "close", newCurriculumCtrl.getInitialComponent(), true, translate("add.curriculum"));
		listenTo(cmc);
		cmc.activate();
	}
	
	private void doEditCurriculum(UserRequest ureq, CurriculumRow row) {
		removeAsListenerAndDispose(editCurriculumCtrl);
		
		Curriculum curriculum = curriculumService.getCurriculum(row);
		if(curriculum == null) {
			showWarning("warning.curriculum.deleted");
		} else {
			editCurriculumCtrl = new EditCurriculumOverviewController(ureq, getWindowControl(), curriculum, secCallback);
			listenTo(editCurriculumCtrl);
			toolbarPanel.pushController(row.getDisplayName(), editCurriculumCtrl);
		}
	}
	
	private void doSelectCurriculum(UserRequest ureq, CurriculumRow row) {
		Curriculum curriculum = curriculumService.getCurriculum(row);
		if(curriculum == null) {
			showWarning("warning.curriculum.deleted");
		} else {
			WindowControl swControl = addToHistory(ureq, OresHelper.createOLATResourceableInstance(Curriculum.class, row.getKey()), null);
			CurriculumComposerController composerCtrl = new CurriculumComposerController(ureq, swControl, toolbarPanel, curriculum, secCallback);
			listenTo(composerCtrl);
			toolbarPanel.pushController(row.getDisplayName(), composerCtrl);
		}
	}
	
	private void doOpenTools(UserRequest ureq, CurriculumRow row, FormLink link) {
		removeAsListenerAndDispose(toolsCtrl);
		removeAsListenerAndDispose(toolsCalloutCtrl);

		Curriculum curriculum = curriculumService.getCurriculum(row);
		if(curriculum == null) {
			tableEl.reloadData();
			showWarning("warning.curriculum.deleted");
		} else {
			toolsCtrl = new ToolsController(ureq, getWindowControl(), row, curriculum);
			listenTo(toolsCtrl);
	
			toolsCalloutCtrl = new CloseableCalloutWindowController(ureq, getWindowControl(),
					toolsCtrl.getInitialComponent(), link.getFormDispatchId(), "", true, "");
			listenTo(toolsCalloutCtrl);
			toolsCalloutCtrl.activate();
		}
	}
	
	private class ToolsController extends BasicController {
		
		private final VelocityContainer mainVC;
		private Link editLink, deleteLink;
		
		private CurriculumRow row;
		
		public ToolsController(UserRequest ureq, WindowControl wControl, CurriculumRow row, Curriculum curriculum) {
			super(ureq, wControl);
			this.row = row;
			
			mainVC = createVelocityContainer("tools");
			
			List<String> links = new ArrayList<>(4);
			
			//edit
			editLink = addLink("edit", "o_icon_edit", links);
			if(!CurriculumManagedFlag.isManaged(curriculum, CurriculumManagedFlag.delete)) {
				links.add("-");
				deleteLink = addLink("delete", "o_icon_delete_item", links);
			}
			mainVC.contextPut("links", links);
			
			putInitialPanel(mainVC);
		}
		
		private Link addLink(String name, String iconCss, List<String> links) {
			Link link = LinkFactory.createLink(name, name, getTranslator(), mainVC, this, Link.LINK);
			mainVC.put(name, link);
			links.add(name);
			link.setIconLeftCSS("o_icon " + iconCss);
			return link;
		}

		@Override
		protected void doDispose() {
			//
		}

		@Override
		protected void event(UserRequest ureq, Component source, Event event) {
			if(editLink == source) {
				close();
				doEditCurriculum(ureq, row);
			} else if(deleteLink == source) {
				close();
				showWarning("Not implemented");
			}
		}
		
		private void close() {
			toolsCalloutCtrl.deactivate();
			cleanUp();
		}
	}
}
