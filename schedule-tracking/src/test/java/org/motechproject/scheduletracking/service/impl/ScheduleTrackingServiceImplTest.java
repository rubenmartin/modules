package org.motechproject.scheduletracking.service.impl;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.motechproject.commons.date.model.Time;
import org.motechproject.commons.date.util.DateUtil;
import org.motechproject.scheduler.service.MotechSchedulerService;
import org.motechproject.scheduletracking.domain.Alert;
import org.motechproject.scheduletracking.domain.Enrollment;
import org.motechproject.scheduletracking.domain.EnrollmentBuilder;
import org.motechproject.scheduletracking.domain.EnrollmentStatus;
import org.motechproject.scheduletracking.domain.Milestone;
import org.motechproject.scheduletracking.domain.MilestoneFulfillment;
import org.motechproject.scheduletracking.domain.Schedule;
import org.motechproject.scheduletracking.domain.ScheduleFactory;
import org.motechproject.scheduletracking.domain.WindowName;
import org.motechproject.scheduletracking.domain.exception.InvalidEnrollmentException;
import org.motechproject.scheduletracking.domain.exception.ScheduleTrackingException;
import org.motechproject.scheduletracking.repository.dataservices.EnrollmentDataService;
import org.motechproject.scheduletracking.service.EnrollmentRecord;
import org.motechproject.scheduletracking.service.EnrollmentRequest;
import org.motechproject.scheduletracking.service.EnrollmentsQuery;
import org.motechproject.scheduletracking.repository.dataservices.ScheduleDataService;
import org.motechproject.scheduletracking.service.ScheduleTrackingService;
import org.motechproject.scheduletracking.service.contract.UpdateCriteria;
import org.motechproject.server.config.SettingsFacade;
import org.motechproject.server.config.domain.SettingsRecord;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.motechproject.commons.date.util.DateUtil.newDate;
import static org.motechproject.commons.date.util.DateUtil.newDateTime;
import static org.motechproject.commons.date.util.DateUtil.now;
import static org.motechproject.commons.date.util.DateUtil.today;
import static org.motechproject.scheduletracking.utility.DateTimeUtil.weeksAgo;
import static org.motechproject.scheduletracking.utility.PeriodUtil.days;
import static org.motechproject.scheduletracking.utility.PeriodUtil.weeks;

public class ScheduleTrackingServiceImplTest {

    private ScheduleTrackingService scheduleTrackingService;

    @Mock
    private ScheduleDataService scheduleDataService;
    @Mock
    private EnrollmentDataService enrollmentDataService;
    @Mock
    private MotechSchedulerService schedulerService;
    @Mock
    private EnrollmentServiceImpl enrollmentService;
    @Mock
    private EnrollmentsQueryService enrollmentsQueryService;
    @Mock
    private EnrollmentRecordMapper enrollmentRecordMapper;
    @Mock
    private SettingsFacade settingsFacade;
    @Mock
    private SettingsRecord settings;

    private ScheduleFactory scheduleFactory;

    public static final Map<String, String> EMPTY_METADATA_LIST = new HashMap<String, String>();

    @Before
    public void setUp() {
        initMocks(this);
        scheduleFactory = new ScheduleFactory();
        scheduleTrackingService = new ScheduleTrackingServiceImpl(enrollmentsQueryService, scheduleDataService,
                enrollmentService, enrollmentRecordMapper, enrollmentDataService, settingsFacade);
    }

    @Test
    public void shouldEnrollEntityIntoFirstMilestoneOfSchedule() {
        String scheduleName = "my_schedule";
        Schedule schedule = new Schedule(scheduleName);
        Milestone secondMilestone = new Milestone("second_milestone", weeks(1), weeks(1), weeks(1), weeks(1));
        Milestone firstMilestone = new Milestone("first_milestone", weeks(1), weeks(1), weeks(1), weeks(1));
        schedule.addMilestones(firstMilestone, secondMilestone);
        when(scheduleDataService.findByName(scheduleName)).thenReturn(schedule);

        String externalId = "my_entity_1";
        DateTime referenceDateTime = now().minusDays(10);
        Time preferredAlertTime = new Time(8, 10);
        scheduleTrackingService.enroll(new EnrollmentRequest().setExternalId(externalId).setScheduleName(scheduleName).setPreferredAlertTime(preferredAlertTime).setReferenceDate(referenceDateTime.toLocalDate()));

        verify(enrollmentService).enroll(externalId, scheduleName, firstMilestone.getName(), newDateTime(referenceDateTime.toLocalDate(), new Time(0, 0)), newDateTime(now().toLocalDate(), new Time(0, 0)), preferredAlertTime, EMPTY_METADATA_LIST);
    }

