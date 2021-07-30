package toulouse.gospl.configuration;

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
import java.util.function.Function;
import java.util.stream.Collectors;

import core.configuration.GenstarConfigurationFile;
import core.configuration.GenstarJsonUtil;
import core.configuration.dictionary.AttributeDictionary;
import core.metamodel.attribute.Attribute;
import core.metamodel.attribute.AttributeFactory;
import core.metamodel.io.GSSurveyType;
import core.metamodel.io.GSSurveyWrapper;
import core.metamodel.value.IValue;
import core.metamodel.value.categoric.NominalValue;
import core.metamodel.value.categoric.template.GSCategoricTemplate;
import core.metamodel.value.numeric.RangeValue;
import core.util.data.GSDataParser;
import core.util.data.GSEnumDataType;
import core.util.excpetion.GSIllegalRangedData;
import gospl.io.exception.InvalidSurveyFormatException;

public class GSCToulouse {
	
	public static String CONF_CLASS_PATH = "src/main/java/toulouse/gospl/data";
	public static String CONF_EXPORT = "toulouse_demographics.gns";

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
			inputFiles.add(new GSSurveyWrapper(relativePath.resolve("Sexe & CSP_Toulouse.csv"), 
					GSSurveyType.ContingencyTable, ';', 1, 1));
			inputFiles.add(new GSSurveyWrapper(relativePath.resolve("Age & Sexe_Toulouse.csv"), 
					GSSurveyType.ContingencyTable, ';', 1, 1));
			inputFiles.add(new GSSurveyWrapper(relativePath.resolve("iris_POP_toulouse2017.csv"), 
					GSSurveyType.ContingencyTable, ';', 1, 1));

			try {

				// --------------------------
				// Setup "AGE" attribute: INDIVIDUAL
				// --------------------------

				// Instantiate a referent attribute
				Attribute<RangeValue> referentAgeAttribute = attf.createRangeAttribute("Age", 
						Arrays.asList("Moins de 5 ans", "5 à 9 ans", "10 à 14 ans", "15 à 19 ans", "20 à 24 ans", 
						"25 à 29 ans", "30 à 34 ans", "35 à 39 ans", "40 à 44 ans", "45 à 49 ans", 
						"50 à 54 ans", "55 à 59 ans", "60 à 64 ans", "65 à 69 ans", "70 à 74 ans", "75 à 79 ans", 
						"80 à 84 ans", "85 à 89 ans", "90 à 94 ans", "95 à 99 ans", "100 ans ou plus"));
				dd.addAttributes(referentAgeAttribute);		
				
				/*// Create a mapper
				Map<String, Collection<String>> mapperA1 = new LinkedHashMap<>(); 
				mapperA1.put("0 à 14 ans", new HashSet<>(Arrays.asList("Moins de 5 ans", "5 à 9 ans", "10 à 14 ans")));
				mapperA1.put("15 à 29 ans", new HashSet<>(Arrays.asList("15 à 19 ans, 20 à 24 ans", "25 à 29 ans")));
				mapperA1.put("30 à 44 ans", new HashSet<>(Arrays.asList("30 à 34 ans", "35 à 39 ans", "40 à 44 ans")));
				mapperA1.put("45 à 59 ans", new HashSet<>(Arrays.asList("45 à 49 ans", "50 à 54 ans", "55 à 59 ans")));
				mapperA1.put("60 à 74 ans", new HashSet<>(Arrays.asList("60 à 64 ans", "65 à 69 ans", "70 à 74 ans")));
				mapperA1.put("75 à 90 ans", new HashSet<>(Arrays.asList("75 à 79 ans", "80 à 84 ans", "85 à 89 ans")));
				mapperA1.put("90 ans ou plus", new HashSet<>(Arrays.asList("90 à 94 ans", "95 à 99 ans", "100 ans ou plus")));
				
				// Instantiate an aggregated attribute using previously referent attribute
				dd.addAttributes(attf.createRangeAggregatedAttribute("Age_2", new GSDataParser()
						.getRangeTemplate(mapperA1.keySet().stream().collect(Collectors.toList())),
						referentAgeAttribute, mapperA1));*/
				
				/*// --------------------------
				// Setup "COUPLE" attribute: INDIVIDUAL
				// --------------------------

				Attribute<? extends IValue> attCouple = attf.createAttribute("Couple", GSEnumDataType.Nominal, 
						Arrays.asList("Vivant en couple", "Ne vivant pas en couple")); 
				// WARNING: special empty value, not to get null empty value
				attCouple.getValueSpace().setEmptyValue("Ne vivant pas en couple");
				dd.addAttributes(attCouple);*/

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
						Arrays.asList("Agriculteurs exploitants", "Artisans ou commerçants ou chefs d'entreprise", 
								"Cadres et professions intellectuelles supérieures", "Professions intermédiaires", 
								"Employés", "Ouvriers", "Retraités", "Autres personnes sans activité professionnelle")); 
				// WARNING: special empty value, not to get null empty value
				attCSP.getValueSpace().setEmptyValue("Autres personnes sans activité professionnelle");
				dd.addAttributes(attCSP);
				
				// -------------------------
				// Setup "IRIS" attribute: INDIVIDUAL
				// -------------------------
				Attribute<? extends IValue> attIris = attf.createAttribute("IRIS", GSEnumDataType.Nominal, 
						Arrays.asList("315550101", "315550102", "315550201", "315550202", "315550203", "315550301", "315550302", "315550401", "315550402", "315550501",
								"315550502", "315550601", "315550602", "315550701", "315550702", "315550801", "315550802", "315550901", "315550902", "315551001",
								"315551002", "315551101", "315551102", "315551103", "315551201", "315551202", "315551203", "315551301", "315551302", "315551303",
								"315551401", "315551501", "315551502", "315551601", "315551602", "315551603", "315551701", "315551702", "315551801", "315551802",
								"315551803", "315551804", "315551805", "315551806", "315551807", "315551901", "315551902", "315552001", "315552002", "315552003",
								"315552004", "315552005", "315552101", "315552102", "315552103", "315552104", "315552201", "315552202", "315552203", "315552204",
								"315552301", "315552302", "315552303", "315552304", "315552401", "315552402", "315552403", "315552404", "315552501", "315552502",
								"315552601", "315552701", "315552702", "315552801", "315552802", "315552803", "315552901", "315552902", "315552903", "315553001",
								"315553002", "315553101", "315553201", "315553202", "315553203", "315553301", "315553401", "315553402", "315553501", "315553502",
								"315553503", "315553504", "315553601", "315553701", "315553702", "315553703", "315553802", "315553803", "315553803", "315553901",
								"315554001", "315554003", "315554004", "315554005", "315554006", "315554007", "315554101", "315554201", "315554202", "315554203",
								"315554301", "315554302", "315554303", "315554401", "315554402", "315554501", "315554502", "315554503", "315554601", "315554602",
								"315554603", "315554701", "315554702", "315554703", "315554801", "315554802", "315554803", "315554804", "315554805", "315554901",
								"315555001", "315555101", "315555102", "315555201", "315555202", "315555203", "315555204", "315555301", "315555302", "315555303",
								"315555401", "315555402", "315555502", "315555503", "315555601", "315555701", "315555702", "315555801", "315555802", "315555901",
								"315555902", "315556001", "315556002"));
				dd.addAttributes(attIris);
				
				dd.addRecords(attf.createRecordAttribute("P17_POP_RNDD", GSEnumDataType.Integer, attIris));

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
