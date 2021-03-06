/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.api.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNameTag;
import org.openmrs.ConceptProposal;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptSearchResult;
import org.openmrs.ConceptSet;
import org.openmrs.ConceptSource;
import org.openmrs.Drug;
import org.openmrs.api.APIException;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseContextSensitiveTest;

/**
 * Unit tests for methods that are specific to the {@link ConceptServiceImpl}. General tests that
 * would span implementations should go on the {@link ConceptService}.
 */
public class ConceptServiceImplTest extends BaseContextSensitiveTest {
	
	protected ConceptService conceptService = null;
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	
	/**
	 * Run this before each unit test in this class. The "@Before" method in
	 * {@link BaseContextSensitiveTest} is run right before this method.
	 * 
	 * @throws Exception
	 */
	@Before
	public void runBeforeAllTests() throws Exception {
		conceptService = Context.getConceptService();
	}
	
	/**
	 * @see ConceptServiceImpl#saveConcept(Concept)
	 * @verifies return the concept with new conceptID if creating new concept
	 */
	@Test
	public void saveConcept_shouldReturnTheConceptWithNewConceptIDIfCreatingNewConcept() throws Exception {
		Concept c = new Concept();
		ConceptName fullySpecifiedName = new ConceptName("requires one name min", new Locale("fr", "CA"));
		c.addName(fullySpecifiedName);
		c.addDescription(new ConceptDescription("some description", null));
		c.setDatatype(new ConceptDatatype(1));
		c.setConceptClass(new ConceptClass(1));
		Concept savedC = Context.getConceptService().saveConcept(c);
		assertNotNull(savedC);
		assertTrue(savedC.getConceptId() > 0);
	}
	
	/**
	 * @see ConceptServiceImpl#saveConcept(Concept)
	 * @verifies return the concept with same conceptID if updating existing concept
	 */
	
	@Test
	public void saveConcept_shouldReturnTheConceptWithSameConceptIDIfUpdatingExistingConcept() throws Exception {
		Concept c = new Concept();
		ConceptName fullySpecifiedName = new ConceptName("requires one name min", new Locale("fr", "CA"));
		c.addName(fullySpecifiedName);
		c.addDescription(new ConceptDescription("some description", null));
		c.setDatatype(new ConceptDatatype(1));
		c.setConceptClass(new ConceptClass(1));
		Concept savedC = Context.getConceptService().saveConcept(c);
		assertNotNull(savedC);
		Concept updatedC = Context.getConceptService().saveConcept(c);
		assertNotNull(updatedC);
		assertEquals(updatedC.getConceptId(), savedC.getConceptId());
	}
	
	/**
	 * @see ConceptServiceImpl#saveConcept(Concept)
	 * @verifies leave preferred name preferred if set
	 */
	@Test
	public void saveConcept_shouldLeavePreferredNamePreferredIfSet() throws Exception {
		Locale loc = new Locale("fr", "CA");
		ConceptName fullySpecifiedName = new ConceptName("fully specified", loc);
		fullySpecifiedName.setConceptNameType(ConceptNameType.FULLY_SPECIFIED); //be explicit for test case
		ConceptName shortName = new ConceptName("short name", loc);
		shortName.setConceptNameType(ConceptNameType.SHORT); //be explicit for test case
		ConceptName synonym = new ConceptName("synonym", loc);
		synonym.setConceptNameType(null); //synonyms are id'd by a null type
		ConceptName indexTerm = new ConceptName("indexTerm", loc);
		indexTerm.setConceptNameType(ConceptNameType.INDEX_TERM); //synonyms are id'd by a null type
		
		//saveConcept never picks an index term for default, so we'll use it for the test
		indexTerm.setLocalePreferred(true);
		
		Concept c = new Concept();
		c.addName(fullySpecifiedName);
		c.addName(synonym);
		c.addName(indexTerm);
		c.addName(shortName);
		
		//ignore it so we can test the set default preferred name  functionality
		try {
			Context.getConceptService().saveConcept(c);
		}
		catch (org.openmrs.api.APIException e) {
			//ignore it
		}
		assertNotNull("there's a preferred name", c.getPreferredName(loc));
		assertTrue("name was explicitly marked preferred", c.getPreferredName(loc).isPreferred());
		assertEquals("name matches", c.getPreferredName(loc).getName(), indexTerm.getName());
	}
	
