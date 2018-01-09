package rouen.spll;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.geotools.feature.SchemaException;

import core.metamodel.IPopulation;
import core.metamodel.attribute.demographic.DemographicAttribute;
import core.metamodel.entity.ADemoEntity;
import core.metamodel.value.IValue;
import core.util.GSPerformanceUtil;
import gospl.distribution.GosplInputDataManager;
import gospl.io.exception.InvalidSurveyFormatException;
import spll.SpllPopulation;
import spll.io.SPLGeofileBuilder;
import spll.io.SPLVectorFile;
import spll.io.exception.InvalidGeoFormatException;
import spll.popmapper.SPLocalizer;

public class LocalisationRouenIJGIS_1 {

	//path to the main census shapefile - the entities are generated at this level
	static String stringPathToCensusShapefile = "src/main/java/rouen/spll/data/shp/Rouen_iris.shp";

	
	//name of the property that contains the id of the census spatial areas in the shapefile
	static String stringOfCensusIdInShapefile = "CODE_IRIS";

	static String stringPathToGenstarConfiguration = "src/main/java/rouen/gospl/output/rouen_demo_spll.gns";
	//name of the property that contains the id of the census spatial areas in the csv file (and population)
	static String stringOfCensusIdInCSVfile = "iris";
	//name of the property that define the number of entities per census spatial areas.
	static String stringOfNumberProperty = "P13_POP";

	static String stringPathToOutputFile = "src/main/java/rouen/spll/output/RouenSpll_IJGIS1.shp";

	
	public static void main(String[] args) {

		GSPerformanceUtil gspu = new GSPerformanceUtil("Localisation de la population de Rouen");
		
		// INPUT POPULATION
		GosplInputDataManager gdb = null;
		try {
			gdb = new GosplInputDataManager(Paths.get(stringPathToGenstarConfiguration));
		} catch (IllegalArgumentException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		// READ SAMPLE DATA (I.E. THE POPULATION TO LOCALIZE)
		try {
			gdb.buildSamples();
		} catch (final RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final InvalidSurveyFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		IPopulation<ADemoEntity, DemographicAttribute<? extends IValue>> population = gdb.getRawSamples().iterator().next();
		
		gspu.sysoStempPerformance("Population ("+population.size()+") have been retrieve from data", 
				LocalisationRouenIJGIS_1.class.getSimpleName());

		SPLVectorFile sfAdmin = null;
		
		try {
			sfAdmin = SPLGeofileBuilder.getShapeFile(new File(stringPathToCensusShapefile), null);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidGeoFormatException e) {
			e.printStackTrace();
		}
		gspu.sysoStempPerformance("Import main shapefiles", LocalisationRouenIJGIS_1.class.getSimpleName());
		
		// SETUP THE LOCALIZER
		SPLocalizer localizer = new SPLocalizer(population, sfAdmin);
		
		// SETUP GEOGRAPHICAL MATCHER
		// use of the IRIS attribute of the population
		localizer.setMatcher(sfAdmin, stringOfCensusIdInCSVfile, stringOfCensusIdInShapefile);
				
				
		//localize the population
		SpllPopulation localizedPop = localizer.localisePopulation();

		gspu.sysoStempPerformance("Population ("+population.size()+") have been localized", 
				LocalisationRouenIJGIS_1.class.getSimpleName());

		
		try {
			new SPLGeofileBuilder().setFile(new File(stringPathToOutputFile)).setPopulation(localizedPop).buildShapeFile();
		} catch (IOException | SchemaException | InvalidGeoFormatException e) {
			e.printStackTrace();
		}
		gspu.sysoStempPerformance("Population ("+population.size()+") have been saved", 
				LocalisationRouenIJGIS_1.class.getSimpleName());

	}

}
