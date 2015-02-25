package uk.ac.ebi.intact.editor.controller.curate.cloner;

import psidev.psi.mi.jami.model.*;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.extension.CvTermAlias;
import uk.ac.ebi.intact.jami.model.extension.CvTermAnnotation;
import uk.ac.ebi.intact.jami.model.extension.CvTermXref;
import uk.ac.ebi.intact.jami.model.extension.IntactCvTerm;

/**
 * Cv cloner
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02/12/14</pre>
 */

public class CvTermCloner extends AbstractCvTermCloner<CvTerm, IntactCvTerm>{
    @Override
    protected Annotation instantiateAnnotation(CvTerm topic, String value) {
        return new CvTermAnnotation(topic, value);
    }

    @Override
    protected Xref instantiateXref(CvTerm database, String id, String version, CvTerm qualifier) {
        return new CvTermXref(database, id, version, qualifier);
    }

    @Override
    protected Alias instantiateAliase(CvTerm type, String name) {
        return new CvTermAlias(type, name);
    }

    @Override
    protected IntactCvTerm instantiateNewCloneFrom(CvTerm cv) {
        return new IntactCvTerm(cv.getShortName());
    }

    @Override
    public IntactCvTerm clone(CvTerm cv, IntactDao dao) {
        IntactCvTerm clone = super.clone(cv, dao);

        if (cv instanceof OntologyTerm){
            clone.setDefinition(((OntologyTerm)cv).getDefinition());
            if (cv instanceof IntactCvTerm){
                clone.setObjClass(((IntactCvTerm)cv).getObjClass());
            }
        }

        return clone;
    }

    @Override
    public void copyInitialisedProperties(IntactCvTerm source, IntactCvTerm target) {
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
        target.setDefinition(source.getDefinition());
        target.setObjClass(source.getObjClass());
        if (source.areParentsInitialized()){
            target.getParents().clear();
            for (OntologyTerm p : source.getParents()){
                target.addParent(p);
            }
        }
    }
}
