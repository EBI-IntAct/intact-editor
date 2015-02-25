package uk.ac.ebi.intact.editor.controller.search;

import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.primefaces.model.LazyDataModel;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.services.summary.ComplexSummary;
import uk.ac.ebi.intact.editor.services.search.SearchQueryService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 26/03/2013
 * Time: 11:21
 * To change this template use File | Settings | File Templates.
 */
@Controller("complexOrganismController")
@Scope( "conversation.access" )
@ConversationName("search")
@SuppressWarnings("unchecked")
public class SearchComplexOrganismController extends BaseController {

	private String ac;
	private String shortLabel;
	private String numComplexes;
    private String resultsOutcome;

	private LazyDataModel<ComplexSummary> complexes = null;

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
				complexes = getSearchQueryService().loadComplexesByOrganism(ac);
			}
		}
	}

	public LazyDataModel<ComplexSummary> getComplexes() {
		return complexes;
	}

	public String getShortLabel() {
		return shortLabel;
	}

	public void setShortLabel(String shortLabel) {
		this.shortLabel = shortLabel;
	}

	public void setNumComplexes(String numInteractors) {
		this.numComplexes = numInteractors;
	}

	public String getNumComplexes() {
		return numComplexes;
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

