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
package uk.ac.ebi.intact.editor.converter;

import psidev.psi.mi.jami.model.Participant;
import uk.ac.ebi.intact.editor.controller.curate.feature.FeatureController;
import uk.ac.ebi.intact.editor.controller.curate.feature.ModelledFeatureController;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.extension.AbstractIntactParticipant;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

/**
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 */
@FacesConverter( value = "participantConverter", forClass = AbstractIntactParticipant.class )
public class ParticipantConverter implements Converter {

    @Override
    public Object getAsObject( FacesContext facesContext, UIComponent uiComponent, String ac ) throws ConverterException {
        if ( ac == null ) return null;
        FeatureController dao = ApplicationContextProvider.getBean("featureController");
        Participant p = dao.getParticipantsMap().get(ac);
        if (p == null){
            ModelledFeatureController dao2 = ApplicationContextProvider.getBean("modelledFeatureController");
            p = dao2.getParticipantsMap().get(ac);
        }
        return p;
    }

    @Override
    public String getAsString( FacesContext facesContext, UIComponent uiComponent, Object o ) throws ConverterException {
        if ( o == null ) return null;

        if ( o instanceof AbstractIntactParticipant ) {
            AbstractIntactParticipant part = ( AbstractIntactParticipant ) o;
            return part.getAc();
        } else {
            throw new IllegalArgumentException( "Argument must be a participant: " + o + " (" + o.getClass() + ")" );
        }
    }
}