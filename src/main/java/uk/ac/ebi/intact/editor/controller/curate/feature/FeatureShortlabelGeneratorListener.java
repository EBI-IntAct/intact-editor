package uk.ac.ebi.intact.editor.controller.curate.feature;

import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.*;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.listener.ShortlabelGeneratorListener;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class FeatureShortlabelGeneratorListener implements ShortlabelGeneratorListener {

    private FeatureController featureController;
    private static String SHORTLABEL_NOT_GENERATED_MSG="Shortlabel could not be generated";
    private static String SHORTLABEL_MODIFIED_MSG="Shortlabel modified";
    private static String SHORTLABEL_UNMODIFIED_MSG="Shortlabel unmodified";
    private static String RESULTING_SEQUENCE_CHANGED="Resulting sequence changed";

    public FeatureShortlabelGeneratorListener(FeatureController fc){
           this.featureController=fc;
    }
    @Override
    public void onRangeError(RangeErrorEvent event) {
        // validated
        featureController.addErrorMessage(SHORTLABEL_NOT_GENERATED_MSG, event.getMessage());
    }

    @Override
    public void onModifiedMutationShortlabel(ModifiedMutationShortlabelEvent event) {
        // validated
        featureController.changed();
        featureController.addInfoMessage(SHORTLABEL_MODIFIED_MSG, "Previous Shortlabel: " + event.getOriginalShortlabel() + ", New Shortlabel: " + event.getFeatureEvidence().getShortName());
        }

    @Override
    public void onUnmodifiedMutationShortlabel(UnmodifiedMutationShortlabelEvent event) {
        // tested
        featureController.addInfoMessage(SHORTLABEL_UNMODIFIED_MSG, "Original Shortlabel was correct");
    }

    @Override
    public void onRetrieveObjectError(ObjRetrieveErrorEvent event) {
        // validated
        featureController.addErrorMessage(SHORTLABEL_NOT_GENERATED_MSG, event.getMessage());
    }

    @Override
    public void onAnnotationFound(AnnotationFoundEvent event) {
        // validated
        featureController.addInfoMessage(SHORTLABEL_NOT_GENERATED_MSG, "Because of annotation: " +event.getMessage());
    }

    @Override
    public void onSequenceError(SequenceErrorEvent event) {
        // validated
        featureController.addErrorMessage(SHORTLABEL_NOT_GENERATED_MSG, event.getMessage());
    }

    @Override
    public void onResultingSequenceChanged(ResultingSequenceChangedEvent event) {
        // tested
        if(event.getChangeType().equals(ResultingSequenceChangedEvent.ChangeType.WRONG_INSERTION)){
            featureController.addErrorMessage(SHORTLABEL_NOT_GENERATED_MSG, event.getMessage());
        }else {
            featureController.addInfoMessage(RESULTING_SEQUENCE_CHANGED, event.getMessage());
        }
    }

    @Override
    public void onObjectTypeError(TypeErrorEvent event) {
        // validated
        featureController.addErrorMessage(SHORTLABEL_NOT_GENERATED_MSG, event.getMessage());
    }
}
