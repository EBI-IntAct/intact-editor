/**
 * Copyright 2011 The European Bioinformatics Institute, and others.
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
package uk.ac.ebi.intact.editor.controller.curate.interaction;

import org.hibernate.Hibernate;
import psidev.psi.mi.jami.model.Range;
import psidev.psi.mi.jami.utils.RangeUtils;
import uk.ac.ebi.intact.jami.model.extension.AbstractIntactFeature;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * TODO comment this class header.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class FeatureWrapper {

    private AbstractIntactFeature feature;
    private boolean selected;
    private String ranges;
    private List<AbstractIntactFeature> linkedFeatures;

    private AbstractIntactFeature selectedLinkedFeature;

    public FeatureWrapper(AbstractIntactFeature feature) {
        this.feature = feature;
        this.ranges = feature.getRanges().toString();

        initialiseRangesAsString();
        this.linkedFeatures = new ArrayList<AbstractIntactFeature>(this.feature.getLinkedFeatures());

        for (AbstractIntactFeature linked : this.linkedFeatures){
            if (linked.getAc() != null){
                Hibernate.initialize(linked.getLinkedFeatures());
            }
        }
    }

    private void initialiseRangesAsString(){

        StringBuffer buffer = new StringBuffer();
        buffer.append("[");

        Iterator<Range> rangeIterator = feature.getRanges().iterator();
        while (rangeIterator.hasNext()){
            Range range = rangeIterator.next();
            buffer.append(RangeUtils.convertRangeToString(range));
            if (rangeIterator.hasNext()){
                buffer.append(", ");
            }
        }
        buffer.append("]");
        this.ranges = buffer.toString();
    }


    public AbstractIntactFeature getFeature() {
        return feature;
    }

    public void setFeature(AbstractIntactFeature feature) {
        this.feature = feature;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getRanges() {
        return ranges;
    }

    public List<AbstractIntactFeature> getLinkedFeatures() {
        return linkedFeatures;
    }

    public AbstractIntactFeature getSelectedLinkedFeature() {
        return selectedLinkedFeature;
    }

    public void setSelectedLinkedFeature(AbstractIntactFeature selectedLinkedFeature) {
        this.selectedLinkedFeature = selectedLinkedFeature;
    }

    public String getRelatedFeatureDivs(){
        StringBuffer buffer = new StringBuffer();
        Iterator<AbstractIntactFeature> linkedIterator = this.linkedFeatures.iterator();
        while ( linkedIterator.hasNext()){
            AbstractIntactFeature linked = linkedIterator.next();
            if (linked.getAc() != null){
                 buffer.append("feature_").append(linked.getAc());
            }
            else{
                buffer.append("feature_").append(Integer.toString(linked.hashCode()));
            }
            if (linkedIterator.hasNext()){
                buffer.append(" ");
            }
        }
        return buffer.toString();
    }

    public String getRelatedFeatureDivs(AbstractIntactFeature linked){
        StringBuffer buffer = new StringBuffer();
        if (linked.getAc() != null){
            buffer.append("feature_").append(linked.getAc().replaceAll("\\-","_"));
        }
        else{
            buffer.append("feature_").append(Integer.toString(linked.hashCode()));
        }
        return buffer.toString();
    }

    public void reloadLinkedFeatures(){
        this.linkedFeatures.clear();
        this.linkedFeatures = new ArrayList<AbstractIntactFeature>(this.feature.getLinkedFeatures());
    }
}
