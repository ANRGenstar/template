package vietnam.gospl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import core.metamodel.IPopulation;
import core.metamodel.attribute.Attribute;
import core.metamodel.entity.ADemoEntity;
import core.metamodel.io.GSSurveyType;
import core.metamodel.value.IValue;
import core.util.GSPerformanceUtil;
import gospl.GosplPopulation;
import gospl.algo.co.MultiLayerSampleBasedAlgorithm;
import gospl.algo.co.hillclimbing.MultiHillClimbing;
import gospl.algo.co.metamodel.AMultiLayerOptimizationAlgorithm;
import gospl.algo.sr.ISyntheticReconstructionAlgo;
import gospl.algo.sr.ds.DirectSamplingAlgo;
import gospl.distribution.GosplInputDataManager;
import gospl.distribution.GosplNDimensionalMatrixFactory;
import gospl.distribution.exception.IllegalControlTotalException;
import gospl.distribution.exception.IllegalDistributionCreation;
import gospl.distribution.matrix.AFullNDimensionalMatrix;
import gospl.distribution.matrix.INDimensionalMatrix;
import gospl.distribution.matrix.coordinate.ACoordinate;
import gospl.generator.DistributionBasedGenerator;
import gospl.generator.ISyntheticGosplPopGenerator;
import gospl.generator.SampleBasedGenerator;
import gospl.io.GosplSurveyFactory;
import gospl.io.exception.InvalidSurveyFormatException;
import gospl.sampler.IDistributionSampler;
import gospl.sampler.ISampler;
import gospl.sampler.multilayer.co.GosplBiLayerOptimizationSampler;
import gospl.sampler.sr.GosplBasicSampler;
import gospl.validation.GosplIndicator;
import gospl.validation.GosplIndicatorFactory;
import rouen.gospl.CO;

public class CoVid19_mainSP {
	
	final static Path IPUMS_confFile = Paths.get("src/main/java/vietnam/gospl/data/vietnam_multi.gns");
	
	// Output file path 
	final static Path outputPath = Paths.get("src/main/java/vietnam/gospl/data/cov/output");
	
	// Parameter
	final static boolean ONLY_INDIVIDUAL = false;
	private static final double FITNESS_THRESHOLD_RATIO = 0.05;
	private static final int MAX_ITER = 0;

	private static final boolean SAVE_SAMPLE = false;
	
