package bangkok.spll;

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
import spll.popmapper.constraint.SpatialConstraintMaxNumber;
import spll.popmapper.normalizer.SPLUniformNormalizer;

public class LocalisationBangkok {

	//path to the main census shapefile - the entities are generated at this level
	static String stringPathToCensusShapefile = "src/main/java/bangkok/spll/data/overall/kwaeng.shp";

	//path to the file that will be used as support for the spatial regression (bring additional spatial data)
	static String stringPathToLandUseGrid = "src/main/java/bangkok/spll/data/overall/occsol_bma_tif.tif";

	static String stringPathToPopulationShapefile = "src/main/java/bangkok/spll/output/spllOutput.shp";
	
	static String stringPathToGenstarConfiguration = "src/main/java/bangkok/gospl/output/GSC_Bangkok_Localisation.xml";
	
	
	public static void main(String[] args) {

		GSPerformanceUtil gspu = new GSPerformanceUtil("Localisation de la population de Bangkok");
		
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
				LocalisationBangkok.class.getSimpleName());
		
		// IMPORT DATA FILES
		SPLGeofileFactory gf = new SPLGeofileFactory();

		SPLVectorFile sfAdmin = null;
		
		try {
			sfAdmin = gf.getShapeFile(new File(stringPathToCensusShapefile));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidGeoFormatException e) {
			e.printStackTrace();
		}

		gspu.sysoStempPerformance("Import main shapefiles", LocalisationBangkok.class.getSimpleName());

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
				LocalisationBangkok.class.getSimpleName());
		
		// SETUP THE LOCALIZER
		SPUniformLocalizer localizer = new SPUniformLocalizer(new SpllPopulation(population, sfAdmin));
		
		// SETUP GEOGRAPHICAL MATCHER
		// use of the IRIS attribute of the population
		localizer.setMatcher(sfAdmin, "PAT", "code");
		//localizer.getLocalizationConstraint().setIncreaseStep(100.0);
		//localizer.getLocalizationConstraint().setMaxIncrease(100.0); 
		/*SpatialConstraintMaxDensity densityConstr = new SpatialConstraintMaxDensity(sfBuildings.getGeoEntity(), 1.0/100.0);
		densityConstr.setPriority(-1);
		densityConstr.setIncreaseStep(1.0/100.0);
		densityConstr.setMaxIncrease(1.0/20.0);
		localizer.getConstraints().add(densityConstr);
		*/
		/*SpatialConstraintMaxNumber numberConstr = new SpatialConstraintMaxNumber(sfBuildings.getGeoEntity(), 1.0);
		numberConstr.setPriority(10);
		numberConstr.setIncreaseStep(2);
		numberConstr.setMaxIncrease(60);
		localizer.getConstraints().add(numberConstr);*/
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
		SpllPopulation localizedPop = localizer.localisePopulation();
		try {
			new SPLGeofileFactory().createShapeFile(new File(stringPathToPopulationShapefile), localizedPop);
		} catch (IOException | SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
