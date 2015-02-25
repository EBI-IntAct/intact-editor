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
package uk.ac.ebi.intact.editor.services.curate.publication;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.editor.controller.curate.publication.PublicationController;
import uk.ac.ebi.intact.editor.services.AbstractEditorService;

import javax.faces.model.SelectItem;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Service
public class DatasetPopulator extends AbstractEditorService {

    private static final Log log = LogFactory.getLog( DatasetPopulator.class );

    private List<String> allDatasets;
    private List<SelectItem> allDatasetSelectItems;

    private boolean isInitialised = false;

    public DatasetPopulator() {

    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void loadData( ) {
        if ( log.isInfoEnabled() ) log.info( "Loading datasets" );

        synchronized (this) {
            if (isInitialised) {
                this.allDatasets = null;
                this.allDatasetSelectItems = null;
                isInitialised = false;
            }
            final Query query = getIntactDao().getEntityManager()
                    .createQuery("select distinct(a.value) from PublicationAnnotation a where a.topic.shortName = :datasetTopic order by a.value asc");
            query.setParameter("datasetTopic", PublicationController.DATASET);

            allDatasets = query.getResultList();

            allDatasetSelectItems = new ArrayList<SelectItem>(allDatasets.size() + 1);
            allDatasetSelectItems.add(new SelectItem(null, "-- Select Dataset --"));

            for (String dataset : allDatasets) {
                if (dataset != null) {
                    final SelectItem selectItem = createSelectItem(dataset);
                    allDatasetSelectItems.add(selectItem);
                }
            }
            isInitialised = true;
        }
    }

    public List<String> getAllDatasets() {
        return allDatasets;
    }

    public List<SelectItem> getAllDatasetSelectItems() {
        return allDatasetSelectItems;
    }

    public SelectItem createSelectItem( String dataset ) {
        if (dataset == null) throw new IllegalArgumentException("null dataset passed");
        SelectItem selectItem = null;

        if ( dataset.contains( "-" ) ) {
            String[] tokens = dataset.split( "-" );
            selectItem = new SelectItem( dataset, tokens[0].trim() );
        } else {
            selectItem = new SelectItem( dataset );
        }

        return selectItem;
    }

    public boolean isInitialised() {
        return isInitialised;
    }
}
