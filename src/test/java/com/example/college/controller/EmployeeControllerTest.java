package com.example.college.controller;

import com.example.college.model.EmployeeModel;
import com.example.college.repository.EmployeeRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Unit tests for EmployeeController (standalone)
@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper mapper;

    @Mock
    private EmployeeRepo repo;

    @InjectMocks
    private EmployeeController controller;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getEmployees_returnsAll() throws Exception {
        EmployeeModel e1 = new EmployeeModel();
        e1.setId(1L);
        e1.setName("Alice");
        when(repo.findAll()).thenReturn(List.of(e1));

        mockMvc.perform(get("/api/employe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice"));

        verify(repo, times(1)).findAll();
    }

    @Test
    void getEmployee_returnsOptionalWrappedModel() throws Exception {
        EmployeeModel e = new EmployeeModel();
        e.setId(2L);
        e.setName("Bob");
        when(repo.findById(2L)).thenReturn(Optional.of(e));

        mockMvc.perform(get("/api/employe/{id}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Bob"));

        verify(repo, times(1)).findById(2L);
    }

    @Test
    void postAndPut_andDelete_callRepo() throws Exception {
        EmployeeModel e = new EmployeeModel();
        e.setId(3L);
        e.setName("Carol");

        mockMvc.perform(post("/api/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(e)))
                .andExpect(status().isOk());

        verify(repo, times(1)).save(any(EmployeeModel.class));

        mockMvc.perform(put("/api/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(e)))
                .andExpect(status().isOk());

        verify(repo, times(2)).save(any(EmployeeModel.class)); // called twice in total

        mockMvc.perform(delete("/api/employee/{id}", 3L))
                .andExpect(status().isOk());

        verify(repo, times(1)).deleteById(3L);
    }
}
