package rouen.gospl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import core.metamodel.IPopulation;
import core.metamodel.pop.APopulationAttribute;
import core.metamodel.pop.APopulationEntity;
import core.metamodel.pop.APopulationValue;
import core.metamodel.pop.io.GSSurveyType;
import core.util.GSPerformanceUtil;
import gospl.algo.generator.ISyntheticGosplPopGenerator;
import gospl.algo.generator.SampleBasedGenerator;
import gospl.algo.sampler.ISampler;
import gospl.algo.sampler.co.AOptiAlgoSampler;
import gospl.algo.sampler.co.RandomSampler;
import gospl.algo.sampler.co.SimAnnealingSampler;
import gospl.algo.sampler.co.TabuSampler;
import gospl.algo.sb.SampleBasedAlgorithm;
import gospl.algo.sb.simannealing.SimulatedAnnealing;
import gospl.algo.sb.tabusearch.TabuSearch;
import gospl.distribution.GosplInputDataManager;
import gospl.distribution.exception.IllegalControlTotalException;
import gospl.distribution.exception.IllegalDistributionCreation;
import gospl.distribution.matrix.AFullNDimensionalMatrix;
import gospl.io.GosplSurveyFactory;
import gospl.io.exception.InvalidSurveyFormatException;
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
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}

		try {
			gdf.buildDataTables();
		} catch (final RuntimeException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final InvalidSurveyFormatException e) {
			e.printStackTrace();
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			gdf.buildSamples();
		} catch (final RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final InvalidSurveyFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//---------------------------------------//
		//----------- SETUP ALGORITHM -----------//
		//---------------------------------------//

		// Build a sampler according to what
		ISampler<APopulationEntity> sampler = null;
		
		// Retrieve sample to setup the CO sampler
		IPopulation<APopulationEntity, APopulationAttribute, APopulationValue> sample = 
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
		IPopulation<APopulationEntity, APopulationAttribute, APopulationValue> population = generator.generate(popSize);

		// Setup survey factory to export output population
		GosplSurveyFactory gsf = new GosplSurveyFactory();
		GosplIndicatorFactory gif = GosplIndicatorFactory.getFactory();
		try {
			gsf.createSummary(Paths.get(outputPath.toString(), "outputSample.csv").toFile(), 
					GSSurveyType.Sample, population);
			gsf.createSummary(Paths.get(outputPath.toString(), "outputFrequency.csv").toFile(), 
					GSSurveyType.GlobalFrequencyTable, population);
			
			// FORMAT Output
			List<Set<APopulationAttribute>> formats = gdf.getRawDataTables()
					.stream().map(data -> data.getDimensions().stream().filter(dim -> !dim.isRecordAttribute())
							.map(dim -> population.getPopulationAttributes().contains(dim) ? dim : 
								population.getPopulationAttributes().stream()
								.filter(dimPop -> dimPop.getReferentAttribute().equals(dim)).findFirst().get())
							.collect(Collectors.toSet()))
					.collect(Collectors.toList());
			// TEST PURPOSE
			for(Set<APopulationAttribute> format : formats)
				gsf.createContingencyTable(Paths.get(outputPath.toString(), "tables"+
						format.stream().map(dim -> dim.getAttributeName().substring(0, 3))
							.collect(Collectors.joining("X"))+".csv").toFile(), 
					format, population);
			
			gif.saveReport(Paths.get(outputPath.toString(), "outputReport.csv").toFile(), 
					gif.getReport(Arrays.asList(GosplIndicator.values()), 
					gdf.collapseDataTablesIntoDistributions(), population), ALGO, population.size());
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidSurveyFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalDistributionCreation e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalControlTotalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
