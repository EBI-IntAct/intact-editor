/**
 * Copyright 2010 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.intact.editor.controller.admin;

import org.apache.myfaces.orchestra.conversation.annotations.ConversationName;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.services.admin.HqlQueryService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.IntactPrimaryObject;

import javax.annotation.Resource;
import javax.faces.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Controller
@Scope( "conversation.access" )
@ConversationName("hqlSearch")
public class HqlRunnerController extends BaseController {

    private String hqlQuery;
    private int maxResults;
    private String nativeQuery;

    private Collection<? extends IntactPrimaryObject> results;
    private Collection<Object[]> nativeResults;
    private List<String> columns;

    @Resource(name = "hqlQueryService")
    private transient HqlQueryService hqlQueryService;

    public HqlRunnerController() {
        super();
        maxResults = HqlQueryService.MAX_RESULTS;
    }

    public List<String> getColumns(){
        return columns != null ? columns :Collections.EMPTY_LIST;
    }

    public  void runQuery(ActionEvent evt) {
        if (hqlQuery != null && hqlQuery.length() > 0){
            try {
                long startTime = System.currentTimeMillis();

                results = getHqlQueryService().runQuery(maxResults, hqlQuery);

                long duration = System.currentTimeMillis() - startTime;

                addInfoMessage("Execution successful ("+duration+"ms)", "Results: "
                        +(results.size() == HqlQueryService.MAX_RESULTS ? "More than " : "")+results.size());

            } catch (Throwable e) {
                addErrorMessage("Problem running query", e.getMessage());
            }
        }
    }

    public void runNativeQuery(ActionEvent evt) {
        if (nativeQuery != null && nativeQuery.length() > 0){
            try {
                long startTime = System.currentTimeMillis();

                nativeResults = getHqlQueryService().runNativeQuery(maxResults, nativeQuery);

                columns = new ArrayList<String>();
                if (!nativeResults.isEmpty()){
                    Object[] firstObjects = nativeResults.iterator().next();
                    columns = new ArrayList<String>(firstObjects.length);
                    int index = 0;
                    for (Object o : firstObjects){
                        index++;
                        columns.add("Column "+index);
                    }
                }

                long duration = System.currentTimeMillis() - startTime;

                addInfoMessage("Execution successful ("+duration+"ms)", "Results: "
                        +(nativeResults.size() == HqlQueryService.MAX_RESULTS ? "More than " : "")+nativeResults.size());

            } catch (Throwable e) {
                addErrorMessage("Problem running query", e.getMessage());
            }
        }
    }

    public String getHqlQuery() {
        return hqlQuery;
    }

    public void setHqlQuery(String hqlQuery) {
        this.hqlQuery = hqlQuery;
    }

    public String getNativeQuery() {
        return nativeQuery;
    }

    public void setNativeQuery(String nativeQuery) {
        this.nativeQuery = nativeQuery;
    }

    public Collection<Object[]> getNativeResults() {
        return nativeResults;
    }

    public Collection<? extends IntactPrimaryObject> getResults() {
        return results;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public HqlQueryService getHqlQueryService() {
        if (this.hqlQueryService == null){
            this.hqlQueryService = ApplicationContextProvider.getBean("hqlQueryService");
        }
        return hqlQueryService;
    }
}
