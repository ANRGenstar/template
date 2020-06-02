package vietnam.gospl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import core.configuration.dictionary.IGenstarDictionary;
import core.metamodel.attribute.Attribute;
import core.metamodel.entity.ADemoEntity;
import core.metamodel.io.GSSurveyType;
import core.metamodel.value.IValue;
import core.util.GSPerformanceUtil;
import core.util.random.GenstarRandomUtils;
import gospl.GosplMultitypePopulation;
import gospl.GosplPopulation;
import gospl.algo.co.MultiLayerSampleBasedAlgorithm;
import gospl.algo.co.hillclimbing.MultiHillClimbing;
import gospl.algo.co.metamodel.AMultiLayerOptimizationAlgorithm;
import gospl.algo.co.metamodel.neighbor.MultiPopulationNeighborSearch;
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
	
	final static Path IPUMS_confFile = Paths.get("src/main/java/vietnam/gospl/data/comokit_conf.gns");
	
	// Output file path 
	final static Path outputPath = Paths.get("src/main/java/vietnam/gospl/data/cov/output");
	
	// Parameter
	final static boolean ONLY_INDIVIDUAL = false;
	private static final double FITNESS_THRESHOLD_RATIO = 0.05;
	private static final int MAX_ITER = 10;

	private static final boolean SAVE_SAMPLE = false;
	private static final int MAX_SIZE_SAMPLE = (int) Math.pow(10, 6);
	
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
		
		gspu.sysoStempMessage("Marginals have been build");
		gspu.sysoStempPerformance(0, CoVid19_mainSP.class);
		
		// RETRIEVE SAMPLE OF HOUSEHOLD
		try {
			df_hh.buildMultiLayerSamples(MAX_SIZE_SAMPLE);
		} catch (final IOException | InvalidFormatException | InvalidSurveyFormatException e) {
			e.printStackTrace();
		}
		
		// MAKE REPORT
		final GosplSurveyFactory sf = new GosplSurveyFactory(0, ';', 1, 1);
		final GosplIndicatorFactory gif = GosplIndicatorFactory.getFactory();
		outputPath.toFile().mkdirs();
		
		if (!ONLY_INDIVIDUAL) {
			
			// Name of layers
			Map<Integer, String> layersID = df_hh.getConfiguration().getDictionaries().stream()
					.collect(Collectors.toMap(
							IGenstarDictionary::getLevel,
							IGenstarDictionary::getIdentifierAttributeName
							));
			
			// -----------------------------
			// Samples and Marginals -------
			// -----------------------------
			
			// Inidividual layer marginal data retrieval
			Set<AFullNDimensionalMatrix<Integer>> objectives = df_hh.getContingencyTables();
			
			gspu.sysoStempMessage("There is "+objectives.size()+" marginal table at layer "
					+layersID.get(0)+" to fit with "+objectives.stream().flatMap(obj -> obj.getDimensions().stream())
					.map(Attribute::getAttributeName).collect(Collectors.joining("; ")));
			
			// Only take into account targeted attributes
			Set<Attribute<? extends IValue>> marginalAttributes = Stream.of(
					df_hh.getConfiguration().getDictionary(0).getAttribute("age"),
					df_hh.getConfiguration().getDictionary(0).getAttribute("sex")
					).collect(Collectors.toSet());
			AFullNDimensionalMatrix<Integer> objectif = GosplNDimensionalMatrixFactory.getFactory()
					.cloneContingency(marginalAttributes, objectives.iterator().next());
					
			int pop_size = objectif.getVal().getValue();
			gspu.sysoStempMessage("Begin to setup the creation of CO sampler to draw a population of "+pop_size+" individuals");

			// Retrieve sample to setup the CO sampler
			GosplMultitypePopulation<ADemoEntity> sample = df_hh.getMultiSamples().iterator().next();
			
			Set<ADemoEntity> withoutSEX = sample.getSubPopulation(layersID.get(0)).stream()
					.filter(e -> e.getValueForAttribute("SEX")==null).collect(Collectors.toSet());
			if (!withoutSEX.isEmpty()) {
				System.err.println("There is "+withoutSEX.size()+" individual with gender attribute equal to null");
				System.err.println(GenstarRandomUtils.oneOf(withoutSEX).getValueForAttribute("SEX"));
				System.exit(1);
			}
			
			gspu.sysoStempMessage("There is "+sample.getSubPopulation(layersID.get(1)).size()+" household in the sample");
			gspu.sysoStempMessage("There is "+sample.getSubPopulation(layersID.get(0)).size()+" individual in the sample");
			gspu.sysoStempMessage(objectives.stream().flatMap(mat -> mat.getAspects().stream())
					.filter(val -> val.getValueSpace().getAttribute().getAttributeName().equals("commune"))
					.findFirst().get().getStringValue()+" commune contains "+pop_size+" individuals");
			
			if(SAVE_SAMPLE) {
				try {
					sf.createSummary(outputPath.resolve("indiv_sample.csv").toFile(), GSSurveyType.Sample, 
							sample.getSubPopulation(layersID.get(0)));
					sf.createSummary(outputPath.resolve("indiv_frequency.csv").toFile(), GSSurveyType.GlobalFrequencyTable, 
							sample.getSubPopulation(layersID.get(0)));
					sf.createSummary(outputPath.resolve("hh_sample.csv").toFile(), GSSurveyType.Sample, 
							sample.getSubPopulation(layersID.get(1)));
					sf.createSummary(outputPath.resolve("hh_frequency.csv").toFile(), GSSurveyType.GlobalFrequencyTable, 
							sample.getSubPopulation(layersID.get(1)));
				} catch (InvalidFormatException | IOException | InvalidSurveyFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			// ---------------
			// Sampler -------
			// ---------------
			
			GosplBiLayerOptimizationSampler<AMultiLayerOptimizationAlgorithm> samplerCO;
			
			//MultiPopulationNeighborSearch pvns = new MultiPopulationNeighborSearch(new PopulationVectorNeighborSearch());
			MultiPopulationNeighborSearch pvns = new MultiPopulationNeighborSearch();
			
			samplerCO = new GosplBiLayerOptimizationSampler<>(new MultiHillClimbing(pvns, MAX_ITER,0.5,pop_size*FITNESS_THRESHOLD_RATIO));
			samplerCO.addObjectives(objectif,0);
				
			ISampler<ADemoEntity> sampler = new MultiLayerSampleBasedAlgorithm<>().setupCOSampler(1, sample, true, samplerCO);
			
			// -----------------
			// Generator -------
			// -----------------
			
			
			// Setup the generator using ipf-based sampler
			ISyntheticGosplPopGenerator generator = new SampleBasedGenerator(sampler);
			
			gspu.sysoStempMessage("Start generating synthetic population");
			
			// Generate the population
			population = generator.generate(pop_size);
			GosplPopulation indiv_pop = new GosplPopulation(population.stream()
					.flatMap(e  -> e.getChildren().stream()).map(e -> (ADemoEntity)e)
					.collect(Collectors.toList()));
			
			
			gspu.sysoStempMessage("Ends up with a synthetic population of "+(population.size()-indiv_pop.size())+" households and "
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
			/*
			gspu.sysoStempMessage("Main outputs: "+outputs.entrySet().stream()
					.map(e -> e.getKey().getStringValue()+" : "+String.valueOf(e.getValue()[0] - e.getValue()[1])
					+" ("+String.valueOf(Math.abs(e.getValue()[1]-e.getValue()[0])*1d/e.getValue()[0])+")")
					.collect(Collectors.joining("\n")));
					*/
			gspu.sysoStempMessage("Total Absolute Error should be : "+outputs.keySet().stream()
					.mapToInt(k -> Math.abs(outputs.get(k)[1]-outputs.get(k)[0])).sum());
			gspu.sysoStempMessage("Total Absolute Error should be : "+outputs.keySet().stream()
					.mapToDouble(k -> Math.abs(outputs.get(k)[1]-outputs.get(k)[0])*1d/outputs.get(k)[0])
					.average());
			
			
			try {
				sf.createSummary(outputPath.resolve("multi_pop.csv").toFile(), GSSurveyType.Sample, indiv_pop);
				gif.saveReport(outputPath.resolve("multi_report.csv").toFile(), gif.getReport(Arrays.asList(GosplIndicator.values()), 
						GosplNDimensionalMatrixFactory.getFactory().createDistribution(objectif), indiv_pop), 
						"MultiLayer CO", indiv_pop.size());
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
