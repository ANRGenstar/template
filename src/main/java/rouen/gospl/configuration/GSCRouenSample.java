package rouen.gospl.configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
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
import java.util.stream.IntStream;
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

public class GSCRouenSample {

	public static String CONF_CLASS_PATH = "src/main/java/rouen/gospl/data/";
	public static String CONF_EXPORT = "GSC_Rouen_Sample";

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

		// Make file path absolute
		Path relativePath = Paths.get(CONF_CLASS_PATH).toAbsolutePath();

		if(new ArrayList<>(Arrays.asList(args)).isEmpty()){

			// Setup input files' configuration for individual aggregated data
			inputFiles.add(new GSSurveyWrapper(relativePath.resolve("Age & Couple-Tableau 1.csv").toString(), 
					GSSurveyType.ContingencyTable, ';', 1, 1));
			inputFiles.add(new GSSurveyWrapper(relativePath.resolve("Age & Sexe & CSP-Tableau 1.csv").toString(), 
					GSSurveyType.ContingencyTable, ';', 2, 1));
			inputFiles.add(new GSSurveyWrapper(relativePath.resolve("Age & Sexe-Tableau 1.csv").toString(), 
					GSSurveyType.ContingencyTable, ';', 1, 1));
			inputFiles.add(new GSSurveyWrapper(relativePath.resolve("Rouen_iris.csv").toString(), 
					GSSurveyType.ContingencyTable, ',', 1, 1));
			inputFiles.add(new GSSurveyWrapper(relativePath.resolve("sampleRouen.csv").toString(), 
					GSSurveyType.Sample, ',', 1, 1));

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

				// Create another "age" attribute with diverging data and model modalities
				List<String> refList = Arrays.asList("Moins de 5 ans", "5 à 9 ans", "10 à 14 ans", "15 à 19 ans", "20 à 24 ans", 
						"25 à 29 ans", "30 à 34 ans", "35 à 39 ans", "40 à 44 ans", "45 à 49 ans", 
						"50 à 54 ans", "55 à 59 ans", "60 à 64 ans", "65 à 69 ans", "70 à 74 ans", "75 à 79 ans", 
						"80 à 84 ans", "85 à 89 ans", "90 à 94 ans", "95 à 99 ans");
				List<String> mapList = Arrays.asList("000", "005", "010", "015", "020", "025", "030",
						"035", "040", "045", "050", "055", "060", "065", "070", "075", "080", "085",
						"090", "095");
				Map<Set<String>, Set<String>> mapperA3 = new HashMap<>();
				IntStream.range(0, refList.size()).forEach(i -> mapperA3.put(new HashSet<>(Arrays.asList(mapList.get(i))), 
						new HashSet<>(Arrays.asList(refList.get(i)))));
				mapperA3.put(Stream.of("100", "105", "110", "115", "120").collect(Collectors.toSet()),
						Stream.of("100 ans ou plus").collect(Collectors.toSet()));
				inputAttributes.add(attf.createAttribute("agerevq", GSEnumDataType.Integer, 	
						Arrays.asList("000", "005", "010", "015", "020", "025", "030",
								"035", "040", "045", "050", "055", "060", "065", "070", "075", "080", "085",
								"090", "095", "100", "105", "110", "115", "120"), 
						GSEnumAttributeType.unique, referentAgeAttribute, mapperA3));

				// --------------------------
				// Setup "COUPLE" attribute: INDIVIDUAL
				// --------------------------

				String vec = "Vivant en couple";
				String nvpec = "Ne vivant pas en couple";
				
				APopulationAttribute attCouple = attf.createAttribute("Couple", GSEnumDataType.String, 
						Arrays.asList(vec, nvpec), 
						GSEnumAttributeType.unique); 
				inputAttributes.add(attCouple);

				Map<Set<String>, Set<String>> mapperC1 = new HashMap<>();
				mapperC1.put(Stream.of("1").collect(Collectors.toSet()),
						Stream.of(vec).collect(Collectors.toSet()));
				mapperC1.put(Stream.of("2").collect(Collectors.toSet()),
						Stream.of(nvpec).collect(Collectors.toSet()));
				
				inputAttributes.add(attf.createAttribute("couple", GSEnumDataType.String, 
						Arrays.asList("1", "2"), Arrays.asList(vec, nvpec), 
						GSEnumAttributeType.unique, attCouple, mapperC1));

				// --------------------------
				// Setup "IRIS" attribute: INDIVIDUAL
				// --------------------------

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

				// -------------------------
				// Setup "SEXE" attribute: INDIVIDUAL
				// -------------------------

				String h = "Hommes";
				String f = "Femmes";
				
				APopulationAttribute attSexe = attf.createAttribute("Sexe", GSEnumDataType.String,
						Arrays.asList(h, f), GSEnumAttributeType.unique);
				inputAttributes.add(attSexe);

				Map<Set<String>, Set<String>> mapperS1 = new HashMap<>();
				mapperS1.put(Stream.of("1").collect(Collectors.toSet()),
						Stream.of(h).collect(Collectors.toSet()));
				mapperS1.put(Stream.of("2").collect(Collectors.toSet()),
						Stream.of(f).collect(Collectors.toSet()));
				inputAttributes.add(attf.createAttribute("sexe", GSEnumDataType.String,
						Arrays.asList("1", "2"), Arrays.asList(h, f), 
						GSEnumAttributeType.unique, attSexe, mapperS1));

				// -------------------------
				// Setup "CSP" attribute: INDIVIDUAL
				// -------------------------
				APopulationAttribute attCSP = attf.createAttribute("csp", GSEnumDataType.String, 
						Arrays.asList("Agriculteurs exploitants", "Artisans. commerçants. chefs d'entreprise", 
								"Cadres et professions intellectuelles supérieures", "Professions intermédiaires", 
								"Employés", "Ouvriers", "Retraités", "Autres personnes sans activité professionnelle"), 
						GSEnumAttributeType.unique); 
				inputAttributes.add(attCSP);
				
				Map<Set<String>, Set<String>> mapperCS1 = new HashMap<>();
				mapperCS1.put(Stream.of("1").collect(Collectors.toSet()), 
						Stream.of("Agriculteurs exploitants").collect(Collectors.toSet()));
				mapperCS1.put(Stream.of("2").collect(Collectors.toSet()), 
						Stream.of("Artisans. commerçants. chefs d'entreprise").collect(Collectors.toSet()));
				mapperCS1.put(Stream.of("3").collect(Collectors.toSet()), 
						Stream.of("Cadres et professions intellectuelles supérieures").collect(Collectors.toSet()));
				mapperCS1.put(Stream.of("4").collect(Collectors.toSet()), 
						Stream.of("Professions intermédiaires").collect(Collectors.toSet()));
				mapperCS1.put(Stream.of("5").collect(Collectors.toSet()), 
						Stream.of("Employés").collect(Collectors.toSet()));
				mapperCS1.put(Stream.of("6").collect(Collectors.toSet()), 
						Stream.of("Ouvriers").collect(Collectors.toSet()));
				mapperCS1.put(Stream.of("7").collect(Collectors.toSet()), 
						Stream.of("Retraités").collect(Collectors.toSet()));
				mapperCS1.put(Stream.of("8").collect(Collectors.toSet()), 
						Stream.of("Autres personnes sans activité professionnelle").collect(Collectors.toSet()));
				inputAttributes.add(attf.createAttribute("cs1", GSEnumDataType.String, 
						Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8"), 
						GSEnumAttributeType.unique, attCSP, mapperCS1));

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