    @Test
    public void shouldEnrollEntityIntoGivenMilestoneOfTheSchedule() {
        Milestone secondMilestone = new Milestone("second_milestone", weeks(1), weeks(1), weeks(1), weeks(1));
        Milestone firstMilestone = new Milestone("first_milestone", weeks(1), weeks(1), weeks(1), weeks(1));
        String scheduleName = "my_schedule";
        Schedule schedule = new Schedule(scheduleName);
        schedule.addMilestones(firstMilestone, secondMilestone);
        when(scheduleDataService.findByName(scheduleName)).thenReturn(schedule);

        String externalId = "entity_1";
        Time preferredAlertTime = new Time(8, 10);
        DateTime referenceDateTime = newDateTime(2012, 11, 2, 0, 0, 0);
        scheduleTrackingService.enroll(new EnrollmentRequest().setExternalId(externalId).setScheduleName(scheduleName).setPreferredAlertTime(preferredAlertTime).setReferenceDate(referenceDateTime.toLocalDate()).setStartingMilestoneName(secondMilestone.getName()));

        verify(enrollmentService).enroll(externalId, scheduleName, secondMilestone.getName(), newDateTime(referenceDateTime.toLocalDate(), new Time(0, 0)), newDateTime(now().toLocalDate(), new Time(0, 0)), preferredAlertTime, EMPTY_METADATA_LIST);
    }

    @Test
    public void shouldEnrollEntityIntoAScheduleWithMetadata() {
        Schedule schedule = new Schedule("my_schedule");
        schedule.addMilestones(new Milestone("milestone1", weeks(1), weeks(1), weeks(1), weeks(1)));
        when(scheduleDataService.findByName("my_schedule")).thenReturn(schedule);

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("foo", "bar");
        metadata.put("fuu", "baz");
        scheduleTrackingService.enroll(new EnrollmentRequest().setExternalId("entity_1").setScheduleName("my_schedule").setPreferredAlertTime(new Time(8, 10)).setReferenceDate(newDateTime(2012, 11, 2, 0, 0, 0).toLocalDate()).setStartingMilestoneName("milestone1").setMetadata(metadata));

        Map<String, String> expectedMetadata = new HashMap<String, String>();
        expectedMetadata.put("foo", "bar");
        expectedMetadata.put("fuu", "baz");

        verify(enrollmentService).enroll("entity_1", "my_schedule", "milestone1", newDateTime(newDateTime(2012, 11, 2, 0, 0, 0).toLocalDate(), new Time(0, 0)), newDateTime(now().toLocalDate(), new Time(0, 0)), new Time(8, 10), expectedMetadata);
    }

    @Test
    public void shouldScheduleOneRepeatJobForTheSingleAlertInTheFirstMilestone() {
        Milestone milestone = new Milestone("milestone", weeks(1), weeks(1), weeks(1), weeks(1));
        milestone.addAlert(WindowName.earliest, new Alert(days(0), days(1), 3, 0, false));
        String scheduleName = "my_schedule";
        Schedule schedule = new Schedule(scheduleName);
        schedule.addMilestones(milestone);
        when(scheduleDataService.findByName(scheduleName)).thenReturn(schedule);

        String externalId = "entity_1";
        DateTime referenceDateTime = newDateTime(2012, 11, 2, 0, 0, 0);
        Time preferredAlertTime = new Time(8, 10);
        scheduleTrackingService.enroll(new EnrollmentRequest().setExternalId(externalId).setScheduleName(scheduleName).setPreferredAlertTime(preferredAlertTime).setReferenceDate(referenceDateTime.toLocalDate()));

        verify(enrollmentService).enroll(externalId, scheduleName, milestone.getName(), newDateTime(referenceDateTime.toLocalDate(), new Time(0, 0)), newDateTime(now().toLocalDate(), new Time(0, 0)), preferredAlertTime, EMPTY_METADATA_LIST);
    }

