package rouen.gospl.configuration;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import core.configuration.GenstarConfigurationFile;
import core.configuration.GenstarJsonUtil;
import core.configuration.dictionary.DemographicDictionary;
import core.metamodel.attribute.demographic.DemographicAttribute;
import core.metamodel.attribute.demographic.DemographicAttributeFactory;
import core.metamodel.attribute.demographic.MappedDemographicAttribute;
import core.metamodel.io.GSSurveyType;
import core.metamodel.io.GSSurveyWrapper;
import core.metamodel.value.IValue;
import core.metamodel.value.categoric.NominalValue;
import core.metamodel.value.categoric.template.GSCategoricTemplate;
import core.metamodel.value.numeric.RangeValue;
import core.util.data.GSEnumDataType;
import core.util.excpetion.GSIllegalRangedData;

public class GSCRouenSample {

	public static String CONF_CLASS_PATH = "src/main/java/rouen/gospl/data/";
	public static String CONF_EXPORT = "rouen_demographics_with_sample.gns";

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// Setup the factory that build attribute
		DemographicAttributeFactory attf = DemographicAttributeFactory.getFactory();

		// What to define in this configuration file
		List<GSSurveyWrapper> inputFiles = new ArrayList<>();
		DemographicDictionary<DemographicAttribute<? extends IValue>> dd = new DemographicDictionary<>();
		DemographicDictionary<MappedDemographicAttribute<? extends IValue, ? extends IValue>> records = new DemographicDictionary<>();

		// Make file path absolute
		Path relativePath = Paths.get(CONF_CLASS_PATH);

