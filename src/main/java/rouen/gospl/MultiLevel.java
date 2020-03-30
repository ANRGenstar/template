package rouen.gospl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import core.metamodel.attribute.Attribute;
import core.metamodel.io.GSSurveyType;
import core.metamodel.value.IValue;
import core.util.GSPerformanceUtil;
import gospl.GosplPopulation;
import gospl.algo.sr.multilayer.ISynthethicReconstructionMultiLayerAlgo;
import gospl.algo.sr.multilayer.ds.DirectSamplingMultiLayerAlgo;
import gospl.distribution.GosplInputDataManager;
import gospl.distribution.exception.IllegalControlTotalException;
import gospl.distribution.exception.IllegalDistributionCreation;
import gospl.distribution.matrix.INDimensionalMatrix;
import gospl.distribution.matrix.coordinate.GosplMultiLayerCoordinate;
import gospl.generator.ISyntheticGosplPopGenerator;
import gospl.generator.MultiLayerGenerator;
import gospl.io.GosplSurveyFactory;
import gospl.io.exception.InvalidSurveyFormatException;
import gospl.sampler.ISampler;
import gospl.sampler.multilayer.GosplBiLayerSampler;

public class MultiLevel {

	static int hhNb = -1;
	static int iNb = 110755;
	
	// Output file path 
	final static Path outputPath = Paths.get("src/main/java/rouen/gospl/output/MLPop_export.csv"); 
	final static Path reportPath = Paths.get("src/main/java/rouen/gospl/output/MLPop_report.csv");
	final static Path statPath = Paths.get("src/main/java/rouen/gospl/output/MLPop_stat.csv");
	// Configuration file path
	final static Path confHHFile = Paths.get("src/main/java/rouen/gospl/data/rouen_multi.gns");
	final static Path confIndivFile = Paths.get("src/main/java/rouen/gospl/data/rouen_demographics.gns");

	public static void main(String[] args) {

		// THE POPULATION TO BE GENERATED
		GosplPopulation population = null;
		
		// -----------------------------------
		// BOTTOM LAYER POPULATION (Household)
		// -----------------------------------

		GosplInputDataManager indivdm = null;
		try {
			indivdm = new GosplInputDataManager(confIndivFile);
		} catch (final IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}
		
		// RETRIEV INFORMATION FROM DATA IN FORM OF A SET OF JOINT DISTRIBUTIONS
		try {
			indivdm.buildDataTables();
		} catch (final RuntimeException | IOException | 
				InvalidSurveyFormatException | InvalidFormatException e) {
			e.printStackTrace();
		}

		// HERE IS A CHOICE TO MAKE BASED ON THE TYPE OF GENERATOR WE WANT:
		// Choice is made here to use distribution based generator

		// so we collapse all distribution build from the data
		INDimensionalMatrix<Attribute<? extends IValue>, IValue, Double> iDistribution = null;
		try {
			iDistribution = indivdm.collapseDataTablesIntoDistribution();
		} catch (final IllegalDistributionCreation | IllegalControlTotalException e1) {
			e1.printStackTrace();
		}
		
		
		// -----------------------------------
		// BOTTOM LAYER POPULATION (Household)
		// -----------------------------------
		
		GosplInputDataManager hhdm = null;
		try {
			hhdm = new GosplInputDataManager(confHHFile);
		} catch (final IllegalArgumentException | IOException e) {
			e.printStackTrace();
		}

		// RETRIEV INFORMATION FROM DATA IN FORM OF A SET OF JOINT DISTRIBUTIONS
		try {
			hhdm.buildDataTables();
		} catch (final RuntimeException | IOException | 
				InvalidSurveyFormatException | InvalidFormatException e) {
			e.printStackTrace();
		}
		
		hhNb = hhdm.getRawDataTables().stream()
				.filter(m -> m.getMetaDataType().equals(GSSurveyType.ContingencyTable))
				.findFirst().get()
				.getVal().getValue().intValue();
		if(hhNb <= 0)
			hhNb = iNb / 15;

		// HERE IS A CHOICE TO MAKE BASED ON THE TYPE OF GENERATOR WE WANT:
		// Choice is made here to use distribution based generator

		// so we collapse all distribution build from the data
		INDimensionalMatrix<Attribute<? extends IValue>, IValue, Double> hhDistribution = null;
		try {
			hhDistribution = hhdm.collapseDataTablesIntoDistribution();
		} catch (final IllegalDistributionCreation | IllegalControlTotalException e1) {
			e1.printStackTrace();
		}
		
		// ----------------------------------------------
		// BUILD THE SAMPLER WITH THE INFERENCE ALGORITHM
		// ----------------------------------------------
		
		ISampler<GosplMultiLayerCoordinate> sampler = null;
		
		ISynthethicReconstructionMultiLayerAlgo<GosplBiLayerSampler> algo = new DirectSamplingMultiLayerAlgo();
		
		try {
			sampler = algo.inferSRMLSampler(hhDistribution, iDistribution, new GosplBiLayerSampler());
		} catch (IllegalDistributionCreation e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		final GSPerformanceUtil gspu =
				new GSPerformanceUtil("Start generating synthetic population of size " + hhNb);

		// BUILD THE GENERATOR
		final ISyntheticGosplPopGenerator ispGenerator = new MultiLayerGenerator(sampler);

		// TEST ON BUILDING ONE ENTITY
		population = ispGenerator.generate(1);
		System.exit(1);
		
		// BUILD THE POPULATION
		try {
			population = ispGenerator.generate(hhNb);
			gspu.sysoStempPerformance("End generating synthetic population: elapse time",
					SRnoSample.class.getName());
		} catch (final NumberFormatException e) {
			e.printStackTrace();
		}

		// MAKE REPORT
		final GosplSurveyFactory sf = new GosplSurveyFactory(0, ';', 1, 1);
		//final GosplIndicatorFactory gif = GosplIndicatorFactory.getFactory();

		try {
			outputPath.getParent().toFile().mkdirs();
			sf.createSummary(outputPath.toFile(), GSSurveyType.Sample, population);
			sf.createSummary(reportPath.toFile(), GSSurveyType.GlobalFrequencyTable, population);
			/*
			gif.saveReport(statPath.toFile(), gif.getReport(Arrays.asList(GosplIndicator.values()), 
					distribution, population), ALGO, population.size());
					*/
		} catch (IOException | InvalidSurveyFormatException | InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
	
}