    @Test
    public void shouldFulfillTheCurrentMilestone() {
        Milestone milestone = new Milestone("milestone", weeks(1), weeks(1), weeks(1), weeks(1));
        milestone.addAlert(WindowName.earliest, new Alert(days(0), days(1), 3, 0, false));
        Schedule schedule = new Schedule("my_schedule");
        schedule.addMilestones(milestone);
        when(scheduleDataService.findByName("my_schedule")).thenReturn(schedule);

        when(enrollmentDataService.findByExternalIdScheduleNameAndStatus("entity_1", "my_schedule", EnrollmentStatus.ACTIVE)).thenReturn(null);
        scheduleTrackingService.enroll(new EnrollmentRequest().setExternalId("entity_1").setScheduleName("my_schedule").setPreferredAlertTime(new Time(8, 10)).setReferenceDate(new LocalDate(2012, 11, 2)));

        Enrollment enrollment = mock(Enrollment.class);
        when(enrollmentDataService.findByExternalIdScheduleNameAndStatus("entity_1", "my_schedule", EnrollmentStatus.ACTIVE)).thenReturn(enrollment);

        scheduleTrackingService.fulfillCurrentMilestone("entity_1", "my_schedule", today(), new Time(0, 0));

        verify(enrollmentService).fulfillCurrentMilestone(enrollment, newDateTime(now().toLocalDate(), new Time(0, 0)));
    }

    @Test
    public void shouldFulfillTheCurrentMilestoneDefaultingTheTimeComponent() {
        Enrollment enrollment = mock(Enrollment.class);
        MilestoneFulfillment fulfillment = mock(MilestoneFulfillment.class);
        when(enrollment.getFulfillments()).thenReturn(asList(new MilestoneFulfillment[]{fulfillment}));
        when(enrollment.getLastFulfilledDate()).thenReturn(newDateTime(2012, 2, 10, 8, 20, 0));
        when(enrollmentDataService.findByExternalIdScheduleNameAndStatus("entity_1", "my_schedule", EnrollmentStatus.ACTIVE)).thenReturn(enrollment);

        scheduleTrackingService.fulfillCurrentMilestone("entity_1", "my_schedule", newDate(2012, 2, 10));

        verify(enrollmentService).fulfillCurrentMilestone(enrollment, newDateTime(2012, 2, 10, new Time(0, 0)));
    }

    @Test
    public void shouldNotFulfillTheCurrentMilestoneIftheFulfillmentDateTimeMatchesLastMilestoneFulfillmentDate() {
        Enrollment enrollment = mock(Enrollment.class);
        MilestoneFulfillment fulfillment = mock(MilestoneFulfillment.class);
        when(enrollment.getFulfillments()).thenReturn(asList(new MilestoneFulfillment[]{fulfillment}));
        when(enrollment.getLastFulfilledDate()).thenReturn(newDateTime(2012, 2, 10, 8, 20, 0));
        when(enrollmentDataService.findByExternalIdScheduleNameAndStatus("entity_1", "my_schedule", EnrollmentStatus.ACTIVE)).thenReturn(enrollment);

        scheduleTrackingService.fulfillCurrentMilestone("entity_1", "my_schedule", newDate(2012, 2, 10), new Time(8, 20));

        verifyZeroInteractions(enrollmentService);
    }

