package uk.ac.ebi.intact.editor.controller.search;

import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.primefaces.model.LazyDataModel;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.services.search.SearchQueryService;
import uk.ac.ebi.intact.editor.services.summary.MoleculeSummary;
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
public class SearchMoleculeSetsController extends BaseController {

	private String ac;
	private LazyDataModel<MoleculeSummary> moleculeSets = null;
	private String shortLabel;
	private String numMoleculeSets;

    @Resource(name = "searchQueryService")
    private transient SearchQueryService searchQueryService;

	public String getAc() {
		return ac;
	}

	public void setAc(String ac) {
		this.ac = ac;
	}

	public void loadData(ComponentSystemEvent evt) {
		if (!FacesContext.getCurrentInstance().isPostback()) {

			if (ac != null) {
				moleculeSets = getSearchQueryService().loadMoleculeSetsByMolecule(ac);
			}
		}
	}

	public LazyDataModel<MoleculeSummary> getMoleculeSets() {
		return moleculeSets;
	}

	public String getShortLabel() {
		return shortLabel;
	}

	public void setShortLabel(String shortLabel) {
		this.shortLabel = shortLabel;
	}

	public void setNumMoleculeSets(String numInteractions) {
		this.numMoleculeSets = numInteractions;
	}

	public String getNumMoleculeSets() {
		return numMoleculeSets;
	}

    public SearchQueryService getSearchQueryService() {
        if (this.searchQueryService == null){
            this.searchQueryService = ApplicationContextProvider.getBean("searchQueryService");
        }
        return searchQueryService;
    }
}
