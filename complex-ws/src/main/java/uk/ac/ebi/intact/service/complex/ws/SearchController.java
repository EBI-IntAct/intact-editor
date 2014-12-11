package uk.ac.ebi.intact.service.complex.ws;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ebi.intact.dataexchange.psimi.solr.complex.ComplexFieldNames;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.extension.IntactComplex;
import uk.ac.ebi.intact.service.complex.ws.model.*;
import uk.ac.ebi.intact.service.complex.ws.utils.IntactComplexUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Controller
public class SearchController {

    /*
     -- BASIC KNOWLEDGE ABOUT SPRING MVC CONTROLLERS --
      * They look like the next one:
      @RequestMapping(value = "/<path to listen>/{<variable>}")
	  public <ResultType> search(@PathVariable String <variable>) {
          ...
	  }

	  * First of all, we have the @RequestMapping annotation where you can
	    use these options:
	     - headers: Same format for any environment: a sequence of
	                "My-Header=myValue" style expressions
	     - method: The HTTP request methods to map to, narrowing the primary
	               mapping: GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE.
	     - params: Same format for any environment: a sequence of
	               "myParam=myValue" style expressions
	     - value: Ant-style path patterns are also supported (e.g. "/myPath/*.do").

      * Next we have the function signature, with the result type to return,
        the name of the function and the parameters to it. We could see the
        @PathVariable in the parameters it is to say that the content between
        { and } is assigned to this variable. NOTE: They must have the same name

        Moreover, we can have @RequestedParam if we need to read or use a parameter
        provided using "?name=value" way. WE WANT TO DO THAT WITH THE FORMAT,
        BUT THIS PARAMETER IS CONTROLLED BY THE ContentNegotiatingViewResolver BEAN
        IN THE SPRING FILE.
     */

    /********************************/
    /*      Private attributes      */
    /********************************/
    @Autowired
    private DataProvider dataProvider ;
    @Qualifier("intactDao")
    private IntactDao intactDao;
    private static final Log log = LogFactory.getLog(SearchController.class);

