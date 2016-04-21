package org.motechproject.openmrs19.tasks.impl;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.motechproject.openmrs19.domain.Concept;
import org.motechproject.openmrs19.domain.ConceptName;
import org.motechproject.openmrs19.domain.Encounter;
import org.motechproject.openmrs19.domain.EncounterType;
import org.motechproject.openmrs19.domain.Identifier;
import org.motechproject.openmrs19.domain.IdentifierType;
import org.motechproject.openmrs19.domain.Location;
import org.motechproject.openmrs19.domain.Observation;
import org.motechproject.openmrs19.domain.Patient;
import org.motechproject.openmrs19.domain.Person;
import org.motechproject.openmrs19.domain.Provider;
import org.motechproject.openmrs19.service.OpenMRSConceptService;
import org.motechproject.openmrs19.service.OpenMRSEncounterService;
import org.motechproject.openmrs19.service.OpenMRSLocationService;
import org.motechproject.openmrs19.service.OpenMRSPatientService;
import org.motechproject.openmrs19.service.OpenMRSProviderService;
import org.motechproject.openmrs19.tasks.OpenMRSActionProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the {@link org.motechproject.openmrs19.tasks.OpenMRSActionProxyService} interface.
 */
@Service("openMRSActionProxyService")
public class OpenMRSActionProxyServiceImpl implements OpenMRSActionProxyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenMRSActionProxyServiceImpl.class);

    private OpenMRSConceptService conceptService;
    private OpenMRSEncounterService encounterService;
    private OpenMRSLocationService locationService;
    private OpenMRSPatientService patientService;
    private OpenMRSProviderService providerService;

    @Override
    public void createEncounter(DateTime encounterDatetime, String encounterType, String locationName, String patientUuid, String providerUuid, Map<String, String> observations) {
        Location location = getLocationByName(locationName);
        Patient patient = patientService.getPatientByUuid(null, patientUuid);
        Provider provider = providerService.getProviderByUuid(null, providerUuid);

        List<Observation> observationList = convertObservationMapToList(observations, encounterDatetime);

        EncounterType type = new EncounterType(encounterType);

        Encounter encounter = new Encounter(location, type, encounterDatetime.toDate(), patient, provider.getPerson(), observationList);
        encounterService.createEncounter(null, encounter);
    }

    @Override
    public void createPatient(String givenName, String middleName, String familyName, String address, DateTime birthdate,
                              Boolean birthdateEstimated, String gender, Boolean dead, String causeOfDeathUUID, String motechId,
                              String locationForMotechId, Map<String, String> identifiers) {
        Concept causeOfDeath = StringUtils.isNotEmpty(causeOfDeathUUID) ? conceptService.getConceptByUuid(null, causeOfDeathUUID) : null;

        Person person = new Person();

        Person.Name personName = new Person.Name();
        personName.setGivenName(givenName);
        personName.setMiddleName(middleName);
        personName.setFamilyName(familyName);
        person.setPreferredName(personName);
        person.setNames(Collections.singletonList(personName));

        Person.Address personAddress = new Person.Address();
        personAddress.setAddress1(address);
        person.setPreferredAddress(personAddress);
        person.setAddresses(Collections.singletonList(personAddress));

        person.setBirthdate(birthdate.toDate());
        person.setBirthdateEstimated(birthdateEstimated);
        person.setDead(dead);
        person.setCauseOfDeath(causeOfDeath);
        person.setGender(gender);

        Location location = StringUtils.isNotEmpty(locationForMotechId) ? getLocationByName(locationForMotechId) : getDefaultLocation();

        List<Identifier> identifierList = convertIdentifierMapToList(identifiers);

        Patient patient = new Patient(identifierList, person, motechId, location);
        patientService.createPatient(null, patient);
    }

    private Location getDefaultLocation() {
        return getLocationByName(DEFAULT_LOCATION_NAME);
    }

    private Location getLocationByName(String locationName) {
        Location location = null;

        if (StringUtils.isNotEmpty(locationName)) {
            List<Location> locations = locationService.getLocations(null, locationName);
            if (locations.isEmpty()) {
                LOGGER.warn("There is no location with name {}", locationName);
            } else {
                if (locations.size() > 1) {
                    LOGGER.warn("There is more than one location with name {}.", locationName);
                }
                location = locations.get(0);
            }
        }

        return location;
    }

    private List<Identifier> convertIdentifierMapToList(Map<String, String> identifiers) {
        List<Identifier> identifierList = new ArrayList<>();

        for (String identifierTypeName : identifiers.keySet()) {
            IdentifierType identifierType = new IdentifierType();
            identifierType.setName(identifierTypeName);

            Identifier identifier = new Identifier(identifiers.get(identifierTypeName), identifierType);

            identifierList.add(identifier);
        }

        return identifierList;
    }

    private List<Observation> convertObservationMapToList(Map<String, String> observations, DateTime obsDatetime) {
        List<Observation> observationList = new ArrayList<>();

        for (String observationConceptName : observations.keySet()) {
            Observation observation = new Observation();

            ConceptName conceptName = new ConceptName(observationConceptName);
            Concept concept = new Concept(conceptName);
            observation.setConcept(concept);

            String observationMapValue = observations.get(observationConceptName);
            Observation.ObservationValue observationValue = new Observation.ObservationValue(observationMapValue);
            observation.setValue(observationValue);

            observation.setObsDatetime(obsDatetime.toDate());

            observationList.add(observation);
        }
        return observationList;
    }

    @Autowired
    public void setConceptService(OpenMRSConceptService conceptService) {
        this.conceptService = conceptService;
    }

    @Autowired
    public void setEncounterService(OpenMRSEncounterService encounterService) {
        this.encounterService = encounterService;
    }

    @Autowired
    public void setLocationService(OpenMRSLocationService locationService) {
        this.locationService = locationService;
    }

    @Autowired
    public void setPatientService(OpenMRSPatientService patientService) {
        this.patientService = patientService;
    }

    @Autowired
    public void setProviderService(OpenMRSProviderService providerService) {
        this.providerService = providerService;
    }
}
