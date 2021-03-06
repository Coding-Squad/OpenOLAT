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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.olat.basesecurity.OrganisationRoles;
import org.olat.core.configuration.AbstractSpringModule;
import org.olat.core.id.Roles;
import org.olat.core.logging.OLog;
import org.olat.core.logging.StartupException;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Encoder;
import org.olat.core.util.StringHelper;
import org.olat.core.util.cache.CacheWrapper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.login.auth.AuthenticationProvider;
import org.olat.user.UserModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Initial Date:  04.08.2004
 *
 * @author Mike Stock
 * @author guido
 */
@Service("loginModule")
public class LoginModule extends AbstractSpringModule {
	
	private static final OLog log = Tracing.createLoggerFor(LoginModule.class);
	
	private static final OrganisationRoles[] policyRoles = new OrganisationRoles[] {
		OrganisationRoles.sysadmin, OrganisationRoles.administrator, OrganisationRoles.principal,
		OrganisationRoles.rolesmanager, OrganisationRoles.usermanager,
		OrganisationRoles.learnresourcemanager, OrganisationRoles.groupmanager,
		OrganisationRoles.lecturemanager, OrganisationRoles.qualitymanager,
		OrganisationRoles.poolmanager, OrganisationRoles.linemanager	
	};
	
	private static final String CHANGE_ONCE = "password.change.once";
	private static final String MAX_AGE = "password.max.age";
	private static final String MAX_AGE_AUTHOR = "password.max.age.author";
	private static final String MAX_AGE_GROUPMANAGER = "password.max.age.groupmanager";
	private static final String MAX_AGE_USERMANAGER = "password.max.age.usermanager";
	private static final String MAX_AGE_ROLESMANAGER = "password.max.age.rolesmanager";
	private static final String MAX_AGE_LEARNRESOURCEMANAGER = "password.max.age.learnresourcemanager";
	private static final String MAX_AGE_POOLMANAGER = "password.max.age.poolmanager";
	private static final String MAX_AGE_CURRICULUMMANAGER = "password.max.age.curriculummanager";
	private static final String MAX_AGE_LINEMANAGER = "password.max.age.linemanager";
	private static final String MAX_AGE_QUALITYMANAGER = "password.max.age.qualitymanager";
	private static final String MAX_AGE_LECTUREMANAGER = "password.max.age.lecturemanager";
	private static final String MAX_AGE_PRINCIPAL = "password.max.age.principal";
	private static final String MAX_AGE_ADMINISTRATOR = "password.max.age.administrator";
	private static final String MAX_AGE_SYSADMIN = "password.max.age.sysadmin";
	private static final String HISTORY = "password.history";

	@Autowired
	private List<AuthenticationProvider> authenticationProviders;
	
	@Value("${login.attackPreventionEnabled:true}")
	private boolean attackPreventionEnabled;
	@Value("${login.AttackPreventionMaxattempts:5}")
	private int attackPreventionMaxAttempts;
	@Value("${login.AttackPreventionTimeoutmin:5}")
	private int attackPreventionTimeout;
	
	@Value("${password.change.once:false}")
	private boolean passwordChangeOnce;
	
	@Value("${password.max.age}")
	private int passwordMaxAge;
	@Value("${password.max.age.author}")
	private int passwordMaxAgeAuthor;
	@Value("${password.max.age.groupmanager}")
	private int passwordMaxAgeGroupManager;
	@Value("${password.max.age.poolmanager}")
	private int passwordMaxAgePoolManager;
	@Value("${password.max.age.usermanager}")
	private int passwordMaxAgeUserManager;
	@Value("${password.max.age.rolesmanager}")
	private int passwordMaxAgeRolesManager;
	@Value("${password.max.age.learnresourcemanager}")
	private int passwordMaxAgeLearnResourceManager;
	@Value("${password.max.age.curriculummanager}")
	private int passwordMaxAgeCurriculumManager;
	@Value("${password.max.age.linemanager}")
	private int passwordMaxAgeLineManager;
	@Value("${password.max.age.qualitymanager}")
	private int passwordMaxAgeQualityManager;
	@Value("${password.max.age.lecturemanager}")
	private int passwordMaxAgeLectureManager;
	@Value("${password.max.age.principal}")
	private int passwordMaxAgePrincipal;
	@Value("${password.max.age.administrator}")
	private int passwordMaxAgeAdministrator;
	@Value("${password.max.age.sysadmin}")
	private int passwordMaxAgeSysAdmin;
	
	@Value("${password.history:0}")
	private int passwordHistory;

