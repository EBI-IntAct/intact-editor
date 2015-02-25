package uk.ac.ebi.intact.editor.controller.search;

import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.primefaces.model.LazyDataModel;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.services.search.SearchQueryService;
import uk.ac.ebi.intact.editor.services.summary.InteractionSummary;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 19/03/2013
 * Time: 11:22
 * To change this template use File | Settings | File Templates.
 */
@Controller
@Scope( "conversation.access" )
@ConversationName("search")
public class SearchInteractionsMoleculeController extends BaseController {

	private String ac;
	private LazyDataModel<InteractionSummary> interactions = null;
	private String shortLabel;
	private String numInteractions;
    private String resultsOutcome;

	public String getAc() {
		return ac;
	}

	public void setAc(String ac) {
		this.ac = ac;
	}

    @Resource(name = "searchQueryService")
    private transient SearchQueryService searchQueryService;

	public void loadData(ComponentSystemEvent evt) {
		if (!FacesContext.getCurrentInstance().isPostback()) {

			if (ac != null) {
				interactions = getSearchQueryService().loadInteractionsByMolecule(ac);
			}
		}
	}

	public LazyDataModel<InteractionSummary> getInteractions() {
		return interactions;
	}

	public String getShortLabel() {
		return shortLabel;
	}

	public void setShortLabel(String shortLabel) {
		this.shortLabel = shortLabel;
	}

	public void setNumInteractions(String numInteractions) {
		this.numInteractions = numInteractions;
	}

	public String getNumInteractions() {
		return numInteractions;
	}

    public SearchQueryService getSearchQueryService() {
        if (this.searchQueryService == null){
            this.searchQueryService = ApplicationContextProvider.getBean("searchQueryService");
        }
        return searchQueryService;
    }

    public String getResultsOutcome() {
        return resultsOutcome;
    }

    public void setResultsOutcome(String resultsOutcome) {
        this.resultsOutcome = resultsOutcome;
    }
}
