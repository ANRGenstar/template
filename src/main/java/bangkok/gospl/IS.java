package bangkok.gospl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import core.metamodel.pop.APopulationAttribute;
import core.metamodel.pop.APopulationValue;
import core.metamodel.pop.io.GSSurveyType;
import core.util.GSPerformanceUtil;
import gospl.GosplPopulation;
import gospl.algo.sr.ISyntheticReconstructionAlgo;
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
import gospl.sampler.ISampler;
import gospl.sampler.sr.GosplBasicSampler;

public class IS {

	final static String report = "PopReport.csv";
	final static String export = "PopExport.csv";
	
	static int targetPopulation = 10000;
	final static String attributeNamePopulation = "population";
	
	final static Path confFile = Paths.get("src/main/java/bangkok/gospl/data/GSC_Bangkok.xml");
	
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
		final ISyntheticReconstructionAlgo<IDistributionSampler> distributionInfAlgo = new IndependantHypothesisAlgo();
		ISampler<ACoordinate<APopulationAttribute, APopulationValue>> sampler = null;
		try {
			sampler = distributionInfAlgo.inferSRSampler(distribution, new GosplBasicSampler());
		} catch (final IllegalDistributionCreation e1) {
			e1.printStackTrace();
		}
		
		targetPopulation = targetPopulation <= 0 ? 
				distribution.getVal(distribution.getDimensions()
						.stream().filter(dim -> dim.getAttributeName().equals(attributeNamePopulation))
						.findAny().get().getValues()).getValue().intValue() : targetPopulation;

		final GSPerformanceUtil gspu =
				new GSPerformanceUtil("Start generating synthetic population of size " + targetPopulation);

		// BUILD THE GENERATOR
		final ISyntheticGosplPopGenerator ispGenerator = new DistributionBasedGenerator(sampler);

		// BUILD THE POPULATION
		try {
			population = ispGenerator.generate(targetPopulation);
			gspu.sysoStempPerformance("End generating synthetic population: elapse time",
					IS.class.getName());
		} catch (final NumberFormatException e) {
			e.printStackTrace();
		}

		// MAKE REPORT
		final GosplSurveyFactory sf = new GosplSurveyFactory(0, ';', 1, 1);
		final String pathFolder = confFile.getParent().getParent().toString() + File.separator + "output" + File.separator;

		try {
			sf.createSummary(new File(pathFolder+export), GSSurveyType.Sample, population);
			sf.createSummary(new File(pathFolder+report), GSSurveyType.GlobalFrequencyTable, population);
		} catch (IOException | InvalidSurveyFormatException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

}
