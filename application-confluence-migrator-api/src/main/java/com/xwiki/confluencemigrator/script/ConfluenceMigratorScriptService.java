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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.stability.Unstable;

import com.xwiki.confluencemigrator.ConfluenceMigratorManager;

/**
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("confluencemigrator")
@Singleton
@Unstable
public class ConfluenceMigratorScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    static final String ERROR_KEY = "scriptservice.confluencemigrator.error";

    /**
     * Provides access to the current context.
     */
    @Inject
    private Execution execution;

    @Inject
    private ConfluenceMigratorManager manager;

    @Inject
    @Named("confluencemigrator.prerequisites")
    private ScriptService prerequisitesScriptService;

    @Inject
    private ScriptServiceManager scriptServiceManager;

    /**
     * @param profileRef the reference of the profile containing connection details
     * @return true if connection is established, false otherwise
     */
    public boolean checkConnection(DocumentReference profileRef)
    {
        try {
            return manager.checkConnection(profileRef);
        } catch (Exception e) {
            setError(e);
            return false;
        }
    }

    /**
     * Get the error generated while performing the previously called action. An error can happen for example when:
     *
     * @return the exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    protected void setError(Exception e)
    {
        this.execution.getContext().setProperty(ERROR_KEY, e);
    }

    /**
     * @param <S> the type of the {@link ScriptService}
     * @param serviceName the name of the sub {@link ScriptService}
     * @return the {@link ScriptService} or null of none could be found
     */
    @SuppressWarnings("unchecked")
    public <S extends ScriptService> S get(String serviceName)
    {
        return (S) this.scriptServiceManager.get(String.format("confluencemigrator.%s", serviceName));
    }
}
