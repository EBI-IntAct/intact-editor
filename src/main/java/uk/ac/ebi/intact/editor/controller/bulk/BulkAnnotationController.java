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
package uk.ac.ebi.intact.editor.controller.bulk;

import org.apache.commons.collections.CollectionUtils;
import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.model.impl.DefaultAnnotation;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.services.bulk.BulkOperationsService;
import uk.ac.ebi.intact.editor.services.curate.cvobject.CvObjectService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.extension.*;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import javax.annotation.Resource;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "bulk" )
public class BulkAnnotationController extends BaseController {

    private String acs[];
    private String updatedAcs[];
    private String couldNotUpdateAcs[];
    private String aoClassName;
    private List<SelectItem> topicSelectItems;
    private boolean replaceIfTopicExists = true;

    @Resource(name = "bulkOperationsService")
    private transient BulkOperationsService bulkOperations;
    @Resource(name = "cvObjectService")
    private transient CvObjectService cvService;

    private IntactCvTerm topic;
    private String value;

    public BulkAnnotationController() {
        topicSelectItems = new ArrayList<SelectItem>(1);
        topicSelectItems.add(new SelectItem(null, "* Choose object type first"));
    }

    public void addBulkAnnotation(ActionEvent evt) {

        Class aoClass = null;
        try {
            aoClass = Thread.currentThread().getContextClassLoader().loadClass(aoClassName);

            try {
                getBulkOperations().getIntactDao().getUserContext().setUser(getCurrentUser());
                updatedAcs = getBulkOperations().addAnnotation(new DefaultAnnotation(this.topic, this.value), acs, aoClass, replaceIfTopicExists);
            } catch (SynchronizerException e) {
                addErrorMessage("Cannot add annotation " + this.topic, e.getCause() + ": " + e.getMessage());
            } catch (FinderException e) {
                addErrorMessage("Cannot add annotation " + this.topic, e.getCause() + ": " + e.getMessage());
            } catch (PersisterException e) {
                addErrorMessage("Cannot add annotation " + this.topic, e.getCause() + ": " + e.getMessage());
            } catch (Throwable e) {
                addErrorMessage("Cannot add annotation " + this.topic, e.getCause() + ": " + e.getMessage());
            }

            if (acs.length > 0 && updatedAcs.length == 0) {
                addErrorMessage("Operation failed. The acs may not exist in the database", "None of the ACs could be updated (do they exist?)");
                couldNotUpdateAcs = acs;
            } else if (acs.length != updatedAcs.length) {
                List<String> acsList = Arrays.asList(acs);
                List<String> updatedAcsList = Arrays.asList(updatedAcs);

                Collection<String> couldNotUpdateList = CollectionUtils.subtract(acsList, updatedAcsList);
                couldNotUpdateAcs = couldNotUpdateList.toArray(new String[couldNotUpdateList.size()]);

                addWarningMessage("Finished with warnings", updatedAcs.length + " objects were updated, "+
                        couldNotUpdateAcs.length+" objects couldn't be updated (do they exist?)");
            } else {
                addInfoMessage("Operation successful", updatedAcs.length+" objects were updated");
            }

        } catch (ClassNotFoundException e) {
            addErrorMessage("Could not find class: "+aoClassName, e.getMessage());
        }
    }

    public void aoClassNameChanged() {

        String newClassname = aoClassName;

        if (IntactPublication.class.getName().equals(newClassname)) {
            topicSelectItems = getCvService().getPublicationTopicSelectItems();
        } else if (IntactExperiment.class.getName().equals(newClassname)) {
            topicSelectItems = getCvService().getExperimentTopicSelectItems();
        } else if (IntactInteractionEvidence.class.getName().equals(newClassname)) {
            topicSelectItems = getCvService().getInteractionTopicSelectItems();
        } else if (IntactInteractor.class.getName().equals(newClassname)) {
            topicSelectItems = getCvService().getInteractorTopicSelectItems();
        }else if (IntactComplex.class.getName().equals(newClassname)) {
            topicSelectItems = getCvService().getComplexTopicSelectItems();
        }else if (IntactParticipantEvidence.class.getName().equals(newClassname)
                || IntactModelledParticipant.class.getName().equals(newClassname)) {
            topicSelectItems = getCvService().getParticipantTopicSelectItems();
        } else if (IntactFeatureEvidence.class.getName().equals(newClassname)
                || IntactModelledFeature.class.getName().equals(newClassname)) {
            topicSelectItems = getCvService().getFeatureTopicSelectItems();
        } else if (IntactSource.class.getName().equals(newClassname)) {
            topicSelectItems = getCvService().getNoClassSelectItems();
        } else if (IntactCvTerm.class.getName().equals(newClassname)) {
            topicSelectItems = getCvService().getCvObjectTopicSelectItems();
        } else {
            addErrorMessage("Error", "No class for type: "+newClassname);
        }
    }

    public String[] getAcs() {
        return acs;
    }

    public void setAcs(String[] acs) {
        this.acs = acs;
    }

    public String[] getUpdatedAcs() {
        return updatedAcs;
    }

    public void setUpdatedAcs(String[] updatedAcs) {
        this.updatedAcs = updatedAcs;
    }

    public String getAoClassName() {
        return aoClassName;
    }

    public void setAoClassName(String aoClassName) {
        this.aoClassName = aoClassName;
    }

    public List<SelectItem> getTopicSelectItems() {
        return topicSelectItems;
    }

    public String[] getCouldNotUpdateAcs() {
        return couldNotUpdateAcs;
    }

    public void setCouldNotUpdateAcs(String[] couldNotUpdateAcs) {
        this.couldNotUpdateAcs = couldNotUpdateAcs;
    }

    public boolean isReplaceIfTopicExists() {
        return replaceIfTopicExists;
    }

    public void setReplaceIfTopicExists(boolean replaceIfTopicExists) {
        this.replaceIfTopicExists = replaceIfTopicExists;
    }

    public IntactCvTerm getTopic() {
        return topic;
    }

    public void setTopic(IntactCvTerm topic) {
        this.topic = topic;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public BulkOperationsService getBulkOperations() {
        if (this.bulkOperations == null){
            this.bulkOperations = ApplicationContextProvider.getBean("bulkOperationsService");
        }
        return bulkOperations;
    }

    public CvObjectService getCvService() {
        if (this.cvService == null){
            this.cvService = ApplicationContextProvider.getBean("cvObjectService");
        }
        return cvService;
    }
}
