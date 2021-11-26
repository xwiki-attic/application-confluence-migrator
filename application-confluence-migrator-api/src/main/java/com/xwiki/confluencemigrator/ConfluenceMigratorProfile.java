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
package com.xwiki.confluencemigrator;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id$
 * @since 1.0
 */
@Role
public interface ConfluenceMigratorProfile
{
    /**
     * Check the connection with the details from the profile.
     *
     * @param profileRef the reference of the profile containing connection details
     * @return true if connection is established, false otherwise
     * @throws Exception in case of exceptions
     */
    boolean checkConnection(DocumentReference profileRef) throws Exception;

    /**
     * Get the active profile from the current migration.
     *
     * @return the name of the active profile
     */
    String getActiveProfile();

    /**
     * Get the list of migration profiles.
     *
     * @return the list of profiles
     */
    List<String> getProfiles();

    /**
     * Generate a new profile name.
     *
     * @return the profile name
     */
    String getNewProfileName();

    /**
     * Get the path of the attachment in the file system.
     *
     * @param fileName the name of the uploaded file
     * @return the file path
     */
    String getFilePath(String fileName);

    /**
     * Set the total COnfluence pages number counted in the Space Analyze step.
     *
     * @param totalPages the number to be set in profile
     */
    void setTotalPageNumber(long totalPages);

    /**
     * Reset migration and revert all the actions.
     */
    void resetMigration();

    /**
     * Finish the migration and do the necessary cleanup.
     */
    void finishMigration();

    /**
     * Get the steps taken value to know where to resume the migration.
     *
     * @return the steps taken
     */
    long getStepsTaken();

}
