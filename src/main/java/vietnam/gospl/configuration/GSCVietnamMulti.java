package vietnam.gospl.configuration;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

import core.configuration.GenstarConfigurationFile;
import core.configuration.GenstarJsonUtil;
import core.configuration.dictionary.IGenstarDictionary;
import core.metamodel.attribute.Attribute;
import core.metamodel.io.GSSurveyType;
import core.metamodel.io.GSSurveyWrapper;
import core.metamodel.value.IValue;
import core.util.excpetion.GSIllegalRangedData;
import gospl.io.ipums.ReadIPUMSDictionaryUtils;

public class GSCVietnamMulti {

	public static final String CONF_CLASS_PATH = "src/main/java/vietnam/gospl/data";
	public static final String CONF_EXPORT = "vietnam_multi.gns";
	
	public static final String IPUMS_SAMPLE = "ipumsi_vietnam_10002.csv";
	public static final String IPUMS_DICTIONARY = "ipums_hh_x_indiv_VND.txt";

	public static void main(String[] args) {

		Path baseDirectory = FileSystems.getDefault().getPath(".");
		Path relativePath = Paths.get(CONF_CLASS_PATH);
		
		GSSurveyWrapper sample = new GSSurveyWrapper(relativePath.resolve(IPUMS_SAMPLE), GSSurveyType.Sample, ',', 1, 1);
		
		IGenstarDictionary<Attribute<? extends IValue>> dd = null;
		
		ReadIPUMSDictionaryUtils ipumsReader = new ReadIPUMSDictionaryUtils();
		try {
			dd = ipumsReader.readDictionaryFromRTF(relativePath.resolve(IPUMS_DICTIONARY).toFile());
		} catch (GSIllegalRangedData e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		GenstarConfigurationFile gcf = new GenstarConfigurationFile();
		gcf.setBaseDirectory(baseDirectory);
		//gcf.addSurveyWrapper(sample, gcf.getLayers().toArray(new Integer[gcf.getLevels()]));
		gcf.addSurveyWrapper(sample, 0, 1);
		gcf.setDictionary(dd);

		try {
			new GenstarJsonUtil().marshalToGenstarJson(relativePath.resolve(CONF_EXPORT), gcf, false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
