package rouen.gospl.configuration;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import core.configuration.GenstarConfigurationFile;
import core.configuration.GenstarJsonUtil;
import core.configuration.dictionary.AttributeDictionary;
import core.metamodel.attribute.Attribute;
import core.metamodel.attribute.AttributeFactory;
import core.metamodel.io.GSSurveyType;
import core.metamodel.io.GSSurveyWrapper;
import core.metamodel.value.IValue;
import core.metamodel.value.numeric.RangeValue;
import core.util.data.GSDataParser;
import core.util.data.GSEnumDataType;
import core.util.excpetion.GSIllegalRangedData;

public class GSCRouen {

	public static String CONF_CLASS_PATH = "src/main/java/rouen/gospl/data";
	public static String CONF_EXPORT = "rouen_demographics.gns";

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {

		// Setup the factory that build attribute
		AttributeFactory attf = AttributeFactory.getFactory();

		// What to define in this configuration file
		List<GSSurveyWrapper> inputFiles = new ArrayList<>();
		
		AttributeDictionary dd = new AttributeDictionary();

		Path baseDirectory = FileSystems.getDefault().getPath(".");
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

			try {

				// --------------------------
				// Setupe "AGE" attribute: INDIVIDUAL
				// --------------------------

				// Instantiate a referent attribute
				Attribute<RangeValue> referentAgeAttribute = attf.createRangeAttribute("Age", 
						Arrays.asList("Moins de 5 ans", "5 à 9 ans", "10 à 14 ans", "15 à 19 ans", "20 à 24 ans", 
						"25 à 29 ans", "30 à 34 ans", "35 à 39 ans", "40 à 44 ans", "45 à 49 ans", 
						"50 à 54 ans", "55 à 59 ans", "60 à 64 ans", "65 à 69 ans", "70 à 74 ans", "75 à 79 ans", 
						"80 à 84 ans", "85 à 89 ans", "90 à 94 ans", "95 à 99 ans", "100 ans ou plus"));
				dd.addAttributes(referentAgeAttribute);		

				// Create a mapper
				Map<String, Collection<String>> mapperA1 = new LinkedHashMap<>(); 
				mapperA1.put("15 à 19 ans", new HashSet<>(Arrays.asList("15 à 19 ans")));
				mapperA1.put("20 à 24 ans", new HashSet<>(Arrays.asList("20 à 24 ans")));
				mapperA1.put("25 à 39 ans", new HashSet<>(Arrays.asList("25 à 29 ans", "30 à 34 ans", "35 à 39 ans")));
				mapperA1.put("40 à 54 ans", new HashSet<>(Arrays.asList("40 à 44 ans", "45 à 49 ans", "50 à 54 ans")));
				mapperA1.put("55 à 64 ans", new HashSet<>(Arrays.asList("55 à 59 ans", "60 à 64 ans")));
				mapperA1.put("65 à 79 ans", new HashSet<>(Arrays.asList("65 à 69 ans", "70 à 74 ans", "75 à 79 ans")));
				mapperA1.put("80 ans ou plus", new HashSet<>(Arrays.asList("80 à 84 ans", "85 à 89 ans", "90 à 94 ans", 
								"95 à 99 ans", "100 ans ou plus")));
				// Instantiate an aggregated attribute using previously referent attribute
				dd.addAttributes(attf.createRangeAggregatedAttribute("Age_2", new GSDataParser()
						.getRangeTemplate(mapperA1.keySet().stream().collect(Collectors.toList())),
						referentAgeAttribute, mapperA1));

				// Create another mapper
				Map<String, Collection<String>> mapperA2 = new LinkedHashMap<>();
				mapperA2.put("15 à 19 ans", new HashSet<>(Arrays.asList("15 à 19 ans")));
				mapperA2.put("20 à 24 ans", new HashSet<>(Arrays.asList("20 à 24 ans")));
				mapperA2.put("25 à 39 ans", new HashSet<>(Arrays.asList("25 à 29 ans", "30 à 34 ans", "35 à 39 ans")));
				mapperA2.put("40 à 54 ans", new HashSet<>(Arrays.asList("40 à 44 ans", "45 à 49 ans", "50 à 54 ans")));
				mapperA2.put("55 à 64 ans", new HashSet<>(Arrays.asList("55 à 59 ans", "60 à 64 ans")));
				mapperA2.put("65 ans ou plus", new HashSet<>(Arrays.asList("65 à 69 ans", "70 à 74 ans", "75 à 79 ans", 
								"80 à 84 ans", "85 à 89 ans", "90 à 94 ans", "95 à 99 ans", "100 ans ou plus")));
				
				dd.addAttributes(attf.createRangeAggregatedAttribute("Age_3", new GSDataParser()
						.getRangeTemplate(mapperA2.keySet().stream().collect(Collectors.toList())),
						referentAgeAttribute, mapperA2));		

				// --------------------------
				// Setup "COUPLE" attribute: INDIVIDUAL
				// --------------------------

				Attribute<? extends IValue> attCouple = attf.createAttribute("Couple", GSEnumDataType.Nominal, 
						Arrays.asList("Vivant en couple", "Ne vivant pas en couple")); 
				// WARNING: special empty value, not to get null empty value
				attCouple.getValueSpace().setEmptyValue("Ne vivant pas en couple");
				dd.addAttributes(attCouple);

				// -------------------------
				// Setup "SEXE" attribute: INDIVIDUAL
				// -------------------------

				Attribute<? extends IValue> attSexe = attf.createAttribute("Sexe", GSEnumDataType.Nominal,
						Arrays.asList("Hommes", "Femmes"));
				dd.addAttributes(attSexe);

				// -------------------------
				// Setup "CSP" attribute: INDIVIDUAL
				// -------------------------
				Attribute<? extends IValue> attCSP = attf.createAttribute("CSP", GSEnumDataType.Nominal, 
						Arrays.asList("Agriculteurs exploitants", "Artisans. commerçants. chefs d'entreprise", 
								"Cadres et professions intellectuelles supérieures", "Professions intermédiaires", 
								"Employés", "Ouvriers", "Retraités", "Autres personnes sans activité professionnelle")); 
				// WARNING: special empty value, not to get null empty value
				attCSP.getValueSpace().setEmptyValue("Autres personnes sans activité professionnelle");
				dd.addAttributes(attCSP);
				
				// -------------------------
				// Setup "IRIS" attribute: INDIVIDUAL
				// -------------------------
				Attribute<? extends IValue> attIris = attf.createAttribute("iris", GSEnumDataType.Nominal, 
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
				
				dd.addRecords(attf.createRecordAttribute("P13_POP", GSEnumDataType.Integer, attIris));

			} catch (GSIllegalRangedData e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// ------------------------------
			// SERIALIZE CONFIGURATION FILES
			// ------------------------------

			GenstarConfigurationFile gcf = new GenstarConfigurationFile();
			gcf.setBaseDirectory(baseDirectory);
			gcf.setSurveyWrappers(inputFiles);
			gcf.setDictionary(dd);
			
			try {
				new GenstarJsonUtil().marshalToGenstarJson(relativePath.resolve(CONF_EXPORT), gcf, false);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			GenstarConfigurationFile gcf = null;
			try {
				gcf = new GenstarJsonUtil().unmarshalFromGenstarJson(Paths.get(args[0].trim()), 
						GenstarConfigurationFile.class);
			} catch (IllegalArgumentException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Deserialize Genstar data configuration contains:\n"+
					gcf.getDictionary().getAttributes().size()+" attributs\n"+
					gcf.getSurveyWrappers().size()+" data files");
		}
		
	}

}
