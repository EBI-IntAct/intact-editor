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

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.binary.expansion.InteractionEvidenceSpokeExpansion;
import psidev.psi.mi.jami.bridges.exception.BridgeFailedException;
import psidev.psi.mi.jami.bridges.ols.CachedOlsOntologyTermFetcher;
import psidev.psi.mi.jami.commons.MIWriterOptionFactory;
import psidev.psi.mi.jami.datasource.InteractionWriter;
import psidev.psi.mi.jami.factory.InteractionWriterFactory;
import psidev.psi.mi.jami.html.MIHtml;
import psidev.psi.mi.jami.html.MIHtmlOptionFactory;
import psidev.psi.mi.jami.json.InteractionViewerJson;
import psidev.psi.mi.jami.json.MIJsonOptionFactory;
import psidev.psi.mi.jami.json.MIJsonType;
import psidev.psi.mi.jami.model.Complex;
import psidev.psi.mi.jami.model.ComplexType;
import psidev.psi.mi.jami.model.InteractionCategory;
import psidev.psi.mi.jami.model.InteractionEvidence;
import psidev.psi.mi.jami.tab.MitabVersion;
import psidev.psi.mi.jami.xml.PsiXmlVersion;
import uk.ac.ebi.intact.dataexchange.psimi.mitab.IntactPsiMitab;
import uk.ac.ebi.intact.dataexchange.psimi.xml.IntactPsiXml;
import uk.ac.ebi.intact.dataexchange.structuredabstract.AbstractOutputType;
import uk.ac.ebi.intact.dataexchange.structuredabstract.IntactStructuredAbstract;
import uk.ac.ebi.intact.dataexchange.structuredabstract.StructuredAbstractOptionFactory;
import uk.ac.ebi.intact.jami.service.ComplexService;
import uk.ac.ebi.intact.jami.service.InteractionEvidenceService;

