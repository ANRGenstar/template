package bangkok.gospl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import core.metamodel.attribute.Attribute;
import core.metamodel.io.GSSurveyType;
import core.metamodel.value.IValue;
import core.util.GSPerformanceUtil;
import gospl.GosplPopulation;
import gospl.algo.sr.ISyntheticReconstructionAlgo;
import gospl.algo.sr.ds.DirectSamplingAlgo;
import gospl.algo.sr.hs.HierarchicalHypothesisAlgo;
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

public class SRNoSample {

	final static String report = "PopReport.csv";
	final static String export = "PopExport.csv";
	
	final static String ALGO = "IS";
	
	static int targetPopulation = 80000;
	final static String attributeNamePopulation = "population";
	
	final static Path confFile = Paths.get("src/main/java/bangkok/gospl/data/bangkok_demo.gns");
	
	public static void main(String[] args) {

		// THE POPULATION TO BE GENERATED
		GosplPopulation population = null;

		final GSPerformanceUtil gspu =
				new GSPerformanceUtil("Start processing input data to generate Bangkok population");
		
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
		
		gspu.sysoStempMessage("Start collapse input data into n dimensional matrix");

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
			ISyntheticReconstructionAlgo<IDistributionSampler> distributionInfAlgo = new DirectSamplingAlgo();
			try {
				sampler = distributionInfAlgo.inferSRSampler(distribution, new GosplBasicSampler());
			} catch (final IllegalDistributionCreation e1) {
				e1.printStackTrace();
			}
			break;
		}
		
		targetPopulation = targetPopulation <= 0 ? 
				distribution.getVal(distribution.getDimensions()
						.stream().filter(dim -> dim.getAttributeName().equals(attributeNamePopulation))
						.findAny().get().getValueSpace().getValues()
						.stream().collect(Collectors.toList())).getValue().intValue() : targetPopulation;

		gspu.sysoStempPerformance("Start generating synthetic population of size " + targetPopulation, 
				SRNoSample.class.getCanonicalName());

		// BUILD THE GENERATOR
		final ISyntheticGosplPopGenerator ispGenerator = new DistributionBasedGenerator(sampler);

		// BUILD THE POPULATION
		try {
			population = ispGenerator.generate(targetPopulation);
			gspu.sysoStempPerformance("End generating synthetic population: elapse time",
					SRNoSample.class.getName());
		} catch (final NumberFormatException e) {
			e.printStackTrace();
		}

		// MAKE REPORT
		final GosplSurveyFactory sf = new GosplSurveyFactory(0, ';', 1, 1);
		final String pathFolder = confFile.getParent().getParent().toString() 
				+ File.separator + "output" + File.separator;

		try {
			sf.createSummary(new File(pathFolder+export), GSSurveyType.Sample, population);
			sf.createSummary(new File(pathFolder+report), GSSurveyType.GlobalFrequencyTable, population);
			Set<Attribute<? extends IValue>> popAtt = population.getPopulationAttributes();
			List<Set<Attribute<? extends IValue>>> formats = df.getRawDataTables()
					.stream().map(matrix -> matrix.getDimensions()
							.stream().filter(dim -> popAtt.contains(dim))
							.collect(Collectors.toSet()))
					.collect(Collectors.toList());
			for(Set<Attribute<? extends IValue>> format : formats){
				String name = format.stream().map(dim -> dim.getAttributeName().length() > 2 ?
							dim.getAttributeName().substring(0, 2) : dim.getAttributeName())
						.collect(Collectors.joining("x"));
				sf.createContingencyTable(new File(pathFolder+name+".csv"), 
						format, population);
			}
		} catch (IOException | InvalidSurveyFormatException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}


}
