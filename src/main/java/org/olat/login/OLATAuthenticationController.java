/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.login;

import java.util.List;
import java.util.Locale;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.id.Identity;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.logging.OLATSecurityException;
import org.olat.core.util.StringHelper;
import org.olat.core.util.UserSession;
import org.olat.core.util.Util;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.i18n.I18nManager;
import org.olat.login.auth.AuthenticationController;
import org.olat.login.auth.OLATAuthManager;
import org.olat.login.auth.OLATAuthentcationForm;
import org.olat.registration.DisclaimerController;
import org.olat.registration.PwChangeController;
import org.olat.registration.RegistrationController;
import org.olat.registration.RegistrationManager;
import org.olat.registration.RegistrationModule;
import org.olat.user.UserModule;

/**
 * Initial Date:  04.08.2004
 *
 * @author Mike Stock
 */
public class OLATAuthenticationController extends AuthenticationController implements Activateable2 {

	public static final String PARAM_LOGINERROR = "loginerror";
	
	private VelocityContainer loginComp;
	private OLATAuthentcationForm loginForm;
	private Identity authenticatedIdentity;
	private Controller subController;
	private DisclaimerController disclaimerCtr;

	private CloseableModalController cmc;

	private Link pwLink;
	private Link registerLink;
	
	private final OLATAuthManager olatAuthenticationSpi;
	