	@Value("${invitation.login:enabled}")
	private String invitationEnabled;
	@Value("${guest.login:enabled}")
	private String guestLoginEnabled;
	@Value("${guest.login.links:enabled}")
	private String guestLoginLinksEnabled;
	
	private String defaultProviderName = "OLAT";
	@Value("${login.using.username.or.email.enabled:true}")
	private boolean allowLoginUsingEmail;

	private CoordinatorManager coordinatorManager;
	private CacheWrapper<String,Integer> failedLoginCache;

	@Autowired
	private UserModule userModule;
	
	@Autowired
	public LoginModule(CoordinatorManager coordinatorManager) {
		super(coordinatorManager);
		this.coordinatorManager = coordinatorManager;
	}
	
	@Override
	public void init() {
		// configure timed cache default params: refresh 1 minute, timeout according to configuration
		failedLoginCache = coordinatorManager.getCoordinator().getCacher().getCache(LoginModule.class.getSimpleName(), "blockafterfailedattempts");
				
		updateProperties();
		
		boolean defaultProviderFound = false;
		for (Iterator<AuthenticationProvider> iterator = authenticationProviders.iterator(); iterator.hasNext();) {
			AuthenticationProvider provider = iterator.next();
			if (provider.isDefault()) {
				defaultProviderFound = true;
				defaultProviderName = provider.getName();
				log.info("Using default authentication provider '" + defaultProviderName + "'.");
			}
		}
		
		if (!defaultProviderFound) {
			throw new StartupException("Defined DefaultAuthProvider::" + defaultProviderName + " not existent or not enabled. Please fix.");
		}
	}

	@Override
	protected void initFromChangedProperties() {
		updateProperties();
	}
	
	@Override
	protected void initDefaultProperties() {
		super.initDefaultProperties();
		if (attackPreventionEnabled) {
			log.info("Attack prevention enabled. Max number of attempts: " + attackPreventionMaxAttempts + ", timeout: " + attackPreventionTimeout + " minutes.");
		} else {
			log.info("Attack prevention is disabled.");
		}
		
		//compatibility with older settings
		if("true".equals(guestLoginEnabled)) {
			guestLoginEnabled = "enabled";
		} else if("false".equals(guestLoginEnabled)) {
			guestLoginEnabled = "disabled";
		}
		
		if("true".equals(guestLoginLinksEnabled)) {
			guestLoginLinksEnabled = "enabled";
		} else if("false".equals(guestLoginLinksEnabled)) {
			guestLoginLinksEnabled = "disabled";
		}
		
		if("true".equals(invitationEnabled)) {
			invitationEnabled = "enabled";
		} else if("false".equals(guestLoginLinksEnabled)) {
			invitationEnabled = "disabled";
		}
		
		if (isGuestLoginEnabled()) {
			log.info("Guest login on login page enabled");
		} else {
			log.info("Guest login on login page disabled or not properly configured. ");
		}
		
		if (isInvitationEnabled()) {
			log.info("Invitation login enabled");
		} else {
			log.info("Invitation login disabled");
		}
	}
	