    /****************************/
    /*      Public methods      */
    /****************************/
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String showHomeHelp(){
        return "home";
    }
    @RequestMapping(value = "/search/", method = RequestMethod.GET)
    public String showSearchHelp(){
        return "search";
    }
    @RequestMapping(value = "/details/", method = RequestMethod.GET)
    public String showDetailsHelp(){
        return "details";
    }
    @RequestMapping(value = "/count/{query}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public String count(@PathVariable String query, ModelMap model) throws SolrServerException {
        String q = null;
        try {
            q = URIUtil.decode(query);
        } catch (URIException e) {
            e.printStackTrace();
        }
        long total = query(q, null, null, null, null).getTotalNumberOfResults();
        model.addAttribute("count", total);
        return "count";
    }
    /*
     - We can access to that method using:
         http://<servername>:<port>/search/<something to query>
       and
         http://<servername>:<port>/search/<something to query>?format=<type>
     - If we do not use the format parameter we will receive the answer in json
     - Only listen request via GET never via POST.
     - Does not change the query.
     */
    @RequestMapping(value = "/search/{query}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody
    ComplexRestResult search(@PathVariable String query,
                                    @RequestParam (required = false) String first,
                                    @RequestParam (required = false) String number,
                                    @RequestParam (required = false) String filters,
                                    @RequestParam (required = false) String facets) throws SolrServerException {
        return query(query, first, number, filters, facets);
	}

    /*
     - We can access to that method using:
         http://<servername>:<port>/interactor/<something to query>
       and
         http://<servername>:<port>/interactor/<something to query>?format=<type>
     - If we do not use the format parameter we will receive the answer in json
     - Only listen request via GET never via POST.
     - Force to query only in the id, alias and pxref fields.
     */
    @RequestMapping(value = "/interactor/{query}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ComplexRestResult searchInteractor(@PathVariable String query,
                                              @RequestParam (required = false) String first,
                                              @RequestParam (required = false) String number) throws SolrServerException {

        // Query improvement. Force to query only in the id, alias and pxref
        // fields.
        List<String> fields = new ArrayList<String>();
        fields.add(ComplexFieldNames.INTERACTOR_ID);
        fields.add(ComplexFieldNames.INTERACTOR_ALIAS);
        fields.add(ComplexFieldNames.INTERACTOR_XREF);
        // Retrieve data using that parameters and return it
        return query(improveQuery(query, fields), first, number, null, null);
    }

    /*
     - We can access to that method using:
         http://<servername>:<port>/complex/<something to query>
       and
         http://<servername>:<port>/complex/<something to query>?format=<type>
     - If we do not use the format parameter we will receive the answer in json
     - Only listen request via GET never via POST.
     - Force to query only in the complex_id, complex_alias and complex_xref
       fields.
     */
    @RequestMapping(value = "/complex/{query}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ComplexRestResult searchInteraction(@PathVariable String query,
                                               @RequestParam (required = false) String first,
                                               @RequestParam (required = false) String number) throws SolrServerException {

        // Query improvement. Force to query only in the complex_id,
        // complex_alias and complex_xref fields.
        List<String> fields = new ArrayList<String>();
        fields.add(ComplexFieldNames.COMPLEX_ID);
        fields.add(ComplexFieldNames.COMPLEX_ALIAS);
        fields.add(ComplexFieldNames.COMPLEX_XREF);
        // Retrieve data using that parameters and return it
        return query(improveQuery(query, fields), first, number, null, null);
    }

    /*
     - We can access to that method using:
         http://<servername>:<port>/organism/<something to query>
       and
         http://<servername>:<port>/organism/<something to query>?format=<type>
     - If we do not use the format parameter we will receive the answer in json
     - Only listen request via GET never via POST.
     - Force to query only in the organism_name and species fields.
     */
    @RequestMapping(value = "/organism/{query}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ComplexRestResult searchOrganism(@PathVariable String query,
                                            @RequestParam (required = false) String first,
                                            @RequestParam (required = false) String number) throws SolrServerException {

        // Query improvement. Force to query only in the organism_name and
        // species (complex_organism) fields.
        List<String> fields = new ArrayList<String>();
        fields.add(ComplexFieldNames.ORGANISM_NAME);
        fields.add(ComplexFieldNames.COMPLEX_ORGANISM);
        // Retrieve data using that parameters and return it
        return query(improveQuery(query, fields), first, number, null, null);
    }

    /*
     - We can access to that method using:
         http://<servername>:<port>/details/<ac of a complex>
       and
         http://<servername>:<port>/details/<ac of a complex>?format=<type>
     - If we do not use the format parameter we will receive the answer in json
     - Only listen request via GET never via POST.
     - Query the information in our database about the ac of the complex.
     */
    @RequestMapping(value = "/details/{ac}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public @ResponseBody ComplexDetails retrieveComplex(@PathVariable String ac) throws Exception {
        IntactComplex complex = intactDao.getComplexDao().getByAc(ac);
        ComplexDetails details = null;
        // Function
        if ( complex != null ) {
            details = new ComplexDetails();
            details.setAc(complex.getAc());
            details.setFunction         ( IntactComplexUtils.getFunction(complex) );
            details.setProperties       ( IntactComplexUtils.getProperties(complex) );
            details.setDisease          ( IntactComplexUtils.getDisease(complex) );
            details.setLigand           ( IntactComplexUtils.getLigand(complex) );
            details.setComplexAssembly  ( IntactComplexUtils.getComplexAssembly(complex) );
            details.setName             ( IntactComplexUtils.getComplexName(complex) );
            details.setSynonyms         ( IntactComplexUtils.getComplexSynonyms(complex) );
            details.setSystematicName   ( IntactComplexUtils.getSystematicName(complex) );
            details.setSpecies          ( IntactComplexUtils.getSpeciesName(complex) + "; " +
                                          IntactComplexUtils.getSpeciesTaxId(complex) );

            IntactComplexUtils.setParticipants(complex, details);
            IntactComplexUtils.setCrossReferences(complex, details);
        }
        else{
            throw new Exception();
        }
        return details;
    }

    @RequestMapping(value = "/export/{ac}", method = RequestMethod.GET)
//    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public @ResponseBody ComplexExport exportComplex(@PathVariable String ac) {
        ComplexExport export = null;
        //TODO
        return export;
    }

    /*******************************/
    /*      Protected methods      */
    /*******************************/
    // This method controls the first and number parameters and retrieve data
    protected ComplexRestResult query(String query, String first, String number, String filters, String facets) throws SolrServerException {
        // Get parameters (if we have them)
        int f, n;
        // If we have first parameter parse it to integer
        if ( first != null ) f = Integer.parseInt(first);
            // else set first parameter to 0
        else f = 0;
        // If we have number parameter parse it to integer
        if ( number != null ) n = Integer.parseInt(number);
            // else set number parameter to max integer - first (to avoid problems)
        else n = Integer.MAX_VALUE - f;
        // Retrieve data using that parameters and return it
        return this.dataProvider.getData( query, f, n, filters , facets);
    }

    // This method is to force to query only for a list of fields
    protected String improveQuery(String query, List<String> fields) {
        StringBuilder improvedQuery = new StringBuilder();
        for ( String field : fields ) {
            improvedQuery.append(field)
                    .append(":(")
                    .append(query)
                    .append(")");
        }
        return improvedQuery.toString();
    }



    @ExceptionHandler(SolrServerException.class)
    public ModelAndView handleSolrServerException(SolrServerException e, HttpServletResponse response){
        ModelAndView model = new ModelAndView("error/503");
        response.setStatus(503);
        return model;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAllExceptions(Exception e, HttpServletResponse response){
        ModelAndView model = new ModelAndView("error/404");
        response.setStatus(404);
        return model;
    }

}