	/**
	 * @see ConceptServiceImpl#saveConcept(Concept)
	 * @verifies not set default preferred name to short or index terms
	 */
	@Test
	public void saveConcept_shouldNotSetDefaultPreferredNameToShortOrIndexTerms() throws Exception {
		Locale loc = new Locale("fr", "CA");
		ConceptName shortName = new ConceptName("short name", loc);
		shortName.setConceptNameType(ConceptNameType.SHORT); //be explicit for test case
		ConceptName indexTerm = new ConceptName("indexTerm", loc);
		indexTerm.setConceptNameType(ConceptNameType.INDEX_TERM); //synonyms are id'd by a null type
		
		Concept c = new Concept();
		HashSet<ConceptName> allNames = new HashSet<ConceptName>(4);
		allNames.add(indexTerm);
		allNames.add(shortName);
		c.setNames(allNames);
		
		//The API will throw a validation error because preferred name is an index term
		//ignore it so we can test the set default preferred name  functionality
		try {
			Context.getConceptService().saveConcept(c);
		}
		catch (org.openmrs.api.APIException e) {
			//ignore it
		}
		assertNull("there's a preferred name", c.getPreferredName(loc));
		assertFalse("name was explicitly marked preferred", shortName.isPreferred());
		assertFalse("name was explicitly marked preferred", indexTerm.isPreferred());
	}
	
	/**
	 * @see ConceptServiceImpl#saveConcept(Concept)
	 * @verifies set default preferred name to fully specified first If
	 *           Concept.getPreferredName(locale) returns null, saveConcept chooses one. The default
	 *           first choice is the fully specified name in the locale. The default second choice
	 *           is a synonym in the locale.
	 */
	@Test
	public void saveConcept_shouldSetDefaultPreferredNameToFullySpecifiedFirst() throws Exception {
		Locale loc = new Locale("fr", "CA");
		ConceptName fullySpecifiedName = new ConceptName("fully specified", loc);
		fullySpecifiedName.setConceptNameType(ConceptNameType.FULLY_SPECIFIED); //be explicit for test case
		ConceptName shortName = new ConceptName("short name", loc);
		shortName.setConceptNameType(ConceptNameType.SHORT); //be explicit for test case
		ConceptName synonym = new ConceptName("synonym", loc);
		synonym.setConceptNameType(null); //synonyms are id'd by a null type
		ConceptName indexTerm = new ConceptName("indexTerm", loc);
		indexTerm.setConceptNameType(ConceptNameType.INDEX_TERM); //synonyms are id'd by a null type
		
		Concept c = new Concept();
		c.addName(fullySpecifiedName);
		c.addName(synonym);
		c.addName(indexTerm);
		c.addName(shortName);
		c.addDescription(new ConceptDescription("some description", null));
		c.setDatatype(new ConceptDatatype(1));
		c.setConceptClass(new ConceptClass(1));
		assertFalse("check test assumption - the API didn't automatically set preferred vlag",
		    c.getFullySpecifiedName(loc).isPreferred());
			
		assertNotNull("Concept is legit, save succeeds", Context.getConceptService().saveConcept(c));
		
		Context.getConceptService().saveConcept(c);
		assertNotNull("there's a preferred name", c.getPreferredName(loc));
		assertTrue("name was explicitly marked preferred", c.getPreferredName(loc).isPreferred());
		assertEquals("name matches", c.getPreferredName(loc).getName(), fullySpecifiedName.getName());
	}
	
	/**
	 * @see ConceptServiceImpl#saveConcept(Concept)
	 * @verifies set default preferred name to a synonym second If Concept.getPreferredName(locale)
	 *           returns null, saveConcept chooses one. The default first choice is the fully
	 *           specified name in the locale. The default second choice is a synonym in the locale.
	 */
	@Test
	public void saveConcept_shouldSetDefaultPreferredNameToASynonymSecond() throws Exception {
		Locale loc = new Locale("fr", "CA");
		Locale otherLocale = new Locale("en", "US");
		//Create a fully specified name, but for another locale
		//so the Concept passes validation
		ConceptName fullySpecifiedName = new ConceptName("fully specified", otherLocale);
		fullySpecifiedName.setConceptNameType(ConceptNameType.FULLY_SPECIFIED); //be explicit for test case
		ConceptName shortName = new ConceptName("short name", loc);
		shortName.setConceptNameType(ConceptNameType.SHORT); //be explicit for test case
		ConceptName synonym = new ConceptName("synonym", loc);
		synonym.setConceptNameType(null); //synonyms are id'd by a null type
		ConceptName indexTerm = new ConceptName("indexTerm", loc);
		indexTerm.setConceptNameType(ConceptNameType.INDEX_TERM); //synonyms are id'd by a null type
		
		Concept c = new Concept();
		HashSet<ConceptName> allNames = new HashSet<ConceptName>(4);
		allNames.add(indexTerm);
		allNames.add(fullySpecifiedName);
		allNames.add(synonym);
		c.setNames(allNames);
		c.addDescription(new ConceptDescription("some description", null));
		c.setDatatype(new ConceptDatatype(1));
		c.setConceptClass(new ConceptClass(1));
		
		assertNull("check test assumption - the API hasn't promoted a name to a fully specified name",
		    c.getFullySpecifiedName(loc));
			
		Context.getConceptService().saveConcept(c);
		assertNotNull("there's a preferred name", c.getPreferredName(loc));
		assertTrue("name was explicitly marked preferred", c.getPreferredName(loc).isPreferred());
		assertEquals("name matches", c.getPreferredName(loc).getName(), synonym.getName());
		assertEquals("fully specified name unchanged", c.getPreferredName(otherLocale).getName(),
		    fullySpecifiedName.getName());
			
	}
	
