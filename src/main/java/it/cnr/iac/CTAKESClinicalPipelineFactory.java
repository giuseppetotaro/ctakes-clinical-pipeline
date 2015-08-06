/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package it.cnr.iac;

import org.apache.ctakes.assertion.medfacts.cleartk.*;
import org.apache.ctakes.chunker.ae.Chunker;
import org.apache.ctakes.chunker.ae.adjuster.ChunkAdjuster;
import org.apache.ctakes.constituency.parser.ae.ConstituencyParser;
import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.resource.FileResourceImpl;
import org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE;
import org.apache.ctakes.dictionary.lookup.ae.UmlsDictionaryLookupAnnotator;
import org.apache.ctakes.dictionary.lookup2.ae.AbstractJCasTermAnnotator;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.ctakes.dictionary.lookup2.ae.JCasTermAnnotator;
import org.apache.ctakes.lvg.ae.LvgAnnotator;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.syntax.Chunk;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.LookupWindowAnnotation;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.SAXException;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * This class runs the complete pipeline of Apache cTAKES for annotating
 * clinical documents in plain text format.
 *
 */
final public class CTAKESClinicalPipelineFactory {

	public static AnalysisEngineDescription getDefaultPipeline()
			throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();
		builder.add(getTokenProcessingPipeline());
		builder.add(getNpChunkerPipeline());
		builder.add(AnalysisEngineFactory
				.createEngineDescription(ConstituencyParser.class));
		builder.add(UmlsDictionaryLookupAnnotator.createAnnotatorDescription());
		builder.add(ClearNLPDependencyParserAE.createAnnotatorDescription());
		builder.add(PolarityCleartkAnalysisEngine.createAnnotatorDescription());
		builder.add(UncertaintyCleartkAnalysisEngine
				.createAnnotatorDescription());
		builder.add(HistoryCleartkAnalysisEngine.createAnnotatorDescription());
		builder.add(ConditionalCleartkAnalysisEngine
				.createAnnotatorDescription());
		builder.add(GenericCleartkAnalysisEngine.createAnnotatorDescription());
		builder.add(SubjectCleartkAnalysisEngine.createAnnotatorDescription());