	private void updateProperties() {
		//set properties
		String invitation = getStringPropertyValue("invitation.login", true);
		if(StringHelper.containsNonWhitespace(invitation)) {
			invitationEnabled = invitation;
		}
		String guestLogin = getStringPropertyValue("guest.login", true);
		if(StringHelper.containsNonWhitespace(guestLogin)) {
			guestLoginEnabled = guestLogin;
		}
		String guestLoginLinks = getStringPropertyValue("guest.login.links", true);
		if(StringHelper.containsNonWhitespace(guestLoginLinks)) {
			guestLoginLinksEnabled = guestLoginLinks;
		}
		String usernameOrEmailLogin = getStringPropertyValue("login.using.username.or.email.enabled", true);
		if(StringHelper.containsNonWhitespace(usernameOrEmailLogin)) {
			allowLoginUsingEmail = "true".equals(usernameOrEmailLogin);
		}
		
		String changeOnce = getStringPropertyValue(CHANGE_ONCE, true);
		if(StringHelper.containsNonWhitespace(changeOnce)) {
			passwordChangeOnce = "true".equals(changeOnce);
		}
		
		passwordMaxAge = getAgeValue(MAX_AGE, passwordMaxAge);
		passwordMaxAgeAuthor = getAgeValue(MAX_AGE_AUTHOR, passwordMaxAgeAuthor);
		passwordMaxAgeGroupManager = getAgeValue(MAX_AGE_GROUPMANAGER, passwordMaxAgeGroupManager);
		passwordMaxAgePoolManager = getAgeValue(MAX_AGE_POOLMANAGER, passwordMaxAgePoolManager);
		passwordMaxAgeUserManager = getAgeValue(MAX_AGE_USERMANAGER, passwordMaxAgeUserManager);
		passwordMaxAgeRolesManager = getAgeValue(MAX_AGE_ROLESMANAGER, passwordMaxAgeRolesManager);
		passwordMaxAgeLearnResourceManager = getAgeValue(MAX_AGE_LEARNRESOURCEMANAGER, passwordMaxAgeLearnResourceManager);
		passwordMaxAgeCurriculumManager = getAgeValue(MAX_AGE_CURRICULUMMANAGER, passwordMaxAgeCurriculumManager);
		passwordMaxAgeLectureManager = getAgeValue(MAX_AGE_LECTUREMANAGER, passwordMaxAgeLectureManager);
		passwordMaxAgeQualityManager = getAgeValue(MAX_AGE_QUALITYMANAGER, passwordMaxAgeQualityManager);
		passwordMaxAgeLineManager = getAgeValue(MAX_AGE_LINEMANAGER, passwordMaxAgeLineManager);
		passwordMaxAgePrincipal = getAgeValue(MAX_AGE_PRINCIPAL, passwordMaxAgePrincipal);
		passwordMaxAgeAdministrator = getAgeValue(MAX_AGE_ADMINISTRATOR, passwordMaxAgeAdministrator);
		passwordMaxAgeSysAdmin = getAgeValue(MAX_AGE_SYSADMIN, passwordMaxAgeSysAdmin);
		
		String history = getStringPropertyValue(HISTORY, true);
		if(StringHelper.containsNonWhitespace(history)) {
			passwordHistory = Integer.parseInt(history);
		}
	}

	private int getAgeValue(String propertyName, int defaultValue) {
		String value = getStringPropertyValue(propertyName, Integer.toString(defaultValue));
		if(StringHelper.containsNonWhitespace(value)) {
			return Integer.parseInt(value);
		}
		return defaultValue;
	}

	/**
	 * @return The configured default login provider.
	 */
	public String getDefaultProviderName() {
		return defaultProviderName;
	}
	
	public Encoder.Algorithm getDefaultHashAlgorithm() {
		return Encoder.Algorithm.sha512;
	}
	
	/**
	 * @param provider
	 * @return AuthenticationProvider implementation.
	 */
	public AuthenticationProvider getAuthenticationProvider(String provider) {
		AuthenticationProvider authenticationProvider = null;
		for(AuthenticationProvider authProvider:authenticationProviders) {
			if(authProvider.getName().equalsIgnoreCase(provider) || authProvider.accept(provider)) {
				authenticationProvider = authProvider;
			}
		}
		return authenticationProvider;
	}
	
	/**
	 * This method will always return something and will try to find some
	 * matching provider. It will find LDAP'A0 -> LDAP or return the
	 * default provider.
	 * 
	 * @param provider
	 * @return
	 */
	public AuthenticationProvider getAuthenticationProviderHeuristic(String provider) {
		//first exact match
		AuthenticationProvider authenticationProvider = getAuthenticationProvider(provider);
		if(authenticationProvider == null && StringHelper.containsNonWhitespace(provider)) {
			String upperedCaseProvider = provider.toUpperCase();
			for(AuthenticationProvider authProvider:authenticationProviders) {
				if(upperedCaseProvider.contains(authProvider.getName().toUpperCase())) {
					authenticationProvider = authProvider;
					break;
				}
			}
		}
		if(authenticationProvider == null) {
			//return default
			for(AuthenticationProvider authProvider:authenticationProviders) {
				if(authProvider.isDefault()) {
					authenticationProvider = authProvider;
					break;
				}
			}
		}
		return authenticationProvider;
	}
	
	/**
	 * @return Collection of available AuthenticationProviders
	 */
	public Collection<AuthenticationProvider> getAuthenticationProviders() {
		return new ArrayList<>(authenticationProviders);
	}
	
	/**
	 * Must be called upon each login attempt. Returns true
	 * if number of login attempts has reached the set limit.
	 * @param login
	 * @return True if further logins will be prevented (i.e. max attempts reached).
	 */
	public final boolean registerFailedLoginAttempt(String login) {
		if (!attackPreventionEnabled) return false;
		Integer numAttempts = failedLoginCache.get(login);
		
		if (numAttempts == null) { // create new entry
			numAttempts = Integer.valueOf(1);
			failedLoginCache.put(login, numAttempts);
		} else { // update entry
			numAttempts = Integer.valueOf(numAttempts.intValue() + 1);
			failedLoginCache.update(login, numAttempts);
		}		
		return (numAttempts.intValue() > attackPreventionMaxAttempts);
	}
	