	@Test
	public void saveConcept_shouldTrimWhitespacesInConceptName() throws Exception {
		//Given
		Concept concept = new Concept();
		String nameWithSpaces = "  jwm  ";
		concept.addName(new ConceptName(nameWithSpaces, new Locale("en", "US")));
		concept.addDescription(new ConceptDescription("some description", null));
		concept.setDatatype(new ConceptDatatype(1));
		concept.setConceptClass(new ConceptClass(1));
		//When
		Context.getConceptService().saveConcept(concept);
		//Then
		assertNotEquals(concept.getName().getName(), nameWithSpaces);
		assertEquals(concept.getName().getName(), "jwm");
	}
	
	/**
	 * @see ConceptServiceImpl#saveConcept(Concept)
	 * @verifies force set flag if set members exist
	 */
	@Test
	public void saveConcept_shouldForceSetFlagIfSetMembersExist() throws Exception {
		//Given
		Concept concept = new Concept();
		concept.addName(new ConceptName("Concept", new Locale("en", "US")));
		concept.addDescription(new ConceptDescription("some description", null));
		concept.setDatatype(new ConceptDatatype(1));
		concept.setConceptClass(new ConceptClass(1));
		Concept conceptSetMember = new Concept();
		conceptSetMember.addName(new ConceptName("Set Member", new Locale("en", "US")));
		conceptSetMember.addDescription(new ConceptDescription("some description", null));
		conceptSetMember.setConceptClass(new ConceptClass(1));
		conceptSetMember.setDatatype(new ConceptDatatype(1));
		Context.getConceptService().saveConcept(conceptSetMember);
		concept.addSetMember(conceptSetMember);
		concept.setSet(false);
		//When
		Context.getConceptService().saveConcept(concept);
		//Then
		assertTrue(concept.getSet());
	}
	
	/**
	 * @see ConceptServiceImpl#purgeConcept(Concept)
	 * @verifies should purge the concept if not being used by an obs
	 */
	@Test
	public void purgeConcept_shouldPurgeTheConceptIfNotBeingUsedByAnObs() throws Exception {
		int conceptId = 88;
		conceptService.purgeConcept(conceptService.getConcept(conceptId));
		assertNull(conceptService.getConcept(conceptId));
	}
	
	/**
	 * @see ConceptServiceImpl#retireConcept(Concept,String)
	 * @verifies should fail if no reason is given
	 */
	@Test
	public void retireConcept_shouldFailIfNoReasonIsGiven() throws Exception {
		Concept concept = conceptService.getConcept(3);
		expectedException.expect(IllegalArgumentException.class);
		conceptService.retireConcept(concept, "");
	}
	
	/**
	 * @see ConceptServiceImpl#retireConcept(Concept,String)
	 * @verifies should retire the given concept
	 */
	@Test
	public void retireConcept_shouldRetireTheGivenConcept() throws Exception {
		String retireReason = "dummy reason";
		Concept concept = conceptService.getConcept(3);
		assertFalse(concept.getRetired());
		conceptService.retireConcept(concept, retireReason);
		assertTrue(concept.getRetired());
		assertEquals(retireReason, concept.getRetireReason());
	}
	
	/**
	 * @see ConceptServiceImpl#purgeDrug(Drug)
	 * @verifies should purge the given Drug
	 */
	@Test
	public void purgeDrug_shouldPurgeTheGivenDrug() throws Exception {
		int drugId = 2;
		conceptService.purgeDrug(conceptService.getDrug(drugId));
		assertNull(conceptService.getDrug(drugId));
	}
	
