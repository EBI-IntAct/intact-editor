/**
 * Copyright 2011 The European Bioinformatics Institute, and others.
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
package uk.ac.ebi.intact.editor.ws;

import javax.ws.rs.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@Path("/mi")
public interface MiExportService {

    String FORMAT_XML254_COMPACT = "xml254_compact";
    String FORMAT_XML300_COMPACT = "xml300_compact";
    String FORMAT_XML254_EXPANDED = "xml254_expanded";
    String FORMAT_XML300_EXPANDED = "xml300_expanded";
    String FORMAT_MITAB25 = "tab25";
    String FORMAT_MITAB26 = "tab26";
    String FORMAT_MITAB27 = "tab27";
    String FORMAT_HTML = "html";
    String FORMAT_JSON = "json";
    String FORMAT_JSON_BINARY = "json_binary";
    String FORMAT_FEBS_SDA = "sda";

    @GET
    @Path("/publication")
    Object exportPublication(@QueryParam("ac") String id,
                             @DefaultValue("tab25") @QueryParam("format") String format);

    @GET
    @Path("/experiment")
    Object exportExperiment(@QueryParam("ac") String id,
                            @DefaultValue("tab25") @QueryParam("format") String format);

    @GET
    @Path("/interaction")
    Object exportInteraction(@QueryParam("ac") String id,
                             @DefaultValue("tab25") @QueryParam("format") String format);

    @GET
    @Path("/complex")
    Object exportComplex(@QueryParam("ac") String id,
                             @DefaultValue("xml254") @QueryParam("format") String format);

    public void writeEvidences(OutputStream outputStream, String format, String countQuery, String query,
                      Map<String, Object> parameters) throws IOException;
    public void writeComplexes(OutputStream outputStream, String format, String countQuery, String query,
                               Map<String, Object> parameters) throws IOException;

}