    @Test
    public void shouldFulfillTheCurrentMilestoneWithTheSpecifiedDateOnlyUsingDefaultTime() {
        Milestone milestone = new Milestone("milestone", weeks(1), weeks(1), weeks(1), weeks(1));
        milestone.addAlert(WindowName.earliest, new Alert(days(0), days(1), 3, 0, false));
        Schedule schedule = new Schedule("my_schedule");
        schedule.addMilestones(milestone);
        when(scheduleDataService.findByName("my_schedule")).thenReturn(schedule);

        when(enrollmentDataService.findByExternalIdScheduleNameAndStatus("entity_1", "my_schedule", EnrollmentStatus.ACTIVE)).thenReturn(null);
        scheduleTrackingService.enroll(new EnrollmentRequest().setExternalId("entity_1").setScheduleName("my_schedule").setPreferredAlertTime(new Time(8, 10)).setReferenceDate(new LocalDate(2012, 11, 2)).setReferenceTime(null).setEnrollmentDate(null).setEnrollmentTime(null).setStartingMilestoneName(null).setMetadata(null));

        Enrollment enrollment = mock(Enrollment.class);
        when(enrollmentDataService.findByExternalIdScheduleNameAndStatus("entity_1", "my_schedule", EnrollmentStatus.ACTIVE)).thenReturn(enrollment);

        DateTime fulfillmentDateTime = newDateTime(2012, 12, 10, 0, 0, 0);
        scheduleTrackingService.fulfillCurrentMilestone("entity_1", "my_schedule", fulfillmentDateTime.toLocalDate(), new Time(0, 0));

        verify(enrollmentService).fulfillCurrentMilestone(enrollment, fulfillmentDateTime);
    }

    @Test
    public void shouldFulfillTheCurrentMilestoneWithTheSpecifiedDateAndTime() {
        Milestone milestone = new Milestone("milestone", weeks(1), weeks(1), weeks(1), weeks(1));
        milestone.addAlert(WindowName.earliest, new Alert(days(0), days(1), 3, 0, false));
        Schedule schedule = new Schedule("my_schedule");
        schedule.addMilestones(milestone);
        when(scheduleDataService.findByName("my_schedule")).thenReturn(schedule);

        when(enrollmentDataService.findByExternalIdScheduleNameAndStatus("entity_1", "my_schedule", EnrollmentStatus.ACTIVE)).thenReturn(null);
        scheduleTrackingService.enroll(new EnrollmentRequest().setExternalId("entity_1").setScheduleName("my_schedule").setPreferredAlertTime(new Time(8, 10)).setReferenceDate(new LocalDate(2012, 11, 2)).setReferenceTime(null).setEnrollmentDate(null).setEnrollmentTime(null).setStartingMilestoneName(null).setMetadata(null));

        Enrollment enrollment = mock(Enrollment.class);
        when(enrollmentDataService.findByExternalIdScheduleNameAndStatus("entity_1", "my_schedule", EnrollmentStatus.ACTIVE)).thenReturn(enrollment);

        scheduleTrackingService.fulfillCurrentMilestone("entity_1", "my_schedule", newDate(2012, 12, 10), new Time(3, 30));

        verify(enrollmentService).fulfillCurrentMilestone(enrollment, newDateTime(2012, 12, 10, 3, 30, 0));
    }

    @Test(expected = InvalidEnrollmentException.class)
    public void shouldFailToFulfillCurrentMilestoneIfItIsNotFoundOrNotActive() {
        when(enrollmentDataService.findByExternalIdScheduleNameAndStatus("WRONG-ID", "WRONG-NAME", EnrollmentStatus.ACTIVE)).thenReturn(null);

        scheduleTrackingService.fulfillCurrentMilestone("WRONG-ID", "WRONG-NAME", today(), new Time(0, 0));

        verifyZeroInteractions(enrollmentService);
    }