	/**
	 * @see ConceptServiceImpl#getAllDrugs()
	 * @verifies should return a list of all drugs
	 */
	@Test
	public void getAllDrugs_shouldReturnAListOfAllDrugs() throws Exception {
		int resultWhenTrue = 4;
		List<Drug> allDrugs = conceptService.getAllDrugs();
		assertEquals(resultWhenTrue, allDrugs.size());
	}
	
	/**
	 * @see ConceptServiceImpl#getAllDrugs(boolean)
	 * @verifies should return all drugs including retired ones if given true
	 */
	@Test
	public void getAllDrugs_shouldReturnAllDrugsIncludingRetiredOnesIfGivenTrue() throws Exception {
		int resultWhenTrue = 4;
		List<Drug> allDrugs = conceptService.getAllDrugs(true);
		assertEquals(resultWhenTrue, allDrugs.size());
	}
	
	/**
	 * @see ConceptServiceImpl#getAllDrugs(boolean)
	 * @verifies should return all drugs excluding retired ones if given false
	 */
	@Test
	public void getAllDrugs_shouldReturnAllDrugsExcludingRetiredOnesIfGivenFalse() throws Exception {
		int resultWhenTrue = 2;
		List<Drug> allDrugs = conceptService.getAllDrugs(false);
		assertEquals(resultWhenTrue, allDrugs.size());
	}
	
	/**
	 * @see ConceptServiceImpl#getDrugs(String, Concept, boolean, boolean, boolean, Integer,
	 *      Integer)
	 * @verifies return list of drugs
	 */
	@Test
	public void getDrugs_shouldReturnListOfMatchingDrugs() throws Exception {
		String drugName = "ASPIRIN";
		String drugUuid = "05ec820a-d297-44e3-be6e-698531d9dd3f";
		Concept concept = conceptService.getConceptByUuid(drugUuid);
		List<Drug> drugs = conceptService.getDrugs(drugName, concept, true, true, true, 0, 100);
		assertTrue(drugs.contains(conceptService.getDrug(drugName)));
	}
	
	/**
	 * @see ConceptServiceImpl#getDrug(String)
	 * @verifies should return the matching drug object
	 */
	@Test
	public void getDrug_shouldReturnTheMatchingDrugObject() throws Exception {
		String drugName = "ASPIRIN";
		String drugUuid = "05ec820a-d297-44e3-be6e-698531d9dd3f";
		Drug drug = conceptService.getDrugByUuid(drugUuid);
		assertEquals(drug, conceptService.getDrug(drugName));
	}
	
	/**
	 * @see ConceptServiceImpl#getDrug(String)
	 * @verifies should return null if no matching drug is found
	 */
	@Test
	public void getDrug_shouldReturnNullIfNoMatchingDrugIsFound() throws Exception {
		int drugIdNotPresent = 1234;
		assertNull(conceptService.getDrug(drugIdNotPresent));
	}
	
	/**
	 * @see ConcepTServiceImpl#retireDrug(Drug, String)
	 * @verifies should retire the given Drug
	 */
	@Test
	public void retireDrug_shouldRetireTheGivenDrug() throws Exception {
		String uuidOfDrugToCheck = "05ec820a-d297-44e3-be6e-698531d9dd3f";
		Drug drug = conceptService.getDrugByUuid(uuidOfDrugToCheck);
		conceptService.retireDrug(drug, "some dummy reason");
		assertTrue(drug.getRetired());
	}
	
	/**
	 * @see ConcepTServiceImpl#unretireDrug(Drug)
	 * @verifies should mark Drug as not retired
	 */
	@Test
	public void unretireDrug_shouldMarkDrugAsNotRetired() throws Exception {
		String uuidOfDrugToCheck = "7e2323fa-0fa0-461f-9b59-6765997d849e";
		Drug drug = conceptService.getDrugByUuid(uuidOfDrugToCheck);
		conceptService.unretireDrug(drug);
		assertFalse(drug.getRetired());
	}
	
	/**
	 * @see ConcepTServiceImpl#unretireDrug(Drug)
	 * @verifies should not change attributes of Drug that is already not retired
	 */
	@Test
	public void unretireDrug_shouldNotChangeAttributesOfDrugThatIsAlreadyNotRetired() throws Exception {
		String uuidOfDrugToCheck = "3cfcf118-931c-46f7-8ff6-7b876f0d4202";
		Drug drug = conceptService.getDrugByUuid(uuidOfDrugToCheck);
		assertFalse(drug.getRetired());
		conceptService.unretireDrug(drug);
		assertFalse(drug.getRetired());
	}
	