	/**
	 * Clear all failed login attempts for a given login.
	 * @param login
	 */
	public final void clearFailedLoginAttempts(String login) {
		if (attackPreventionEnabled) {
			failedLoginCache.remove(login);
		}
	}
	
	/**
	 * Tells whether a login is blocked to prevent brute force attacks or not.
	 * @param login
	 * @return True if login is blocked by attack prevention mechanism
	 */
	public final boolean isLoginBlocked(String login) {
		if (!attackPreventionEnabled) return false;
		Integer numAttempts = failedLoginCache.get(login);
		
		if (numAttempts == null) return false;
		else return (numAttempts.intValue() > attackPreventionMaxAttempts);
	}
	
	/**
	 * @return True if guest login must be shown on login screen, false
	 *         otherwise
	 */
	public boolean isGuestLoginEnabled() {
		return "enabled".equals(guestLoginEnabled);
	}
	
	public void setGuestLoginEnabled(boolean enabled) {
		guestLoginEnabled = enabled ? "enabled" : "disabled";
		setStringProperty("guest.login", guestLoginEnabled, true);
	}
	
	public boolean isGuestLoginLinksEnabled() {
		return "enabled".equalsIgnoreCase(guestLoginLinksEnabled);
	}
	
	public void setGuestLoginLinksEnabled(boolean enabled) {
		guestLoginLinksEnabled = enabled ? "enabled" : "disabled";
		setStringProperty("guest.login.links", guestLoginLinksEnabled, true);
	}
	
	public boolean isInvitationEnabled() {
		return "enabled".equals(invitationEnabled);
	}
	
	public void setInvitationEnabled(boolean enabled) {
		invitationEnabled = enabled ? "enabled" : "disabled";
		setStringProperty("invitation.login", invitationEnabled, true);
	}
	
	/**
	 * @return Number of minutes a login gets blocked after too many attempts.
	 */
	public Integer getAttackPreventionTimeoutMin() {
		return Integer.valueOf(attackPreventionTimeout);
	}
	
	/**
	 * @return True if login with email is allowed (set in olat.properties)
	 */
	public boolean isAllowLoginUsingEmail() {
		boolean isAllowLoginUsingEmail = allowLoginUsingEmail;
		if (!userModule.isEmailUnique()) {
			isAllowLoginUsingEmail = false;
		}
		return isAllowLoginUsingEmail;
	}
	
	public void setAllowLoginUsingEmail(boolean allow) {
		allowLoginUsingEmail = allow;
		setStringProperty("login.using.username.or.email.enabled", Boolean.toString(allow), true);
	}

	public boolean isPasswordChangeOnce() {
		return passwordChangeOnce;
	}

	public void setPasswordChangeOnce(boolean passwordChangeOnce) {
		this.passwordChangeOnce = passwordChangeOnce;
		setStringProperty(CHANGE_ONCE, passwordChangeOnce ? "true" : "false", true);
	}
	
	public boolean isPasswordAgePolicyConfigured() {
		return passwordMaxAge > 0 || passwordMaxAgeAuthor > 0
				|| passwordMaxAgeGroupManager > 0 || passwordMaxAgePoolManager > 0
				|| passwordMaxAgeUserManager > 0  || passwordMaxAgeRolesManager > 0 
				|| passwordMaxAgeLearnResourceManager > 0 || passwordMaxAgeLectureManager > 0
				|| passwordMaxAgeQualityManager > 0 || passwordMaxAgeLineManager > 0
				|| passwordMaxAgeCurriculumManager > 0 || passwordMaxAgePrincipal > 0
				|| passwordMaxAgeAdministrator > 0 || passwordMaxAgeSysAdmin > 0;
	}
	

	
	/**
	 * 
	 * @param roles The roles
	 * @return A number of seconds
	 */
	public int getPasswordAgePolicy(Roles roles) {
		int age = passwordMaxAge;
		for(OrganisationRoles policyRole:policyRoles) {
			if(roles.hasRole(policyRole)) {
				age = getMaxAgeOrDefault(age, getPasswordMaxAgeFor(policyRole));
			}
		}
		return age;
	}
	
