package rouen.spll;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.geotools.feature.SchemaException;
import org.opengis.referencing.operation.TransformException;

import core.metamodel.IPopulation;
import core.metamodel.attribute.demographic.DemographicAttribute;
import core.metamodel.entity.ADemoEntity;
import core.metamodel.entity.AGeoEntity;
import core.metamodel.io.IGSGeofile;
import core.metamodel.value.IValue;
import core.util.GSPerformanceUtil;
import gospl.distribution.GosplInputDataManager;
import gospl.io.exception.InvalidSurveyFormatException;
import spll.SpllPopulation;
import spll.entity.GeoEntityFactory;
import spll.io.SPLGeofileBuilder;
import spll.io.SPLVectorFile;
import spll.io.exception.InvalidGeoFormatException;
import spll.popmapper.SPUniformLocalizer;
import spll.popmapper.constraint.SpatialConstraintMaxDensity;

public class LocalisationRouenRoads {

	//path to the main census shapefile - the entities are generated at this level
	static String stringPathToCensusShapefile = "src/main/java/rouen/spll/data/shp/Rouen_iris.shp";

	//path to the shapefile that define the geographical objects on which the entities should be located
	static String stringPathToNestShapefile = "src/main/java/rouen/spll/data/shp/roads.shp";

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
	
	static String stringPathToOutputFile = "src/main/java/rouen/spll/output/RouenSpll2.shp";

	
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
				LocalisationRouenRoads.class.getSimpleName());

		SPLVectorFile sfAdmin = null;
		SPLVectorFile sfRoads = null;
		
		try {
			//building shapefile
			
			sfRoads = SPLGeofileBuilder.getShapeFile(new File(stringPathToNestShapefile), null);
			//Iris shapefile
			sfAdmin = SPLGeofileBuilder.getShapeFile(new File(stringPathToCensusShapefile), null);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidGeoFormatException e) {
			e.printStackTrace();
		}
		gspu.sysoStempPerformance("Import main shapefiles", LocalisationRouenRoads.class.getSimpleName());

		//import the land-use file
		Collection<String> stringPathToAncilaryGeofiles = new ArrayList<>();
		stringPathToAncilaryGeofiles.add(stringPathToLandUseGrid);

		List<IGSGeofile<? extends AGeoEntity<? extends IValue>, ? extends IValue>> endogeneousVarFile = new ArrayList<>();
		for(String path : stringPathToAncilaryGeofiles){
			try {
				File f = new File(path);
				
				endogeneousVarFile.add(new SPLGeofileBuilder().setFile(f).buildGeofile());
			} catch (IllegalArgumentException | TransformException | IOException | InvalidGeoFormatException e2) {
				e2.printStackTrace();
			}
		}
		


		((SPLVectorFile) sfRoads).minMaxDistance(2.0, 10.0, false);
		
		
		gspu.sysoStempPerformance("Proxy geometries have been computed", 
				LocalisationRouenRoads.class.getSimpleName());
		
		// SETUP THE LOCALIZER
		SPUniformLocalizer localizer = new SPUniformLocalizer(new SpllPopulation(population, sfRoads));
		
		// SETUP GEOGRAPHICAL MATCHER
		// use of the IRIS attribute of the population
		localizer.setMatcher(sfAdmin, stringOfCensusIdInCSVfile, stringOfCensusIdInShapefile);
		localizer.getLocalizationConstraint().setIncreaseStep(100.0);
		localizer.getLocalizationConstraint().setMaxIncrease(100.0); 
	
		SpatialConstraintMaxDensity densityConstr = new SpatialConstraintMaxDensity(sfRoads.getGeoEntity(), 1.0/100.0);
		densityConstr.setPriority(-1);
		densityConstr.setIncreaseStep(1.0/100.0);
		densityConstr.setMaxIncrease(1.0/20.0);
		localizer.addConstraint(densityConstr);
		
		//localize the population
		SpllPopulation localizedPop = localizer.localisePopulation();
		
		try {
			new SPLGeofileBuilder().setFile(new File(stringPathToOutputFile)).setPopulation(localizedPop).buildShapeFile();
		} catch (IOException | SchemaException | InvalidGeoFormatException e) {
			e.printStackTrace();
		}
	}

}