		return builder.createAggregateDescription();
	}

	public static AnalysisEngineDescription getFastPipeline()
			throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();
		builder.add(getTokenProcessingPipeline());
		try {
			builder.add(AnalysisEngineFactory
					.createEngineDescription(
							DefaultJCasTermAnnotator.class,
							AbstractJCasTermAnnotator.PARAM_WINDOW_ANNOT_PRP,
							"org.apache.ctakes.typesystem.type.textspan.Sentence",
							JCasTermAnnotator.DICTIONARY_DESCRIPTOR_KEY,
							ExternalResourceFactory.createExternalResourceDescription(
									FileResourceImpl.class,
									FileLocator
											.locateFile("org/apache/ctakes/dictionary/lookup/fast/cTakesHsql.xml"))));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new ResourceInitializationException(e);
		}
		builder.add(ClearNLPDependencyParserAE.createAnnotatorDescription());
		builder.add(PolarityCleartkAnalysisEngine.createAnnotatorDescription());
		builder.add(UncertaintyCleartkAnalysisEngine
				.createAnnotatorDescription());
		builder.add(HistoryCleartkAnalysisEngine.createAnnotatorDescription());
		builder.add(ConditionalCleartkAnalysisEngine
				.createAnnotatorDescription());
		builder.add(GenericCleartkAnalysisEngine.createAnnotatorDescription());
		builder.add(SubjectCleartkAnalysisEngine.createAnnotatorDescription());
		return builder.createAggregateDescription();
	}

	public static AnalysisEngineDescription getParsingPipeline()
			throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();
		builder.add(getTokenProcessingPipeline());
		builder.add(ClearNLPDependencyParserAE.createAnnotatorDescription());
		builder.add(AnalysisEngineFactory
				.createEngineDescription(ConstituencyParser.class));
		return builder.createAggregateDescription();
	}

	public static AnalysisEngineDescription getTokenProcessingPipeline()
			throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();
		builder.add(SimpleSegmentAnnotator.createAnnotatorDescription());
		builder.add(SentenceDetector.createAnnotatorDescription());
		builder.add(TokenizerAnnotatorPTB.createAnnotatorDescription());
		builder.add(LvgAnnotator.createAnnotatorDescription());
		builder.add(ContextDependentTokenizerAnnotator
				.createAnnotatorDescription());
		builder.add(POSTagger.createAnnotatorDescription());
		return builder.createAggregateDescription();
	}

	public static AnalysisEngineDescription getNpChunkerPipeline()
			throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();
		builder.add(Chunker.createAnnotatorDescription());
		builder.add(getStandardChunkAdjusterAnnotator());
		builder.add(AnalysisEngineFactory
				.createEngineDescription(CopyNPChunksToLookupWindowAnnotations.class));
		builder.add(AnalysisEngineFactory
				.createEngineDescription(RemoveEnclosedLookupWindows.class));
		return builder.createAggregateDescription();
	}

	public static AnalysisEngineDescription getStandardChunkAdjusterAnnotator()
			throws ResourceInitializationException {
		AggregateBuilder builder = new AggregateBuilder();
		// adjust NP in NP NP to span both
		builder.add(ChunkAdjuster.createAnnotatorDescription(new String[] {
				"NP", "NP" }, 1));
		// adjust NP in NP PP NP to span all three
		builder.add(ChunkAdjuster.createAnnotatorDescription(new String[] {
				"NP", "PP", "NP" }, 2));
		return builder.createAggregateDescription();
	}

	public static void main(final String... args) throws IOException,
			UIMAException, SAXException {
		if (args.length < 2) {
			System.err.println("Usage: "
					+ CTAKESClinicalPipelineFactory.class.getName()
					+ " /path/to/input /path/to/output");
			System.exit(1);
		}
		
		String input = args[0];
		String output = args[1];
		File inputFile = new File(input);
		File outputFile = new File(output);
		
		if (!inputFile.isFile() || !inputFile.canRead()) {
			System.err.println("Error: " + input
					+ " is not a file or cannot be read!");
			System.exit(1);
		}

		final JCas jcas = JCasFactory.createJCas();

		String note = readFile(inputFile);

		jcas.setDocumentText(note);
		final AnalysisEngineDescription aed = getDefaultPipeline();
		// final AnalysisEngineDescription aed = getFastPipeline(); // Outputs
		// from default and fast pipelines are identical
		SimplePipeline.runPipeline(jcas, aed);

		serialize(jcas, outputFile);

		final boolean printCuis = Arrays.asList(args).contains("cuis");
		final Collection<String> codes = new ArrayList<>();
		for (IdentifiedAnnotation entity : JCasUtil.select(jcas,
				IdentifiedAnnotation.class)) {

			System.out.println("Entity: " + entity.getCoveredText()
					+ " === Polarity: " + entity.getPolarity()
					+ " === Uncertain? "
					+ (entity.getUncertainty() == CONST.NE_UNCERTAINTY_PRESENT)
					+ " === Subject: " + entity.getSubject() + " === Generic? "
					+ (entity.getGeneric() == CONST.NE_GENERIC_TRUE)
					+ " === Conditional? "
					+ (entity.getConditional() == CONST.NE_CONDITIONAL_TRUE)
					+ " === History? "
					+ (entity.getHistoryOf() == CONST.NE_HISTORY_OF_PRESENT));

			if (printCuis) {
				codes.clear();
				codes.addAll(getCUIs(entity));
				for (String cui : codes) {
					System.out.print(cui + " ");
				}
				System.out.println();
			}

		}
		if (args.length > 0) {
			aed.toXML(new FileWriter(args[0]));
		}
	}

	/**
	 * @param identifiedAnnotation
	 *            -
	 * @return list of all cuis
	 */
	static private Collection<String> getCUIs(
			final IdentifiedAnnotation identifiedAnnotation) {
		final FSArray fsArray = identifiedAnnotation.getOntologyConceptArr();
		if (fsArray == null) {
			return Collections.emptySet();
		}
		final FeatureStructure[] featureStructures = fsArray.toArray();
		final Collection<String> cuis = new ArrayList<>(
				featureStructures.length);
		for (FeatureStructure featureStructure : featureStructures) {
			if (featureStructure instanceof UmlsConcept) {
				final UmlsConcept umlsConcept = (UmlsConcept) featureStructure;
				final String cui = umlsConcept.getCui();
				final String tui = umlsConcept.getTui();
				if (tui != null && !tui.isEmpty()) {
					cuis.add(cui + "_" + tui);
				} else {
					cuis.add(cui);
				}
			}
		}
		return cuis;
	}

	public static class CopyNPChunksToLookupWindowAnnotations extends
			JCasAnnotator_ImplBase {

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			for (Chunk chunk : JCasUtil.select(jCas, Chunk.class)) {
				if (chunk.getChunkType().equals("NP")) {
					new LookupWindowAnnotation(jCas, chunk.getBegin(),
							chunk.getEnd()).addToIndexes();
				}
			}
		}
	}

	public static class RemoveEnclosedLookupWindows extends
			JCasAnnotator_ImplBase {

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			List<LookupWindowAnnotation> lws = new ArrayList<>(JCasUtil.select(
					jCas, LookupWindowAnnotation.class));
			// we'll navigate backwards so that as we delete things we shorten
			// the list from the back
			for (int i = lws.size() - 2; i >= 0; i--) {
				LookupWindowAnnotation lw1 = lws.get(i);
				LookupWindowAnnotation lw2 = lws.get(i + 1);
				if (lw1.getBegin() <= lw2.getBegin()
						&& lw1.getEnd() >= lw2.getEnd()) {
					// / lw1 envelops or encloses lw2
					lws.remove(i + 1);
					lw2.removeFromIndexes();
				}
			}
		}
	}

	private static String readFile(File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			return sb.toString();

		} finally {
			br.close();
		}
	}

	private static void serialize(JCas jcas, File file) {
		OutputStream outputStream = null;
		try {
			outputStream = new BufferedOutputStream(new FileOutputStream(file));

			XmiCasSerializer xmiSerializer = new XmiCasSerializer(
					jcas.getTypeSystem());
			XMLSerializer xmlSerializer = new XMLSerializer(outputStream, true);
			xmiSerializer.serialize(jcas.getCas(),
					xmlSerializer.getContentHandler());

		} catch (FileNotFoundException fnfe) {
			// TODO
			fnfe.printStackTrace();
		} catch (SAXException saxe) {
			// TODO
			saxe.printStackTrace();
		} finally {
			try {
				outputStream.close();
			} catch (IOException ioe) {
				// TODO
				ioe.printStackTrace();
			}
		}
	}
}