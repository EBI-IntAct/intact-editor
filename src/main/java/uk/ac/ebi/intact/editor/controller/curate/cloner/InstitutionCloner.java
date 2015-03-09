package uk.ac.ebi.intact.editor.controller.curate.cloner;

import psidev.psi.mi.jami.model.*;
import uk.ac.ebi.intact.jami.model.extension.*;

/**
 * Cv cloner
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02/12/14</pre>
 */

public class  InstitutionCloner extends AbstractCvTermCloner<Source, IntactSource>{
    @Override
    protected Annotation instantiateAnnotation(CvTerm topic, String value) {
        return new CvTermAnnotation(topic, value);
    }

    @Override
    protected AbstractIntactXref instantiateXref(CvTerm database, String id, String version, CvTerm qualifier) {
        return new CvTermXref(database, id, version, qualifier);
    }

    @Override
    protected Alias instantiateAliase(CvTerm type, String name) {
        return new CvTermAlias(type, name);
    }

    @Override
    protected IntactSource instantiateNewCloneFrom(Source cv) {
        return new IntactSource(cv.getShortName());
    }

    @Override
    public void copyInitialisedProperties(IntactSource source, IntactSource target) {
        super.copyInitialisedProperties(source, target);
        // copy collections
        if (source.areSynonymsInitialized()){
            target.getSynonyms().clear();
            target.getSynonyms().addAll(source.getSynonyms());
        }

        if (source.areXrefsInitialized()){
            target.getIdentifiers().clear();
            target.getIdentifiers().addAll(source.getIdentifiers());
            target.getXrefs().clear();
            target.getXrefs().addAll(source.getXrefs());
        }

        if (source.areAnnotationsInitialized()){
            target.getAnnotations().clear();
            target.getAnnotations().addAll(source.getAnnotations());
        }
    }
}