	/**
	 * @see ConceptServiceImpl#getAllConceptClasses()
	 * @verifies should return a list of all concept classes
	 */
	@Test
	public void getAllConceptClasses_shouldReturnAListOfAllConceptClasses() throws Exception {
		int resultSize = 20;
		List<ConceptClass> conceptClasses = conceptService.getAllConceptClasses();
		assertEquals(resultSize, conceptClasses.size());
	}
	
	/**
	 * @see ConceptServiceImpl#getAllConceptClasses(boolean)
	 * @verifies should return all concept classes including retired ones when given true
	 */
	@Test
	public void getAllConceptClasses_shouldReturnAllConceptClassesIncludingRetiredOnesWhenGivenTrue() throws Exception {
		int resultSizeWhenTrue = 20;
		List<ConceptClass> conceptClasses = conceptService.getAllConceptClasses(true);
		assertEquals(resultSizeWhenTrue, conceptClasses.size());
	}
	
	/**
	 * @see ConceptServiceImpl#getAllConceptClasses(boolean)
	 * @verifies should return all concept classes excluding retired ones when given false
	 */
	@Test
	public void getAllConceptClasses_shouldReturnAllConceptClassesExcludingRetiredOnesWhenGivenFalse() throws Exception {
		int resultSizeWhenFalse = 20;
		List<ConceptClass> conceptClasses = conceptService.getAllConceptClasses(false);
		assertEquals(resultSizeWhenFalse, conceptClasses.size());
	}
	
	/**
	 * @see ConceptServiceImpl#saveConceptClass(ConceptClass)
	 * @verifies should save the given ConceptClass
	 */
	@Test
	public void saveConceptClass_shouldSaveTheGivenConceptClass() throws Exception {
		int unusedConceptClassId = 123;
		ConceptClass conceptClass = new ConceptClass(unusedConceptClassId);
		conceptClass.setName("name");
		conceptClass.setDescription("description");
		conceptService.saveConceptClass(conceptClass);
		assertEquals(conceptClass, conceptService.getConceptClass(unusedConceptClassId));
	}
	
	/**
	 * @see ConceptServiceImpl#purgeConceptClass(ConceptClass)
	 * @verifies should delete the given ConceptClass
	 */
	@Test
	public void purgeConceptClass_shouldDeleteTheGivenConceptClass() throws Exception {
		int conceptClassId = 1;
		ConceptClass cc = conceptService.getConceptClass(conceptClassId);
		assertNotNull(cc);
		conceptService.purgeConceptClass(cc);
		assertNull(conceptService.getConceptClass(conceptClassId));
	}
	
	/**
	 * @see ConceptServiceImpl#getAllConceptDatatypes()
	 * @verifies should give a list of all concept datatypes
	 */
	@Test
	public void getAllConceptDatatypes_shouldGiveAListOfAllConceptDataypes() throws Exception {
		int resultSize = 12;
		String uuid = "8d4a4488-c2cc-11de-8d13-0010c6dffd0f";
		List<ConceptDatatype> conceptDatatypes = conceptService.getAllConceptDatatypes();
		assertEquals(resultSize, conceptDatatypes.size());
		assertTrue(conceptDatatypes.contains(conceptService.getConceptDatatypeByUuid(uuid)));
	}
	
	/**
	 * @see ConceptServiceImpl#getAllConceptDatatypes(boolean)
	 * @verifies should return all concept datatypes including retired ones when given true
	 */
	@Test
	public void getAllConceptDatatypes_shouldReturnAllConceptDataypesIncludingRetiredOnesWhenGivenTrue() throws Exception {
		int resultSize = 12;
		String uuid = "8d4a4488-c2cc-11de-8d13-0010c6dffd0f";
		List<ConceptDatatype> conceptDatatypes = conceptService.getAllConceptDatatypes(true);
		assertEquals(resultSize, conceptDatatypes.size());
		assertTrue(conceptDatatypes.contains(conceptService.getConceptDatatypeByUuid(uuid)));
	}
	
	/**
	 * @see ConceptServiceImpl#getAllConceptDatatypes(boolean)
	 * @verifies should return all concept datatypes excluding retired ones when given false
	 */
	@Test
	public void getAllConceptDatatypes_shouldReturnAllConceptDataypesExcludingRetiredOnesWhenGivenFalse() throws Exception {
		int resultSize = 12;
		String uuid = "8d4a4488-c2cc-11de-8d13-0010c6dffd0f";
		List<ConceptDatatype> conceptDatatypes = conceptService.getAllConceptDatatypes(false);
		assertEquals(resultSize, conceptDatatypes.size());
		assertTrue(conceptDatatypes.contains(conceptService.getConceptDatatypeByUuid(uuid)));
	}
	