    @Test
    public void shouldUnenrollEntityFromTheSchedule() {
        Milestone milestone = new Milestone("milestone", weeks(1), weeks(1), weeks(1), weeks(1));
        milestone.addAlert(WindowName.earliest, new Alert(days(0), days(1), 3, 0, false));
        String scheduleName = "my_schedule";
        Schedule schedule = new Schedule(scheduleName);
        schedule.addMilestones(milestone);
        when(scheduleDataService.findByName(scheduleName)).thenReturn(schedule);

        String externalId = "entity_1";
        Enrollment enrollment = new EnrollmentBuilder().withExternalId("entity_1").withSchedule(schedule).withCurrentMilestoneName("milestone").withStartOfSchedule(weeksAgo(4)).withEnrolledOn(weeksAgo(4)).withPreferredAlertTime(new Time(8, 10)).withStatus(EnrollmentStatus.ACTIVE).withMetadata(null).toEnrollment();
        when(enrollmentDataService.findByExternalIdScheduleNameAndStatus("entity_1", "my_schedule", EnrollmentStatus.ACTIVE)).thenReturn(enrollment);
        scheduleTrackingService.unenroll(externalId, Arrays.asList(scheduleName));

        verify(enrollmentService).unenroll(enrollment);
    }

    @Test
    public void shouldSafelyUnenrollEntityFromListOfSchedule() {
        Milestone milestone1 = new Milestone("milestone1", weeks(1), weeks(1), weeks(1), weeks(1));
        milestone1.addAlert(WindowName.earliest, new Alert(days(0), days(1), 3, 0, false));
        String schedule1Name = "my_schedule1";
        Schedule schedule1 = new Schedule(schedule1Name);
        schedule1.addMilestones(milestone1);
        when(scheduleDataService.findByName(schedule1Name)).thenReturn(schedule1);

        Milestone milestone2 = new Milestone("milestone2", weeks(1), weeks(1), weeks(1), weeks(1));
        milestone2.addAlert(WindowName.earliest, new Alert(days(0), days(1), 3, 0, false));
        String schedule2Name = "my_schedule2";
        Schedule schedule2 = new Schedule(schedule2Name);
        schedule2.addMilestones(milestone2);
        when(scheduleDataService.findByName(schedule2Name)).thenReturn(schedule2);

        String externalId = "entity_1";
        Enrollment enrollment1 = new EnrollmentBuilder().withExternalId(externalId).withSchedule(schedule1).withCurrentMilestoneName("milestone1").withStartOfSchedule(weeksAgo(4)).withEnrolledOn(weeksAgo(4)).withPreferredAlertTime(new Time(8, 10)).withStatus(EnrollmentStatus.ACTIVE).withMetadata(null).toEnrollment();
        when(enrollmentDataService.findByExternalIdScheduleNameAndStatus(externalId, schedule1Name, EnrollmentStatus.ACTIVE)).thenReturn(enrollment1);
        Enrollment enrollment2 = new EnrollmentBuilder().withExternalId(externalId).withSchedule(schedule2).withCurrentMilestoneName("milestone2").withStartOfSchedule(weeksAgo(4)).withEnrolledOn(weeksAgo(4)).withPreferredAlertTime(new Time(8, 10)).withStatus(EnrollmentStatus.ACTIVE).withMetadata(null).toEnrollment();
        when(enrollmentDataService.findByExternalIdScheduleNameAndStatus(externalId, schedule2Name, EnrollmentStatus.ACTIVE)).thenReturn(enrollment2);

        scheduleTrackingService.unenroll(externalId, Arrays.asList(schedule1Name, schedule2Name));

        verify(enrollmentService).unenroll(enrollment1);
        verify(enrollmentService).unenroll(enrollment2);
    }

    @Test
    public void shouldNotThrowAnyExceptionIfEntityIsNotEnrolledIntoSchedule() {
        Milestone milestone = new Milestone("milestone", weeks(1), weeks(1), weeks(1), weeks(1));
        milestone.addAlert(WindowName.earliest, new Alert(days(0), days(1), 3, 0, false));
        String scheduleName = "scheduleName";
        Schedule schedule = new Schedule(scheduleName);
        schedule.addMilestones(milestone);
        when(scheduleDataService.findByName(scheduleName)).thenReturn(schedule);

        when(enrollmentDataService.findByExternalIdScheduleNameAndStatus("entity_1", scheduleName, EnrollmentStatus.ACTIVE)).thenReturn(null);
        scheduleTrackingService.unenroll("entity_1", Arrays.asList(scheduleName));
    }

