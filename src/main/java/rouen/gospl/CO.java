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
import core.metamodel.attribute.demographic.DemographicAttribute;
import core.metamodel.entity.ADemoEntity;
import core.metamodel.io.GSSurveyType;
import core.metamodel.value.IValue;
import core.util.GSPerformanceUtil;
import gospl.algo.co.SampleBasedAlgorithm;
import gospl.algo.co.simannealing.SimulatedAnnealing;
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
import gospl.sampler.co.AOptiAlgoSampler;
import gospl.sampler.co.RandomSampler;
import gospl.sampler.co.SimAnnealingSampler;
import gospl.sampler.co.TabuSampler;
import gospl.validation.GosplIndicator;
import gospl.validation.GosplIndicatorFactory;

public class CO {

	// Input nb of entity
	static int popSize = 100000;
	// Output file path 
	final static Path outputPath = Paths.get("src/main/java/rouen/gospl/output"); 
	// Setup configuration file
	final static Path configurationFile = Paths.get("src/main/java/rouen/gospl/data/GSC_Rouen_Sample.xml");
	
	private static final String ALGO = "RANDOM";
	
	private static final int MAX_TABU_ITER = (int) Math.pow(10, 5);
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
		IPopulation<ADemoEntity, DemographicAttribute<? extends IValue>> sample = 
				gdf.getRawSamples().iterator().next();
		
		// Retrieve known objectives from input data
		Set<AFullNDimensionalMatrix<Integer>> objectives = gdf.getContingencyTables();
		
		// TODO: make a colapseContingency(int total) method to use all info to setup contingency objectives
				
		switch (ALGO) {
		case "TABU":
			AOptiAlgoSampler<TabuSearch> tabuSampler = new TabuSampler(MAX_TABU_ITER, SIZE_TABU_LIST);
			objectives.stream().forEach(obj -> tabuSampler.addObjectives(obj));
			sampler = new SampleBasedAlgorithm().setupCOSampler(sample, tabuSampler);
			break;
		case "SA":
			AOptiAlgoSampler<SimulatedAnnealing> simAnnealingSampler = new SimAnnealingSampler();
			objectives.stream().forEach(obj -> simAnnealingSampler.addObjectives(obj));
			sampler = new SampleBasedAlgorithm().setupCOSampler(sample, simAnnealingSampler);
			break;
		case "HC":
			break;
		default:
			sampler = new SampleBasedAlgorithm().setupCOSampler(sample, new RandomSampler());
			break;
		}

		//----------------------------------------------//
		//------- GENERATE POPULATION AND EXPORT -------//
		//----------------------------------------------//

		// Setup the generator using ipf-based sampler
		ISyntheticGosplPopGenerator generator = new SampleBasedGenerator(sampler);
		
		gspu.sysoStempMessage("Start generating synthetic population");
		// Generate the population
		IPopulation<ADemoEntity, DemographicAttribute<? extends IValue>> population = generator.generate(popSize);

		// Setup survey factory to export output population
		GosplSurveyFactory gsf = new GosplSurveyFactory();
		GosplIndicatorFactory gif = GosplIndicatorFactory.getFactory();
		try {
			gsf.createSummary(Paths.get(outputPath.toString(), "outputSample.csv").toFile(), 
					GSSurveyType.Sample, population);
			gsf.createSummary(Paths.get(outputPath.toString(), "outputFrequency.csv").toFile(), 
					GSSurveyType.GlobalFrequencyTable, population);
			
			// FORMAT Output
			List<Set<DemographicAttribute<? extends IValue>>> formats = gdf.getRawDataTables()
					.stream().map(data -> data.getDimensions().stream()
							.map(dim -> population.getPopulationAttributes().contains(dim) ? dim : 
								population.getPopulationAttributes().stream()
								.filter(dimPop -> dimPop.getReferentAttribute().equals(dim)).findFirst().get())
							.collect(Collectors.toSet()))
					.collect(Collectors.toList());
			// TEST PURPOSE
			for(Set<DemographicAttribute<? extends IValue>> format : formats)
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
