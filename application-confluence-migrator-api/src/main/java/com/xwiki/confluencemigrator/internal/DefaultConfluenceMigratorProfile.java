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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.store.filesystem.internal.FilesystemStoreTools;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.confluencemigrator.ConfluenceMigratorProfile;

/**
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
public class DefaultConfluenceMigratorProfile implements ConfluenceMigratorProfile
{
    private static final String WIKI_ENDING = "/wiki";

    private static final LocalDocumentReference PROFILE_CLASS_REFERENCE =
        new LocalDocumentReference(Arrays.asList("Confluence", "Tools"), "MigrationProfileClass");

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private FilesystemStoreTools fstools;

    @Inject
    private Logger logger;

    @Inject
    private QueryManager queryManager;

    @Override
    public boolean checkConnection(DocumentReference profileRef) throws Exception
    {
        XWikiContext context = contextProvider.get();
        XWikiDocument profileDoc;
        HttpURLConnection connection = null;
        try {
            profileDoc = context.getWiki().getDocument(profileRef, context);
            BaseObject profileObj = profileDoc.getXObject(PROFILE_CLASS_REFERENCE);
            String space = profileObj.getStringValue("space");
            String baseURL = profileObj.getStringValue("url");
            String username = profileObj.getStringValue("username");
            String token = profileObj.getStringValue("token");

            if (baseURL.endsWith("/")) {
                baseURL = baseURL.substring(0, baseURL.length() - 1);
            }
            if (!baseURL.endsWith(WIKI_ENDING)) {
                baseURL += WIKI_ENDING;
            }
            String searchURL = String.format("%s/rest/api/content?type=page&spaceKey=%s", baseURL, space);
            String authString = Base64.getEncoder().encodeToString(String.format("%s:%s", username, token).getBytes());
            URL url = new URL(searchURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", String.format("Basic %s", authString));
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return true;
            } else {
                throw new Exception("HTTP response code: " + String.valueOf(responseCode));
            }
        } catch (XWikiException | IOException e) {
            logger.warn("Failed to get Confluence data from [{}].", profileRef, e);
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    public String getActiveProfile()
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();
        DocumentReference profilesHomePageRef = resolver.resolve("Confluence.Migrator.Profiles.WebHome");
        try {
            XWikiDocument profilesHomePageDoc = xwiki.getDocument(profilesHomePageRef, context);
            DocumentReference activeProfileClassRef = resolver.resolve("Confluence.Tools.ActiveMigrationProfileClass");
            BaseObject activeProfileObj = profilesHomePageDoc.getXObject(activeProfileClassRef);
            if (activeProfileObj != null) {
                return activeProfileObj.getStringValue("profile");
            }
        } catch (XWikiException e) {
            logger.error("Failed to get active profile.", e);
        }
        return null;
    }

    @Override
    public List<String> getProfiles()
    {
        StringBuilder statement = new StringBuilder("select doc.title, doc.fullName from Document doc, ");
        statement.append("doc.object(Confluence.Tools.MigrationProfileClass) as obj ");
        statement.append("where doc.fullName <> 'Confluence.Tools.MigrationProfileTemplate'");

        try {
            Query query = this.queryManager.createQuery(statement.toString(), Query.XWQL);
            return query.execute();
        } catch (QueryException e) {
            logger.error("Could not get the migration profiles", e);
        }
        return Collections.emptyList();
    }

    @Override
    public String getNewProfileName()
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();
        long profileId = RandomUtils.nextLong(1000000, 9999999);
        String profileName = String.format("Confluence.Migrator.Profiles.%s.WebHome", profileId);
        if (!xwiki.exists(resolver.resolve(profileName), context)) {
            return profileName;
        }
        return getNewProfileName();
    }

    @Override
    public String getFilePath(String fileName)
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();
        DocumentReference pageRef = resolver.resolve("Confluence.Migrator.WebHome");
        AttachmentReference attachmentRef = new AttachmentReference(fileName, pageRef);

        try {
            XWikiDocument pageDoc = xwiki.getDocument(attachmentRef.getDocumentReference(), context);
            XWikiAttachment attachment = pageDoc.getAttachment(fileName);
            if (attachment != null) {
                String path = fstools.getAttachmentFileProvider(attachmentRef).getAttachmentContentFile().getPath();
                // Delete the attachment from the XWiki document, relying on the temporary file in the file store.
                pageDoc.removeAttachment(attachment);
                return String.format("file:%s", path);
            }
        } catch (XWikiException e) {
            logger.error("Could not get the file path", e);
        }
        return null;
    }

    @Override
    public void setTotalPageNumber(long totalPages)
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        try {
            XWikiDocument profileDoc = xwiki.getDocument(resolver.resolve(getActiveProfile()), context);
            BaseObject profileObj = profileDoc.getXObject(PROFILE_CLASS_REFERENCE);
            profileObj.setLongValue("totalPages", totalPages);
            context.getWiki().saveDocument(profileDoc, "Set the total Confluence pages number.", context);
        } catch (XWikiException e) {
            logger.error("Failed to set the total Confluence pages number.", e);
        }
    }
}