	/**
	 * @see org.olat.login.auth.AuthenticationController#init(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	public OLATAuthenticationController(UserRequest ureq, WindowControl winControl) {
		// use fallback translator to registration module
		super(ureq, winControl, Util.createPackageTranslator(RegistrationManager.class, ureq.getLocale()));

		olatAuthenticationSpi = CoreSpringFactory.getImpl(OLATAuthManager.class);
		
		loginComp = createVelocityContainer("olat_log", "olatlogin");
		
		if(UserModule.isPwdchangeallowed(null)) {
			pwLink = LinkFactory.createLink("_olat_login_change_pwd", "menu.pw", loginComp, this);
			pwLink.setElementCssClass("o_login_pwd");
		}
		
		if (CoreSpringFactory.getImpl(RegistrationModule.class).isSelfRegistrationEnabled()
				&& CoreSpringFactory.getImpl(RegistrationModule.class).isSelfRegistrationLoginEnabled()) {
			registerLink = LinkFactory.createLink("_olat_login_register", "menu.register", loginComp, this);
			registerLink.setElementCssClass("o_login_register");
			registerLink.setTitle("menu.register.alt");
		}
		
		// prepare login form
		loginForm = new OLATAuthentcationForm(ureq, winControl, "olat_login", getTranslator());
		listenTo(loginForm);
		
		loginComp.put("loginForm",loginForm.getInitialComponent());
		
		// Check if form is triggered by external loginworkflow that has been failed
		if (ureq.getParameterSet().contains(PARAM_LOGINERROR)) {
			showError(translate("login.error", WebappHelper.getMailConfig("mailReplyTo")));
		}

		putInitialPanel(loginComp);
	}

	/**
	 * @see org.olat.login.auth.AuthenticationController#changeLocale(java.util.Locale)
	 */
	public void changeLocale(Locale newLocale) {
		setLocale(newLocale, true);
	}
	
	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == registerLink) {
			openRegistration(ureq);
		} else if (source == pwLink) {
			openChangePassword(ureq, null);
		}
	}
	
	protected RegistrationController openRegistration(UserRequest ureq) {
		removeAsListenerAndDispose(subController);
		subController = new RegistrationController(ureq, getWindowControl());
		listenTo(subController);
		
		removeAsListenerAndDispose(cmc);
		cmc = new CloseableModalController(getWindowControl(), translate("close"), subController.getInitialComponent());
		listenTo(cmc);

		cmc.activate();
		return (RegistrationController)subController;
	}
	
	protected void openChangePassword(UserRequest ureq, String initialEmail) {
		// double-check if allowed first
		if (!UserModule.isPwdchangeallowed(ureq.getIdentity())) throw new OLATSecurityException("chose password to be changed, but disallowed by config");
		
		removeAsListenerAndDispose(subController);
		subController = new PwChangeController(ureq, getWindowControl(), initialEmail, true);
		listenTo(subController);
		
		removeAsListenerAndDispose(cmc);
		cmc = new CloseableModalController(getWindowControl(), translate("close"), subController.getInitialComponent());
		listenTo(cmc);
		
		cmc.activate();
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		if (source == loginForm && event == Event.DONE_EVENT) {
			String login = loginForm.getLogin();
			String pass = loginForm.getPass();	
			if (LoginModule.isLoginBlocked(login)) {
				// do not proceed when blocked
				showError("login.blocked", LoginModule.getAttackPreventionTimeoutMin().toString());
				getLogger().audit("Login attempt on already blocked login for " + login + ". IP::" + ureq.getHttpReq().getRemoteAddr(), null);
				return;
			}
			authenticatedIdentity = olatAuthenticationSpi.authenticate(null, login, pass);
			if (authenticatedIdentity == null) {
				if (LoginModule.registerFailedLoginAttempt(login)) {
					getLogger().audit("Too many failed login attempts for " + login + ". Login blocked. IP::" + ureq.getHttpReq().getRemoteAddr(), null);
					showError("login.blocked", LoginModule.getAttackPreventionTimeoutMin().toString());
					return;
				} else {
					showError("login.error", WebappHelper.getMailConfig("mailReplyTo"));
					return;
				}
			} else {
				try {
					String language = authenticatedIdentity.getUser().getPreferences().getLanguage();
					UserSession usess = ureq.getUserSession();
					if(StringHelper.containsNonWhitespace(language)) {
						usess.setLocale(I18nManager.getInstance().getLocaleOrDefault(language));
					}
				} catch (Exception e) {
					logError("Cannot set the user language", e);
				}
			}
			
			LoginModule.clearFailedLoginAttempts(login);
			
			// Check if disclaimer has been accepted
			if (RegistrationManager.getInstance().needsToConfirmDisclaimer(authenticatedIdentity)) {
				// accept disclaimer first
				
				removeAsListenerAndDispose(disclaimerCtr);
				disclaimerCtr = new DisclaimerController(ureq, getWindowControl());
				listenTo(disclaimerCtr);
				
				removeAsListenerAndDispose(cmc);
				cmc = new CloseableModalController(getWindowControl(), translate("close"), disclaimerCtr.getInitialComponent());
				listenTo(cmc);
				
				cmc.activate();	
				
			} else {
				// disclaimer acceptance not required		
				authenticated(ureq, authenticatedIdentity);	
			}
		} else if (source == disclaimerCtr) {
			cmc.deactivate();
			if (event == Event.DONE_EVENT) {
				// disclaimer accepted 
				RegistrationManager.getInstance().setHasConfirmedDislaimer(authenticatedIdentity);
				authenticated(ureq, authenticatedIdentity);
			}
		} else if(cmc == source) {
			cleanUp();
		} if (source == subController) {
			if(event == Event.CANCELLED_EVENT) {
				cmc.deactivate();
				cleanUp();
			}
		}
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(subController);
		removeAsListenerAndDispose(cmc);
		subController = null;
		cmc = null;
	}
	
	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(entries == null || entries.isEmpty()) return;
		
		ContextEntry entry = entries.get(0);
		String type = entry.getOLATResourceable().getResourceableTypeName();
		if("changepw".equals(type)) {
			String email = null;
			if(entries.size() > 1) {
				email = entries.get(1).getOLATResourceable().getResourceableTypeName();
			}
			openChangePassword(ureq, email);
		} else if("registration".equals(type)) {
			if (CoreSpringFactory.getImpl(RegistrationModule.class).isSelfRegistrationEnabled()
					&& CoreSpringFactory.getImpl(RegistrationModule.class).isSelfRegistrationLinkEnabled()) {
				List<ContextEntry> subEntries = entries.subList(1, entries.size());
				openRegistration(ureq).activate(ureq, subEntries, entry.getTransientState());
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}
}