	/**
	 * @see ConceptServiceImpl#getSetsContainingConcept(Concept)
	 * @verifies should give a list of ConceptSet containing the given Concept
	 */
	@Test
	public void getSetsContainingConcept_shouldGiveAListOfConceptSetContainingTheGivenConcept() throws Exception {
		Concept concept = conceptService.getConcept(18);
		List<ConceptSet> conceptSets = conceptService.getSetsContainingConcept(concept);
		assertNotNull(conceptSets);
		assertEquals(conceptSets.get(0).getConcept(), concept);
	}
	
	/**
	 * @see ConceptServiceImpl#getSetsContainingConcept(Concept)
	 * @verifies should give an empty list if no matching ConceptSet is found
	 */
	@Test
	public void getSetsContainingConcept_shouldGiveAnEmptyListIfNoMatchingConceptSetIsFound() throws Exception {
		String uuid = "0cbe2ed3-cd5f-4f46-9459-26127c9265ab";
		Concept concept = conceptService.getConceptByUuid(uuid);
		List<ConceptSet> conceptSets = conceptService.getSetsContainingConcept(concept);
		assertEquals(conceptSets, Collections.emptyList());
	}
	
	/**
	 * @see ConceptServiceImpl#getSetsContainingConcept(Concept)
	 * @verifies should give an empty list if concept id is null
	 */
	@Test
	public void getSetsContainingConcept_shouldGiveAnEmptyListIfConceptIdIsNull() throws Exception {
		List<ConceptSet> conceptSets = conceptService.getSetsContainingConcept(new Concept());
		assertEquals(conceptSets, Collections.emptyList());
	}
	
	/**
	 * @see ConceptServiceImpl#getConcepts(String, Locale, boolean)
	 * @verifies give a list of ConceptSearchResult for the matching Concepts
	 */
	@Test
	public void getConcepts_shouldGiveAListOfConceptSearchResultForTheMatchingConcepts() throws Exception {
		Locale locale = new Locale("en", "GB");
		String phrase = "CD4 COUNT";
		List<ConceptSearchResult> res = conceptService.getConcepts(phrase, locale, true);
		assertEquals(res.get(0).getConceptName().getName(), phrase);
	}
	
	/**
	 * @see ConceptServiceImpl#getDrugByIngredients(Concept)
	 * @verifies should raise exception if no concept is given
	 */
	@Test
	public void getDrugsByIngredient_shouldRaiseExceptionIfNoConceptIsGiven() throws Exception {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("ingredient is required");
		conceptService.getDrugsByIngredient(null);
	}
	
	/**
	 * @see ConceptServiceImpl#getAllConceptProposals(boolean)
	 * @verifies should return all concept proposals including retired ones when given true
	 */
	@Test
	public void getAllConceptProposals_shouldReturnAllConceptProposalsIncludingRetiredOnesWhenGivenTrue() throws Exception {
		int matchedConceptProposals = 2;
		List<ConceptProposal> conceptProposals = conceptService.getAllConceptProposals(true);
		assertEquals(matchedConceptProposals, conceptProposals.size());
	}
	
	/**
	 * @see ConceptServiceImpl#getAllConceptProposals(boolean)
	 * @verifies should return all concept proposals excluding retired ones when given false
	 */
	@Test
	public void getAllConceptProposals_shouldReturnAllConceptProposalsExcludingRetiredOnesWhenGivenFalse() throws Exception {
		int matchedConceptProposals = 1;
		List<ConceptProposal> conceptProposals = conceptService.getAllConceptProposals(false);
		assertEquals(matchedConceptProposals, conceptProposals.size());
	}
	
	/**
	 * @see ConceptServiceImpl#purgeConceptProposal(ConceptProposal)
	 * @verifies should purge the given concept proposal
	 */
	@Test
	public void purgeConceptProposal_shouldPurgeTheGivenConceptProposal() throws Exception {
		int conceptProposalId = 2;
		conceptService.purgeConceptProposal(conceptService.getConceptProposal(conceptProposalId));
		assertNull(conceptService.getConceptProposal(conceptProposalId));
	}
	
	/**
	 * @see ConceptServiceImpl#getPrevConcept(Concept)
	 * @verifies should return the concept previous to the given concept
	 */
	@Test
	public void getPrevConcept_shouldReturnTheConceptPreviousToTheGivenConcept() throws Exception {
		Integer conceptId = 4;
		Integer prevConceptId = 3;
		Concept returnedConcept = conceptService.getPrevConcept(conceptService.getConcept(conceptId));
		assertEquals(returnedConcept, conceptService.getConcept(prevConceptId));
	}
	
