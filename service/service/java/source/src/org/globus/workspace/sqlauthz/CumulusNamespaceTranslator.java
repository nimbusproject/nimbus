package org.globus.workspace.sqlauthz;

import org.globus.workspace.NamespaceTranslator;
import org.globus.workspace.NamespaceTranslator;
import org.globus.workspace.WorkspaceException;
import org.globus.workspace.sqlauthz.AuthzDecisionLogic;
import org.globus.workspace.sqlauthz.SqlAuthz;

/**
 * Created by John Bresnahan
 * User: bresnaha
 * Date: May 24, 2010
 * Time: 12:56:49 PM
 * <p/>
 * org.globus.workspace
 */
public class CumulusNamespaceTranslator implements NamespaceTranslator
{
    protected AuthzDecisionLogic dl;

    public CumulusNamespaceTranslator(
        AuthzDecisionLogic dl)
    {
        this.dl = dl;
    }

    public String translateExternaltoInternal(
        String                          publicName)
            throws WorkspaceException
    {
        return dl.translateExternaltoInternal(publicName);
    }
}