		if(new ArrayList<>(Arrays.asList(args)).isEmpty()){

			// Setup input files' configuration for individual aggregated data
			inputFiles.add(new GSSurveyWrapper(relativePath.resolve("Age & Couple-Tableau 1.csv"), 
					GSSurveyType.ContingencyTable, ';', 1, 1));
			inputFiles.add(new GSSurveyWrapper(relativePath.resolve("Age & Sexe & CSP-Tableau 1.csv"), 
					GSSurveyType.ContingencyTable, ';', 2, 1));
			inputFiles.add(new GSSurveyWrapper(relativePath.resolve("Age & Sexe-Tableau 1.csv"), 
					GSSurveyType.ContingencyTable, ';', 1, 1));
			inputFiles.add(new GSSurveyWrapper(relativePath.resolve("Rouen_iris.csv"), 
					GSSurveyType.ContingencyTable, ',', 1, 1));
			inputFiles.add(new GSSurveyWrapper(relativePath.resolve("sampleRouen.csv"), 
					GSSurveyType.Sample, ',', 1, 1));

			try {

				// --------------------------
				// Setupe "AGE" attribute: INDIVIDUAL
				// --------------------------

				// Instantiate a referent attribute
				DemographicAttribute<RangeValue> referentAgeAttribute = attf.createRangeAttribute("Age", 
						Arrays.asList("Moins de 5 ans", "5 à 9 ans", "10 à 14 ans", "15 à 19 ans", "20 à 24 ans", 
								"25 à 29 ans", "30 à 34 ans", "35 à 39 ans", "40 à 44 ans", "45 à 49 ans", 
								"50 à 54 ans", "55 à 59 ans", "60 à 64 ans", "65 à 69 ans", "70 à 74 ans", "75 à 79 ans", 
								"80 à 84 ans", "85 à 89 ans", "90 à 94 ans", "95 à 99 ans", "100 ans ou plus"));
				dd.addAttributes(referentAgeAttribute);		

				// Instantiate an aggregated attribute using previously referent attribute
				dd.addAttributes(attf.createRangeAggregatedAttribute("Age_2", referentAgeAttribute, 
						Map.of("15 à 19 ans", Set.of("15 à 19 ans"), 
								"20 à 24 ans", Set.of("20 à 24 ans"), 
								"25 à 39 ans", Set.of("25 à 29 ans", "30 à 34 ans", "35 à 39 ans"),
								"40 à 54 ans", Set.of("40 à 44 ans", "45 à 49 ans", "50 à 54 ans"),
								"55 à 64 ans", Set.of("55 à 59 ans", "60 à 64 ans"),
								"65 à 79 ans", Set.of("65 à 69 ans", "70 à 74 ans", "75 à 79 ans"),
								"80 ans ou plus", Set.of("80 à 84 ans", "85 à 89 ans", "90 à 94 ans", "95 à 99 ans", "100 ans ou plus"))
						));

				dd.addAttributes(attf.createRangeAggregatedAttribute("Age_3", referentAgeAttribute, 
						Map.of("15 à 19 ans", Set.of("15 à 19 ans"),
								"20 à 24 ans", Set.of("20 à 24 ans"), 
								"25 à 39 ans", Set.of("25 à 29 ans", "30 à 34 ans", "35 à 39 ans"),
								"40 à 54 ans", Set.of("40 à 44 ans", "45 à 49 ans", "50 à 54 ans"), 
								"55 à 64 ans", Set.of("55 à 59 ans", "60 à 64 ans"), 
								"65 ans ou plus", Set.of("65 à 69 ans", "70 à 74 ans", "75 à 79 ans", 
										"80 à 84 ans", "85 à 89 ans", "90 à 94 ans", "95 à 99 ans", "100 ans ou plus"))
						));		

				dd.addAttributes(attf.createIntegerAttribute("agerevq", referentAgeAttribute, 
						Map.ofEntries(
								Map.entry(Set.of("000"), Set.of("Moins de 5 ans")),
								Map.entry(Set.of("005"), Set.of("5 à 9 ans")),
								Map.entry(Set.of("010"), Set.of("10 à 14 ans")),
								Map.entry(Set.of("015"), Set.of("15 à 19 ans")),
								Map.entry(Set.of("020"), Set.of("20 à 24 ans")),
								Map.entry(Set.of("025"), Set.of("25 à 29 ans")),
								Map.entry(Set.of("030"), Set.of("30 à 34 ans")),
								Map.entry(Set.of("035"), Set.of("35 à 39 ans")),
								Map.entry(Set.of("040"), Set.of("40 à 44 ans")),
								Map.entry(Set.of("045"), Set.of("45 à 49 ans")),
								Map.entry(Set.of("050"), Set.of("50 à 54 ans")),
								Map.entry(Set.of("055"), Set.of("55 à 59 ans")),
								Map.entry(Set.of("060"), Set.of("60 à 64 ans")),
								Map.entry(Set.of("065"), Set.of("65 à 69 ans")),
								Map.entry(Set.of("070"), Set.of("70 à 74 ans")),
								Map.entry(Set.of("075"), Set.of("75 à 79 ans")),
								Map.entry(Set.of("080"), Set.of("80 à 84 ans")),
								Map.entry(Set.of("085"), Set.of("85 à 89 ans")),
								Map.entry(Set.of("090"), Set.of("90 à 94 ans")),
								Map.entry(Set.of("095"), Set.of("95 à 99 ans")),
								Map.entry(Set.of("100", "105", "110", "115", "120"), Set.of("100 ans ou plus"))
								)));

				// --------------------------
				// Setup "COUPLE" attribute: INDIVIDUAL
				// --------------------------

				String vec = "Vivant en couple";
				String nvpec = "Ne vivant pas en couple";
				
				DemographicAttribute<NominalValue> attCouple = attf.createNominalAttribute("Couple", new GSCategoricTemplate());
				Stream.of(vec, nvpec).forEach(val -> attCouple.getValueSpace().addValue(val));
				dd.addAttributes(attCouple);
				
				dd.addAttributes(attf.createNominalRecordAttribute("couple", attCouple, Map.of("1", vec, "2", nvpec)));

				// --------------------------
				// Setup "IRIS" attribute: INDIVIDUAL
				// --------------------------

				DemographicAttribute<? extends IValue> attIris = attf.createAttribute("iris", GSEnumDataType.Nominal, 
						Arrays.asList("765400602", "765400104","765400306","765400201",
								"765400601","765400901","765400302","765400604","765400304",
								"765400305","765400801","765400301","765401004","765401003",
								"765400402","765400603","765400303","765400103","765400504",
								"765401006","765400702","765400401","765400202","765400802",
								"765400502","765400106","765400701","765401005","765400204",
								"765401001","765400405","765400501","765400102","765400503",
								"765400404","765400105","765401002","765400902","765400403",
								"765400203","765400101","765400205")); 
				
				dd.addAttributes(attIris);
				
				records.addAttributes(attf.createIntegerRecordAttribute("P13_POP", attIris));

				// -------------------------
				// Setup "SEXE" attribute: INDIVIDUAL
				// -------------------------

				String h = "Hommes";
				String f = "Femmes";
				
				DemographicAttribute<? extends IValue> attSexe = attf.createAttribute("Sexe", 
						GSEnumDataType.Nominal, Arrays.asList(h, f));
				dd.addAttributes(attSexe);

				dd.addAttributes(attf.createNominalRecordAttribute("Sexe2", attSexe, Map.of("1", h, "2", f)));

				// -------------------------
				// Setup "CSP" attribute: INDIVIDUAL
				// -------------------------
				
				List<String> csp = Arrays.asList("Agriculteurs exploitants", "Artisans. commerçants. chefs d'entreprise", 
						"Cadres et professions intellectuelles supérieures", "Professions intermédiaires", 
						"Employés", "Ouvriers", "Retraités", "Autres personnes sans activité professionnelle");
				DemographicAttribute<? extends IValue> attCSP = attf.createAttribute("csp", GSEnumDataType.Nominal, csp); 
				dd.addAttributes(attCSP);
				
				dd.addAttributes(attf.createNominalRecordAttribute("cs1", attCSP, 
						csp.stream().collect(Collectors.toMap(
								p -> Integer.toString(csp.indexOf(p)), 
								Function.identity()))));

			} catch (GSIllegalRangedData e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// ------------------------------
			// SERIALIZE CONFIGURATION FILES
			// ------------------------------

			GenstarConfigurationFile gcf = new GenstarConfigurationFile();
			gcf.setBaseDirectory(FileSystems.getDefault().getPath("."));
			gcf.setSurveyWrappers(inputFiles);
			gcf.setDemoDictionary(dd);
			gcf.setRecords(records);
			
			try {
				new GenstarJsonUtil().marshalToGenstarJson(relativePath.resolve(CONF_EXPORT), 
						gcf);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			GenstarConfigurationFile gcf = null;
			try {
				gcf = new GenstarJsonUtil().unmarshalFromGenstarJson(Paths.get(args[0].trim()), GenstarConfigurationFile.class);
			} catch (IllegalArgumentException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Deserialize Genstar data configuration contains:\n"+
					gcf.getDemoDictionary().getAttributes().size()+" attributs\n"+
					gcf.getSurveyWrappers().size()+" data files");
		}
		
	}

}
