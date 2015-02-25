package uk.ac.ebi.intact.editor.controller.curate.publication;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import psidev.psi.mi.jami.bridges.exception.BridgeFailedException;
import psidev.psi.mi.jami.bridges.imex.DefaultImexCentralClient;
import psidev.psi.mi.jami.bridges.imex.ImexCentralClient;
import psidev.psi.mi.jami.bridges.imex.Operation;
import psidev.psi.mi.jami.bridges.imex.PublicationStatus;
import psidev.psi.mi.jami.bridges.imex.mock.MockImexCentralClient;
import uk.ac.ebi.intact.editor.ApplicationInitializer;

import java.util.Collection;
import java.util.Map;

/**
 * Wrapper of the Imex central client
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16/04/12</pre>
 */

public class ImexCentralClientWrapper implements ImexCentralClient{

    private static final Log log = LogFactory.getLog(ApplicationInitializer.class);
    private ImexCentralClient imexCentralClient;
    
    public ImexCentralClientWrapper(String username, String password, String endpoint) throws BridgeFailedException {
        String localTrustStore = System.getProperty( "javax.net.ssl.trustStore" );
        String localTrustStorePwd = System.getProperty( "javax.net.ssl.keyStorePassword" );
        if(localTrustStore==null) {
            log.error( "It appears you haven't setup a local trust store (other than the one embedded in the JDK)." +
                    "\nShould you want to specify one, use: -Djavax.net.ssl.trustStore=<path.to.keystore> " +
                    "\nAnd if it is password protected, use: -Djavax.net.ssl.keyStorePassword=<password>" );
        } else {
            log.info( "Using local trust store: " + localTrustStore + (localTrustStorePwd == null ? " (no password set)" : " (with password set)" ) );
        }

        if (username != null && password != null && endpoint != null && !username.isEmpty() && !password.isEmpty() && !endpoint.isEmpty()){
            imexCentralClient = new DefaultImexCentralClient(username, password, endpoint);
        }
        else {
            imexCentralClient = new MockImexCentralClient();
        }
    }
    
    @Override
    public String getEndpoint() {
        return imexCentralClient.getEndpoint();
    }

    @Override
    public Collection<psidev.psi.mi.jami.model.Publication> fetchPublicationsByOwner(String owner, int first, int max) throws BridgeFailedException {
        return imexCentralClient.fetchPublicationsByOwner(owner, first, max);
    }

    @Override
    public Collection<psidev.psi.mi.jami.model.Publication> fetchPublicationsByStatus(String status, int first, int max) throws BridgeFailedException {
        return imexCentralClient.fetchPublicationsByStatus(status, first, max);
    }

    @Override
    public psidev.psi.mi.jami.model.Publication updatePublicationStatus(String identifier, String source, PublicationStatus status) throws BridgeFailedException {
        return imexCentralClient.updatePublicationStatus(identifier, source, status);
    }

    @Override
    public psidev.psi.mi.jami.model.Publication updatePublicationAdminGroup(String identifier, String source, Operation operation, String group) throws BridgeFailedException {
        return imexCentralClient.updatePublicationAdminGroup(identifier, source, operation, group);
    }

    @Override
    public psidev.psi.mi.jami.model.Publication updatePublicationAdminUser(String identifier, String source, Operation operation, String user) throws BridgeFailedException {
        return imexCentralClient.updatePublicationAdminUser(identifier, source, operation, user);
    }

    @Override
    public psidev.psi.mi.jami.model.Publication updatePublicationIdentifier(String oldIdentifier, String oldSource, String newIdentifier, String source) throws BridgeFailedException {
        return imexCentralClient.updatePublicationIdentifier(oldIdentifier, oldSource, newIdentifier, source);
    }

    @Override
    public void createPublication(psidev.psi.mi.jami.model.Publication publication) throws BridgeFailedException {
        imexCentralClient.createPublication(publication);
    }

    @Override
    public psidev.psi.mi.jami.model.Publication createPublicationById(String identifier, String source) throws BridgeFailedException {
        return imexCentralClient.createPublicationById(identifier, source);
    }

    @Override
    public psidev.psi.mi.jami.model.Publication fetchPublicationImexAccession(String identifier, String source, boolean assign) throws BridgeFailedException {
        return imexCentralClient.fetchPublicationImexAccession(identifier, source, assign);
    }

    @Override
    public psidev.psi.mi.jami.model.Publication fetchByIdentifier(String identifier, String source) throws BridgeFailedException {
        return imexCentralClient.fetchByIdentifier(identifier, source);
    }

    @Override
    public Collection<psidev.psi.mi.jami.model.Publication> fetchByIdentifiers(Map<String, Collection<String>> identifiers) throws BridgeFailedException {
        return imexCentralClient.fetchByIdentifiers(identifiers);
    }
}
