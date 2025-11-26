package com.example.college.controller;

import com.example.college.model.AttendanceModel;
import com.example.college.service.AttendanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

class AttendanceControllerTest {

    private MockMvc mockMvc;
    private AttendanceService attendanceService;
    private ObjectMapper mapper;


@BeforeEach
void setUp() {
    attendanceService = Mockito.mock(AttendanceService.class);
    AttendanceController controller = new AttendanceController(null, attendanceService);

    mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);   // keep this

    // use the mapper for both request and response conversion
    MappingJackson2HttpMessageConverter jacksonConverter =
            new MappingJackson2HttpMessageConverter(mapper);

    mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setMessageConverters(jacksonConverter)   // <- important
                .build();
}

    @Test
    void markSingle_success_returnsSavedRecord() throws Exception {
        AttendanceController.AttendanceDTO dto = new AttendanceController.AttendanceDTO();
        dto.setEmployeeId(100L);
        dto.setAbsent(Boolean.TRUE);
        dto.setDate(LocalDate.of(2025, 11, 20));

        AttendanceModel saved = new AttendanceModel(100L, dto.getDate(), true);
        Mockito.when(attendanceService.markSingleAttendance(ArgumentMatchers.eq(100L),
                ArgumentMatchers.eq(dto.getDate()), ArgumentMatchers.eq(true)))
                .thenReturn(saved);

        mockMvc.perform(put("/api/attendance/mark")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.employeeId", is(100)))
                .andExpect(jsonPath("$.absent", is(true)))
                .andExpect(jsonPath("$.date", is(dto.getDate().toString())));

        verify(attendanceService, times(1))
                .markSingleAttendance(ArgumentMatchers.eq(100L), ArgumentMatchers.eq(dto.getDate()), ArgumentMatchers.eq(true));
    }

    @Test
    void markSingle_badRequest_whenMissingFields() throws Exception {
        // missing absent
        AttendanceController.AttendanceDTO dto = new AttendanceController.AttendanceDTO();
        dto.setEmployeeId(5L);
        dto.setDate(LocalDate.now());

        mockMvc.perform(put("/api/attendance/mark")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("employeeId and absent required")));

        // verify service not called
        verify(attendanceService, times(0)).markSingleAttendance(ArgumentMatchers.anyLong(), ArgumentMatchers.any(), ArgumentMatchers.anyBoolean());
    }

    @Test
    void markBatch_success_savesMultipleAndReturnsSummary() throws Exception {
        AttendanceController.AttendanceDTO a = new AttendanceController.AttendanceDTO();
        a.setEmployeeId(1L);
        a.setAbsent(false);
        a.setDate(LocalDate.of(2025, 11, 21));

        AttendanceController.AttendanceDTO b = new AttendanceController.AttendanceDTO();
        b.setEmployeeId(2L);
        b.setAbsent(true);
        b.setDate(LocalDate.of(2025, 11, 21));

        AttendanceModel savedA = new AttendanceModel(1L, a.getDate(), false);
        AttendanceModel savedB = new AttendanceModel(2L, b.getDate(), true);

        Mockito.when(attendanceService.markSingleAttendance(1L, a.getDate(), false)).thenReturn(savedA);
        Mockito.when(attendanceService.markSingleAttendance(2L, b.getDate(), true)).thenReturn(savedB);

        List<AttendanceController.AttendanceDTO> payload = List.of(a, b);

        mockMvc.perform(post("/api/attendance/mark-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.savedCount", is(2)))
                .andExpect(jsonPath("$.presentCount", is(1)))
                .andExpect(jsonPath("$.absentCount", is(1)))
                .andExpect(jsonPath("$.savedRecords", hasSize(2)))
                .andExpect(jsonPath("$.invalidEntries", hasSize(0)));

        verify(attendanceService, times(1)).markSingleAttendance(1L, a.getDate(), false);
        verify(attendanceService, times(1)).markSingleAttendance(2L, b.getDate(), true);
    }

    @Test
    void markBatch_badRequest_whenPayloadEmpty() throws Exception {
        mockMvc.perform(post("/api/attendance/mark-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("payload is empty")));

        verify(attendanceService, times(0)).markSingleAttendance(ArgumentMatchers.anyLong(), ArgumentMatchers.any(), ArgumentMatchers.anyBoolean());
    }

    @Test
    void markBatch_skipsInvalidEntries_andReportsInvalidIds() throws Exception {
        // first valid, second invalid (missing absent but has id), third completely null (represented as null in JSON array)
        AttendanceController.AttendanceDTO valid = new AttendanceController.AttendanceDTO();
        valid.setEmployeeId(10L);
        valid.setAbsent(false);
        valid.setDate(LocalDate.of(2025, 11, 22));

        AttendanceController.AttendanceDTO invalidWithId = new AttendanceController.AttendanceDTO();
        invalidWithId.setEmployeeId(11L);
        // absent is null -> invalid

        AttendanceController.AttendanceDTO nullEntry = null;

        AttendanceModel saved = new AttendanceModel(10L, valid.getDate(), false);
        Mockito.when(attendanceService.markSingleAttendance(10L, valid.getDate(), false)).thenReturn(saved);

        // Object payload = List.of(valid, invalidWithId, nullEntry);

        Object payload = Arrays.asList(valid, invalidWithId, nullEntry);
        mockMvc.perform(post("/api/attendance/mark-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount", is(1)))
                .andExpect(jsonPath("$.presentCount", is(1)))
                .andExpect(jsonPath("$.absentCount", is(0)))
                .andExpect(jsonPath("$.invalidEntries", hasSize(1)))
                .andExpect(jsonPath("$.invalidEntries[0]", is(11)));

        verify(attendanceService, times(1)).markSingleAttendance(10L, valid.getDate(), false);
    }
}
