package uk.ac.ebi.intact.editor.controller.curate.experiment;

import java.util.Collection;

/**
 * Summary for a feature
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>21/10/14</pre>
 */

public class FeatureSummary {

    private String feature;
    private Collection<String> bindDomains;

    public FeatureSummary(String feature, Collection<String> bind){
        this.feature = feature;
        this.bindDomains = bind;
    }

    public String getFeature() {
        return feature;
    }

    public Collection<String> getBindDomains() {
        return bindDomains;
    }
}
