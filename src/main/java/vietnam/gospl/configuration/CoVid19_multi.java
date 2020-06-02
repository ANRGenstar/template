package vietnam.gospl.configuration;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import core.configuration.GenstarConfigurationFile;
import core.configuration.GenstarJsonUtil;
import core.configuration.dictionary.IGenstarDictionary;
import core.metamodel.attribute.Attribute;
import core.metamodel.attribute.AttributeFactory;
import core.metamodel.io.GSSurveyType;
import core.metamodel.io.GSSurveyWrapper;
import core.metamodel.value.IValue;
import core.metamodel.value.numeric.IntegerValue;
import core.metamodel.value.numeric.RangeValue;
import core.util.data.GSEnumDataType;
import core.util.excpetion.GSIllegalRangedData;
import gospl.io.ipums.ReadIPUMSDictionaryUtils;

public class CoVid19_multi {

	public static final String CONF_CLASS_PATH_IPUMS = "src/main/java/vietnam/gospl/data";
	public static final String CONF_CLASS_PATH_CENSUS = "src/main/java/vietnam/gospl/data/cov";
	
	public static final String CONF_EXPORT = "vietnam_multi.gns";
	public static final String COMOKIT_CONF_EXPORT = "comokit_conf.gns";
	public static final String CONF_EXPORT_BT = "BT_demographics.gns";
	public static final String CONF_EXPORT_VP = "VP_demographics.gns";
	
	public static final String IPUMS_SAMPLE = "ipumsi_00003.csv";
	public static final String UPDATED_IPUMS_SAMPLE = "ipumsi_00004.csv";
	public static final String UPDATED_IPUMS_DICO = "ipumsi_00004.cbk";
	
	public static final String IPUMS_SAMPLE_SHORT_10K = "head10k_ipumsi_vietnam.csv";
	public static final String IPUMS_DICTIONARY = "ipums_hh_x_indiv_VND.txt";
	