    @Test
    public void shouldReturnEnrollmentDetails() {
        String externalId = "external id";
        String scheduleName = "schedule name";
        Schedule schedule = new Schedule("some_schedule");
        final Enrollment enrollment = new EnrollmentBuilder().withExternalId(externalId).withSchedule(schedule).withCurrentMilestoneName(null).withStartOfSchedule(null).withEnrolledOn(null).withPreferredAlertTime(null).withStatus(EnrollmentStatus.ACTIVE).withMetadata(null).toEnrollment();
        when(enrollmentDataService.findByExternalIdScheduleNameAndStatus(externalId, scheduleName, EnrollmentStatus.ACTIVE)).thenReturn(enrollment);

        EnrollmentRecord record = mock(EnrollmentRecord.class);
        when(enrollmentRecordMapper.map(enrollment)).thenReturn(record);

        assertEquals(record, scheduleTrackingService.getEnrollment(externalId, scheduleName));
    }

    @Test(expected = InvalidEnrollmentException.class)
    public void shouldNotFulfillAnyInactiveEnrollment() {
        String externalId = "externalId";
        String scheduleName = "scheduleName";
        when(enrollmentDataService.findByExternalIdScheduleNameAndStatus(externalId, scheduleName, EnrollmentStatus.ACTIVE)).thenReturn(null);

        scheduleTrackingService.fulfillCurrentMilestone(externalId, scheduleName, today(), new Time(0, 0));
    }

    @Test
    public void shouldReturnListOfExternalIdsForTheGivenQuery() {
        EnrollmentsQuery enrollmentQuery = mock(EnrollmentsQuery.class);
        Enrollment enrollment1 = mock(Enrollment.class);
        Enrollment enrollment2 = mock(Enrollment.class);
        List<Enrollment> enrollments = asList(enrollment1, enrollment2);
        when(enrollmentsQueryService.search(enrollmentQuery)).thenReturn(enrollments);

        EnrollmentRecord record1 = mock(EnrollmentRecord.class);
        when(enrollmentRecordMapper.map(enrollment1)).thenReturn(record1);

        EnrollmentRecord record2 = mock(EnrollmentRecord.class);
        when(enrollmentRecordMapper.map(enrollment2)).thenReturn(record2);

        assertEquals(asList(new EnrollmentRecord[]{record1, record2}), scheduleTrackingService.search(enrollmentQuery));
    }

    @Test
    public void shouldReturnListOfEnrollmentRecordsForTheGivenQuery() {
        Schedule schedule = new Schedule("some_schedule");
        EnrollmentsQuery enrollmentQuery = mock(EnrollmentsQuery.class);
        Enrollment enrollment1 = new EnrollmentBuilder().withExternalId("external_id_1").withSchedule(schedule).withCurrentMilestoneName(null).withStartOfSchedule(null).withEnrolledOn(null).withPreferredAlertTime(null).withStatus(null).withMetadata(null).toEnrollment();
        Enrollment enrollment2 = new EnrollmentBuilder().withExternalId("external_id_2").withSchedule(schedule).withCurrentMilestoneName(null).withStartOfSchedule(null).withEnrolledOn(null).withPreferredAlertTime(null).withStatus(null).withMetadata(null).toEnrollment();
        List<Enrollment> enrollments = asList(enrollment1, enrollment2);

        when(enrollmentsQueryService.search(enrollmentQuery)).thenReturn(enrollments);

        EnrollmentRecord record1 = mock(EnrollmentRecord.class);
        when(enrollmentRecordMapper.mapWithDates(enrollment1)).thenReturn(record1);
        EnrollmentRecord record2 = mock(EnrollmentRecord.class);
        when(enrollmentRecordMapper.mapWithDates(enrollment2)).thenReturn(record2);

        assertEquals(asList(new EnrollmentRecord[]{record1, record2}), scheduleTrackingService.searchWithWindowDates(enrollmentQuery));
    }

