package rouen.gospl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import core.metamodel.IPopulation;
import core.metamodel.attribute.Attribute;
import core.metamodel.entity.ADemoEntity;
import core.metamodel.io.GSSurveyType;
import core.metamodel.value.IValue;
import core.util.GSPerformanceUtil;
import gospl.algo.co.SampleBasedAlgorithm;
import gospl.algo.co.hillclimbing.HillClimbing;
import gospl.algo.co.metamodel.IOptimizationAlgorithm;
import gospl.algo.co.metamodel.solution.ISyntheticPopulationSolution;
import gospl.algo.co.simannealing.SimulatedAnnealing;
import gospl.algo.co.tabusearch.TabuList;
import gospl.algo.co.tabusearch.TabuSearch;
import gospl.distribution.GosplInputDataManager;
import gospl.distribution.exception.IllegalControlTotalException;
import gospl.distribution.exception.IllegalDistributionCreation;
import gospl.distribution.matrix.AFullNDimensionalMatrix;
import gospl.generator.ISyntheticGosplPopGenerator;
import gospl.generator.SampleBasedGenerator;
import gospl.io.GosplSurveyFactory;
import gospl.io.exception.InvalidSurveyFormatException;
import gospl.sampler.ISampler;
import gospl.sampler.co.CombinatorialOptimizationSampler;
import gospl.validation.GosplIndicator;
import gospl.validation.GosplIndicatorFactory;

public class CO {

	// Input nb of entity
	static int popSize = 100000;
	// Output file path 
	final static Path outputPath = Paths.get("src/main/java/rouen/gospl/output"); 
	// Setup configuration file
	final static Path configurationFile = Paths.get("src/main/java/rouen/gospl/data/rouen_demographics_with_sample.gns");
	
	private static final String ALGO = "SA";
	
	private static final int MAX_ITER = (int) Math.pow(10, 5);
	private static final int SIZE_TABU_LIST = 10;

	public static void main(String[] args) {
		
		GSPerformanceUtil gspu = new GSPerformanceUtil("Main process of population generation through CO algorithm");
		gspu.getStempPerformance(0);

		//---------------------------------------//
		//------- START TO PROCESS INPUTS -------//
		//---------------------------------------//

		GosplInputDataManager gdf = null;
		try {
			gdf = new GosplInputDataManager(configurationFile);
		} catch (final IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}

		try {
			gdf.buildDataTables();
		} catch (final RuntimeException | IOException 
				| InvalidSurveyFormatException | InvalidFormatException e) {
			e.printStackTrace();
		}

		try {
			gdf.buildSamples();
		} catch (final RuntimeException | IOException | 
				InvalidSurveyFormatException | InvalidFormatException e) {
			e.printStackTrace();
		}

		//---------------------------------------//
		//----------- SETUP ALGORITHM -----------//
		//---------------------------------------//

		// Build a sampler according to what
		ISampler<ADemoEntity> sampler = null;
		
		// Retrieve sample to setup the CO sampler
		IPopulation<ADemoEntity, Attribute<? extends IValue>> sample = 
				gdf.getRawSamples().iterator().next();
		
		// Retrieve known objectives from input data
		Set<AFullNDimensionalMatrix<Integer>> objectives = gdf.getContingencyTables();
		CombinatorialOptimizationSampler<IOptimizationAlgorithm<? extends ISyntheticPopulationSolution>> samplerCO;
		
		// TODO: make a colapseContingency(int total) method to use all info to setup contingency objectives
				
		switch (ALGO) {
		case "TABU":
			samplerCO = new CombinatorialOptimizationSampler<>(
					new TabuSearch(new TabuList(SIZE_TABU_LIST), 1d, MAX_ITER), sample);
			break;
		case "SA":
			samplerCO = new CombinatorialOptimizationSampler<>(
					new SimulatedAnnealing(), sample);
			break;
		default:
			samplerCO = new CombinatorialOptimizationSampler<>(
					new HillClimbing(1d, MAX_ITER), sample);
			break;
		}

		objectives.stream().forEach(obj -> samplerCO.addObjectives(obj));
		sampler = new SampleBasedAlgorithm().setupCOSampler(sample, false, samplerCO);

		//----------------------------------------------//
		//------- GENERATE POPULATION AND EXPORT -------//
		//----------------------------------------------//

		// Setup the generator using ipf-based sampler
		ISyntheticGosplPopGenerator generator = new SampleBasedGenerator(sampler);
		
		gspu.sysoStempMessage("Start generating synthetic population");
		gspu.sysoStempPerformance(0, CO.class);
		// Generate the population
		IPopulation<ADemoEntity, Attribute<? extends IValue>> population = generator.generate(popSize);
		
		gspu.sysoStempPerformance(1, CO.class);
		gspu.sysoStempMessage("Ends up with a synthetic population");

		// Setup survey factory to export output population
		GosplSurveyFactory gsf = new GosplSurveyFactory();
		GosplIndicatorFactory gif = GosplIndicatorFactory.getFactory();
		try {
			gsf.createSummary(Paths.get(outputPath.toString(), "outputSample.csv").toFile(), 
					GSSurveyType.Sample, population);
			gsf.createSummary(Paths.get(outputPath.toString(), "outputFrequency.csv").toFile(), 
					GSSurveyType.GlobalFrequencyTable, population);
			
			// FORMAT Output
			List<Set<Attribute<? extends IValue>>> formats = gdf.getRawDataTables()
					.stream().map(data -> data.getDimensions().stream()
							.map(dim -> population.getPopulationAttributes().contains(dim) ? dim : 
								population.getPopulationAttributes().stream()
								.filter(dimPop -> dimPop.getReferentAttribute().equals(dim)).findFirst().get())
							.collect(Collectors.toSet()))
					.collect(Collectors.toList());
			// TEST PURPOSE
			for(Set<Attribute<? extends IValue>> format : formats)
				gsf.createContingencyTable(Paths.get(outputPath.toString(), "tables"+
						format.stream().map(dim -> dim.getAttributeName().substring(0, 3))
							.collect(Collectors.joining("X"))+".csv").toFile(), 
					format, population);
			
			gif.saveReport(Paths.get(outputPath.toString(), "outputReport.csv").toFile(), 
					gif.getReport(Arrays.asList(GosplIndicator.values()), 
					gdf.collapseDataTablesIntoDistribution(), population), ALGO, population.size());
		} catch (InvalidFormatException | IOException | InvalidSurveyFormatException 
				| IllegalDistributionCreation | IllegalControlTotalException e) {
			e.printStackTrace();
		}

	}

}