	@SuppressWarnings({ "unused", "unchecked" })
	public static void main(String[] args) {
		
		// -----
		// IPUMS
		// -----
		
		Path baseDirectory = FileSystems.getDefault().getPath(".");
		Path relativePath = Paths.get(CONF_CLASS_PATH_IPUMS);

		// Setup the factory that build attribute
		AttributeFactory attf = AttributeFactory.getFactory();
		
		GSSurveyWrapper sample = new GSSurveyWrapper(relativePath.resolve(UPDATED_IPUMS_SAMPLE), false, GSSurveyType.Sample, ',', 1, 1);
		
		ReadIPUMSDictionaryUtils ipumsReader = new ReadIPUMSDictionaryUtils();
		
		// Dictionaries from ipums
		Set<IGenstarDictionary<Attribute<? extends IValue>>> dds = null;
		try {
			dds = ipumsReader.readDictionariesFromIPUMSDescription(relativePath.resolve(UPDATED_IPUMS_DICO).toFile());
		} catch (GSIllegalRangedData e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// Refactor a bit the individual attribute to match marginals
		IGenstarDictionary<Attribute<? extends IValue>> multi_indiv_dico = dds.stream().
				filter(dico -> dico.getLevel()==0).findFirst().get();
		
		Attribute<? extends IValue> age_multi = multi_indiv_dico.getAttribute("AGE");
		// Create a mapper
		Map<String, String> age_mapper = new LinkedHashMap<>(); 
		age_mapper.put("0", "Less than 1 year"); age_mapper.put("1", "1 year"); age_mapper.put("2", "2 years");
		
		age_mapper.putAll(IntStream.range(3, 100).mapToObj(age -> String.valueOf(age))
				.collect(Collectors.toMap(Function.identity(), Function.identity())));
		age_mapper.put("100", "100+");
		Attribute<IntegerValue> ageIntMulti = attf.createIntegerRecordAttribute(age_multi.getAttributeName()+"_int", age_multi, age_mapper);
		multi_indiv_dico.addAttributes(ageIntMulti);
		
		Attribute<? extends IValue> gender_multi = multi_indiv_dico.getAttribute("SEX");

		// ------
		// CENSUS
		// ------
		
		
		String Ben_Tre = "census2019_BT.csv";
		String localitiesBT = "Xã Thừa Đức";
		String Vinh_Phuc = "census2019_VP.csv";
		String localitiesVP = "Xã Sơn Lôi";
		String Me_Linh = "census2019_ML.csv";
		String localitiesML = "Xã Mê Linh";
		
		String[] caseStudy = new String[] {Vinh_Phuc,localitiesVP};
		
		// Setup input files' configuration for individual aggregated data
		GSSurveyWrapper census = new GSSurveyWrapper(Paths.get(CONF_CLASS_PATH_CENSUS).resolve(caseStudy[0]), 
				GSSurveyType.ContingencyTable, ',', 1, 6);
		try {
						
			Attribute<? extends IValue> limitedAttDistrict = attf.createAttribute("commune", GSEnumDataType.Nominal, 
					Arrays.asList(caseStudy[1])); // Limit the number of commune attribute to the case study
			multi_indiv_dico.addAttributes(limitedAttDistrict);
			
			List<String> ageVal =  IntStream.range(0, 80)
					.mapToObj(age -> "["+String.valueOf(age)+", "+String.valueOf(age+1)+")")
					.collect(Collectors.toList());
			ageVal.add("[80, Inf)");
			Attribute<RangeValue> ageAttribute = attf.createRangeAttribute("age", ageVal);
			
			// Create a mapper
			Map<String, Collection<String>> mapperWithAGE = new LinkedHashMap<>();
			mapperWithAGE.put("[0, 1)",Arrays.asList("Less than 1 year"));
			mapperWithAGE.put("[1, 2)",Arrays.asList("1 year"));
			mapperWithAGE.put("[2, 3)",Arrays.asList("2 years"));
			mapperWithAGE.putAll(
					IntStream.range(3, 80).mapToObj(age -> String.valueOf(age)).collect(Collectors.toMap(
							age -> "["+age+", "+String.valueOf(Integer.valueOf(age).intValue() + 1)+")", age -> Arrays.asList(age)))
					);
			mapperWithAGE.put("[80, Inf)", IntStream.range(80, 100).mapToObj(age -> String.valueOf(age)).collect(Collectors.toList()));
			mapperWithAGE.get("[80, Inf)").add("100+"); 
			
			Map<String, Collection<String>> mapperWithAGE_int = new LinkedHashMap<>();
			mapperWithAGE_int.putAll(
					IntStream.range(0, 80).mapToObj(age -> String.valueOf(age)).collect(Collectors.toMap(
							age -> "["+age+", "+String.valueOf(Integer.valueOf(age).intValue() + 1)+")", age -> Arrays.asList(age)))
					);
			mapperWithAGE_int.put("[80, Inf)", IntStream.range(80, 100).mapToObj(age -> String.valueOf(age)).collect(Collectors.toList()));
			
			// Instantiate an aggregated attribute using previously referent attribute
			Attribute<RangeValue> ageToIntAttribute = attf.createRangeToIntegerAggregateAttribute(
					ageAttribute.getAttributeName(), ageIntMulti, mapperWithAGE_int); 
			Attribute<RangeValue> ageToAGE = attf.createRangeAggregatedRecordAttribute(ageAttribute.getAttributeName(), age_multi, mapperWithAGE);
			multi_indiv_dico.addAttributes(ageToAGE);
			
			
			Map<String,String> genderMap = new HashMap<>();
			genderMap.put("male", "Male");
			genderMap.put("female", "Female");
			Attribute<? extends IValue> attSex = attf.createNominalRecordAttribute("sex", gender_multi, genderMap);
			multi_indiv_dico.addAttributes(attSex); 
			
			// Instantiate a record attribute: just count the number of occurrences
			multi_indiv_dico.addRecords(attf.createRecordAttribute("n", GSEnumDataType.Integer, attf.createAttribute("false", GSEnumDataType.Boolean)));	
			
		} catch (GSIllegalRangedData e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	


		GenstarConfigurationFile gcf = new GenstarConfigurationFile();
		gcf.setBaseDirectory(baseDirectory);
		gcf.addSurveyWrapper(sample, 0, 1);
		gcf.addSurveyWrapper(census, 0);
		gcf.setDictionaries(dds);
		try {
			new GenstarJsonUtil().marshalToGenstarJson(relativePath.resolve(COMOKIT_CONF_EXPORT), gcf, false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
