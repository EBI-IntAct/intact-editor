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

import psidev.psi.mi.jami.model.*;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.extension.*;

import java.lang.reflect.InvocationTargetException;

/**
 * Editor specific cloning routine for interactors.
 *
 * @author Prem Anand (prem@ebi.ac.uk)
 * @version $Id: InteractionIntactCloner.java 14783 2010-07-29 12:52:28Z brunoaranda $
 * @since 2.0.1-SNAPSHOT
 */
public class InteractorCloner extends AbstractEditorCloner<Interactor, IntactInteractor> {


    public IntactInteractor clone(Interactor interactor, IntactDao dao) {
        IntactInteractor clone = null;
        try {
            clone = (IntactInteractor)interactor.getClass().getConstructor(String.class).newInstance(interactor.getShortName());

            initAuditProperties(clone, dao);

            clone.setShortName(interactor.getShortName());
            clone.setFullName(interactor.getFullName());
            clone.setInteractorType(interactor.getInteractorType());
            clone.setOrganism(interactor.getOrganism());
            clone.getChecksums().addAll(interactor.getChecksums());

            for (Object obj : interactor.getAliases()){
                Alias alias = (Alias)obj;
                clone.getAliases().add(new InteractorAlias(alias.getType(), alias.getName()));
            }

            for (Object obj : interactor.getXrefs()){
                Xref ref = (Xref)obj;
                clone.getXrefs().add(new InteractorXref(ref.getDatabase(), ref.getId(), ref.getVersion(), ref.getQualifier()));
            }

            for (Object obj : interactor.getAnnotations()){
                Annotation annotation = (Annotation)obj;
                clone.getAnnotations().add(new InteractorAnnotation(annotation.getTopic(), annotation.getValue()));
            }

            if (interactor instanceof Polymer){
                ((Polymer)clone).setSequence(((Polymer) interactor).getSequence());
            }
            else if (interactor instanceof InteractorPool){
                ((InteractorPool)clone).addAll((InteractorPool)interactor);
            }

            return clone;
        } catch (InstantiationException e) {
            throw new IllegalStateException("Cannot clone interactor "+interactor, e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot clone interactor "+interactor, e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Cannot clone interactor "+interactor, e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Cannot clone interactor "+interactor, e);
        }
    }

    @Override
    public void copyInitialisedProperties(IntactInteractor source, IntactInteractor target) {
        target.setShortName(source.getShortName());
        target.setFullName(source.getFullName());
        target.setInteractorType(source.getInteractorType());
        target.setOrganism(source.getOrganism());
        target.getChecksums().addAll(source.getChecksums());

        if (source.areAliasesInitialized()){
            target.getAliases().clear();
            target.getAliases().addAll(source.getAliases());
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

        if (source instanceof Polymer && target instanceof Polymer){
            ((Polymer)target).setSequence(((Polymer) source).getSequence());
        }
        else if (source instanceof IntactInteractorPool
                && target instanceof InteractorPool
                && ((IntactInteractorPool) source).areInteractorsInitialized()){
            ((InteractorPool)target).addAll((InteractorPool)source);
        }
    }
}

