package vietnam.gospl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import core.metamodel.IPopulation;
import core.metamodel.attribute.Attribute;
import core.metamodel.entity.ADemoEntity;
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
	
	final static Path IPUMS_confFile = Paths.get("src/main/java/vietnam/gospl/data/vietnam_multi.gns");
	
	// Output file path 
	final static Path outputPath = Paths.get("src/main/java/vietnam/gospl/data/cov/output");
	
	// Parameter
	final static boolean ONLY_INDIVIDUAL = false;
	
	public static void main(String[] args) {
		
		// THE POPULATION TO BE GENERATED
		GosplPopulation population = null;

		final GSPerformanceUtil gspu =
				new GSPerformanceUtil("Start processing input data to generate population of BT");
		
		// INSTANCIATE FACTORIES FOR INDIVIDUAL AND HOUSEHOLD LEVEL DATA
		GosplInputDataManager df_indiv = null;
		GosplInputDataManager df_hh = null;
		try {
			df_indiv = new GosplInputDataManager(BT_confFile);
			df_hh = new GosplInputDataManager(IPUMS_confFile);
		} catch (final IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}
		
		// RETRIEV INFORMATION FROM INDIVIDUAL LEVEL DATA IN FORM OF A SET OF JOINT DISTRIBUTIONS
		try {
			df_indiv.buildDataTables();
		} catch (final RuntimeException | IOException | 
				InvalidSurveyFormatException | InvalidFormatException e) {
			e.printStackTrace();
		}
		
		// so we collapse all distribution build from the data
		INDimensionalMatrix<Attribute<? extends IValue>, IValue, Double> distribution = null;
		try {
			distribution = df_indiv.collapseDataTablesIntoDistribution();
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
			

			// Retrieve sample to setup the CO sampler
			List<IPopulation<ADemoEntity, Attribute<? extends IValue>>> samples = new ArrayList<>(df_hh.getRawSamples());
			
			System.out.println("There is "+samples.size()+" population level");
			
			Collections.sort(samples, (s1,s2) ->  s1.size() > s2.size() ? -1 : 1);
				
			try {
				sf.createSummary(outputPath.resolve("indiv_sample.csv").toFile(), GSSurveyType.Sample, samples.get(0));
				sf.createSummary(outputPath.resolve("indiv_frequency.csv").toFile(), GSSurveyType.GlobalFrequencyTable, samples.get(0));
				sf.createSummary(outputPath.resolve("hh_sample.csv").toFile(), GSSurveyType.Sample, samples.get(1));
				sf.createSummary(outputPath.resolve("hh_frequency.csv").toFile(), GSSurveyType.GlobalFrequencyTable, samples.get(1));
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
			int nb_pop = df_indiv.getContingencyTables().stream().findFirst().get().getVal().getValue();
			System.out.println("Total population in contingency table is: "+nb_pop);
			
			// Test Xã Thạnh Phước
			int tp_pop = df_indiv.getContingencyTables().stream().findFirst().get().getVal("commune","Xã Thạnh Phước").getValue();
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