    @Test
    public void shouldUpdateValuesOnEnrollment() {
        Schedule schedule = new Schedule("some_schedule");
        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("foo1", "bar1");
        metadata.put("foo2", "bar2");
        Enrollment enrollment = new EnrollmentBuilder().withExternalId("external_id_1").withSchedule(schedule).withCurrentMilestoneName(null).withStartOfSchedule(null).withEnrolledOn(null).withPreferredAlertTime(null).withStatus(null).withMetadata(metadata).toEnrollment();
        HashMap<String, String> toBeUpdatedMetadata = new HashMap<String, String>();
        toBeUpdatedMetadata.put("foo2", "val2");
        toBeUpdatedMetadata.put("foo3", "val3");

        when(enrollmentDataService.findByExternalIdScheduleNameAndStatus("foo", "some_schedule", EnrollmentStatus.ACTIVE)).thenReturn(enrollment);

        ArgumentCaptor<Enrollment> enrollmentArgumentCaptor = ArgumentCaptor.forClass(Enrollment.class);

        scheduleTrackingService.updateEnrollment("foo", "some_schedule", new UpdateCriteria().metadata(toBeUpdatedMetadata));

        verify(enrollmentDataService).update(enrollmentArgumentCaptor.capture());
        Enrollment updatedEnrollment = enrollmentArgumentCaptor.getValue();

        Map<String, String> updatedMetadata = updatedEnrollment.getMetadata();
        assertEquals(3, updatedMetadata.size());
        assertEquals("bar1", updatedMetadata.get("foo1"));
        assertEquals("val2", updatedMetadata.get("foo2"));
        assertEquals("val3", updatedMetadata.get("foo3"));
    }

    @Test(expected = InvalidEnrollmentException.class)
    public void UpdateShouldThrowExceptionForInvalidData() {
        when(enrollmentDataService.findByExternalIdScheduleNameAndStatus("foo", "some_schedule", EnrollmentStatus.ACTIVE)).thenReturn(null);
        scheduleTrackingService.updateEnrollment("foo", "some_schedule", new UpdateCriteria().metadata(new HashMap<String, String>()));
        verifyNoMoreInteractions(enrollmentDataService);
    }

    @Test
    public void shouldInvokeEnrollmentServiceForAlertTimings() {
        String scheduleName = "my_schedule";
        Schedule schedule = new Schedule(scheduleName);
        Milestone secondMilestone = new Milestone("second_milestone", weeks(1), weeks(1), weeks(1), weeks(1));
        Milestone firstMilestone = new Milestone("first_milestone", weeks(1), weeks(1), weeks(1), weeks(1));
        schedule.addMilestones(firstMilestone, secondMilestone);
        when(scheduleDataService.findByName(scheduleName)).thenReturn(schedule);

        String externalId = "my_entity_1";
        LocalDate referenceDate = DateUtil.today();
        Time referenceTime = new Time(8, 10);
        LocalDate enrollmentDate = DateUtil.today();
        Time enrollmentTime = new Time(8, 10);
        Time preferredAlertTime = new Time(8, 10);

        scheduleTrackingService.getAlertTimings(new EnrollmentRequest().setExternalId(externalId).setScheduleName(scheduleName).setPreferredAlertTime(preferredAlertTime).setReferenceDate(referenceDate).setReferenceTime(referenceTime).setEnrollmentDate(enrollmentDate).setEnrollmentTime(enrollmentTime).setStartingMilestoneName("first_milestone").setMetadata(null));

        verify(enrollmentService).getAlertTimings(externalId, scheduleName, firstMilestone.getName(), newDateTime(referenceDate, new Time(8, 10)), newDateTime(now().toLocalDate(), new Time(8, 10)), preferredAlertTime);
    }

