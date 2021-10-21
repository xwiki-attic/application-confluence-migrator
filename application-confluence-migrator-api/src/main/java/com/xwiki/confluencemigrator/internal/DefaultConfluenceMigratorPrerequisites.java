/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.confluencemigrator.internal;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.confluencemigrator.ConfluenceMigratorPrerequisites;

/**
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
public class DefaultConfluenceMigratorPrerequisites implements ConfluenceMigratorPrerequisites
{
    private static final String ERROR = "cross";

    private static final String SUCCESS = "check";

    private static final String WARNING = "exclamation";

    @Inject
    private ComponentManager componentManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Logger logger;

    @Inject
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @Inject
    private ObservationManager observationManager;

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource xwikiCfgSource;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public String checkMemory()
    {
        long memory = getMemory();
        if (getXms() >= memory / 2 && getXmx() >= memory) {
            return SUCCESS;
        }
        return ERROR;
    }

    @Override
    public String checkCache()
    {
        if (getCache() >= 5000) {
            return SUCCESS;
        }
        return ERROR;
    }

    @Override
    public String checkWikiNotifications()
    {
        if (!xwikiPropertiesSource.getProperty("notifications.enabled", true)
            || !xwikiPropertiesSource.getProperty("notifications.emails.enabled", true)) {
            return SUCCESS;
        }
        return WARNING;
    }

    /**
     * Deactivate notifications on current user's profile ("never" for automatic page watching, disable page.
     * notifications)
     */
    @Override
    public String checkCurrentUserNotification()
    {
        XWikiContext xcontext = contextProvider.get();
        DocumentReference userReference = xcontext.getUserReference();
        try {
            XWikiDocument userDocument = xcontext.getWiki().getDocument(userReference, xcontext);
            BaseObject autoWatchObj = userDocument
                .getXObject(documentReferenceResolver.resolve("XWiki.Notifications.Code.AutomaticWatchModeClass"));
            if (autoWatchObj != null && !autoWatchObj.getStringValue("automaticWatchMode").equals("NONE")) {
                return ERROR;
            }
            Collection<NotificationFilterPreference> filters =
                notificationFilterPreferenceManager.getFilterPreferences(userReference);
            for (NotificationFilterPreference filter : filters) {
                if (filter.isEnabled()) {
                    return ERROR;
                }
            }
            return SUCCESS;
        } catch (NotificationException | XWikiException e) {
            logger.error("Could not get notification filter for user [{}]", userReference, e);
        }
        return ERROR;
    }

    @Override
    public String checkListener(String listenerClassName, boolean isMandatory)
    {
        try {
            List<EventListener> listeners = componentManager.getInstanceList(EventListener.class);
            for (EventListener listener : listeners) {
                if (listener.getClass().getName().contains(listenerClassName)) {
                    EventListener enabledListener = observationManager.getListener(listener.getName());
                    if (enabledListener == null) {
                        return SUCCESS;
                    }
                    if (isMandatory) {
                        return ERROR;
                    } else {
                        return WARNING;
                    }
                }
            }
        } catch (ComponentLookupException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ERROR;
    }

    @Override
    public long getMemory()
    {
        return Runtime.getRuntime().totalMemory();
    }

    @Override
    public String readableSize(long size)
    {
        if (size <= 0) {
            return "0";
        }
        String[] units = new String[] {"B", "KB", "MB", "GB", "TB"};

        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    @Override
    public long getXms()
    {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        return memoryBean.getHeapMemoryUsage().getInit();
    }

    @Override
    public long getXmx()
    {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        return memoryBean.getHeapMemoryUsage().getMax();
    }

    @Override
    public int getCache()
    {
        return xwikiCfgSource.getProperty("xwiki.store.cache.capacity", 500);
    }

    @Override
    public String checkCurrentUserNotificationCleanup()
    {
        DocumentReference userReference = contextProvider.get().getUserReference();
        try {
            Collection<NotificationFilterPreference> filters =
                notificationFilterPreferenceManager.getFilterPreferences(userReference);
            for (NotificationFilterPreference filter : filters) {
                notificationFilterPreferenceManager.deleteFilterPreference(userReference, filter.getId());
            }
            if (notificationFilterPreferenceManager.getFilterPreferences(userReference).size() == 0) {
                return SUCCESS;
            }
        } catch (NotificationException e) {
            logger.error("Could not clean the notification filters for user [{}]", userReference, e);
        }
        return WARNING;
    }
}
