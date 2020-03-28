package vietnam.gospl.configuration;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import core.configuration.GenstarConfigurationFile;
import core.configuration.GenstarJsonUtil;
import core.configuration.dictionary.AttributeDictionary;
import core.metamodel.attribute.Attribute;
import core.metamodel.attribute.AttributeFactory;
import core.metamodel.io.GSSurveyType;
import core.metamodel.io.GSSurveyWrapper;
import core.metamodel.value.IValue;
import core.metamodel.value.numeric.RangeValue;
import core.util.data.GSEnumDataType;
import core.util.excpetion.GSIllegalRangedData;

public class CoVid19_conf {
	
	public static String CONF_CLASS_PATH = "src/main/java/vietnam/gospl/data/cov";
	public static String CONF_EXPORT_BT = "BT_demographics.gns";
	public static String CONF_EXPORT_VP = "VP_demographics.gns";

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {

		// Setup the factory that build attribute
		AttributeFactory attf = AttributeFactory.getFactory();

		// What to define in this configuration file
		List<GSSurveyWrapper> inputFiles = new ArrayList<>();
		
		AttributeDictionary dd = new AttributeDictionary();

		Path relativePath = Paths.get(CONF_CLASS_PATH);
		
		// Setup input files' configuration for individual aggregated data
		inputFiles.add(new GSSurveyWrapper(relativePath.resolve("census2019_BT.csv"), 
				GSSurveyType.ContingencyTable, ',', 1, 6));
		
		try {
			
			Attribute<? extends IValue> attDistrict = attf.createAttribute("commune", GSEnumDataType.Nominal, 
					Arrays.asList("Xã Bình Thắng","Xã Bình Thới","Xã Châu Hưng","Xã Đại Hòa Lộc",
							"Xã Định Trung","Xã Lộc Thuận","Xã Long Định","Xã Long Hòa","Xã Phú Long",
							"Xã Phú Thuận","Xã Phú Vang","Thị trấn Bình Đại","Xã Tam Hiệp","Xã Thạnh Phước",
							"Xã Thạnh Trị","Xã Thới Lai","Xã Thới Thuận","Xã Thừa Đức","Xã Vang Quới Đông",
							"Xã Vang Quới Tây"));
			dd.addAttributes(attDistrict);
			
			Attribute<RangeValue> ageAttribute = attf.createRangeAttribute("age", 
					Arrays.asList("[0, 1)","[1, 2)","[2, 3)","[3, 4)","[4, 5)","[5, 6)","[6, 7)","[7, 8)","[8, 9)",
							"[9, 10)","[10, 11)","[11, 12)","[12, 13)","[13, 14)","[14, 15)","[15, 16)","[16, 17)",
							"[17, 18)","[18, 19)","[19, 20)","[20, 21)","[21, 22)","[22, 23)","[23, 24)","[23, 24)",
							"[24, 25)","[25, 26)","[26, 27)","[27, 28)","[28, 29)","[29, 30)","[30, 31)",
							"[31, 32)","[32, 33)","[33, 34)","[34, 35)","[35, 36)","[36, 37)","[37, 38)",
							"[38, 39)","[39, 40)","[40, 41)","[41, 42)","[42, 43)","[43, 44)","[44, 45)",
							"[45, 46)","[46, 47)","[47, 48)","[48, 49)","[49, 50)","[50, 51)","[51, 52)",
							"[52, 53)","[53, 54)","[54, 55)","[55, 56)","[56, 57)","[57, 58)","[58, 59)",
							"[59, 60)","[60, 61)","[61, 62)","[62, 63)","[63, 64)","[64, 65)","[65, 66)",
							"[66, 67)","[67, 68)","[68, 69)","[69, 70)","[70, 71)","[71, 72)","[72, 73)",
							"[73, 74)","[74, 75)","[75, 76)","[76, 77)","[77, 78)","[78, 79)","[79, 80)",
							"[80, Inf)"));
			dd.addAttributes(ageAttribute);
			
			Attribute<? extends IValue> attSex = attf.createAttribute("sex", GSEnumDataType.Nominal,
					Arrays.asList("male", "female"));
			dd.addAttributes(attSex	); 
			
			// Instantiate a record attribute: just count the number of occurrences
			dd.addRecords(attf.createRecordAttribute("n", GSEnumDataType.Integer, attf.createAttribute("false", GSEnumDataType.Boolean)));	
			
			GenstarConfigurationFile gcf = new GenstarConfigurationFile();
			gcf.setBaseDirectory(FileSystems.getDefault().getPath("."));
			gcf.setSurveyWrappers(inputFiles);
			gcf.setDictionary(dd);
			
			try {
				new GenstarJsonUtil().marshalToGenstarJson(relativePath.resolve(CONF_EXPORT_BT), gcf, false);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (GSIllegalRangedData e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
	}

}
