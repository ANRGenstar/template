package rouen.gospl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import core.metamodel.pop.APopulationAttribute;
import core.metamodel.pop.APopulationValue;
import core.metamodel.pop.io.GSSurveyType;
import core.util.GSPerformanceUtil;
import gospl.GosplPopulation;
import gospl.algo.ISyntheticReconstructionAlgo;
import gospl.algo.generator.DistributionBasedGenerator;
import gospl.algo.generator.ISyntheticGosplPopGenerator;
import gospl.algo.hs.HierarchicalHypothesisAlgo;
import gospl.algo.is.IndependantHypothesisAlgo;
import gospl.algo.sampler.IDistributionSampler;
import gospl.algo.sampler.IHierarchicalSampler;
import gospl.algo.sampler.ISampler;
import gospl.algo.sampler.sr.GosplBasicSampler;
import gospl.algo.sampler.sr.GosplHierarchicalSampler;
import gospl.distribution.GosplInputDataManager;
import gospl.distribution.exception.IllegalControlTotalException;
import gospl.distribution.exception.IllegalDistributionCreation;
import gospl.distribution.matrix.INDimensionalMatrix;
import gospl.distribution.matrix.coordinate.ACoordinate;
import gospl.io.GosplSurveyFactory;
import gospl.io.exception.InvalidSurveyFormatException;
import gospl.validation.GosplIndicator;
import gospl.validation.GosplIndicatorFactory;

/**
 * TODO: describe motives and method
 * 
 * IS stands for Independent Sampling <br>
 * HS stands for Hierarchical Sampling
 * 
 * @author kevinchapuis
 *
 */
public class SRnoSample {

	private static final String ALGO = "IS";
	static int targetPopulation = 100000;
	
	// Output file path 
	final static Path outputPath = Paths.get("src/main/java/rouen/gospl/output/SRNoSample_export.csv"); 
	final static Path reportPath = Paths.get("src/main/java/rouen/gospl/output/SRNoSample_report.csv");
	final static Path statPath = Paths.get("src/main/java/rouen/gospl/output/SRNoSample_stat.csv");
	// Configuration file path
	final static Path confFile = Paths.get("src/main/java/rouen/gospl/data/GSC_Rouen.xml");
	
	public static void main(String[] args) {

		// THE POPULATION TO BE GENERATED
		GosplPopulation population = null;

		// INSTANCIATE FACTORY
		GosplInputDataManager df = null;
		try {
			df = new GosplInputDataManager(confFile);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}

		// RETRIEV INFORMATION FROM DATA IN FORM OF A SET OF JOINT DISTRIBUTIONS
		try {
			df.buildDataTables();
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

		// HERE IS A CHOICE TO MAKE BASED ON THE TYPE OF GENERATOR WE WANT:
		// Choice is made here to use distribution based generator

		// so we collapse all distribution build from the data
		INDimensionalMatrix<APopulationAttribute, APopulationValue, Double> distribution = null;
		try {
			distribution = df.collapseDataTablesIntoDistributions();
		} catch (final IllegalDistributionCreation e1) {
			e1.printStackTrace();
		} catch (final IllegalControlTotalException e1) {
			e1.printStackTrace();
		}

		// BUILD THE SAMPLER WITH THE INFERENCE ALGORITHM
		ISampler<ACoordinate<APopulationAttribute, APopulationValue>> sampler = null;

		switch (ALGO) {
		case "HS":
			ISyntheticReconstructionAlgo<IHierarchicalSampler> hierarchicalInfAlgo = new HierarchicalHypothesisAlgo();
			try {
				sampler = hierarchicalInfAlgo.inferSRSampler(distribution, new GosplHierarchicalSampler());
			} catch (IllegalDistributionCreation e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			break;
		default:
			ISyntheticReconstructionAlgo<IDistributionSampler> distributionInfAlgo = new IndependantHypothesisAlgo();
			try {
				sampler = distributionInfAlgo.inferSRSampler(distribution, new GosplBasicSampler());
			} catch (final IllegalDistributionCreation e1) {
				e1.printStackTrace();
			}
			break;
		}

		targetPopulation = targetPopulation <= 0 ? distribution.getVal().getValue().intValue() : targetPopulation;
		
		final GSPerformanceUtil gspu =
				new GSPerformanceUtil("Start generating synthetic population of size " + targetPopulation);

		// BUILD THE GENERATOR
		final ISyntheticGosplPopGenerator ispGenerator = new DistributionBasedGenerator(sampler);

		// BUILD THE POPULATION
		try {
			population = ispGenerator.generate(targetPopulation);
			gspu.sysoStempPerformance("End generating synthetic population: elapse time",
					SRnoSample.class.getName());
		} catch (final NumberFormatException e) {
			e.printStackTrace();
		}

		// MAKE REPORT
		final GosplSurveyFactory sf = new GosplSurveyFactory(0, ';', 1, 1);
		final GosplIndicatorFactory gif = GosplIndicatorFactory.getFactory();

		try {
			sf.createSummary(outputPath.toFile(), GSSurveyType.Sample, population);
			sf.createSummary(reportPath.toFile(), GSSurveyType.GlobalFrequencyTable, population);
			gif.saveReport(statPath.toFile(), gif.getReport(Arrays.asList(GosplIndicator.values()), 
					distribution, population), ALGO, population.size());
		} catch (IOException | InvalidSurveyFormatException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

}
