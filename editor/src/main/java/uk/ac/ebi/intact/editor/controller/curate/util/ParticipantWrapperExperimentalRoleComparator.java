package uk.ac.ebi.intact.editor.controller.curate.util;

import psidev.psi.mi.jami.model.ParticipantEvidence;
import uk.ac.ebi.intact.editor.controller.curate.interaction.ParticipantWrapper;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 29/04/2013
 * Time: 14:32
 * Sort the Participant Wrapper by the first experimental role. The bait should be the first one and the rest
 * can be sorted by alphabetic order. If they are equals we can try to short by name.
 */
public class ParticipantWrapperExperimentalRoleComparator implements Comparator<ParticipantWrapper> {

    @Override
    public int compare(ParticipantWrapper pw1, ParticipantWrapper pw2) {
        if (pw1 != null && pw2 != null) {
            ExperimentalRoleComparator comparator = new ExperimentalRoleComparator();
            int result = comparator.compare(((ParticipantEvidence)pw1.getParticipant()).getExperimentalRole(),
                    ((ParticipantEvidence)pw2.getParticipant()).getExperimentalRole());

            //If they are identical we sort by author given name
            if (result == 0) {
                return pw1.getParticipant().getInteractor().getShortName().compareTo(pw2.getParticipant().getInteractor().getShortName());

            } else {
                return result;
            }
        } else {
            if (pw1 == null && pw2 != null) {
                return -1;
            } else if (pw1 != null && pw2 == null) {
                return 1;
            }
        }
        return 0;
    }
}
