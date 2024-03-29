package uk.ac.ebi.intact.editor.controller.curate.util;

import psidev.psi.mi.jami.bridges.ensembl.EnsemblInteractorFetcher;
import psidev.psi.mi.jami.bridges.rna.central.RNACentralFetcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 30/05/2013
 * Time: 15:37
 * To change this template use File | Settings | File Templates.
 */
public class CheckIdentifier {

    private static final String CHEBI_ID_REGEX = "CHEBI\\:\\d+";
    private static final String ENSEMBLE_ID_REGEX = "^ENS[A-Z]*[FPTG]\\d{11}(\\.\\d+)?$";


    /**
     * Checks the Chebi identifier
     *
     * @param identifier
     * @return returns true if the identifier passed is in the form
     *         CHEBI:[0-9]+
     */
    public static boolean checkChebiId(String identifier) {
        return checkIdentifier(identifier, CHEBI_ID_REGEX);

    }

    /**
     * Checks the ENSEMBL identifier
     *
     * @param identifier
     * @return returns true if the identifier is in the form
     *         ^ENS[A-Z]*[FPTG]\d{11}(\.\d+)?$
     */
    public static boolean checkEnsembleId(String identifier) {
         return EnsemblInteractorFetcher.identifierPattern.matcher(identifier).find();
    }

    public static boolean checkRNACentralId(String identifier) {
        return RNACentralFetcher.identifierPattern.matcher(identifier).find();
    }

    private static boolean checkIdentifier(String identifier, String regex){
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(identifier);
        return m.find();
    }
}
