/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package alpine.tasks;

import alpine.Config;
import alpine.event.LdapSyncEvent;
import alpine.event.framework.Event;
import alpine.event.framework.Subscriber;
import alpine.logging.Logger;
import alpine.model.LdapUser;
import alpine.persistence.QueryManager;
import org.apache.commons.lang3.StringUtils;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

/**
 * A task to synchronize LDAP users. This should be added to a concrete class that
 * extends {@link AlpineTaskScheduler}.
 *
 * @since 1.0.0
 */
public class LdapSyncTask implements Subscriber {

    private static final Logger logger = Logger.getLogger(LdapSyncTask.class);
    private static final boolean ldapEnabled = Config.getInstance().getPropertyAsBoolean(Config.Key.LDAP_ENABLED);
    private static final String ldapUrl = Config.getInstance().getProperty(Config.Key.LDAP_SERVER_URL);
    private static final String domainName = Config.getInstance().getProperty(Config.Key.LDAP_DOMAIN);
    private static final String baseDn = Config.getInstance().getProperty(Config.Key.LDAP_SERVER_URL);
    private static final String bindUsername = Config.getInstance().getProperty(Config.Key.LDAP_BIND_USERNAME);
    private static final String bindPassword = Config.getInstance().getProperty(Config.Key.LDAP_BIND_PASSWORD);
    private static final String attributeMail = Config.getInstance().getProperty(Config.Key.LDAP_ATTRIBUTE_MAIL);

    public void inform(Event e) {

        if (!ldapEnabled || StringUtils.isBlank(ldapUrl)) {
            return;
        }

        if (e instanceof LdapSyncEvent) {
            logger.info("Starting LDAP synchronization task");
            LdapSyncEvent event = (LdapSyncEvent) e;

            Hashtable<String, String> props = new Hashtable<>();
            String principalName = formatPrincipal(bindUsername);
            props.put(Context.SECURITY_PRINCIPAL, principalName);
            props.put(Context.SECURITY_CREDENTIALS, bindPassword);
            props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            props.put(Context.PROVIDER_URL, ldapUrl);

            String[] attributeFilter = {};
            SearchControls sc = new SearchControls();
            sc.setReturningAttributes(attributeFilter);
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

            DirContext ctx = null;
            QueryManager qm = null;
            try {
                ctx = new InitialDirContext(props);
                qm = new QueryManager();

                if (event.getUsername() == null) {
                    // If username was null, we are going to sync all users
                    List<LdapUser> users = qm.getLdapUsers();
                    for (LdapUser user: users) {
                        sync(ctx, qm, sc, user);
                    }
                } else {
                    // If username was specified, we will only sync the one
                    LdapUser user = qm.getLdapUser(event.getUsername());
                    if (user != null) {
                        sync(ctx, qm, sc, user);
                    }
                }
            } catch (NamingException ex) {
                logger.error("Error occurred during LDAP synchronization");
                logger.error(ex.getMessage());
            } finally {
                if (qm != null) {
                    qm.close();
                }
                if (ctx != null) {
                    try {
                        ctx.close();
                    } catch (NamingException ex) {
                    }
                }
                logger.info("LDAP synchronization complete");
            }
        }
    }

    private void sync(DirContext ctx, QueryManager qm, SearchControls sc, LdapUser user) throws NamingException {
        String searchFor = "userPrincipalName=" + formatPrincipal(user.getUsername());

        logger.debug("Syncing: " + user.getUsername());

        List<SearchResult> results = Collections.list(ctx.search(baseDn, searchFor, sc));
        if (results.size() > 0) {
            // Should only return 1 result, but just in case, get the very first one
            SearchResult result = results.get(0);

            user.setDN(result.getNameInNamespace());
            Attribute mail = result.getAttributes().get(attributeMail);
            if (mail != null) {
                // user.setMail(mail.get()); //todo
            }
        } else {
            // This is an invalid user - a username that exists in the database that does not exist in LDAP
            user.setDN("INVALID");
            // user.setMail(null); //todo
        }
        qm.updateLdapUser(user);
    }

    private String formatPrincipal(String username) {
        if (StringUtils.isNotBlank(domainName)) {
            return username + "@" + domainName;
        }
        return username;
    }
}