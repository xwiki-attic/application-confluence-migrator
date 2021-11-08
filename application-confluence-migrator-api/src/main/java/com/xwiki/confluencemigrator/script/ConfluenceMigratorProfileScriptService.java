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
package com.xwiki.confluencemigrator.script;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import com.xwiki.confluencemigrator.ConfluenceMigratorProfile;

/**
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("confluencemigrator.profile")
@Singleton
@Unstable
public class ConfluenceMigratorProfileScriptService extends AbstractConfluenceMigratorScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    static final String ERROR_KEY = "scriptservice.confluencemigrator.profile.error";

    /**
     * Provides access to the current context.
     */
    @Inject
    protected Execution execution;

    @Inject
    private ConfluenceMigratorProfile profile;

    /**
     * Check the connection with the details from the profile.
     *
     * @param profileRef the reference of the profile containing connection details
     * @return true if connection is established, false otherwise
     */
    public boolean checkConnection(DocumentReference profileRef)
    {
        try {
            return profile.checkConnection(profileRef);
        } catch (Exception e) {
            setError(e);
            return false;
        }
    }

    /**
     * Get the active profile from the current migration.
     *
     * @return the name of the active profile
     */
    public String getActiveProfile()
    {
        return profile.getActiveProfile();
    }

    /**
     * Get the list of migration profiles.
     *
     * @return the list of profiles
     */
    public List<String> getProfiles()
    {
        return profile.getProfiles();
    }

    /**
     * Generate a new profile name.
     *
     * @return the profile name
     */
    public String getNewProfileName()
    {
        return profile.getNewProfileName();
    }

    /**
     * Set the total COnfluence pages number counted in the Space Analyze step.
     *
     * @param totalPages the number to be set in profile
     */
    public void setTotalPageNumber(long totalPages) {
        profile.setTotalPageNumber(totalPages);
    }

    /**
     * Get the path of the attachment in the file system.
     *
     * @param fileName the name of the uploaded file
     * @return the file path
     */
    public String getFilePath(String fileName)
    {
        return profile.getFilePath(fileName);
    }

    /**
     * Reset migration and revert all the actions.
     */
    public void resetMigration() {
        profile.resetMigration();
    }

    @Override
    protected String getErrorKey()
    {
        return ERROR_KEY;
    }
}
