package uk.ac.ebi.intact.editor.controller.curate.util;

import psidev.psi.mi.jami.model.CvTerm;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 29/04/2013
 * Time: 15:54
 * To change this template use File | Settings | File Templates.
 */
public class ExperimentalRoleComparator implements Comparator<CvTerm> {

    @Override
    public int compare(CvTerm cvExpRole1, CvTerm cvExpRole2) {
        if (cvExpRole1 != null && cvExpRole2 != null) {
            if (cvExpRole1.getShortName() != null && cvExpRole2.getShortName() != null) {
                if (cvExpRole1.getShortName().equals("bait") && !cvExpRole2.getShortName().equals("bait")) {
                    return -1;
                } else if (cvExpRole2.getShortName().equals("bait") && !cvExpRole1.getShortName().equals("bait")) {
                    return 1;
                } else {
                    return cvExpRole1.getShortName().compareTo(cvExpRole2.getShortName());
                }
            }
            else if (cvExpRole1.getShortName() == null && cvExpRole2.getShortName() != null) {
                    return -1;
            } else if (cvExpRole1.getShortName() != null && cvExpRole2.getShortName() == null) {
                    return 1;
            }
        } else if (cvExpRole1 == null && cvExpRole2 != null) {
                return -1;
        } else if (cvExpRole1 != null && cvExpRole2 == null) {
                return 1;
        }
        return 0;
    }
}