	/**
	 * @see ConceptServiceImpl#getNextConcept(Concept)
	 * @verifies should return the concept next to the given concept
	 */
	@Test
	public void getNextConcept_shouldReturnTheConceptNextToTheGivenConcept() throws Exception {
		Integer conceptId = 3;
		Integer nextConceptId = 4;
		Concept returnedConcept = conceptService.getNextConcept(conceptService.getConcept(conceptId));
		assertEquals(returnedConcept, conceptService.getConcept(nextConceptId));
	}
	
	/**
	 * @see ConceptServiceImpl#getConceptsWithDrugsInFormulary()
	 * @verifies should give a list of all matching concepts
	 */
	@Test
	public void getConceptsWithDrugsInFormulary_shouldGiveAListOfAllMatchingConcepts() throws Exception {
		int matchingConcepts = 2;
		List<Concept> concepts = conceptService.getConceptsWithDrugsInFormulary();
		assertEquals(matchingConcepts, concepts.size());
	}
	
	/**
	 * @see ConceptService#getConceptsByAnswer(Concept)
	 * @verifies should return an empty list if concept id is null
	 */
	@Test
	public void getConceptsByAnswer_shouldReturnAnEmptyListIfConceptIdIsNull() throws Exception {
		List<Concept> concepts = conceptService.getConceptsByAnswer(new Concept());
		assertEquals(concepts, Collections.emptyList());
	}
	
	/**
	 * @see ConceptServiceImpl#getMaxConceptId()
	 * @verifies should give the max number of conceptId
	 */
	@Test
	public void getMaxConceptId_shouldGiveTheMaximumConceptId() throws Exception {
		int maxConceptId = 5497;
		assertEquals(new Integer(maxConceptId), conceptService.getMaxConceptId());
	}
	
	/**
	 * @see ConceptServiceImpl#getLocalesOfConceptNames()
	 * @verifies should return a list of matching locales
	 */
	@Test
	public void getLocalesOfConceptNames_shouldReturnAListOfMatchingLocales() throws Exception {
		Locale localeToSearch = new Locale("en", "GB");
		Set<Locale> locales = conceptService.getLocalesOfConceptNames();
		assertTrue(locales.contains(localeToSearch));
	}
	
	/**
	 * @see ConceptServiceImpl#getAllConceptSources(boolean)
	 * @verifies should return all concept sources including retired ones when given true
	 */
	@Test
	public void getAllConceptSources_shouldReturnAllConceptSourcesIncludingRetiredOnesWhenGivenTrue() throws Exception {
		int conceptSourcesInDataset = 5;
		List<ConceptSource> conceptSources = conceptService.getAllConceptSources(true);
		assertEquals(conceptSourcesInDataset, conceptSources.size());
	}
	
	/**
	 * @see ConceptServiceImpl#getAllConceptSources(boolean)
	 * @verifies should return all concept sources excluding retired ones when given false
	 */
	@Test
	public void getAllConceptSources_shouldReturnAllConceptSourcesExcludingRetiredOnesWhenGivenFalse() throws Exception {
		int conceptSourcesInDataset = 3;
		List<ConceptSource> conceptSources = conceptService.getAllConceptSources(false);
		assertEquals(conceptSourcesInDataset, conceptSources.size());
	}
	
	/**
	 * @see ConceptServiceImpl#getAllConceptNameTags()
	 * @verifies should return a list of all concept name tags
	 */
	@Test
	public void getAllConceptNameTags_shouldReturnAListOfAllConceptNameTags() throws Exception {
		int conceptNameTagsInDataset = 15;
		List<ConceptNameTag> conceptNameTags = conceptService.getAllConceptNameTags();
		assertEquals(conceptNameTagsInDataset, conceptNameTags.size());
	}
	
	/**
	 * @see ConceptServiceImpl#purgeConceptSource(ConceptSource)
	 * @verifies should purge the given concept source
	 */
	@Test
	public void purgeConceptSource_shouldPurgetTheGivenConceptSource() throws Exception {
		Integer conceptSourceId = 1;
		ConceptSource conceptSource = conceptService.getConceptSource(conceptSourceId);
		conceptService.purgeConceptSource(conceptSource);
		assertNull(conceptService.getConceptSource(conceptSourceId));
	}
	