	public static void main(String[] args) {
		
		// THE POPULATION TO BE GENERATED
		GosplPopulation population = null;

		final GSPerformanceUtil gspu =
				new GSPerformanceUtil("Start processing input data to generate multi-layer population of BT");
		
		// INSTANCIATE FACTORIES FOR INDIVIDUAL AND HOUSEHOLD LEVEL DATA
		GosplInputDataManager df_hh = null;
		try {
			df_hh = new GosplInputDataManager(IPUMS_confFile);
		} catch (final IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}
		

		// RETRIEV INFORMATION FROM INDIVIDUAL LEVEL DATA IN FORM OF A SET OF JOINT DISTRIBUTIONS
		try {
			df_hh.buildDataTables();
		} catch (final RuntimeException | IOException | 
				InvalidSurveyFormatException | InvalidFormatException e) {
			e.printStackTrace();
		}
		
		// so we collapse all distribution build from the data
		INDimensionalMatrix<Attribute<? extends IValue>, IValue, Double> distribution = null;
		try {
			distribution = df_hh.collapseDataTablesIntoDistribution();
		} catch (final IllegalDistributionCreation e1) {
			e1.printStackTrace();
		} catch (final IllegalControlTotalException e1) {
			e1.printStackTrace();
		}
		
		// RETRIEVE SAMPLE OF HOUSEHOLD
		try {
			df_hh.buildMultiLayerSamples();
		} catch (final IOException | InvalidFormatException | InvalidSurveyFormatException e) {
			e.printStackTrace();
		}
		
		// MAKE REPORT
		final GosplSurveyFactory sf = new GosplSurveyFactory(0, ';', 1, 1);
		final GosplIndicatorFactory gif = GosplIndicatorFactory.getFactory();
		outputPath.toFile().mkdirs();
		
		if (!ONLY_INDIVIDUAL) {
			
			// -----------------------------
			// Samples and Marginals -------
			// -----------------------------
			
			// Inidividual layer marginal data retrieval
			int indiv_layer = df_hh.getConfiguration().getLayers().stream().min((l1,l2) -> l1 < l2 ? -1 : 1).orElse(0);
			// TODO : should be transpose to distribution or only take into account expected individual
			Set<AFullNDimensionalMatrix<Integer>> objectives = df_hh.getContingencyTables();
			
			int pop_size = -1;
			for(AFullNDimensionalMatrix<Integer> objectif : objectives) {
				int current_objectif_size = objectif.getVal().getValue();
				if(pop_size == -1) {pop_size = current_objectif_size;}
				else if(pop_size != current_objectif_size) {
					throw new IllegalArgumentException("Contingency tables do not have same total: "+pop_size+" and "+current_objectif_size);
				}
			}
			gspu.sysoStempMessage("There is "+objectives.size()+" marginal table at layer "
					+indiv_layer+" to fit multi layer sample with "+objectives.stream().flatMap(obj -> obj.getDimensions().stream())
					.map(Attribute::getAttributeName).collect(Collectors.joining("; ")));

			// Retrieve sample to setup the CO sampler
			List<IPopulation<ADemoEntity, Attribute<? extends IValue>>> samples = new ArrayList<>(df_hh.getRawSamples());
			
			// ONLY TRUE IN THIS PARTICULAR CASE
			// Sort sample according to the size : the largest one is individual, the smallest one is household
			Collections.sort(samples, (s1,s2) ->  s1.size() > s2.size() ? -1 : 1);
			Map<Integer, IPopulation<ADemoEntity, Attribute<? extends IValue>>> layered_samples = samples.stream()
					.collect(Collectors.toMap(s -> samples.indexOf(s), Function.identity()));
			
			gspu.sysoStempMessage("There is "+samples.get(1).size()+" household in the sample");
			gspu.sysoStempMessage("There is "+samples.get(0).size()+" individual in the sample");
			gspu.sysoStempMessage(objectives.stream().flatMap(mat -> mat.getAspects().stream())
					.filter(val -> val.getValueSpace().getAttribute().getAttributeName().equals("commune"))
					.findFirst().get().getStringValue()+" commune contains "+pop_size+" individuals");
			
			if(SAVE_SAMPLE) {
				try {
					sf.createSummary(outputPath.resolve("indiv_sample.csv").toFile(), GSSurveyType.Sample, samples.get(0));
					sf.createSummary(outputPath.resolve("indiv_frequency.csv").toFile(), GSSurveyType.GlobalFrequencyTable, samples.get(0));
					sf.createSummary(outputPath.resolve("hh_sample.csv").toFile(), GSSurveyType.Sample, samples.get(1));
					sf.createSummary(outputPath.resolve("hh_frequency.csv").toFile(), GSSurveyType.GlobalFrequencyTable, samples.get(1));
				} catch (InvalidFormatException | IOException | InvalidSurveyFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			// ---------------
			// Sampler -------
			// ---------------
			
			GosplBiLayerOptimizationSampler<AMultiLayerOptimizationAlgorithm> samplerCO;
			samplerCO = new GosplBiLayerOptimizationSampler<>(new MultiHillClimbing(MAX_ITER,0.05,pop_size*FITNESS_THRESHOLD_RATIO));
			
			for (AFullNDimensionalMatrix<Integer> objectif : objectives)
				samplerCO.addObjectives(objectif, indiv_layer);
				
			ISampler<ADemoEntity> sampler = new MultiLayerSampleBasedAlgorithm<>().setupCOSampler(1, layered_samples, true, samplerCO);
			
			// -----------------
			// Generator -------
			// -----------------
			
			
			// Setup the generator using ipf-based sampler
			ISyntheticGosplPopGenerator generator = new SampleBasedGenerator(sampler);
			
			gspu.sysoStempMessage("Start generating synthetic population");
			gspu.sysoStempPerformance(0, CO.class);
			// Generate the population
			population = generator.generate(pop_size);
			GosplPopulation indiv_pop = new GosplPopulation(population.stream()
					.flatMap(e  -> e.getChildren().stream()).map(e -> (ADemoEntity)e)
					.collect(Collectors.toList()));
			
			gspu.sysoStempPerformance(1, CO.class);
			gspu.sysoStempMessage("Ends up with a synthetic population of "+population.size()+" households and "
					+indiv_pop.size()+" individuals");
			
			// TODO make a simple report on objectives dimensions
			Set<Attribute<? extends IValue>> dims = objectives.stream().flatMap(m -> m.getDimensions().stream())
					.filter(d -> !d.getAttributeName().equals("commune")).collect(Collectors.toSet());
			AFullNDimensionalMatrix<Integer> res = GosplNDimensionalMatrixFactory.getFactory().createContingency(dims, indiv_pop);
			Map<IValue,int[]> outputs = new HashMap<>();
			for(IValue v : dims.stream().flatMap(att -> att.getValueSpace().getValues().stream()).collect(Collectors.toSet())) {
				int output = res.getVal(v,true).getValue();
				int input = objectives.stream().filter(mat -> mat.getAspects().contains(v)).findFirst().get().getVal(v).getValue();
				outputs.put(v,new int[] {input,output});
			}
			gspu.sysoStempMessage("Main outputs: "+outputs.entrySet().stream()
					.map(e -> e.getKey().getStringValue()+" : "+String.valueOf(e.getValue()[0] - e.getValue()[1]))
					.collect(Collectors.joining("\n")));
			gspu.sysoStempMessage("Total Absolute Error should be : "+outputs.keySet().stream()
					.mapToInt(k -> Math.abs(outputs.get(k)[1]-outputs.get(k)[0])).sum());
			
			try {
				sf.createSummary(outputPath.resolve("multi_pop.csv").toFile(), GSSurveyType.Sample, indiv_pop);
			} catch (InvalidFormatException | IOException | InvalidSurveyFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else {
			
			// BUILD THE SAMPLER WITH THE INFERENCE ALGORITHM
			ISampler<ACoordinate<Attribute<? extends IValue>, IValue>> sampler = null;
	
			ISyntheticReconstructionAlgo<IDistributionSampler> distributionInfAlgo = new DirectSamplingAlgo();
			try {
				sampler = distributionInfAlgo.inferSRSampler(distribution, new GosplBasicSampler());
			} catch (final IllegalDistributionCreation e1) {
				e1.printStackTrace();
			}
		
			// BUILD THE GENERATOR
			final ISyntheticGosplPopGenerator ispGenerator = new DistributionBasedGenerator(sampler);
	
			// Pop
			int nb_pop = df_hh.getContingencyTables().stream().findFirst().get().getVal().getValue();
			System.out.println("Total population in contingency table is: "+nb_pop);
			
			// Test Xã Thạnh Phước
			int tp_pop = df_hh.getContingencyTables().stream().findFirst().get().getVal("commune","Xã Thạnh Phước").getValue();
			System.out.println("Population for Thạnh Phước commune is: "+tp_pop);
			
			// BUILD THE POPULATION
			try {
				population = ispGenerator.generate(nb_pop);
				gspu.sysoStempPerformance("End generating synthetic population: elapse time",
						CoVid19_mainSP.class.getName());
			} catch (final NumberFormatException e) {
				e.printStackTrace();
			}
	
			try {
				sf.createSummary(outputPath.resolve("indiv_sample.csv").toFile(), GSSurveyType.Sample, population);
				sf.createSummary(outputPath.resolve("indiv_contingency.csv").toFile(), GSSurveyType.ContingencyTable, population);
				gif.saveReport(outputPath.resolve("indiv_report.csv").toFile(), gif.getReport(Arrays.asList(GosplIndicator.values()), 
						distribution, population), "DirectSampling", population.size());
			} catch (IOException | InvalidSurveyFormatException | InvalidFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}
