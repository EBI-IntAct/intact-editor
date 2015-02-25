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
package uk.ac.ebi.intact.editor.component;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.ac.ebi.intact.editor.controller.BaseController;
import uk.ac.ebi.intact.editor.services.curate.cvobject.CvObjectService;
import uk.ac.ebi.intact.jami.ApplicationContextProvider;
import uk.ac.ebi.intact.jami.model.extension.IntactCvTerm;

import javax.annotation.Resource;
import javax.faces.event.ComponentSystemEvent;

/**
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 */
@Component
@Scope("conversation.access")
public class InputCvTermController extends BaseController{

    private static final Log log = LogFactory.getLog( InputCvTermController.class );

    private TreeNode root;

    private String id;
    private String cvClass;
    private boolean visible;
    private String dialogId;

    private IntactCvTerm selected;

    @Resource(name = "cvObjectService")
    private transient CvObjectService cvService;

    public InputCvTermController() {
    }

    public void load( ComponentSystemEvent evt) {
        log.trace( "Loading CvObject with id '"+id+"'" );

        if (id == null) {
            throw new NullPointerException("id is null");
        }

        this.root = getCvService().loadCvTreeNode(id, this.cvClass);

        if (root == null) {
            throw new IllegalArgumentException("Root does not exist: " + id);
        }
        log.trace( "\tLoading completed. Root: "+root+ "(children="+root.getChildCount()+")" );
    }

    public String getDescription(IntactCvTerm cvObject) {
        if (cvObject == null) return null;

        return cvObject.getDefinition();
    }

    public String getIdentifier(IntactCvTerm cvObject) {
        if (cvObject == null) return null;

        String id = cvObject.getMIIdentifier();
        if (id == null){
            id = cvObject.getMODIdentifier();
        }
        if (id == null){
            id = cvObject.getPARIdentifier();
        }
        if (id == null){
            id = !cvObject.getIdentifiers().isEmpty() ? cvObject.getIdentifiers().iterator().next().getId() : null;
        }
        return id;
    }

    public TreeNode getRoot() {
        if (root == null) {
            return new DefaultTreeNode(null, null);
        }
        return root;
    }

    public IntactCvTerm getSelected() {
        return selected;
    }

    public void setSelected(IntactCvTerm selected) {
        this.selected = selected;
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCvClass() {
        return cvClass;
    }

    public void setCvClass(String cvClass) {
        this.cvClass = cvClass;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getDialogId() {
        return dialogId;
    }

    public void setDialogId(String dialogId) {
        this.dialogId = dialogId;
    }

    public CvObjectService getCvService() {
        if (this.cvService == null){
            this.cvService = ApplicationContextProvider.getBean("cvObjectService");
        }
        return cvService;
    }
}
