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
package uk.ac.ebi.intact.editor.controller.curate;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.model.*;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;

import java.util.Collection;
import java.util.Iterator;

/**
 * Keeps the changes for each annotated object by AC.
 * 
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope("session")
public class CuratorContextController extends BaseController {

    public CuratorContextController() {
    }

    public String intactObjectSimpleName(IntactPrimaryObject io) {
        if (io == null) return "";
        else if (io instanceof Publication){
            return "Publication";
        }
        else if (io instanceof Experiment){
            return "Experiment";
        }
        else if (io instanceof InteractionEvidence){
            return "Interaction evidence";
        }
        else if (io instanceof Complex){
            return "Complex";
        }
        else if (io instanceof ParticipantEvidence){
            return "Participant";
        }
        else if (io instanceof ModelledParticipant){
            return "Complex Participant";
        }
        else if (io instanceof FeatureEvidence){
            return "Feature";
        }
        else if (io instanceof ModelledFeature){
            return "Complex Feature";
        }
        else if (io instanceof Source){
            return "Institution";
        }
        else if (io instanceof CvTerm){
            return "Controlled vocabulary";
        }
        else if (io instanceof Organism){
            return "Organism";
        }
        else if (io instanceof Interactor){
            return "Interactor";
        }
        return "";
    }

    public String acList(Collection<? extends IntactPrimaryObject> aos) {
        StringBuffer buffer = new StringBuffer();
        Iterator<? extends IntactPrimaryObject> iterator = aos.iterator();
        while (iterator.hasNext() ){
           buffer.append(iterator.next().getAc());
           if (iterator.hasNext()){
              buffer.append(", ");
           }
        }
        return buffer.toString();
    }
}
