package uk.ac.ebi.intact.editor.controller.curate.interaction;

import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.Interactor;
import psidev.psi.mi.jami.model.InteractorPool;
import psidev.psi.mi.jami.model.Xref;
import psidev.psi.mi.jami.utils.XrefUtils;
import uk.ac.ebi.intact.editor.controller.curate.util.ExperimentalRoleComparator;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.context.IntactConfiguration;
import uk.ac.ebi.intact.jami.model.extension.AbstractIntactFeature;
import uk.ac.ebi.intact.jami.model.extension.AbstractIntactParticipant;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractor;
import uk.ac.ebi.intact.jami.model.extension.IntactStoichiometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Wrapped participant to allow handling of special fields (eg. author given name) from the interaction view.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 2.0
 */
public class ParticipantWrapper {

    private AbstractIntactParticipant participant;
    private List<FeatureWrapper> features;
    private String interactorIdentity;

    public ParticipantWrapper( AbstractIntactParticipant participant) {
        this.participant = participant;
        features = new ArrayList<FeatureWrapper>(participant.getFeatures().size());

        for (Object feature : participant.getFeatures()) {
            features.add(new FeatureWrapper((AbstractIntactFeature)feature));
        }

        IntactInteractor interactor = (IntactInteractor)participant.getInteractor();

        this.interactorIdentity = initialiseInteractorIdentity(interactor);
    }

    private String initialiseInteractorIdentity(Interactor interactor) {
        // build list of identifiers
        StringBuilder sb = new StringBuilder(64);
        if (interactor instanceof InteractorPool){
            Iterator<Interactor> memberIterator = ((InteractorPool)interactor).iterator();
            while (memberIterator.hasNext()){
                sb.append( initialiseInteractorIdentity(memberIterator.next()));
                if( memberIterator.hasNext() ) {
                    sb.append( "|" );
                }
            }
        }
        else{
            final Collection<Xref> identities = XrefUtils.collectAllXrefsHavingQualifier(interactor.getIdentifiers(), Xref.IDENTITY_MI, Xref.IDENTITY);
            Iterator<Xref> refIterator = identities.iterator();
            // filter first intact acs
            IntactConfiguration intactConfig = ApplicationContextProvider.getBean("intactJamiConfiguration");
            while(refIterator.hasNext()){
                if (XrefUtils.isXrefFromDatabase(refIterator.next(),
                        intactConfig.getDefaultInstitution().getMIIdentifier(), intactConfig.getDefaultInstitution().getShortName())){
                    refIterator.remove();
                }
            }
            for ( Iterator<Xref> iterator = identities.iterator(); iterator.hasNext(); ) {
                Xref xref = iterator.next();
                sb.append( xref.getId() );
                if( iterator.hasNext() ) {
                    sb.append( "|" );
                }
            }
        }
        return sb.toString();
    }

    public AbstractIntactParticipant getParticipant() {
        return participant;
    }

    public void setParticipant( AbstractIntactParticipant participant ) {
        if (participant != null){
            this.participant = participant;
        }
    }

    public List<FeatureWrapper> getFeatures() {
        return features;
    }

    public int getMinStoichiometry(){
        return this.participant.getStoichiometry() != null ? this.participant.getStoichiometry().getMinValue() : 0;
    }

    public int getMaxStoichiometry(){
        return this.participant.getStoichiometry() != null ? this.participant.getStoichiometry().getMaxValue() : 0;
    }

    public void setMinStoichiometry(int stc){
        if (this.participant.getStoichiometry() == null){
            this.participant.setStoichiometry(new IntactStoichiometry(stc));
        }
        else {
            IntactStoichiometry stoichiometry = (IntactStoichiometry)participant.getStoichiometry();
            this.participant.setStoichiometry(new IntactStoichiometry(stc, Math.max(stc, stoichiometry.getMaxValue())));
        }
    }

    public void setMaxStoichiometry(int stc){
        if (this.participant.getStoichiometry() == null){
            this.participant.setStoichiometry(new IntactStoichiometry(stc));
        }
        else {
            IntactStoichiometry stoichiometry = (IntactStoichiometry)participant.getStoichiometry();
            this.participant.setStoichiometry(new IntactStoichiometry(Math.min(stc, stoichiometry.getMinValue()), stc));
        }
    }

    public int sortByExperimentalRole(Object cvExpRole1, Object cvExpRole2) {
        if(cvExpRole1 instanceof CvTerm && cvExpRole2 instanceof CvTerm){
            ExperimentalRoleComparator comparator = new ExperimentalRoleComparator();
            return comparator.compare((CvTerm)cvExpRole1, (CvTerm) cvExpRole2);
        }
        return 0;
    }

    public String getInteractorIdentity() {
        return interactorIdentity;
    }
}
