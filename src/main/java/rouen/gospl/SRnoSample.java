package rouen.gospl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import core.metamodel.attribute.demographic.DemographicAttribute;
import core.metamodel.io.GSSurveyType;
import core.metamodel.value.IValue;
import core.util.GSPerformanceUtil;
import gospl.GosplPopulation;
import gospl.algo.sr.ISyntheticReconstructionAlgo;
import gospl.algo.sr.hs.HierarchicalHypothesisAlgo;
import gospl.algo.sr.is.IndependantHypothesisAlgo;
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
import gospl.sampler.IHierarchicalSampler;
import gospl.sampler.ISampler;
import gospl.sampler.sr.GosplBasicSampler;
import gospl.sampler.sr.GosplHierarchicalSampler;
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
	static int targetPopulation = 10000;
	
	// Output file path 
	final static Path outputPath = Paths.get("src/main/java/rouen/gospl/output/SRNoSample_export.csv"); 
	final static Path reportPath = Paths.get("src/main/java/rouen/gospl/output/SRNoSample_report.csv");
	final static Path statPath = Paths.get("src/main/java/rouen/gospl/output/SRNoSample_stat.csv");
	// Configuration file path
	final static Path confFile = Paths.get("src/main/java/rouen/gospl/data/rouen_demographics.gns");
	
	public static void main(String[] args) {

		// THE POPULATION TO BE GENERATED
		GosplPopulation population = null;

		// INSTANCIATE FACTORY
		GosplInputDataManager df = null;
		try {
			df = new GosplInputDataManager(confFile);
		} catch (final IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}

		// RETRIEV INFORMATION FROM DATA IN FORM OF A SET OF JOINT DISTRIBUTIONS
		try {
			df.buildDataTables();
		} catch (final RuntimeException | IOException | 
				InvalidSurveyFormatException | InvalidFormatException e) {
			e.printStackTrace();
		}

		// HERE IS A CHOICE TO MAKE BASED ON THE TYPE OF GENERATOR WE WANT:
		// Choice is made here to use distribution based generator

		// so we collapse all distribution build from the data
		INDimensionalMatrix<DemographicAttribute<? extends IValue>, IValue, Double> distribution = null;
		try {
			distribution = df.collapseDataTablesIntoDistribution();
		} catch (final IllegalDistributionCreation | IllegalControlTotalException e1) {
			e1.printStackTrace();
		}

		// BUILD THE SAMPLER WITH THE INFERENCE ALGORITHM
		ISampler<ACoordinate<DemographicAttribute<? extends IValue>, IValue>> sampler = null;

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
			outputPath.getParent().toFile().mkdirs();
			sf.createSummary(outputPath.toFile(), GSSurveyType.Sample, population);
			sf.createSummary(reportPath.toFile(), GSSurveyType.GlobalFrequencyTable, population);
			gif.saveReport(statPath.toFile(), gif.getReport(Arrays.asList(GosplIndicator.values()), 
					distribution, population), ALGO, population.size());
		} catch (IOException | InvalidSurveyFormatException | InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

}
