package rouen.spll.configuration;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import com.sun.tools.javac.util.List;

import core.configuration.GenstarConfigurationFile;
import core.configuration.GenstarJsonUtil;
import core.configuration.dictionary.DemographicDictionary;
import core.metamodel.attribute.demographic.DemographicAttribute;
import core.metamodel.attribute.demographic.DemographicAttributeFactory;
import core.metamodel.attribute.demographic.MappedDemographicAttribute;
import core.metamodel.io.GSSurveyType;
import core.metamodel.io.GSSurveyWrapper;
import core.metamodel.value.IValue;
import core.util.data.GSEnumDataType;
import core.util.excpetion.GSIllegalRangedData;

public class GSCRouen_localisation {

	public static String CONF_CLASS_PATH = "src/main/java/rouen/gospl/output/";
	public static String CONF_EXPORT = "rouen_demo_spll.gns";
	
	public static String SAMPLE = "SRNoSample_export.csv";
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {

		// Setup the factory that build attribute
		DemographicAttributeFactory attf = DemographicAttributeFactory.getFactory();

		// Make file path absolute
		Path relativePath = Paths.get(CONF_CLASS_PATH);
		
		// What to define in this configuration file
		GSSurveyWrapper populationInput = new GSSurveyWrapper(relativePath.resolve(SAMPLE), 
					GSSurveyType.Sample, ';', 1, 1);
		
		DemographicDictionary<DemographicAttribute<? extends IValue>> dd = new DemographicDictionary<>();
		DemographicDictionary<MappedDemographicAttribute<? extends IValue, ? extends IValue>> records = new DemographicDictionary<>();
		
		try {
			// Add attributes to the dictionnary
			dd.addAttributes(
					// AGE
					attf.createAttribute("Age", GSEnumDataType.Range, 
							Arrays.asList("Moins de 5 ans", "5 à 9 ans", "10 à 14 ans", "15 à 19 ans", "20 à 24 ans", 
									"25 à 29 ans", "30 à 34 ans", "35 à 39 ans", "40 à 44 ans", "45 à 49 ans", 
									"50 à 54 ans", "55 à 59 ans", "60 à 64 ans", "65 à 69 ans", "70 à 74 ans", "75 à 79 ans", 
									"80 à 84 ans", "85 à 89 ans", "90 à 94 ans", "95 à 99 ans", "100 ans ou plus")),
					// COUPLE
					attf.createAttribute("Couple", GSEnumDataType.Nominal, 
						Arrays.asList("Vivant en couple", "Ne vivant pas en couple")),
					//IRIS
					attf.createAttribute("iris", GSEnumDataType.Nominal, 
							Arrays.asList("765400602", "765400104","765400306","765400201",
							"765400601","765400901","765400302","765400604","765400304",
							"765400305","765400801","765400301","765401004","765401003",
							"765400402","765400603","765400303","765400103","765400504",
							"765401006","765400702","765400401","765400202","765400802",
							"765400502","765400106","765400701","765401005","765400204",
							"765401001","765400405","765400501","765400102","765400503",
							"765400404","765400105","765401002","765400902","765400403",
							"765400203","765400101","765400205")),
					//GENRE
					attf.createAttribute("Sexe", GSEnumDataType.Nominal,
						Arrays.asList("Hommes", "Femmes")),
					//CSP
					attf.createAttribute("CSP", GSEnumDataType.Nominal, 
						Arrays.asList("Agriculteurs exploitants", "Artisans. commerçants. chefs d'entreprise", 
								"Cadres et professions intellectuelles supérieures", "Professions intermédiaires", 
								"Employés", "Ouvriers", "Retraités", "Autres personnes sans activité professionnelle"))
			);
		} catch (GSIllegalRangedData e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		GenstarConfigurationFile gcf = new GenstarConfigurationFile();
		gcf.setBaseDirectory(FileSystems.getDefault().getPath("."));
		gcf.setSurveyWrappers(List.of(populationInput));
		gcf.setDemoDictionary(dd);
		gcf.setRecords(records);
		
		try {
			new GenstarJsonUtil().marshalToGenstarJson(relativePath.resolve(CONF_EXPORT), gcf);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
