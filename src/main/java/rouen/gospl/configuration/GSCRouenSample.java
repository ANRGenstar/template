package rouen.gospl.configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import core.configuration.GenstarConfigurationFile;
import core.configuration.GenstarJsonUtil;
import core.metamodel.attribute.AttributeFactory;
import core.metamodel.io.GSSurveyType;
import core.metamodel.io.GSSurveyWrapper;

public class GSCRouenSample {

	public static String CONF_CLASS_PATH = "src/main/java/rouen/gospl/data/";
	public static String CONF_EXPORT = "rouen_demographics_with_sample.gns";

	public static String WITHOUT_SAMPLE_CONF_INPUT = "rouen_demographics.gns";

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// Setup the factory that build attribute
		AttributeFactory attf = AttributeFactory.getFactory();



		// Make file path absolute
		Path relativePath = Paths.get(CONF_CLASS_PATH);

		// Recall configuration from GSCRouen
		GenstarConfigurationFile gcf = null;
		try {
			gcf = new GenstarJsonUtil().unmarshalFromGenstarJson(
					relativePath.resolve(WITHOUT_SAMPLE_CONF_INPUT).toAbsolutePath(), 
					GenstarConfigurationFile.class);
		} catch (IllegalArgumentException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// What to define in this configuration file
		gcf.addSurveyWrapper(new GSSurveyWrapper(relativePath.resolve("sampleRouen.csv"), 
				GSSurveyType.Sample, ',', 1, 1));

		// --------------------------
		// Setupe "AGE" attribute: INDIVIDUAL
		// --------------------------

		// Instantiate a referent attribute	

		Map<Collection<String>, Collection<String>> theMap = new LinkedHashMap<>(); 
		theMap.put(Arrays.asList("000"), Arrays.asList("Moins de 5 ans"));
		theMap.put(Arrays.asList("005"), Arrays.asList("5 à 9 ans"));
		theMap.put(Arrays.asList("010"), Arrays.asList("10 à 14 ans"));
		theMap.put(Arrays.asList("015"), Arrays.asList("15 à 19 ans"));
		theMap.put(Arrays.asList("020"), Arrays.asList("20 à 24 ans"));
		theMap.put(Arrays.asList("025"), Arrays.asList("25 à 29 ans"));
		theMap.put(Arrays.asList("030"), Arrays.asList("30 à 34 ans"));
		theMap.put(Arrays.asList("035"), Arrays.asList("35 à 39 ans"));
		theMap.put(Arrays.asList("040"), Arrays.asList("40 à 44 ans"));
		theMap.put(Arrays.asList("045"), Arrays.asList("45 à 49 ans"));
		theMap.put(Arrays.asList("050"), Arrays.asList("50 à 54 ans"));
		theMap.put(Arrays.asList("055"), Arrays.asList("55 à 59 ans"));
		theMap.put(Arrays.asList("060"), Arrays.asList("60 à 64 ans"));
		theMap.put(Arrays.asList("065"), Arrays.asList("65 à 69 ans"));
		theMap.put(Arrays.asList("070"), Arrays.asList("70 à 74 ans"));
		theMap.put(Arrays.asList("075"), Arrays.asList("75 à 79 ans"));
		theMap.put(Arrays.asList("080"), Arrays.asList("80 à 84 ans"));
		theMap.put(Arrays.asList("085"), Arrays.asList("85 à 89 ans"));
		theMap.put(Arrays.asList("090"), Arrays.asList("90 à 94 ans"));
		theMap.put(Arrays.asList("095"), Arrays.asList("95 à 99 ans"));
		theMap.put(Arrays.asList("100", "105", "110", "115", "120"), Arrays.asList("100 ans ou plus"));
		gcf.getDictionary().addAttributes(attf.createIntegerAttribute("agerevq", 
				gcf.getDictionary().getAttribute("Age"), theMap));

		// --------------------------
		// Setup "COUPLE" attribute: INDIVIDUAL
		// -------------------------- 

		Map<String, String> theRecord = new HashMap<>();
		theRecord.put("1", "Vivant en couple");
		theRecord.put("2", "Ne vivant pas en couple");
		gcf.getDictionary().addAttributes(attf.createNominalRecordAttribute("couple", 
				gcf.getDictionary().getAttribute("Couple"), theRecord));

		// -------------------------
		// Setup "SEXE" attribute: INDIVIDUAL
		// -------------------------

		Map<String, String> theRecord2 = new HashMap<>();
		theRecord2.put("1", "Hommes");
		theRecord2.put("2", "Femmes");
		gcf.getDictionary().addAttributes(attf.createNominalRecordAttribute("Gender", 
				gcf.getDictionary().getAttribute("Sexe"), theRecord2));

		// -------------------------
		// Setup "CSP" attribute: INDIVIDUAL
		// -------------------------

		List<String> csp = Arrays.asList("Agriculteurs exploitants", "Artisans. commerçants. chefs d'entreprise", 
				"Cadres et professions intellectuelles supérieures", "Professions intermédiaires", 
				"Employés", "Ouvriers", "Retraités", "Autres personnes sans activité professionnelle");

		gcf.getDictionary().addAttributes(attf.createNominalRecordAttribute("cs1", 
				gcf.getDictionary().getAttribute("CSP"), 
				csp.stream().collect(Collectors.toMap(
						p -> Integer.toString(csp.indexOf(p)+1), 
						Function.identity()))));

		// ------------------------------
		// SERIALIZE CONFIGURATION FILES
		// ------------------------------

		try {
			new GenstarJsonUtil().marshalToGenstarJson(relativePath.resolve(CONF_EXPORT), gcf, false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

}