    @Test
    public void shouldInvokeEnrollmentServiceForAlertTimingsWithStartingMilestoneFromEnrollmentRequest() {
        String scheduleName = "my_schedule";
        Schedule schedule = new Schedule(scheduleName);
        Milestone secondMilestone = new Milestone("second_milestone", weeks(1), weeks(1), weeks(1), weeks(1));
        Milestone firstMilestone = new Milestone("first_milestone", weeks(1), weeks(1), weeks(1), weeks(1));
        schedule.addMilestones(firstMilestone, secondMilestone);
        when(scheduleDataService.findByName(scheduleName)).thenReturn(schedule);

        String externalId = "my_entity_1";
        LocalDate referenceDate = DateUtil.today();
        Time referenceTime = new Time(8, 10);
        LocalDate enrollmentDate = DateUtil.today();
        Time enrollmentTime = new Time(8, 10);
        Time preferredAlertTime = new Time(8, 10);

        scheduleTrackingService.getAlertTimings(new EnrollmentRequest().setExternalId(externalId).setScheduleName(scheduleName).setPreferredAlertTime(preferredAlertTime).setReferenceDate(referenceDate).setReferenceTime(referenceTime).setEnrollmentDate(enrollmentDate).setEnrollmentTime(enrollmentTime).setStartingMilestoneName("second_milestone").setMetadata(null));

        verify(enrollmentService).getAlertTimings(externalId, scheduleName, secondMilestone.getName(), newDateTime(referenceDate, new Time(8, 10)), newDateTime(now().toLocalDate(), new Time(8, 10)), preferredAlertTime);
    }

    @Test
    public void shouldInvokeEnrollmentServiceForAlertTimingsWithStartingMilestoneFromScheduleFirstMilestone() {
        String scheduleName = "my_schedule";
        Schedule schedule = new Schedule(scheduleName);
        Milestone secondMilestone = new Milestone("second_milestone", weeks(1), weeks(1), weeks(1), weeks(1));
        Milestone firstMilestone = new Milestone("first_milestone", weeks(1), weeks(1), weeks(1), weeks(1));
        schedule.addMilestones(firstMilestone, secondMilestone);
        when(scheduleDataService.findByName(scheduleName)).thenReturn(schedule);

        String externalId = "my_entity_1";
        LocalDate referenceDate = DateUtil.today();
        Time referenceTime = new Time(8, 10);
        LocalDate enrollmentDate = DateUtil.today();
        Time enrollmentTime = new Time(8, 10);
        Time preferredAlertTime = new Time(8, 10);

        scheduleTrackingService.getAlertTimings(new EnrollmentRequest().setExternalId(externalId).setScheduleName(scheduleName).setPreferredAlertTime(preferredAlertTime).setReferenceDate(referenceDate).setReferenceTime(referenceTime).setEnrollmentDate(enrollmentDate).setEnrollmentTime(enrollmentTime).setStartingMilestoneName(null).setMetadata(null));

        verify(enrollmentService).getAlertTimings(externalId, scheduleName, firstMilestone.getName(), newDateTime(referenceDate, new Time(8, 10)), newDateTime(now().toLocalDate(), new Time(8, 10)), preferredAlertTime);
    }

    @Test(expected = ScheduleTrackingException.class)
    public void shouldThrowAnExceptionIfScheduleIsInvalidWhileAskingForAlertTimings() {
        String scheduleName = "my_schedule";
        when(scheduleDataService.findByName(scheduleName)).thenReturn(null);

        scheduleTrackingService.getAlertTimings(new EnrollmentRequest().setExternalId(null).setScheduleName(scheduleName).setPreferredAlertTime(null).setReferenceDate(null).setReferenceTime(null).setEnrollmentDate(null).setEnrollmentTime(null).setStartingMilestoneName(null).setMetadata(null));
    }

    @Test
    public void shouldSaveGivenSchedulesInDb() throws IOException, URISyntaxException {
        String scheduleJson = readFileToString(new File(getClass().getResource("/schedules/simple-schedule.json").toURI()));
        when(settingsFacade.getPlatformSettings()).thenReturn(settings);
        when(settings.getLanguage()).thenReturn("en");

        scheduleTrackingService.add(scheduleJson);

        ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleDataService).create(scheduleCaptor.capture());

        Schedule schedule = scheduleCaptor.getValue();
        assertEquals("IPTI Schedule", schedule.getName());
    }

    @Test
    public void shouldDeleteScheduleFromDb() throws IOException, URISyntaxException {
        scheduleTrackingService.remove("IPTI Schedule");

        verify(scheduleDataService).delete(any(Schedule.class));
    }
}
