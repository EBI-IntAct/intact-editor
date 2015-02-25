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

import uk.ac.ebi.intact.jami.ApplicationContextProvider;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class IntactEntryStreamingOutput implements StreamingOutput {

    private String format;
    private String query;
    private String countQuery;
    private Map<String, Object> parameters;
    private MiExportService miExportService;
    private boolean isEvidence = true;

    public IntactEntryStreamingOutput(String format,
                                      String query, String countQuery, Map<String, Object> parameters,
                                      boolean isEvidence) {
        this.format = format;
        this.miExportService = ApplicationContextProvider.getBean("exportService");
        this.query = query;
        this.countQuery = countQuery;
        this.parameters = parameters;
        this.isEvidence = isEvidence;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException, WebApplicationException {
        if (isEvidence){
            this.miExportService.writeEvidences(outputStream, this.format, this.countQuery, this.query, this.parameters);
        }
        else{
            this.miExportService.writeComplexes(outputStream, this.format, this.countQuery, this.query, this.parameters);
        }
    }
}
