package rouen.gospl.configuration;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import core.configuration.GenstarConfigurationFile;
import core.configuration.GenstarJsonUtil;
import core.configuration.dictionary.AttributeDictionary;
import core.configuration.dictionary.IGenstarDictionary;
import core.metamodel.attribute.Attribute;
import core.metamodel.attribute.AttributeFactory;
import core.metamodel.attribute.emergent.filter.GSMatchFilter;
import core.metamodel.attribute.emergent.filter.GSMatchSelection;
import core.metamodel.attribute.emergent.filter.predicate.GSMatchPredicate;
import core.metamodel.attribute.emergent.filter.predicate.GSPredicateFactory;
import core.metamodel.entity.matcher.MatchType;
import core.metamodel.entity.matcher.TagMatcher;
import core.metamodel.entity.tag.EntityTag;
import core.metamodel.io.GSSurveyType;
import core.metamodel.io.GSSurveyWrapper;
import core.metamodel.value.IValue;
import core.util.data.GSEnumDataType;

public class GSCRouenMulti {

	public static String CONF_CLASS_PATH = "src/main/java/rouen/gospl/data";
	public static String CONF_EXPORT = "rouen_multi.gns";
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {

		// Setup the factory that build attribute
		AttributeFactory attf = AttributeFactory.getFactory();

		// What to define in this configuration file
		Map<GSSurveyWrapper, List<Integer>> inputFiles = new HashMap<>();
		
		AttributeDictionary dd = new AttributeDictionary();
		dd.setLevel(1);

		Path baseDirectory = FileSystems.getDefault().getPath(".");
		Path relativePath = Paths.get(CONF_CLASS_PATH);
		
		// GET THE DICTIONARY OF INDIVIDUAL ENTITY
		GenstarConfigurationFile gcfIndividual = null;
		try {
			gcfIndividual = new GenstarJsonUtil().unmarshalFromGenstarJson(Paths.get(CONF_CLASS_PATH, "rouen_demographics.gns"), 
					GenstarConfigurationFile.class);
		} catch (IllegalArgumentException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		IGenstarDictionary<Attribute<? extends IValue>> indivDico = gcfIndividual.getDictionary();

		// Setup input files' configuration for individual aggregated data
		inputFiles.put(new GSSurveyWrapper(relativePath.resolve("Ménage & Enfants-Tableau 1.csv"), 
				GSSurveyType.ContingencyTable, ';', 1, 1), Arrays.asList(1));
		inputFiles.put(new GSSurveyWrapper(relativePath.resolve("Taille ménage & CSP référent-Tableau 1.csv"), 
				GSSurveyType.ContingencyTable, ';', 1, 1), Arrays.asList(1));
		inputFiles.put(new GSSurveyWrapper(relativePath.resolve("Taille ménage & Sex & Age-Tableau 1.csv"), 
				GSSurveyType.ContingencyTable, ';', 2, 1), Arrays.asList(1));
		

		
		// ----------------------------
		// Number of child in household
		// ----------------------------

		List<String> values = Arrays.asList("Aucun enfant de moins de 25 ans",
				"1 enfant de moins de 25 ans", "2 enfant de moins de 25 ans",
				"3 enfant de moins de 25 ans", "4 enfants ou plus de moins de 25 ans");
		
		// TODO make something with NumericValueMapper
		Map<Integer,String> childMap = new HashMap<>(); 
		childMap.put(0, "Aucun enfant de moins de 25 ans");
		childMap.put(1, "1 enfant de moins de 25 ans");
		childMap.put(2, "2 enfant de moins de 25 ans");
		childMap.put(3, "3 enfant de moins de 25 ans");
		childMap.put(4, "4 enfants ou plus de moins de 25 ans");
		
		dd.addAttributes(attf.createCountAttribute("Number of child", values, childMap, EntityTag.Child));
		
		// --------------
		// Household structure
		// --------------
		
		List<String> hhStructureValues = Arrays.asList("Couple sans enfant", "Couple avec enfant(s)",
				"Famille monoparentale composée d'une femme avec enfant(s)",
				"Famille monoparentale composée d'un homme avec enfant(s)");
		String hhSName = "Household structure";
				
		// 2 - Mapping - value vs predicates
		
		Map<Collection<GSMatchPredicate<?,?>>, String> mapping = new HashMap<>();
		
		GSMatchFilter<EntityTag> parentPredicate = new GSMatchFilter<>(new TagMatcher(EntityTag.Parent));
		GSMatchSelection<EntityTag> monoParentPredicate = new GSMatchSelection<>(new TagMatcher(EntityTag.HHHead));
		GSMatchFilter<EntityTag> childPredicate = new GSMatchFilter<>(new TagMatcher(EntityTag.Child));
		
		mapping.put(Arrays.asList(
				GSPredicateFactory.getFactory().createExistPredicate(parentPredicate, 
						MatchType.ALL, indivDico.getValue("Vivant en couple")), 
				GSPredicateFactory.getFactory().createExistPredicate(childPredicate, MatchType.NONE)), 
				"Couple sans enfant");
		
 		mapping.put(Arrays.asList(
 				GSPredicateFactory.getFactory().createExistPredicate(parentPredicate, 
						MatchType.ALL, indivDico.getValue("Vivant en couple")),
 				GSPredicateFactory.getFactory().createExistPredicate(childPredicate, MatchType.ANY)), 
 				"Couple avec enfant(s)");
 		
 		mapping.put(Arrays.asList(GSPredicateFactory.getFactory().createExistPredicate(
 				monoParentPredicate, indivDico.getValue("Ne vivant pas en couple"), indivDico.getValue("Femmes")),
 				GSPredicateFactory.getFactory().createExistPredicate(childPredicate, MatchType.ANY)), 
 				"Famille monoparentale composée d'une femme avec enfant(s)");
 		
 		mapping.put(Arrays.asList(GSPredicateFactory.getFactory().createExistPredicate(
 				monoParentPredicate, indivDico.getValue("Ne vivant pas en couple"), indivDico.getValue("Hommes")),
 				GSPredicateFactory.getFactory().createExistPredicate(childPredicate, MatchType.ANY)), 
 				"Famille monoparentale composée d'un homme avec enfant(s)");

		attf.createTransposedValuesAttribute(hhSName, hhStructureValues, GSEnumDataType.Nominal, mapping);
		
		// ---------------------------------
		// Number of people in the household
		// ---------------------------------
		
		LinkedHashMap<Integer, String> hhSizeMapper = new LinkedHashMap<>();
		hhSizeMapper.put(1, "1 personne");
		hhSizeMapper.put(2, "2 personnes");
		hhSizeMapper.put(3, "3 personnes");
		hhSizeMapper.put(4, "4 personnes");
		hhSizeMapper.put(5, "5 personnes");
		hhSizeMapper.put(6, "6 personnes ou plus"); // What to do for top value ?
		
		dd.setSizeAttribute(attf.createCountAttribute("Number of people in household", 
				Arrays.asList("1 personne", "2 personnes", "3 personnes", 
						"4 personnes", "5 personnes", "6 personnes ou plus"), hhSizeMapper));
		
		// ------------------
		// Household head
		// ------------------
		
		// ---
		// CSP
		// ---
		
		// Retrieve the head of household
		Attribute<? extends IValue> headCSP = attf.createValueOfAttribute("CSP of head", 
				indivDico.getAttribute("CSP"), EntityTag.HHHead);
		
		dd.addAttributes(headCSP);
		
		// ---
		// Age
		// ---
		
		Attribute<? extends IValue> headAge = attf.createValueOfAttribute("Age of head", 
				indivDico.getAttribute("Age"), EntityTag.HHHead);
		
		dd.addAttributes(headAge);
		
		// ------
		// Gender
		// ------
		
		Attribute<? extends IValue> headGender = attf.createValueOfAttribute("Gender of head", 
				indivDico.getAttribute("Sexe"), EntityTag.HHHead);
		
		dd.addAttributes(headGender);
		
		// ------------------------------
		// SERIALIZE CONFIGURATION FILES
		// ------------------------------

		GenstarConfigurationFile gcf = new GenstarConfigurationFile();
		gcf.setBaseDirectory(baseDirectory);
		
		// HH Level survey wrapper
		gcf.setWrappers(inputFiles);
		// Inidividual level survey wrapper
		gcfIndividual.getSurveyWrappers().stream().forEach(wrapper -> gcf.addSurveyWrapper(wrapper, 0));
		
		gcf.setDictionaries(Stream.of(dd, gcfIndividual.getDictionary()).collect(Collectors.toSet()));

		try {
			new GenstarJsonUtil().marshalToGenstarJson(relativePath.resolve(CONF_EXPORT), gcf, false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
