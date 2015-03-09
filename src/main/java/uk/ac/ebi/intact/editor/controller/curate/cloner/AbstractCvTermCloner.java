/**
 * Copyright 2010 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.intact.editor.controller.curate.cloner;

import psidev.psi.mi.jami.model.Alias;
import psidev.psi.mi.jami.model.Annotation;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.Xref;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.extension.AbstractIntactCvTerm;
import uk.ac.ebi.intact.jami.model.extension.AbstractIntactXref;

/**
 * Editor specific cloning routine for cvs.
 *
 * @author Prem Anand (prem@ebi.ac.uk)
 * @version $Id: InteractionIntactCloner.java 14783 2010-07-29 12:52:28Z brunoaranda $
 * @since 2.0.1-SNAPSHOT
 */
public abstract class AbstractCvTermCloner<I extends CvTerm, T extends AbstractIntactCvTerm> extends AbstractEditorCloner<I,T> {


    public T clone(I cv, IntactDao dao) {
        T clone = instantiateNewCloneFrom(cv);

        initAuditProperties(clone, dao);

        clone.setFullName(cv.getFullName());
        // copy collections
        for (Alias alias : cv.getSynonyms()){
            clone.getSynonyms().add(instantiateAliase(alias.getType(), alias.getName()));
        }

        for (Xref ref : cv.getXrefs()){
            AbstractIntactXref intactRef = instantiateXref(ref.getDatabase(), ref.getId(), ref.getVersion(), ref.getQualifier());
            if (ref instanceof AbstractIntactXref){
                intactRef.setSecondaryId(((AbstractIntactXref) ref).getSecondaryId());
            }
            clone.getXrefs().add(intactRef);
        }

        for (Annotation annotation : cv.getAnnotations()){
            clone.getAnnotations().add(instantiateAnnotation(annotation.getTopic(), annotation.getValue()));
        }

        return clone;
    }

    protected abstract Annotation instantiateAnnotation(CvTerm topic, String value);

    protected abstract AbstractIntactXref instantiateXref(CvTerm database, String id, String version, CvTerm qualifier);

    protected abstract Alias instantiateAliase(CvTerm type, String name);

    protected abstract T instantiateNewCloneFrom(I cv);

    public void copyInitialisedProperties(T source, T target) {
        target.setShortName(source.getShortName());
        target.setFullName(source.getShortName());
    }
}

