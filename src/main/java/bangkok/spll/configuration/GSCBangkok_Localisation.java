package bangkok.spll.configuration;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
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
import core.util.data.GSEnumDataType;
import core.util.excpetion.GSIllegalRangedData;

public class GSCBangkok_Localisation {

	public static String CONF_CLASS_PATH = "src/main/java/bangkok/gospl/output/";
	public static String CONF_EXPORT = "bangkok_demo_spll.gns";
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {

		// Setup the factory that build attribute
		DemographicAttributeFactory attf = DemographicAttributeFactory.getFactory();

		Path relativePath = Paths.get(CONF_CLASS_PATH).toAbsolutePath();
	
		// What to define in this configuration file
		GSSurveyWrapper populationInput = new GSSurveyWrapper(Paths.get(CONF_CLASS_PATH).resolve("PopExport.csv"), 
					GSSurveyType.Sample, ';', 1, 1);
		
		DemographicDictionary<DemographicAttribute<? extends IValue>> dd = new DemographicDictionary<>();
		DemographicDictionary<MappedDemographicAttribute<? extends IValue, ? extends IValue>> records = new DemographicDictionary<>();
		
		/*
		Set<DemographicAttribute<? extends IValue>> inputAttributes = new HashSet<>();
		Map<String, IAttribute<? extends IValue>> inputKeyMap = new HashMap<>();
		*/
		
		try {
			// -------------------------
			// Setup "PAT" attribute: INDIVIDUAL & MENAGE
			// -------------------------

			DemographicAttribute<NominalValue> khwaeng = attf.createNominalAttribute("PAT", new GSCategoricTemplate()); 
			Stream.of("100101", "100102", "100103", "100104", "100105", "100106", "100107", "100108", "100109", "100110", "100111", "100112",
					"100201", "100202", "100203", "100204", "100206", "100301", "100302", "100303", "100304", "100305", "100306", "100307", 
					"100308", "100401", "100402", "100403", "100404", "100405", "100502", "100508", "100601", "100608", "100701", "100702",
					"100703", "100704", "100801", "100802", "100803", "100804", "100805", "100905", "101001", "101002", "101101", "101102",
					"101103", "101104", "101105", "101106", "101203", "101204", "101301", "101302", "101303", "101401", "101501", "101502",
					"101503", "101504", "101505", "101506", "101507", "101601", "101602", "101701", "101702", "101704", "101801", "101802",
					"101803", "101804", "101901", "101902", "101903", "101904", "101905", "101907", "102004", "102005", "102006", "102007",
					"102009", "102105", "102107", "102201", "102202", "102206", "102207", "102208", "102209", "102210", "102302", "102303",
					"102401", "102402", "102501", "102502", "102503", "102504", "102601", "102701", "102801", "102802", "102803", "102901",
					"103001", "103002", "103003", "103004", "103005", "103101", "103102", "103103", "103201", "103202", "103203", "103301",
					"103302", "103303", "103401", "103501", "103502", "103503", "103504", "103602", "103701", "103702", "103703", "103704",
					"103801", "103802", "103901", "103902", "103903", "104001", "104002", "104003", "104004", "104101", "104102", "104201",
					"104202", "104203", "104301", "104401", "104501", "104601", "104602", "104603", "104604", "104605", "104701", "104801",
					"104802", "104901", "104902", "105001").forEach(value -> khwaeng.getValueSpace().addValue(value));
			dd.addAttributes(khwaeng);

			// -------------------------
			// Setup "PA" attribute: INDIVIDUAL & MENAGE
			// -------------------------

			dd.addAttributes(attf.createNominalAggregatedAttribute("PA", new GSCategoricTemplate(), khwaeng, 
					khwaeng.getValueSpace().getValues().stream().map(IValue::getStringValue)
						.collect(Collectors.groupingBy(value -> value.substring(0, 3), 
							Collectors.mapping(Function.identity(), Collectors.toCollection(HashSet::new))))));

			// --------------------------
			// Setupe "Count" attribute: INDIVIDUAL
			// --------------------------

			// Instantiate a record attribute: just count the number of occurrences
			dd.addAttributes(attf.createIntegerRecordAttribute("POP", khwaeng));		

			// --------------------------
			// Setup "EDU" attribute: INDIVIDUAL
			// --------------------------

			//				indivAttributes.add(attf.createAttribute("education", GSEnumDataType.String, 
			//						Arrays.asList("GC00", "GC01", "GC02", "GC03", "GC04", "GC05", "GC06"), 
			//						GSEnumAttributeType.unique));

			// -------------------------
			// Setup "WRK" attribute: INDIVIDUAL
			// -------------------------

			dd.addAttributes(attf.createAttribute("occupation", GSEnumDataType.Nominal,
					Arrays.asList("TOCC1", "TOCC2", "TOCC3", "TOCC4", "TOCC5", "TOCC6", "TOCC7",
							"TOCC8", "TOCC9", "TTOCC")));

			// -------------------------
			// Setup "AGE" attribute: INDIVIDUAL
			// -------------------------

			dd.addAttributes(attf.createAttribute("tranche age", GSEnumDataType.Range, 
					Arrays.asList("0-4", "5-9", "10-14", "15-19", "20-24", "25-29", "30-34", "35-39",
							"40-44", "45-49", "50-54", "55-59", "60-64", "65-69", "70-74", "75-79",
							"80-84", "85-89", "90-94", "95-99", "100+")));

			// --------------------------
			// Setup "SEXE" attribute: INDIVIDUAL
			// --------------------------

			dd.addAttributes(attf.createAttribute("gender", GSEnumDataType.Nominal, Arrays.asList("Male", "Female")));


		} catch (GSIllegalRangedData e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		GenstarConfigurationFile gcf = new GenstarConfigurationFile();
		gcf.setBaseDirectory(FileSystems.getDefault().getPath("."));
		gcf.setSurveyWrappers(Arrays.asList(populationInput));
		gcf.setDemoDictionary(dd);
		gcf.setRecords(records);
		
		try {
			new GenstarJsonUtil().marshalToGenstarJson(relativePath.resolve(CONF_EXPORT), gcf, false);
							
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
