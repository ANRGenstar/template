package rouen;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import core.metamodel.IPopulation;
import core.metamodel.pop.APopulationAttribute;
import core.metamodel.pop.APopulationEntity;
import core.metamodel.pop.APopulationValue;
import core.metamodel.pop.io.GSSurveyType;
import gospl.algo.ISyntheticReconstructionAlgo;
import gospl.algo.ipf.DistributionInferenceIPFAlgo;
import gospl.algo.sampler.IDistributionSampler;
import gospl.algo.sampler.ISampler;
import gospl.algo.sampler.sr.GosplBasicSampler;
import gospl.distribution.GosplDistributionBuilder;
import gospl.distribution.exception.IllegalControlTotalException;
import gospl.distribution.exception.IllegalDistributionCreation;
import gospl.distribution.matrix.INDimensionalMatrix;
import gospl.distribution.matrix.coordinate.ACoordinate;
import gospl.generator.DistributionBasedGenerator;
import gospl.generator.ISyntheticGosplPopGenerator;
import gospl.io.GosplSurveyFactory;
import gospl.io.exception.InvalidSurveyFormatException;

public class IPF {

	public static void main(String[] args) {

		// Output file path 
		Path outputPath = Paths.get(args[1]);
		// Input nb of entity
		int popSize = Integer.valueOf(args[0]);
		// Setup configuration file
		Path configurationFile = Paths.get(args[2]);

		//---------------------------------------//
		//------- START TO PROCESS INPUTS -------//
		//---------------------------------------//

		GosplDistributionBuilder gdf = null;
		try {
			gdf = new GosplDistributionBuilder(configurationFile);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}
		
		try {
			gdf.buildDistributions();
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

		// Input sample
		IPopulation<APopulationEntity, APopulationAttribute, APopulationValue> seed = gdf.getRawSamples().iterator().next();

		// Input control tables (also known as marginals)
		INDimensionalMatrix<APopulationAttribute, APopulationValue, Double> matrix = null;
		try {
			matrix = gdf.collapseDistributions();
		} catch (IllegalDistributionCreation e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalControlTotalException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Setup IPF with seed, number of maximum fitting iteration, and delta convergence criteria 
		ISyntheticReconstructionAlgo<IDistributionSampler> ipf = new DistributionInferenceIPFAlgo(seed, 100, Math.pow(10, -2));

		// Build a sample from the IPF process
		ISampler<ACoordinate<APopulationAttribute, APopulationValue>> sampler = null;
		try {
			sampler = ipf.inferSRSampler(matrix, new GosplBasicSampler());
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
		try {
			gsf.createSurvey(outputPath.toFile(), GSSurveyType.Sample, population);
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
