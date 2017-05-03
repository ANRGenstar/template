package rouen.gospl.configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import core.configuration.GenstarConfigurationFile;
import core.configuration.GenstarXmlSerializer;
import core.metamodel.IAttribute;
import core.metamodel.IValue;
import core.metamodel.pop.APopulationAttribute;
import core.metamodel.pop.io.GSSurveyType;
import core.metamodel.pop.io.GSSurveyWrapper;
import core.util.data.GSEnumDataType;
import core.util.excpetion.GSIllegalRangedData;
import gospl.entity.attribute.GSEnumAttributeType;
import gospl.entity.attribute.GosplAttributeFactory;

public class GSCRouen_IS {

	public static String CONF_CLASS_PATH = "src/main/java/rouen/gospl/data/";
	public static String CONF_EXPORT = "GSC_Rouen_IS";

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// Setup the serializer that save configuration file
		GenstarXmlSerializer gxs = null;
		try {
			gxs = new GenstarXmlSerializer();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Setup the factory that build attribute
		GosplAttributeFactory attf = new GosplAttributeFactory();

		// What to define in this configuration file
		List<GSSurveyWrapper> inputFiles = new ArrayList<>();
		Set<APopulationAttribute> inputAttributes = new HashSet<>();
		Map<String, IAttribute<? extends IValue>> inputKeyMap = new HashMap<>();


		if(new ArrayList<>(Arrays.asList(args)).isEmpty()){

			// Setup input files' configuration for individual aggregated data
			inputFiles.add(new GSSurveyWrapper("Age & Couple-Tableau 1.csv", 
					GSSurveyType.ContingencyTable, ';', 1, 1));
			inputFiles.add(new GSSurveyWrapper("Age & Sexe & CSP-Tableau 1.csv", 
					GSSurveyType.ContingencyTable, ';', 2, 1));
			inputFiles.add(new GSSurveyWrapper("Age & Sexe-Tableau 1.csv", 
					GSSurveyType.ContingencyTable, ';', 1, 1));
			inputFiles.add(new GSSurveyWrapper("Rouen_iris.csv", 
					GSSurveyType.ContingencyTable, ',', 1, 1));

			try {

				// --------------------------
				// Setupe "AGE" attribute: INDIVIDUAL
				// --------------------------

				// Instantiate a referent attribute
				APopulationAttribute referentAgeAttribute = attf.createAttribute("Age", GSEnumDataType.Integer, 
						Arrays.asList("Moins de 5 ans", "5 à 9 ans", "10 à 14 ans", "15 à 19 ans", "20 à 24 ans", 
								"25 à 29 ans", "30 à 34 ans", "35 à 39 ans", "40 à 44 ans", "45 à 49 ans", 
								"50 à 54 ans", "55 à 59 ans", "60 à 64 ans", "65 à 69 ans", "70 à 74 ans", "75 à 79 ans", 
								"80 à 84 ans", "85 à 89 ans", "90 à 94 ans", "95 à 99 ans", "100 ans ou plus"), GSEnumAttributeType.range);
				inputAttributes.add(referentAgeAttribute);		

				// Create a mapper
				Map<Set<String>, Set<String>> mapperA1 = new HashMap<>();
				mapperA1.put(Stream.of("15 à 19 ans").collect(Collectors.toSet()), 
						Stream.of("15 à 19 ans").collect(Collectors.toSet()));
				mapperA1.put(Stream.of("20 à 24 ans").collect(Collectors.toSet()), 
						Stream.of("20 à 24 ans").collect(Collectors.toSet())); 
				mapperA1.put(Stream.of("25 à 39 ans").collect(Collectors.toSet()), 
						Stream.of("25 à 29 ans", "30 à 34 ans", "35 à 39 ans").collect(Collectors.toSet()));
				mapperA1.put(Stream.of("40 à 54 ans").collect(Collectors.toSet()), 
						Stream.of("40 à 44 ans", "45 à 49 ans", "50 à 54 ans").collect(Collectors.toSet()));
				mapperA1.put(Stream.of("55 à 64 ans").collect(Collectors.toSet()),
						Stream.of("55 à 59 ans", "60 à 64 ans").collect(Collectors.toSet()));
				mapperA1.put(Stream.of("65 à 79 ans").collect(Collectors.toSet()), 
						Stream.of("65 à 69 ans", "70 à 74 ans", "75 à 79 ans").collect(Collectors.toSet()));
				mapperA1.put(Stream.of("80 ans ou plus").collect(Collectors.toCollection(HashSet::new)),
						Stream.of("80 à 84 ans", "85 à 89 ans", "90 à 94 ans", 
								"95 à 99 ans", "100 ans ou plus").collect(Collectors.toCollection(HashSet::new)));
				// Instantiate an aggregated attribute using previously referent attribute
				inputAttributes.add(attf.createAttribute("Age_2", GSEnumDataType.Integer,
						mapperA1.keySet().stream().flatMap(set -> set.stream()).collect(Collectors.toList()), 
						GSEnumAttributeType.range, referentAgeAttribute, mapperA1));

				// Create another mapper
				Map<Set<String>, Set<String>> mapperA2 = new HashMap<>();
				mapperA2.put(Stream.of("15 à 19 ans").collect(Collectors.toSet()), 
						Stream.of("15 à 19 ans").collect(Collectors.toSet()));
				mapperA2.put(Stream.of("20 à 24 ans").collect(Collectors.toSet()), 
						Stream.of("20 à 24 ans").collect(Collectors.toSet()));
				mapperA2.put(Stream.of("25 à 39 ans").collect(Collectors.toSet()), 
						Stream.of("25 à 29 ans", "30 à 34 ans", "35 à 39 ans").collect(Collectors.toSet()));
				mapperA2.put(Stream.of("40 à 54 ans").collect(Collectors.toSet()), 
						Stream.of("40 à 44 ans", "45 à 49 ans", "50 à 54 ans").collect(Collectors.toSet()));
				mapperA2.put(Stream.of("55 à 64 ans").collect(Collectors.toSet()),
						Stream.of("55 à 59 ans", "60 à 64 ans").collect(Collectors.toSet()));
				mapperA2.put(Stream.of("65 ans ou plus").collect(Collectors.toSet()), 
						Stream.of("65 à 69 ans", "70 à 74 ans", "75 à 79 ans", 
								"80 à 84 ans", "85 à 89 ans", "90 à 94 ans", "95 à 99 ans", "100 ans ou plus").collect(Collectors.toSet()));
				inputAttributes.add(attf.createAttribute("Age_3", GSEnumDataType.Integer,
						mapperA2.keySet().stream().flatMap(set -> set.stream()).collect(Collectors.toList()), 
						GSEnumAttributeType.range, referentAgeAttribute, mapperA2));		

				// --------------------------
				// Setup "COUPLE" attribute: INDIVIDUAL
				// --------------------------

				APopulationAttribute attCouple = attf.createAttribute("Couple", GSEnumDataType.String, 
						Arrays.asList("Vivant en couple", "Ne vivant pas en couple"), 
						GSEnumAttributeType.unique); 
				inputAttributes.add(attCouple);

				// -------------------------
				// Setup "SEXE" attribute: INDIVIDUAL
				// -------------------------

				APopulationAttribute attSexe = attf.createAttribute("Sexe", GSEnumDataType.String,
						Arrays.asList("Hommes", "Femmes"), GSEnumAttributeType.unique);
				inputAttributes.add(attSexe);

				// -------------------------
				// Setup "CSP" attribute: INDIVIDUAL
				// -------------------------
				APopulationAttribute attCSP = attf.createAttribute("CSP", GSEnumDataType.String, 
						Arrays.asList("Agriculteurs exploitants", "Artisans. commerçants. chefs d'entreprise", 
								"Cadres et professions intellectuelles supérieures", "Professions intermédiaires", 
								"Employés", "Ouvriers", "Retraités", "Autres personnes sans activité professionnelle"), 
						GSEnumAttributeType.unique); 
				inputAttributes.add(attCSP);
				
				// -------------------------
				// Setup "IRIS" attribute: INDIVIDUAL
				// -------------------------
				APopulationAttribute attIris = attf.createAttribute("iris", GSEnumDataType.String, 
						Arrays.asList("765400602", "765400104","765400306","765400201",
								"765400601","765400901","765400302","765400604","765400304",
								"765400305","765400801","765400301","765401004","765401003",
								"765400402","765400603","765400303","765400103","765400504",
								"765401006","765400702","765400401","765400202","765400802",
								"765400502","765400106","765400701","765401005","765400204",
								"765401001","765400405","765400501","765400102","765400503",
								"765400404","765400105","765401002","765400902","765400403",
								"765400203","765400101","765400205"), 
						GSEnumAttributeType.unique); 
				
				APopulationAttribute attIrisRecord = attf.createAttribute("population", GSEnumDataType.Integer, 
						Arrays.asList("P13_POP"), GSEnumAttributeType.record, attIris, Collections.emptyMap());
				
				inputAttributes.add(attIris);
				inputAttributes.add(attIrisRecord);

			} catch (GSIllegalRangedData e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// ------------------------------
			// SERIALIZE CONFIGURATION FILES
			// ------------------------------

			try {
				gxs.setMkdir(Paths.get(CONF_CLASS_PATH).toAbsolutePath());
				GenstarConfigurationFile gsdI = new GenstarConfigurationFile(inputFiles, inputAttributes, inputKeyMap);
				gxs.serializeGSConfig(gsdI, CONF_EXPORT);
				System.out.println("Serialize Genstar input data with:\n"+
						gsdI.getAttributes().size()+" attributs\n"+
						gsdI.getSurveyWrappers().size()+" data files");

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			GenstarConfigurationFile gcf = null;
			try {
				gcf = gxs.deserializeGSConfig(Paths.get(args[0].trim()));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Deserialize Genstar data configuration contains:\n"+
					gcf.getAttributes().size()+" attributs\n"+
					gcf.getSurveyWrappers().size()+" data files");
		}
		
	}

}
