package rouen.spll;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.geotools.feature.SchemaException;
import org.opengis.referencing.operation.TransformException;

import core.metamodel.IPopulation;
import core.metamodel.attribute.Attribute;
import core.metamodel.entity.ADemoEntity;
import core.metamodel.entity.AGeoEntity;
import core.metamodel.io.IGSGeofile;
import core.metamodel.value.IValue;
import core.util.GSPerformanceUtil;
import core.util.excpetion.GSIllegalRangedData;
import gospl.distribution.GosplInputDataManager;
import gospl.io.exception.InvalidSurveyFormatException;
import spll.SpllPopulation;
import spll.algo.LMRegressionOLS;
import spll.algo.exception.IllegalRegressionException;
import spll.datamapper.exception.GSMapperException;
import spll.datamapper.normalizer.SPLUniformNormalizer;
import spll.entity.GeoEntityFactory;
import spll.io.SPLGeofileBuilder;
import spll.io.SPLRasterFile;
import spll.io.SPLVectorFile;
import spll.io.exception.InvalidGeoFormatException;
import spll.localizer.SPLocalizer;
import spll.localizer.constraint.SpatialConstraintMaxNumber;

public class LocalisationRouenBuildings {

	//path to the main census shapefile - the entities are generated at this level
	static String stringPathToCensusShapefile = "src/main/java/rouen/spll/data/shp/Rouen_iris.shp";

	//path to the shapefile that define the geographical objects on which the entities should be located
	static String stringPathToNestShapefile = "src/main/java/rouen/spll/data/shp/buildings.shp";

	//path to the file that will be used as support for the spatial regression (bring additional spatial data)
	static String stringPathToLandUseGrid = "src/main/java/rouen/spll/data/raster/CLC12_D076_RGF_S.tif";

	static String stringPathToPopulationShapefile = "src/main/java/rouen/spll/output/spllOutput.shp";
	
	//name of the property that contains the id of the census spatial areas in the shapefile
	static String stringOfCensusIdInShapefile = "CODE_IRIS";

	static String stringPathToGenstarConfiguration = "src/main/java/rouen/gospl/output/rouen_demo_spll.gns";
	//name of the property that contains the id of the census spatial areas in the csv file (and population)
	static String stringOfCensusIdInCSVfile = "iris";
	//name of the property that define the number of entities per census spatial areas.
	static String stringOfNumberProperty = "P13_POP";

	//name of the property that will by generated by the regression and that specifies the number of entities per regression areas
	static String stringOfNumberAttribute = GeoEntityFactory.ATTRIBUTE_PIXEL_BAND+0;
	
	static String stringPathToOutputFile = "src/main/java/rouen/spll/output/RouenSpll.shp";

	
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
		
		IPopulation<ADemoEntity, Attribute<? extends IValue>> population = gdb.getRawSamples().iterator().next();
		
		gspu.sysoStempPerformance("Population ("+population.size()+") have been retrieve from data", 
				LocalisationRouenBuildings.class.getSimpleName());

		SPLVectorFile sfAdmin = null;
		SPLVectorFile sfBuildings = null;
		
		try {
			//building shapefile
			
			sfBuildings = SPLGeofileBuilder.getShapeFile(new File(stringPathToNestShapefile), Arrays.asList("name", "type"), null);
			//Iris shapefile
			sfAdmin = SPLGeofileBuilder.getShapeFile(new File(stringPathToCensusShapefile), null);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidGeoFormatException e) {
			e.printStackTrace();
		} catch (GSIllegalRangedData e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		gspu.sysoStempPerformance("Import main shapefiles", LocalisationRouenBuildings.class.getSimpleName());

		//import the land-use file
		Collection<String> stringPathToAncilaryGeofiles = new ArrayList<>();
		stringPathToAncilaryGeofiles.add(stringPathToLandUseGrid);

		List<IGSGeofile<? extends AGeoEntity<? extends IValue>, ? extends IValue>> endogeneousVarFile = new ArrayList<>();
		for(String path : stringPathToAncilaryGeofiles){
			try {
				endogeneousVarFile.add(new SPLGeofileBuilder().setFile(new File(path)).buildGeofile());
			} catch (IllegalArgumentException | TransformException | IOException | InvalidGeoFormatException | GSIllegalRangedData e2) {
				e2.printStackTrace();
			}
		}
		
		gspu.sysoStempPerformance("GIS data have been import to process population localization", 
				LocalisationRouenBuildings.class.getSimpleName());
		
		// SETUP THE LOCALIZER
		SPLocalizer localizer = new SPLocalizer(population, sfBuildings);
		
		// SETUP GEOGRAPHICAL MATCHER
		// use of the IRIS attribute of the population
		localizer.setMatcher(sfAdmin, stringOfCensusIdInCSVfile, stringOfCensusIdInShapefile);
		localizer.getLocalizationConstraint().setIncreaseStep(100.0);
		localizer.getLocalizationConstraint().setMaxIncrease(100.0); 
		
		SpatialConstraintMaxNumber numberConstr = new SpatialConstraintMaxNumber(sfBuildings.getGeoEntity(), 1.0);
		numberConstr.setPriority(10);
		numberConstr.setIncreaseStep(2);
		numberConstr.setMaxIncrease(60);
		localizer.addConstraint(numberConstr);
		
		// SETUP REGRESSION
		try {
			localizer.setMapper(endogeneousVarFile, new ArrayList<>(), 
					new LMRegressionOLS(), new SPLUniformNormalizer(0, SPLRasterFile.DEF_NODATA));
		} catch (IndexOutOfBoundsException | IOException | TransformException | InterruptedException
				| ExecutionException | IllegalRegressionException | GSMapperException | SchemaException 
				| IllegalArgumentException | InvalidGeoFormatException e) {
			e.printStackTrace();
		}
		
		//localize the population
		SpllPopulation localizedPop = localizer.localisePopulation();
		
		try {
			new SPLGeofileBuilder().setFile(new File(stringPathToOutputFile)).setPopulation(localizedPop).buildShapeFile();
		} catch (IOException | SchemaException | InvalidGeoFormatException e) {
			e.printStackTrace();
		}
	}

}
