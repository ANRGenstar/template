package vietnam.gospl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import core.metamodel.attribute.Attribute;
import core.metamodel.io.GSSurveyType;
import core.metamodel.value.IValue;
import core.util.GSPerformanceUtil;
import gospl.GosplPopulation;
import gospl.algo.sr.ISyntheticReconstructionAlgo;
import gospl.algo.sr.ds.DirectSamplingAlgo;
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

public class CoVid19_mainSP {

	final static Path BT_confFile = Paths.get("src/main/java/vietnam/gospl/data/cov/BT_demographics.gns");
	final static Path VP_confFile = Paths.get("src/main/java/vietnam/gospl/data/cov/VP_demographics.gns");
	
	// Output file path 
	final static Path outputPath = Paths.get("src/main/java/vietnam/gospl/data/cov/output/SRNoSample_export.csv"); 
	final static Path reportPath = Paths.get("src/main/java/vietnam/gospl/data/cov/output/SRNoSample_report.csv");
	final static Path statPath = Paths.get("src/main/java/vietnam/gospl/data/cov/output/SRNoSample_stat.csv");
	
	public static void main(String[] args) {
		
		// THE POPULATION TO BE GENERATED
		GosplPopulation population = null;

		final GSPerformanceUtil gspu =
				new GSPerformanceUtil("Start processing input data to generate population of BT");
		
		// INSTANCIATE FACTORY
		GosplInputDataManager df = null;
		try {
			df = new GosplInputDataManager(BT_confFile);
		} catch (final IllegalArgumentException | IOException e) {
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
		
		// so we collapse all distribution build from the data
		INDimensionalMatrix<Attribute<? extends IValue>, IValue, Double> distribution = null;
		try {
			distribution = df.collapseDataTablesIntoDistribution();
		} catch (final IllegalDistributionCreation e1) {
			e1.printStackTrace();
		} catch (final IllegalControlTotalException e1) {
			e1.printStackTrace();
		}
		
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
		int nb_pop = df.getContingencyTables().stream().findFirst().get().getVal().getValue();
		System.out.println(nb_pop);
		
		// Test Xã Thạnh Phước
		int tp_pop = df.getContingencyTables().stream().findFirst().get().getVal("commune","Xã Thạnh Phước").getValue();
		System.out.println(tp_pop);
		
		// BUILD THE POPULATION
		try {
			population = ispGenerator.generate(nb_pop);
			gspu.sysoStempPerformance("End generating synthetic population: elapse time",
					CoVid19_mainSP.class.getName());
		} catch (final NumberFormatException e) {
			e.printStackTrace();
		}

		// MAKE REPORT
		final GosplSurveyFactory sf = new GosplSurveyFactory(0, ';', 1, 1);
		final GosplIndicatorFactory gif = GosplIndicatorFactory.getFactory();

		try {
			outputPath.getParent().toFile().mkdirs();
			sf.createSummary(outputPath.toFile(), GSSurveyType.Sample, population);
			sf.createSummary(reportPath.toFile(), GSSurveyType.ContingencyTable, population);
			gif.saveReport(statPath.toFile(), gif.getReport(Arrays.asList(GosplIndicator.values()), 
					distribution, population), "DirectSampling", population.size());
		} catch (IOException | InvalidSurveyFormatException | InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