import javax.annotation.Resource;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * TODO comment this class header.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class MiExportServiceImpl implements MiExportService {
    private final static String RELEASED_EVT_ID = "PL:0028";

    @Resource(name = "interactionEvidenceService")
    private InteractionEvidenceService interactionEvidenceService;

    @Resource(name = "complexService")
    private ComplexService complexService;

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Object exportPublication(final String ac, final String format) {
        Response response = null;
        InteractionWriter writer = null;
        try {
            String responseType = getResponseType(format);
            StreamingOutput output = null;

            final String query = "select distinct i from IntactInteractionEvidence i join i.dbExperiments as e join e.publication as p " +
                    "where p.ac = :ac";
            final String countQuery = "select count(distinct i.ac) from IntactInteractionEvidence i join i.dbExperiments as e join e.publication as p " +
                    "where p.ac = :ac";
            final Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("ac", ac);
            output = new IntactEntryStreamingOutput(format, query, countQuery, parameters, true);

            response = Response.status(200).type(responseType).entity(output).build();
        } catch (Throwable e) {
            throw new RuntimeException("Problem exporting publication: "+ac, e);
        } finally {
            if (writer != null){
                writer.close();
            }
        }

        return response;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Object exportExperiment(final String ac, final String format) {
        Response response = null;
        InteractionWriter writer = null;
        try {
            String responseType = getResponseType(format);
            StreamingOutput output = null;

            final String query = "select distinct i from IntactInteractionEvidence i join i.dbExperiments as e " +
                    "where e.ac = :ac";
            final String countQuery = "select count(distinct i.ac) from IntactInteractionEvidence i join i.dbExperiments as e " +
                    "where e.ac = :ac";
            final Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("ac", ac);
            output = new IntactEntryStreamingOutput(format, query, countQuery, parameters, true);

            response = Response.status(200).type(responseType).entity(output).build();
        } catch (Throwable e) {
            throw new RuntimeException("Problem exporting experiment: "+ac, e);
        } finally {
            if (writer != null){
                writer.close();
            }
        }

        return response;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Object exportInteraction(final String ac, final String format) {
        Response response = null;
        try {
            String responseType = getResponseType(format);
            StreamingOutput output = null;

            final String query = "select distinct i from IntactInteractionEvidence i " +
                    "where i.ac = :ac";
            final String countQuery = "select count(distinct i.ac) from IntactInteractionEvidence i " +
                    "where i.ac = :ac";
            final Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("ac", ac);
            output = new IntactEntryStreamingOutput(format, query, countQuery, parameters, true);

            response = Response.status(200).type(responseType).entity(output).build();
        } catch (Throwable e) {
            throw new RuntimeException("Problem exporting interaction: "+ac, e);
        }

        return response;
    }

    @Override
    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public Object exportComplex(String ac, @DefaultValue("xml254") String format) {
        Response response = null;
        try {
            String responseType = getResponseType(format);
            StreamingOutput output = null;

            final String query = "select distinct i from IntactComplex i " +
                    "where i.ac = :ac";
            final String countQuery = "select count(distinct i.ac) from IntactComplex i " +
                    "where i.ac = :ac";
            final Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("ac", ac);
            output = new IntactEntryStreamingOutput(format, query, countQuery, parameters, false);

            response = Response.status(200).type(responseType).entity(output).build();
        } catch (Throwable e) {
            throw new RuntimeException("Problem exporting complex: "+ac, e);
        }

        return response;
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void writeEvidences(OutputStream outputStream, String format, String countQuery, String query,
                      Map<String, Object> parameters) throws WebApplicationException {
        InteractionWriter writer = null;
        try {

            writer = createInteractionEvidenceWriterFor(format, outputStream);

            writer.start();
            if (!format.equals(MiExportService.FORMAT_XML254_COMPACT) && !format.equals(MiExportService.FORMAT_XML300_COMPACT)){
                Iterator<InteractionEvidence> evidenceIterator = interactionEvidenceService.iterateAll(countQuery, query, parameters, false);
                writer.write(evidenceIterator);
            }
            else{
                // load all interactions to they appear in the entry
                Collection<InteractionEvidence> evidences = interactionEvidenceService.fetchIntactObjects(query, parameters);
                writer.write(evidences);
            }
            writer.end();
        } finally {
            if (writer != null){
                writer.close();
            }
        }
    }

    @Transactional(value = "jamiTransactionManager", readOnly = true, propagation = Propagation.REQUIRED)
    public void writeComplexes(OutputStream outputStream, String format, String countQuery, String query,
                               Map<String, Object> parameters) throws WebApplicationException {
        InteractionWriter writer = null;
        try {

            writer = createComplexWriterFor(format, outputStream);

            writer.start();
            if (!format.equals(MiExportService.FORMAT_XML254_COMPACT) && !format.equals(MiExportService.FORMAT_XML300_COMPACT)){
                Iterator<Complex> complexIterator = complexService.iterateAll(countQuery, query, parameters, false);
                writer.write(complexIterator);
            }
            else{
                // load all interactions to they appear in the entry
                Collection<Complex> complexes = complexService.fetchIntactObjects(query, parameters);
                writer.write(complexes);
            }
            writer.end();
        } finally {
            if (writer != null){
                writer.close();
            }
        }
    }

    private InteractionWriter createInteractionEvidenceWriterFor(String format, Object output){
        InteractionWriterFactory writerFactory = InteractionWriterFactory.getInstance();

        if (format.equals(MiExportService.FORMAT_MITAB25)){
            IntactPsiMitab.initialiseAllIntactMitabWriters();
            MIWriterOptionFactory optionFactory = MIWriterOptionFactory.getInstance();
            return writerFactory.getInteractionWriterWith(optionFactory.getMitabOptions(output, InteractionCategory.evidence, ComplexType.n_ary,
                    new InteractionEvidenceSpokeExpansion(), true, MitabVersion.v2_5, false)) ;
        }
        else if (format.equals(MiExportService.FORMAT_MITAB26)){
            IntactPsiMitab.initialiseAllIntactMitabWriters();
            MIWriterOptionFactory optionFactory = MIWriterOptionFactory.getInstance();
            return writerFactory.getInteractionWriterWith(optionFactory.getMitabOptions(output, InteractionCategory.evidence, ComplexType.n_ary,
                    new InteractionEvidenceSpokeExpansion(), true, MitabVersion.v2_6, false)) ;
        }
        else if (format.equals(MiExportService.FORMAT_MITAB27)){
            IntactPsiMitab.initialiseAllIntactMitabWriters();
            MIWriterOptionFactory optionFactory = MIWriterOptionFactory.getInstance();
            return writerFactory.getInteractionWriterWith(optionFactory.getMitabOptions(output, InteractionCategory.evidence, ComplexType.n_ary,
                    new InteractionEvidenceSpokeExpansion(), true, MitabVersion.v2_7, false)) ;
        }
        else if (format.equals(MiExportService.FORMAT_XML254_COMPACT)){
            IntactPsiXml.initialiseAllIntactXmlWriters();
            MIWriterOptionFactory optionFactory = MIWriterOptionFactory.getInstance();
            return writerFactory.getInteractionWriterWith(optionFactory.getDefaultCompactXmlOptions(output, InteractionCategory.evidence, ComplexType.n_ary,
                    PsiXmlVersion.v2_5_4)) ;
        }
        else if (format.equals(MiExportService.FORMAT_XML254_EXPANDED)){
            IntactPsiXml.initialiseAllIntactXmlWriters();
            MIWriterOptionFactory optionFactory = MIWriterOptionFactory.getInstance();
            return writerFactory.getInteractionWriterWith(optionFactory.getDefaultExpandedXmlOptions(output, InteractionCategory.evidence, ComplexType.n_ary,
                    PsiXmlVersion.v2_5_4)) ;
        }
        else if (format.equals(MiExportService.FORMAT_XML300_COMPACT)){
            IntactPsiXml.initialiseAllIntactXmlWriters();
            MIWriterOptionFactory optionFactory = MIWriterOptionFactory.getInstance();
            return writerFactory.getInteractionWriterWith(optionFactory.getDefaultCompactXmlOptions(output, InteractionCategory.evidence, ComplexType.n_ary,
                    PsiXmlVersion.v3_0_0)) ;
        }
        else if (format.equals(MiExportService.FORMAT_XML300_EXPANDED)){
            IntactPsiXml.initialiseAllIntactXmlWriters();
            MIWriterOptionFactory optionFactory = MIWriterOptionFactory.getInstance();
            return writerFactory.getInteractionWriterWith(optionFactory.getDefaultExpandedXmlOptions(output, InteractionCategory.evidence, ComplexType.n_ary,
                    PsiXmlVersion.v3_0_0)) ;
        }
        else if (format.equals(MiExportService.FORMAT_HTML)){
            MIHtml.initialiseAllMIHtmlWriters();
            MIHtmlOptionFactory optionFactory = MIHtmlOptionFactory.getInstance();
            return writerFactory.getInteractionWriterWith(optionFactory.getHtmlOptions(output, InteractionCategory.evidence, true)) ;
        }
        else if (format.equals(MiExportService.FORMAT_JSON)){
            InteractionViewerJson.initialiseAllMIJsonWriters();
            MIJsonOptionFactory optionFactory = MIJsonOptionFactory.getInstance();
            try {
                return writerFactory.getInteractionWriterWith(optionFactory.getJsonOptions(output, InteractionCategory.evidence, null,
                        MIJsonType.n_ary_only, new CachedOlsOntologyTermFetcher(), null)) ;
            } catch (BridgeFailedException e) {
                return writerFactory.getInteractionWriterWith(optionFactory.getJsonOptions(output, InteractionCategory.evidence, null,
                        MIJsonType.n_ary_only, null, null)) ;
            }
        }
        else if (format.equals(MiExportService.FORMAT_JSON_BINARY)){
            InteractionViewerJson.initialiseAllMIJsonWriters();
            MIJsonOptionFactory optionFactory = MIJsonOptionFactory.getInstance();
            try {
                return writerFactory.getInteractionWriterWith(optionFactory.getJsonOptions(output, InteractionCategory.evidence, ComplexType.n_ary,
                        MIJsonType.binary_only, new CachedOlsOntologyTermFetcher(), new InteractionEvidenceSpokeExpansion())) ;
            } catch (BridgeFailedException e) {
                return writerFactory.getInteractionWriterWith(optionFactory.getJsonOptions(output, InteractionCategory.evidence, ComplexType.n_ary,
                        MIJsonType.binary_only, null, new InteractionEvidenceSpokeExpansion())) ;
            }
        }
        else if (format.equals(MiExportService.FORMAT_FEBS_SDA)){
            IntactStructuredAbstract.initialiseAllStructuredAbstractWriters();
            StructuredAbstractOptionFactory optionFactory = StructuredAbstractOptionFactory.getInstance();
            return writerFactory.getInteractionWriterWith(optionFactory.getStructuredAbstractOptions(output, InteractionCategory.evidence,
                    AbstractOutputType.ABSTRACT_HTML_OUTPUT)) ;
        }
        else{
            throw new IllegalArgumentException("The format "+format +" is not recognized");
        }
    }

    private InteractionWriter createComplexWriterFor(String format, Object output){
        InteractionWriterFactory writerFactory = InteractionWriterFactory.getInstance();

        if (format.equals(MiExportService.FORMAT_XML254_COMPACT)){
            IntactPsiXml.initialiseAllIntactXmlWriters();
            MIWriterOptionFactory optionFactory = MIWriterOptionFactory.getInstance();
            return writerFactory.getInteractionWriterWith(optionFactory.getDefaultCompactXmlOptions(output, InteractionCategory.complex, ComplexType.n_ary,
                    PsiXmlVersion.v2_5_4)) ;
        }
        else if (format.equals(MiExportService.FORMAT_XML254_EXPANDED)){
            IntactPsiXml.initialiseAllIntactXmlWriters();
            MIWriterOptionFactory optionFactory = MIWriterOptionFactory.getInstance();
            return writerFactory.getInteractionWriterWith(optionFactory.getDefaultExpandedXmlOptions(output, InteractionCategory.complex, ComplexType.n_ary,
                    PsiXmlVersion.v2_5_4)) ;
        }
        else if (format.equals(MiExportService.FORMAT_XML300_COMPACT)){
            IntactPsiXml.initialiseAllIntactXmlWriters();
            MIWriterOptionFactory optionFactory = MIWriterOptionFactory.getInstance();
            return writerFactory.getInteractionWriterWith(optionFactory.getDefaultCompactXmlOptions(output, InteractionCategory.complex, ComplexType.n_ary,
                    PsiXmlVersion.v3_0_0)) ;
        }
        else if (format.equals(MiExportService.FORMAT_XML300_EXPANDED)){
            IntactPsiXml.initialiseAllIntactXmlWriters();
            MIWriterOptionFactory optionFactory = MIWriterOptionFactory.getInstance();
            return writerFactory.getInteractionWriterWith(optionFactory.getDefaultExpandedXmlOptions(output, InteractionCategory.complex, ComplexType.n_ary,
                    PsiXmlVersion.v3_0_0)) ;
        }
        else if (format.equals(MiExportService.FORMAT_HTML)){
            MIHtml.initialiseAllMIHtmlWriters();
            MIHtmlOptionFactory optionFactory = MIHtmlOptionFactory.getInstance();
            return writerFactory.getInteractionWriterWith(optionFactory.getHtmlOptions(output, InteractionCategory.modelled, true)) ;
        }
        else if (format.equals(MiExportService.FORMAT_JSON)){
            InteractionViewerJson.initialiseAllMIJsonWriters();
            MIJsonOptionFactory optionFactory = MIJsonOptionFactory.getInstance();
            try {
                return writerFactory.getInteractionWriterWith(optionFactory.getJsonOptions(output, InteractionCategory.modelled, null,
                        MIJsonType.n_ary_only, new CachedOlsOntologyTermFetcher(), null)) ;
            } catch (BridgeFailedException e) {
                return writerFactory.getInteractionWriterWith(optionFactory.getJsonOptions(output, InteractionCategory.modelled, null,
                        MIJsonType.n_ary_only, null, null)) ;
            }
        }
        else if (format.equals(MiExportService.FORMAT_FEBS_SDA)){
            IntactStructuredAbstract.initialiseAllStructuredAbstractWriters();
            StructuredAbstractOptionFactory optionFactory = StructuredAbstractOptionFactory.getInstance();
            return writerFactory.getInteractionWriterWith(optionFactory.getStructuredAbstractOptions(output, InteractionCategory.complex,
                    AbstractOutputType.ABSTRACT_HTML_OUTPUT)) ;
        }
        else{
            throw new IllegalArgumentException("The format "+format +" is not recognized");
        }
    }
    private String getResponseType( String format) {
        if ( FORMAT_MITAB25.equals( format ) || FORMAT_MITAB26.equals( format ) || FORMAT_MITAB27.equals( format ) ) {
            return "text/plain";
        } else if ( FORMAT_XML254_COMPACT.equals( format ) || FORMAT_XML254_EXPANDED.equals( format )
                || FORMAT_XML300_COMPACT.equals( format ) || FORMAT_XML300_EXPANDED.equals( format )) {
            return "application/xml";
        } else if ( FORMAT_FEBS_SDA.equals( format ) || FORMAT_HTML.equals(format)) {
            return "text/html";
        } else if ( FORMAT_JSON.equals(format) || FORMAT_JSON_BINARY.equals(format)) {
            return "application/json";
        } else{
            return "text/plain";
        }

    }
}
