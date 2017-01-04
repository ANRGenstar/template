package rouen.spll.configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

public class GSCRouen_localisation {

	public static String CONF_CLASS_PATH = "src/main/java/rouen/gospl/output/";
	public static String CONF_EXPORT = "GSC_Rouen_Localisation";
	
	public static void main(String[] args) {

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

		// Make file path absolute
		Path absolutePath = Paths.get(CONF_CLASS_PATH).toAbsolutePath();
		
		// What to define in this configuration file
		GSSurveyWrapper populationInput = new GSSurveyWrapper(absolutePath.resolve("PopExport.csv").toString(), 
					GSSurveyType.Sample, ',', 1, 1);
		populationInput.setRelativePath(Paths.get(CONF_CLASS_PATH).iterator().next().toAbsolutePath().getParent());
		Set<APopulationAttribute> inputAttributes = new HashSet<>();
		Map<String, IAttribute<? extends IValue>> inputKeyMap = new HashMap<>();
		
		try {
			// AGE
			inputAttributes.add(attf.createAttribute("Age", GSEnumDataType.Integer, 
							Arrays.asList("Moins de 5 ans", "5 à 9 ans", "10 à 14 ans", "15 à 19 ans", "20 à 24 ans", 
									"25 à 29 ans", "30 à 34 ans", "35 à 39 ans", "40 à 44 ans", "45 à 49 ans", 
									"50 à 54 ans", "55 à 59 ans", "60 à 64 ans", "65 à 69 ans", "70 à 74 ans", "75 à 79 ans", 
									"80 à 84 ans", "85 à 89 ans", "90 à 94 ans", "95 à 99 ans", "100 ans ou plus"), GSEnumAttributeType.range));
			//COUPLE
			inputAttributes.add(attf.createAttribute("Couple", GSEnumDataType.String, 
						Arrays.asList("Vivant en couple", "Ne vivant pas en couple"), 
						GSEnumAttributeType.unique));
			//IRIS
			inputAttributes.add(attf.createAttribute("IRIS", GSEnumDataType.String, 
					Arrays.asList("765400602", "765400104","765400306","765400201",
							"765400601","765400901","765400302","765400604","765400304",
							"765400305","765400801","765400301","765401004","765401003",
							"765400402","765400603","765400303","765400103","765400504",
							"765401006","765400702","765400401","765400202","765400802",
							"765400502","765400106","765400701","765401005","765400204",
							"765401001","765400405","765400501","765400102","765400503",
							"765400404","765400105","765401002","765400902","765400403",
							"765400203","765400101","765400205"), 
					GSEnumAttributeType.unique));
			//GENRE
			inputAttributes.add(attf.createAttribute("Sexe", GSEnumDataType.String,
						Arrays.asList("Hommes", "Femmes"), GSEnumAttributeType.unique));
			//CSP
			inputAttributes.add(attf.createAttribute("CSP", GSEnumDataType.String, 
						Arrays.asList("Agriculteurs exploitants", "Artisans. commerçants. chefs d'entreprise", 
								"Cadres et professions intellectuelles supérieures", "Professions intermédiaires", 
								"Employés", "Ouvriers", "Retraités", "Autres personnes sans activité professionnelle"), 
								GSEnumAttributeType.unique));
		} catch (GSIllegalRangedData e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			gxs.setMkdir(Paths.get(CONF_CLASS_PATH).toAbsolutePath());
			GenstarConfigurationFile gsdI = new GenstarConfigurationFile(Arrays.asList(populationInput), inputAttributes, inputKeyMap);
			gxs.serializeGSConfig(gsdI, CONF_EXPORT);
			System.out.println("Serialize Genstar input data with:\n"+
					gsdI.getAttributes().size()+" attributs\n"+
					gsdI.getSurveyWrapper().size()+" data files");
							
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
