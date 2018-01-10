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
import core.metamodel.attribute.demographic.DemographicAttribute;
import core.metamodel.attribute.geographic.GeographicAttributeFactory;
import core.metamodel.entity.ADemoEntity;
import core.metamodel.entity.AGeoEntity;
import core.metamodel.io.IGSGeofile;
import core.metamodel.value.IValue;
import core.util.GSPerformanceUtil;
import core.util.data.GSEnumDataType;
import gospl.GosplPopulation;
import gospl.distribution.GosplInputDataManager;
import gospl.io.exception.InvalidSurveyFormatException;
import spll.SpllEntity;
import spll.SpllPopulation;
import spll.algo.LMRegressionOLS;
import spll.algo.exception.IllegalRegressionException;
import spll.datamapper.exception.GSMapperException;
import spll.entity.GeoEntityFactory;
import spll.io.SPLGeofileBuilder;
import spll.io.SPLRasterFile;
import spll.io.SPLVectorFile;
import spll.io.exception.InvalidGeoFormatException;
import spll.popmapper.SPLocalizer;
import spll.popmapper.constraint.SpatialConstraintMaxDensity;
import spll.popmapper.distribution.SpatialDistributionFactory;
import spll.popmapper.linker.ISPLinker;
import spll.popmapper.linker.SPLinker;
import spll.popmapper.normalizer.SPLUniformNormalizer;

public class LocalisationRouenIJGIS_4 {

	//path to the main census shapefile - the entities are generated at this level
	static String stringPathToCensusShapefile = "src/main/java/rouen/spll/data/shp/Rouen_iris.shp";

	//path to the shapefile that define the geographical objects on which the entities should be located
	static String stringPathToNestShapefile = "src/main/java/rouen/spll/data/shp/buildings_residential.shp";
	

	//path to the shapefile that define the geographical objects linked to the entities created
	static String stringPathToLinkerShapefile = "src/main/java/rouen/spll/data/shp/schools_fusion.shp";

	//path to the shapefile that define the geographical objects linked to the entities created
	static String stringPathToLinkerWithIdShapefile = "src/main/java/rouen/spll/data/shp/schoolsid.shp";

		
	//path to the file that will be used as support for the spatial regression (bring additional spatial data)
	static String stringPathToLandUseGrid = "src/main/java/rouen/spll/data/raster/occsol_rouen.tif";

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
	
	static String stringPathToOutputFile = "src/main/java/rouen/spll/output/RouenSpll_IJGIS4.shp";

	
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
				LocalisationRouenIJGIS_4.class.getSimpleName());

		SPLVectorFile sfAdmin = null;
		SPLVectorFile sfBuildings = null;
		SPLVectorFile sfSchools = null;
		try {
			//building shapefile
			sfBuildings = SPLGeofileBuilder.getShapeFile(new File(stringPathToNestShapefile), Arrays.asList("name", "type"), null);
			
			//school shapefile
			sfSchools = SPLGeofileBuilder.getShapeFile(new File(stringPathToLinkerShapefile), Arrays.asList("name", "type", "id"), null);
			
			//Iris shapefile
			sfAdmin = SPLGeofileBuilder.getShapeFile(new File(stringPathToCensusShapefile), null);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidGeoFormatException e) {
			e.printStackTrace();
		}
		gspu.sysoStempPerformance("Import main shapefiles", LocalisationRouenIJGIS_4.class.getSimpleName());

		//import the land-use file
		Collection<String> stringPathToAncilaryGeofiles = new ArrayList<>();
		stringPathToAncilaryGeofiles.add(stringPathToLandUseGrid);

		List<IGSGeofile<? extends AGeoEntity<? extends IValue>, ? extends IValue>> endogeneousVarFile = new ArrayList<>();
		for(String path : stringPathToAncilaryGeofiles){
			try {
				endogeneousVarFile.add(new SPLGeofileBuilder().setFile(new File(path)).buildGeofile());
			} catch (IllegalArgumentException | TransformException | IOException | InvalidGeoFormatException e2) {
				e2.printStackTrace();
			}
		}
		
		gspu.sysoStempPerformance("GIS data have been import to process population localization", 
				LocalisationRouenIJGIS_4.class.getSimpleName());
		
		GosplPopulation kids = new GosplPopulation(population);
		kids.removeIf(e -> ! e.getValueForAttribute("CSP").toString().equals("?"));
		
		// SETUP THE LOCALIZER
		SPLocalizer localizer = new SPLocalizer(kids, sfBuildings);
		
		localizer.setDistribution(SpatialDistributionFactory.getInstance().getAreaBasedDistribution(sfBuildings));
		
		// SETUP GEOGRAPHICAL MATCHER
		// use of the IRIS attribute of the population
		localizer.setMatcher(sfAdmin, stringOfCensusIdInCSVfile, stringOfCensusIdInShapefile);
		
		
		SpatialConstraintMaxDensity maxDens = new SpatialConstraintMaxDensity(sfBuildings.getGeoEntity(), 0.0005);
		maxDens.setPriority(10);
		maxDens.setIncreaseStep(0.0002);
		maxDens.setMaxIncrease(0.003);
		localizer.addConstraint(maxDens);
		
		// SETUP REGRESSION
		try {
			localizer.setMapper(endogeneousVarFile, new ArrayList<>(), 
					new LMRegressionOLS(), new SPLUniformNormalizer(0, SPLRasterFile.DEF_NODATA));
		} catch (IndexOutOfBoundsException | IOException | TransformException | InterruptedException
				| ExecutionException | IllegalRegressionException | GSMapperException | SchemaException 
				| IllegalArgumentException | InvalidGeoFormatException e) {
			e.printStackTrace();
		}
		
		gspu.sysoStempPerformance("Mapper ("+kids.size()+") has been computed", 
				LocalisationRouenIJGIS_4.class.getSimpleName());

	
		//localize the population
		SpllPopulation localizedPop = localizer.localisePopulation();
		gspu.sysoStempPerformance("Population ("+kids.size()+") have been localized", 
				LocalisationRouenIJGIS_4.class.getSimpleName());

		
		
		ISPLinker<SpllEntity> linker = new SPLinker<>(SpatialDistributionFactory.getInstance()
				.getGravityModelDistribution(sfSchools.getGeoEntity(), 3.0,
						localizedPop.toArray(new SpllEntity[localizedPop.size()])));
		
		Collection<? extends AGeoEntity<? extends IValue>> candidates = sfSchools.getGeoEntity();
		
		localizer.linkPopulation(localizedPop, linker, candidates, 
				GeographicAttributeFactory.getFactory().createAttribute("Schools", GSEnumDataType.Nominal));
		

		gspu.sysoStempPerformance("Population ("+localizedPop.size()+") have been linked", 
				LocalisationRouenIJGIS_4.class.getSimpleName());

		
		try {
			new SPLGeofileBuilder().setFile(new File(stringPathToOutputFile)).setPopulation(localizedPop).buildShapeFile();
		} catch (IOException | SchemaException | InvalidGeoFormatException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
		}
		
		gspu.sysoStempPerformance("Population ("+population.size()+") have been saved", 
				LocalisationRouenIJGIS_4.class.getSimpleName());
	}

}
