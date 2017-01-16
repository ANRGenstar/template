package rouen.spll;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.geotools.feature.SchemaException;
import org.opengis.referencing.operation.TransformException;

import core.metamodel.IPopulation;
import core.metamodel.geo.AGeoEntity;
import core.metamodel.geo.io.IGSGeofile;
import core.metamodel.pop.APopulationAttribute;
import core.metamodel.pop.APopulationEntity;
import core.metamodel.pop.APopulationValue;
import core.util.GSPerformanceUtil;
import gospl.distribution.GosplDistributionBuilder;
import gospl.io.exception.InvalidSurveyFormatException;
import spll.SpllPopulation;
import spll.algo.LMRegressionOLS;
import spll.algo.exception.IllegalRegressionException;
import spll.datamapper.exception.GSMapperException;
import spll.entity.GeoEntityFactory;
import spll.io.SPLGeofileFactory;
import spll.io.SPLRasterFile;
import spll.io.SPLVectorFile;
import spll.io.exception.InvalidGeoFormatException;
import spll.popmapper.SPUniformLocalizer;
import spll.popmapper.normalizer.SPLUniformNormalizer;

public class Localisation {

	//path to the main census shapefile - the entities are generated at this level
	static String stringPathToCensusShapefile = "src/main/java/rouen/spll/data/shp/Rouen_iris.shp";

	//path to the shapefile that define the geographical objects on which the entities should be located
	static String stringPathToNestShapefile = "src/main/java/rouen/spll/data/shp/buildings.shp";

	//path to the file that will be used as support for the spatial regression (bring additional spatial data)
	static String stringPathToLandUseGrid = "src/main/java/rouen/spll/data/raster/CLC12_D076_RGF_S.tif";

	static String stringPathToPopulationShapefile = "src/main/java/rouen/spll/output/spllOutput.shp";
	
	//name of the property that contains the id of the census spatial areas in the shapefile
	static String stringOfCensusIdInShapefile = "CODE_IRIS";

	static String stringPathToGenstarConfiguration = "src/main/java/rouen/gospl/output/GSC_Rouen_Localisation.xml";
	//name of the property that contains the id of the census spatial areas in the csv file (and population)
	static String stringOfCensusIdInCSVfile = "IRIS";
	//name of the property that define the number of entities per census spatial areas.
	static String stringOfNumberProperty = "P13_POP";

	//name of the property that will by generated by the regression and that specifies the number of entities per regression areas
	static String stringOfNumberAttribute = GeoEntityFactory.ATTRIBUTE_PIXEL_BAND+0;

	public static void main(String[] args) {

		GSPerformanceUtil gspu = new GSPerformanceUtil("Localisation de la population de Rouen");
		
		// INPUT POPULATION
		GosplDistributionBuilder gdb = null;
		try {
			gdb = new GosplDistributionBuilder(Paths.get(stringPathToGenstarConfiguration));
		} catch (FileNotFoundException e) {
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

		IPopulation<APopulationEntity, APopulationAttribute, APopulationValue> population = gdb.getRawSamples().iterator().next();
		
		gspu.sysoStempPerformance("Population ("+population.size()+") have been retrieve from data", 
				Localisation.class.getSimpleName());
		
		// IMPORT DATA FILES
		SPLGeofileFactory gf = new SPLGeofileFactory();

		SPLVectorFile sfAdmin = null;
		SPLVectorFile sfBuildings = null;

		try {
			//building shapefile
			sfBuildings = gf.getShapeFile(new File(stringPathToNestShapefile));
			//Iris shapefile
			sfAdmin = gf.getShapeFile(new File(stringPathToCensusShapefile));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidGeoFormatException e) {
			e.printStackTrace();
		}


		//import the land-use file
		Collection<String> stringPathToAncilaryGeofiles = new ArrayList<>();
		stringPathToAncilaryGeofiles.add(stringPathToLandUseGrid);

		List<IGSGeofile<? extends AGeoEntity>> endogeneousVarFile = new ArrayList<>();
		for(String path : stringPathToAncilaryGeofiles){
			try {
				endogeneousVarFile.add(gf.getGeofile(new File(path)));
			} catch (IllegalArgumentException | TransformException | IOException | InvalidGeoFormatException e2) {
				e2.printStackTrace();
			}
		}
		
		gspu.sysoStempPerformance("GIS data have been import to process population localization", 
				Localisation.class.getSimpleName());
		
		//Definition of a SpllPopulation from the GOSPLpopulation in order to localize the entities
		SpllPopulation spllPopulation = new SpllPopulation(population, sfBuildings);
		
		// SETUP THE LOCALIZER
		SPUniformLocalizer localizer = new SPUniformLocalizer(spllPopulation);
		
		// SETUP GEOGRAPHICAL MATCHER
		// use of the IRIS attribute of the population
		localizer.setMatcher(sfAdmin, stringOfCensusIdInCSVfile, stringOfCensusIdInShapefile);
		
		// SETUP REGRESSION
		try {
			localizer.setMapper(endogeneousVarFile, new ArrayList<>(), 
					new LMRegressionOLS(), new SPLUniformNormalizer(0, SPLRasterFile.DEF_NODATA));
		} catch (IndexOutOfBoundsException | IOException | TransformException | InterruptedException
				| ExecutionException | IllegalRegressionException | GSMapperException | SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//localize the population
		localizer.localisePopulation();
		
		//save the SpllPopulation into a shapefile.
		try {
			gf.createShapeFile(new File(stringPathToPopulationShapefile), spllPopulation);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		

	}

}
