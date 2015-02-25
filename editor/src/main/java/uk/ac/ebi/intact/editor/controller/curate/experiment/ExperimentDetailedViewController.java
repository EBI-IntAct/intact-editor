/**
 * Copyright 2012 The European Bioinformatics Institute, and others.
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
package uk.ac.ebi.intact.editor.controller.curate.experiment;

import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import psidev.psi.mi.jami.model.Annotation;
import psidev.psi.mi.jami.model.Parameter;
import psidev.psi.mi.jami.model.Stoichiometry;
import psidev.psi.mi.jami.utils.AnnotationUtils;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.services.curate.experiment.ExperimentDetailedViewService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.extension.IntactExperiment;
import uk.ac.ebi.intact.jami.model.lifecycle.Releasable;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName( "general" )
public class ExperimentDetailedViewController extends BaseController {

    private String ac;
    private ExperimentWrapper experimentWrapper;

	private List<String> annotationTopicsForExpOverview =
			Arrays.asList(
					Releasable.ACCEPTED,
                    Releasable.TO_BE_REVIEWED,
                    Releasable.ON_HOLD,
					"hidden",
					Annotation.COMMENT_MI,
					"remark-internal",
                    Releasable.CORRECTION_COMMENT,
                    Annotation.COMMENT,
					"MI:0591", //experiment description
					"MI:0627", //experiment modification
					"MI:0633" //data-processing
					);

    @Autowired
    private ExperimentController experimentController;

    private List<Annotation> annotations;

    @Resource(name = "experimentDetailedViewService")
    private transient ExperimentDetailedViewService experimentDetailedViewService;

    public ExperimentDetailedViewController() {
    }

    public void loadData( ComponentSystemEvent event ) {
        if (!FacesContext.getCurrentInstance().isPostback()) {
            // experiment already in experiment controller
            if (experimentController.getExperiment() != null && (ac == null || ac.equals(experimentController.getAc()))) {
                IntactExperiment experiment = experimentController.getExperiment();

                this.experimentWrapper = getExperimentDetailedViewService().loadExperimentWrapper(experiment);
                ac = experiment.getAc();
            }
            else if (ac != null) {
                this.experimentWrapper = getExperimentDetailedViewService().loadExperimentWrapperByAc(ac);
                if (experimentWrapper != null) {
                    experimentController.setExperiment(experimentWrapper.getExperiment());
                    experimentController.refreshParentControllers();
                }
            }

            if (experimentWrapper == null) {
                addErrorMessage("No Experiment to view with this AC", ac);
                return;
            }

            annotations = experimentAnnotationsByOverviewCriteria(experimentWrapper.getExperiment());
        }

    }

	private List<Annotation> experimentAnnotationsByOverviewCriteria(IntactExperiment experiment) {

		if (experiment == null) return null;

		List<Annotation> annotations = new ArrayList<Annotation>(experiment.getAnnotations().size());
		for (Annotation annot : experiment.getAnnotations()){
            for (String topics : annotationTopicsForExpOverview){
                if (AnnotationUtils.doesAnnotationHaveTopic(annot, topics, topics)){
                    annotations.add(annot);
                    break;
                }
            }
        }
		return annotations;
	}

    public List<Annotation> getAnnotations() {
        return annotations;
    }


	public String parameterAsString(Parameter param){
		if (param == null){
			return null;
		}

		String value = null;

		if (param.getValue().getFactor() != null) {
			value = String.valueOf(param.getValue().getFactor().doubleValue());

			if ((param.getValue().getExponent() != 0)
                    || ( param.getValue().getBase() != 10)) {
				value = param.getValue().getFactor() + "x" + param.getValue().getBase() + "^" + param.getValue().getExponent();
			}
			if (param.getUncertainty()!=null && param.getUncertainty().doubleValue() != 0.0) {
				value = value + " ~" + param.getUncertainty();
			}
		}
		return value;
	}

    public String stoichiometryAsString(Stoichiometry st){
        if (st == null){
            return null;
        }

        String value = null;

        if (st.getMaxValue() == st.getMinValue()){
           value = Integer.toString(st.getMinValue());
        }
        else{
            value = Integer.toString(st.getMinValue())+"-"+Integer.toString(st.getMaxValue());
        }
        return value;
    }

    public String getAc() {
        return ac;
    }

    public void setAc(String ac) {
        this.ac = ac;
    }

    public ExperimentWrapper getExperimentWrapper() {
        return experimentWrapper;
    }

    public ExperimentDetailedViewService getExperimentDetailedViewService() {
        if (this.experimentDetailedViewService == null){
            this.experimentDetailedViewService = ApplicationContextProvider.getBean("experimentDetailedViewService");
        }
        return experimentDetailedViewService;
    }
}