	/**
	 * @see ConceptServiceImpl#purgeConceptMapType(ConceptMapType)
	 * @verifies should delete the specified conceptMapType from the database
	 */
	@Test
	public void purgeConceptMapType_shouldDeleteTheSpecifiedConceptMapTypeFromTheDatabase() throws Exception {
		Integer conceptMapTypeId = 8;
		ConceptMapType mapType = conceptService.getConceptMapType(conceptMapTypeId);
		assertNotNull(mapType);
		conceptService.purgeConceptMapType(mapType);
		assertNull(conceptService.getConceptMapType(conceptMapTypeId));
	}
	
	/**
	 * @see ConceptServiceImpl#purgeConceptReferenceTerm(ConceptReferenceTerm)
	 * @verifies should purge the given concept reference term
	 */
	@Test
	public void purgeConceptReferenceTerm_shouldPurgeTheGivenConceptReferenceTerm() throws Exception {
		Integer conceptReferenceTermId = 11;
		ConceptReferenceTerm refTerm = conceptService.getConceptReferenceTerm(conceptReferenceTermId);
		conceptService.purgeConceptReferenceTerm(refTerm);
		assertNull(conceptService.getConceptReferenceTerm(conceptReferenceTermId));
	}
	
	/**
	 * @see ConceptServiceImpl#getConceptReferenceTermByName(String, ConceptSource)
	 * @verifies return null if no concept reference term is found
	 */
	@Test
	public void getConceptReferenceTermByName_shouldReturnNullIfNoConceptReferenceTermIsFound() throws Exception {
		assertNull(conceptService.getConceptReferenceTermByName(null, new ConceptSource()));
	}
	
	/**
	 * @see ConceptServiceImpl#purgeConceptReferenceTerm(ConceptReferenceTerm)
	 * @verifies should fail if given concept reference term is in use
	 */
	@Test
	public void purgeConceptReferenceTerm_shouldFailIfGivenConceptReferenceTermIsInUse() throws Exception {
		ConceptReferenceTerm refTerm = conceptService.getConceptReferenceTerm(1);
		assertNotNull(refTerm);
		expectedException.expect(APIException.class);
		expectedException.expectMessage("Reference term is in use");
		conceptService.purgeConceptReferenceTerm(refTerm);
	}
	
	/**
	 * @see ConceptServiceImpl#findConceptAnswers(String, Locale, Concept)
	 * @verifies should return a list of all matching concept search results
	 */
	@Test
	public void findConceptAnswers_shouldReturnAListOfAllMatchingConceptSearchResults() throws Exception {
		Locale locale = new Locale("en", "GB");
		String phrase = "CD4 COUNT";
		int conceptId = 5497;
		List<ConceptSearchResult> concepts = conceptService.findConceptAnswers(phrase, locale,
		    conceptService.getConcept(conceptId));
		assertEquals(concepts.get(0).getConceptName().getName(), phrase);
	}
	
	/**
	 * @see ConceptServiceImpl#getOrderableConcepts(String, List, boolean, Integer, Integer)
	 * @verifies should return an empty list if no concept search result is found
	 */
	@Test
	public void getOrderableConcepts_shouldReturnAnEmptyListIfNoConceptSearchResultIsFound() throws Exception {
		Integer someStartLength = 0;
		Integer someEndLength = 10;
		List<ConceptSearchResult> result = conceptService.getOrderableConcepts("some phrase", null, true, someStartLength,
		    someEndLength);
		assertEquals(result, Collections.emptyList());
	}
	
	/**
	 * @see ConceptServiceImpl#mapConceptProposalToConcept(ConceptProposal, Concept, Locale)
	 * @verifies should throw APIException when mapping to null concept
	 */
	@Test
	public void mapConceptProposalToConcept_shouldThrowAPIExceptionWhenMappingToNullConcept() throws Exception {
		ConceptProposal cp = conceptService.getConceptProposal(2);
		Locale locale = new Locale("en", "GB");
		expectedException.expect(APIException.class);
		conceptService.mapConceptProposalToConcept(cp, null, locale);
	}
	
	/**
	 * @see ConceptServiceImpl#getCountOfDrugs(String, Concept, boolean, boolean, boolean)
	 * @verifies should return the total number of matching drugs
	 */
	@Test
	public void getCountOfDrugs_shouldReturnTheTotalNumberOfMatchingNumbers() throws Exception {
		String phrase = "Triomune-30";
		int conceptId = 792;
		assertEquals(new Integer(1),
		    conceptService.getCountOfDrugs(phrase, conceptService.getConcept(conceptId), true, true, true));
	}
}
