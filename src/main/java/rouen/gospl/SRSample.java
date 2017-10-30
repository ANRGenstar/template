package rouen.gospl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import core.metamodel.IPopulation;
import core.metamodel.pop.APopulationAttribute;
import core.metamodel.pop.APopulationEntity;
import core.metamodel.pop.APopulationValue;
import core.metamodel.pop.io.GSSurveyType;
import gospl.algo.ipf.SRIPFAlgo;
import gospl.algo.sr.ISyntheticReconstructionAlgo;
import gospl.distribution.GosplInputDataManager;
import gospl.distribution.exception.IllegalControlTotalException;
import gospl.distribution.exception.IllegalDistributionCreation;
import gospl.distribution.matrix.INDimensionalMatrix;
import gospl.distribution.matrix.coordinate.ACoordinate;
import gospl.generator.DistributionBasedGenerator;
import gospl.generator.ISyntheticGosplPopGenerator;
import gospl.io.GosplSurveyFactory;
import gospl.io.exception.InvalidSurveyFormatException;
import gospl.sampler.IDistributionSampler;
import gospl.sampler.ISampler;
import gospl.sampler.sr.GosplBasicSampler;
import gospl.validation.GosplIndicator;
import gospl.validation.GosplIndicatorFactory;

public class SRSample {

	// Input nb of entity
	static int popSize = 100000;
	// Output file path 
	final static Path outputPath = Paths.get("src/main/java/rouen/gospl/output/SRSample_export.csv"); 
	final static Path reportPath = Paths.get("src/main/java/rouen/gospl/output/SRSample_report.csv");
	final static Path statPath = Paths.get("src/main/java/rouen/gospl/output/SRSample_stat.csv");
	// Setup configuration file
	final static Path configurationFile = Paths.get("src/main/java/rouen/gospl/data/GSC_Rouen_IPF.xml");
	
	public static void main(String[] args) {

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
		} catch (final IOException e) {
			throw new RuntimeException(e);
		} catch (final InvalidSurveyFormatException e) {
			throw new RuntimeException(e);
		} catch (InvalidFormatException e) {
			throw new RuntimeException(e);
		}
		
		//---------------------------------------//
		//----------- SETUP ALGORITHM -----------//
		//---------------------------------------//

		// Input sample
		IPopulation<APopulationEntity, APopulationAttribute, APopulationValue> seed = gdf.getRawSamples().iterator().next();
		
		
		// Input control tables (also known as marginals)
		INDimensionalMatrix<APopulationAttribute, APopulationValue, Double> collapsedMarginals = null;
		try {
			collapsedMarginals = gdf.collapseDataTablesIntoDistributions();
		} catch (IllegalDistributionCreation e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalControlTotalException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// Setup IPF with seed, number of maximum fitting iteration, and delta convergence criteria 
		ISyntheticReconstructionAlgo<IDistributionSampler> ipf = new SRIPFAlgo(seed, 1000, Math.pow(10, -2));

		// Build a sample from the IPF process
		ISampler<ACoordinate<APopulationAttribute, APopulationValue>> sampler = null;
		try {
			sampler = ipf.inferSRSampler(collapsedMarginals, new GosplBasicSampler());
		} catch (IllegalDistributionCreation e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//----------------------------------------------//
		//------- GENERATE POPULATION AND EXPORT -------//
		//----------------------------------------------//

		// Setup the generator using ipf-based sampler
		ISyntheticGosplPopGenerator generator = new DistributionBasedGenerator(sampler);

		// Generate the population
		IPopulation<APopulationEntity, APopulationAttribute, APopulationValue> population = generator.generate(popSize);

		// Setup survey factory to export output population
		GosplSurveyFactory gsf = new GosplSurveyFactory();
		GosplIndicatorFactory gif = GosplIndicatorFactory.getFactory();
		try {
			gsf.createSummary(outputPath.toFile(), GSSurveyType.Sample, population);
			gsf.createSummary(reportPath.toFile(), GSSurveyType.GlobalFrequencyTable, population);
			gif.saveReport(statPath.toFile(), gif.getReport(Arrays.asList(GosplIndicator.values()), 
					collapsedMarginals, population), "IPF", population.size());
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidSurveyFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
