package uk.ac.ebi.intact.editor.controller.curate.interaction;

import psidev.psi.mi.jami.model.VariableParameter;
import psidev.psi.mi.jami.model.VariableParameterValue;
import uk.ac.ebi.intact.jami.model.extension.IntactVariableParameterValue;

import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A class that contains experimental conditions for a specific variable parameter
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/12/14</pre>
 */

public class ImportExperimentalCondition {

    private String description;
    private String unit;
    private List<SelectItem> variableValues;
    private IntactVariableParameterValue selectedValue;

    public ImportExperimentalCondition(VariableParameter param, Map<String, VariableParameterValue> valuesMap){
        if (param == null){
            throw new IllegalArgumentException("The variable parameter cannot be null");
        }
        this.description = param.getDescription();
        this.unit = param.getUnit() != null ? param.getUnit().getShortName() : "-";
        this.variableValues = new ArrayList<SelectItem>(param.getVariableValues().size());
        variableValues.add( new SelectItem( null, "-- Select variable condition value --", "-- Select variable condition value --", false, false, true ) );

        for (VariableParameterValue v : param.getVariableValues()){
            variableValues.add(new SelectItem( v, v.getValue(), v.getValue()+", order "+v.getOrder()));
            valuesMap.put(Long.toString(((IntactVariableParameterValue)v).getId()), v);
        }
    }

    public String getDescription() {
        return description;
    }

    public String getUnit() {
        return unit;
    }

    public List<SelectItem> getVariableValues() {
        return variableValues;
    }

    public IntactVariableParameterValue getSelectedValue() {
        return selectedValue;
    }

    public void setSelectedValue(IntactVariableParameterValue selectedValue) {
        this.selectedValue = selectedValue;
    }
}
