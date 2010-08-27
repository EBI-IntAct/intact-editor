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
package uk.ac.ebi.intact.editor.application;


import org.primefaces.util.Constants;

import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.application.ViewExpiredException;
import javax.faces.component.UIViewRoot;
import javax.faces.context.*;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Iterator;


/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ViewExpiredExceptionHandler extends ExceptionHandlerWrapper {

  private ExceptionHandler wrapped;

    public ViewExpiredExceptionHandler(ExceptionHandler wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public ExceptionHandler getWrapped() {
        return this.wrapped;
    }

    @Override
    public void handle() throws FacesException {
        String redirectPage = null;
        for (Iterator<ExceptionQueuedEvent> i = getUnhandledExceptionQueuedEvents().iterator(); i.hasNext();) {
            ExceptionQueuedEvent event = i.next();
            ExceptionQueuedEventContext context = (ExceptionQueuedEventContext) event.getSource();

            Throwable t = context.getException();

            if (t instanceof ViewExpiredException) {
                ViewExpiredException vee = (ViewExpiredException) t;
                FacesContext fc = FacesContext.getCurrentInstance();
                HttpSession session = (HttpSession) fc.getExternalContext().getSession(true);
                try {
                    // Push some useful stuff to the request scope for use in the page
                    session.setAttribute("currentViewId", vee.getViewId());

                    redirectPage = "/viewExpired";
                } finally {
                    i.remove();
                }
                doRedirect(fc, redirectPage);
            }
        }

        // At this point, the queue will not contain any ViewExpiredEvents.
        // Therefore, let the parent handle them.
        getWrapped().handle();
    }

    public void doRedirect(FacesContext fc, String redirectPage) throws FacesException{
        ExternalContext ec = fc.getExternalContext();

        try {
            // workaround for PrimeFaces
            //new RequestContextImpl(ec);

            if (ec.getRequestParameterMap().containsKey(Constants.PARTIAL_PROCESS_PARAM)
                    && !ec.getRequestParameterMap().get(Constants.PARTIAL_PROCESS_PARAM).equals("@all")) {
                fc.setViewRoot(new UIViewRoot());
            }

            // fix for renderer kit (Mojarra's and PrimeFaces's ajax redirect)
            if (( fc.getPartialViewContext().isPartialRequest())
                    && fc.getResponseWriter() == null
                    && fc.getRenderKit() == null) {
                ServletResponse response = (ServletResponse) ec.getResponse();
                ServletRequest request = (ServletRequest) ec.getRequest();
                response.setCharacterEncoding(request.getCharacterEncoding());

                RenderKitFactory factory =
                        (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
                RenderKit renderKit =
                        factory.getRenderKit(fc, fc.getApplication().getViewHandler().calculateRenderKitId(fc));
                ResponseWriter responseWriter =
                        renderKit.createResponseWriter(response.getWriter(), null, request.getCharacterEncoding());
                fc.setResponseWriter(responseWriter);
            }

            ec.redirect(ec.getRequestContextPath() + (redirectPage != null ? redirectPage : ""));
        } catch (IOException e) {
            System.out.println("Redirect to the specified page '" + redirectPage + "' failed");
            throw new FacesException(e);
        }
    }
}