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

import psidev.psi.mi.jami.model.*;
import psidev.psi.mi.jami.utils.RangeUtils;
import uk.ac.ebi.intact.jami.model.extension.IntactExperiment;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractionEvidence;
import uk.ac.ebi.intact.jami.model.extension.IntactParticipantEvidence;

import java.util.*;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ExperimentWrapper {

    private IntactExperiment experiment;
    private List<IntactInteractionEvidence> interactions;
    private Map<String, List<Annotation>> interactionAnnotations;
    private Map<String, List<Xref>> interactionXrefs;
    private Map<String, List<Parameter>> interactionsParameters;

    private Map<String, List<IntactParticipantEvidence>> componentsMap;
    private Map<String, List<FeatureSummary>> componentFeatures;

    public ExperimentWrapper(IntactExperiment experiment) {
        this.experiment = experiment;

        // load interactions
        interactions = new ArrayList<IntactInteractionEvidence>(experiment.getInteractionEvidences().size());
        interactionAnnotations = new HashMap<String, List<Annotation>>(interactions.size());
        interactionXrefs = new HashMap<String, List<Xref>>(interactions.size());
        interactionsParameters = new HashMap<String, List<Parameter>>(interactions.size());
        componentsMap = new HashMap<String, List<IntactParticipantEvidence>>(interactions.size());
        componentFeatures = new HashMap<String, List<FeatureSummary>>(interactions.size() * 2);

        for (InteractionEvidence ev : experiment.getInteractionEvidences()){
            IntactInteractionEvidence intactEv = (IntactInteractionEvidence)ev;
            interactions.add(intactEv);
            String ac = intactEv.getAc() != null ? intactEv.getAc() : Integer.toString(intactEv.hashCode());
            interactionAnnotations.put(ac, new ArrayList<Annotation>(intactEv.getDbAnnotations()));
            interactionsParameters.put(ac, new ArrayList<Parameter>(intactEv.getParameters()));
            interactionXrefs.put(ac, new ArrayList<Xref>(intactEv.getDbXrefs()));
            componentsMap.put(ac, new ArrayList<IntactParticipantEvidence>(sortedParticipants(intactEv)));
        }
        Collections.sort(interactions, new InteractionAlphabeticalOrder());
    }

    public String featureAsString(Feature feature) {
        StringBuilder sb = new StringBuilder();
        if (feature.getShortName() != null){
            sb.append(feature.getShortName());
        }
        final Collection<Range> ranges = feature.getRanges();
        final Iterator<Range> iterator = ranges.iterator();

        while (iterator.hasNext()) {
            Range next = iterator.next();
            sb.append("[");
            sb.append(RangeUtils.convertRangeToString(next));
            sb.append("]");

            if (iterator.hasNext()) sb.append(", ");
        }

        if (feature.getType() != null) {
            sb.append(" ");
            sb.append(feature.getType().getShortName());
        }

        return sb.toString();
    }


    public List<IntactParticipantEvidence> sortedParticipants(IntactInteractionEvidence interaction) {
        if (interaction == null ) return Collections.EMPTY_LIST;

        List<IntactParticipantEvidence> components = new ArrayList<IntactParticipantEvidence>(interaction.getParticipants().size());
        for (ParticipantEvidence comp : interaction.getParticipants()){
            IntactParticipantEvidence part = (IntactParticipantEvidence)comp;
            String compAc = part.getAc() != null ? part.getAc() : Integer.toString(part.hashCode());

            List<FeatureSummary> features = new ArrayList<FeatureSummary>(part.getFeatures().size());
            componentFeatures.put(compAc, features);
            for (Feature f : comp.getFeatures()){
                Collection<String> bindDomains = new ArrayList<String>(f.getLinkedFeatures().size());
                for (Object f2 : f.getLinkedFeatures()){
                     bindDomains.add(featureAsString((Feature)f2));
                }
                features.add(new FeatureSummary(featureAsString(f),bindDomains));
            }

            comp.getInteractor().getIdentifiers().size();
            components.add(part);
        }

        Collections.sort(components, new ComponentOrder());
        return components;
    }

    public IntactExperiment getExperiment() {
        return experiment;
    }

    public List<IntactInteractionEvidence> getInteractions() {
        return interactions;
    }

    public List<Annotation> getInteractionAnnotations(IntactInteractionEvidence interaction) {
        String ac = interaction.getAc() != null ? interaction.getAc() : Integer.toString(interaction.hashCode());

        return interactionAnnotations.get(ac);
    }

    public List<Xref> getInteractionXrefs(IntactInteractionEvidence interaction) {
        String ac = interaction.getAc() != null ? interaction.getAc() : Integer.toString(interaction.hashCode());

        return interactionXrefs.get(ac);
    }

    public List<Parameter> getInteractionParameters(IntactInteractionEvidence interaction) {
        String ac = interaction.getAc() != null ? interaction.getAc() : Integer.toString(interaction.hashCode());

        return interactionsParameters.get(ac);
    }

    public List<IntactParticipantEvidence> getParticipants(IntactInteractionEvidence interaction){
        String ac = interaction.getAc() != null ? interaction.getAc() : Integer.toString(interaction.hashCode());

        return componentsMap.get(ac);
    }

    public List<FeatureSummary> getFeatures(IntactParticipantEvidence component){
        String ac = component.getAc() != null ? component.getAc() : Integer.toString(component.hashCode());

        return componentFeatures.get(ac);
    }

    private class InteractionAlphabeticalOrder implements Comparator<IntactInteractionEvidence> {

        @Override
        public int compare(IntactInteractionEvidence o1, IntactInteractionEvidence o2) {
            if (o1.getShortName() == null && o2.getShortName() == null){
                return 0;
            }
            else if (o1.getShortName() == null){
                return 1;
            }
            else if (o2.getShortName() == null){
                return -11;
            }
            else{
                return o1.getShortName().compareTo(o2.getShortName());
            }
        }
    }

    private static class ComponentOrder implements Comparator<IntactParticipantEvidence> {

        private static Map<String,Integer> rolePriorities = new HashMap<String, Integer>();

        static {
            rolePriorities.put(Participant.BAIT_ROLE_MI, 1);
            rolePriorities.put(Participant.ENZYME_ROLE_MI, 5);
            rolePriorities.put(Participant.ENZYME_TARGET_ROLE_MI, 10);
            rolePriorities.put(Participant.PREY_MI, 15);
        }

        @Override
        public int compare(IntactParticipantEvidence o1, IntactParticipantEvidence o2) {
            Integer priority1 = rolePriorities.get(experimentalRoleIdentifierFor(o1));
            Integer priority2 = rolePriorities.get(experimentalRoleIdentifierFor(o2));

            if (priority1 != null && priority2 == null) {
                return 1;
            }

            if (priority1 == null && priority2 != null) {
                return -1;
            }

            if (priority1 != null && !priority1.equals(priority2)) {
                return priority1.compareTo(priority2);
            }

            final Collection<Xref> idXrefs1 = o1.getInteractor().getIdentifiers();
            final Collection<Xref> idXrefs2 = o2.getInteractor().getIdentifiers();

            final Xref idXref1 = (idXrefs1.isEmpty())? null : idXrefs1.iterator().next();
            final Xref idXref2 = (idXrefs2.isEmpty())? null : idXrefs2.iterator().next();

            String id1 = (idXref1 != null)? idXref1.getId() : "";
            String id2 = (idXref2 != null)? idXref2.getId() : "";

            return id1.compareTo(id2);
        }

        private String experimentalRoleIdentifierFor(IntactParticipantEvidence o1) {
            return o1.getExperimentalRole().getMIIdentifier();
        }
    }
}
