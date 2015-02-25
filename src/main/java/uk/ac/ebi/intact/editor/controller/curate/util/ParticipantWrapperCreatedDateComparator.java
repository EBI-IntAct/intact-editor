package uk.ac.ebi.intact.editor.controller.curate.util;

import uk.ac.ebi.intact.editor.controller.curate.interaction.ParticipantWrapper;

import java.util.Comparator;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 29/04/2013
 * Time: 14:32
 * Sort the Participant Wrapper by the first experimental role. The bait should be the first one and the rest
 * can be sorted by alphabetic order. If they are equals we can try to short by name.
 */
public class ParticipantWrapperCreatedDateComparator implements Comparator<ParticipantWrapper> {

    @Override
    public int compare(ParticipantWrapper pw1, ParticipantWrapper pw2) {
        if (pw1 != null && pw2 != null) {
            Date created1 = pw1.getParticipant().getCreated();
            Date created2 = pw2.getParticipant().getCreated();
            if (created1 != null && created2 != null){
                int comp = created1.compareTo(created2);
                if (comp != 0){
                    return comp;
                }
                else{
                    return pw1.getParticipant().getInteractor().getShortName().compareTo(pw2.getParticipant().getInteractor().getShortName());
                }
            }
            else if (created1 == null && created2 != null) {
                return -1;
            } else if (created1 != null && created2 == null) {
                return 1;
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