	/**
	 * 
	 * @param roleMaxAge The max. age
	 * @return A number of seconds
	 */
	private int getMaxAgeOrDefault(int currentAge, int roleMaxAge) {
		if(currentAge <= 0 || (roleMaxAge > 0 && roleMaxAge < currentAge)) {
			return roleMaxAge;
		}
		return currentAge;
	}

	/**
	 * The default max. age for a password in seconds.
	 * 
	 * @return A number of seconds
	 */
	public int getPasswordMaxAge() {
		return passwordMaxAge;
	}

	/**
	 * The default max. age in seconds.
	 * 
	 * @param maxAge The age in seconds
	 */
	public void setPasswordMaxAge(int maxAge) {
		this.passwordMaxAge = maxAge;
		setStringProperty(MAX_AGE, Integer.toString(maxAge), true);
	}
	
	public int getPasswordMaxAgeFor(OrganisationRoles role) {
		switch(role) {
			case user: return passwordMaxAge;
			case author: return passwordMaxAgeAuthor;
			case usermanager: return passwordMaxAgeUserManager;
			case rolesmanager: return passwordMaxAgeRolesManager;
			case groupmanager: return passwordMaxAgeGroupManager;
			case learnresourcemanager: return passwordMaxAgeLearnResourceManager;
			case poolmanager: return passwordMaxAgePoolManager;
			case curriculummanager: return passwordMaxAgeCurriculumManager;
			case lecturemanager: return passwordMaxAgeLectureManager;
			case qualitymanager: return passwordMaxAgeQualityManager;
			case linemanager: return passwordMaxAgeLineManager;
			case principal: return passwordMaxAgePrincipal;
			case administrator: return passwordMaxAgeAdministrator;
			case sysadmin: return passwordMaxAgeSysAdmin;
			default: return passwordMaxAge;
		}
	}
	
	public void setPasswordMaxAgeFor(OrganisationRoles role, int maxAge) {
		switch(role) {
			case user:
				passwordMaxAge = setPasswordMaxAge(MAX_AGE, maxAge);
				break;
			case author:
				passwordMaxAgeAuthor = setPasswordMaxAge(MAX_AGE_AUTHOR, maxAge);
				break;
			case usermanager:
				passwordMaxAgeUserManager = setPasswordMaxAge(MAX_AGE_USERMANAGER, maxAge);
				break;
			case rolesmanager:
				passwordMaxAgeRolesManager = setPasswordMaxAge(MAX_AGE_ROLESMANAGER, maxAge);
				break;
			case groupmanager:
				passwordMaxAgeGroupManager = setPasswordMaxAge(MAX_AGE_GROUPMANAGER, maxAge);
				break;
			case learnresourcemanager:
				passwordMaxAgeLearnResourceManager = setPasswordMaxAge(MAX_AGE_LEARNRESOURCEMANAGER, maxAge);
				break;
			case poolmanager:
				passwordMaxAgePoolManager = setPasswordMaxAge(MAX_AGE_POOLMANAGER, maxAge);
				break;
			case curriculummanager:
				passwordMaxAgeCurriculumManager = setPasswordMaxAge(MAX_AGE_CURRICULUMMANAGER, maxAge);
				break;
			case lecturemanager:
				passwordMaxAgeLectureManager = setPasswordMaxAge(MAX_AGE_LECTUREMANAGER, maxAge);
				break;
			case qualitymanager:
				passwordMaxAgeQualityManager = setPasswordMaxAge(MAX_AGE_QUALITYMANAGER, maxAge);
				break;
			case linemanager:
				passwordMaxAgeLineManager = setPasswordMaxAge(MAX_AGE_LINEMANAGER, maxAge);
				break;
			case principal:
				passwordMaxAgePrincipal = setPasswordMaxAge(MAX_AGE_PRINCIPAL, maxAge);
				break;
			case administrator:
				passwordMaxAgeAdministrator = setPasswordMaxAge(MAX_AGE_ADMINISTRATOR, maxAge);
				break;
			case sysadmin:
				passwordMaxAgeSysAdmin = setPasswordMaxAge(MAX_AGE_SYSADMIN, maxAge);
				break;
			default: /* Ignore the other roles */
		}
	}
	
	public int setPasswordMaxAge(String propertyName, int maxAge) {
		setStringProperty(propertyName, Integer.toString(maxAge), true);
		return maxAge;
	}

	public int getPasswordHistory() {
		return passwordHistory;
	}

	public void setPasswordHistory(int history) {
		passwordHistory = history;
		setStringProperty(HISTORY, Integer.toString(history), true);
	}
}
