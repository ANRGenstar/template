package rouen.spll;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.geotools.feature.SchemaException;

import core.metamodel.IPopulation;
import core.metamodel.attribute.Attribute;
import core.metamodel.entity.ADemoEntity;
import core.metamodel.value.IValue;
import core.util.GSPerformanceUtil;
import core.util.excpetion.GSIllegalRangedData;
import gospl.distribution.GosplInputDataManager;
import gospl.io.exception.InvalidSurveyFormatException;
import spll.SpllEntity;
import spll.SpllPopulation;
import spll.io.SPLGeofileBuilder;
import spll.io.SPLVectorFile;
import spll.io.exception.InvalidGeoFormatException;
import spll.localizer.SPLocalizer;
import spll.localizer.distribution.SpatialDistributionFactory;

public class LocalisationRouenIJGIS_2 {

		//path to the shapefile that define the geographical objects on which the entities should be located
		static String stringPathToNestShapefile = "src/main/java/rouen/spll/data/shp/roads.shp";

		static String stringPathToGenstarConfiguration = "src/main/java/rouen/gospl/output/rouen_demo_spll.gns";
		
		static String stringPathToOutputFile = "src/main/java/rouen/spll/output/RouenSpll_IJGIS2.shp";

		
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
					LocalisationRouenRoads.class.getSimpleName());

			SPLVectorFile sfRoads = null;
			
			try {
				//road shapefile
				
				sfRoads = SPLGeofileBuilder.getShapeFile(new File(stringPathToNestShapefile), null);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InvalidGeoFormatException e) {
				e.printStackTrace();
			} catch (GSIllegalRangedData e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			gspu.sysoStempPerformance("Import main shapefiles", LocalisationRouenRoads.class.getSimpleName());


			((SPLVectorFile) sfRoads).minMaxDistance(2.0, 10.0, false);
			
			
			gspu.sysoStempPerformance("Proxy geometries have been computed", 
					LocalisationRouenRoads.class.getSimpleName());
			
			// SETUP THE LOCALIZER
			SPLocalizer localizer = new SPLocalizer(population, sfRoads);
			localizer.setDistribution(SpatialDistributionFactory.getInstance().getAreaBasedDistribution(sfRoads));
			
			//localize the population
			SpllPopulation localizedPop = localizer.localisePopulation();
			
			gspu.sysoStempPerformance("Population ("+population.size()+") have been localized", 
					LocalisationRouenIJGIS_2.class.getSimpleName());

			try {
				new SPLGeofileBuilder().setFile(new File(stringPathToOutputFile)).setPopulation(localizedPop).buildShapeFile();
			} catch (IOException | SchemaException | InvalidGeoFormatException e) {
				e.printStackTrace();
			}
			
			gspu.sysoStempPerformance("Population ("+population.size()+") have been saved", 
					LocalisationRouenIJGIS_2.class.getSimpleName());
		}

}
