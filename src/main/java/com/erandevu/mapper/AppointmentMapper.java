package com.erandevu.mapper;

import com.erandevu.dto.request.AppointmentRequest;
import com.erandevu.dto.response.AppointmentResponse;
import com.erandevu.entity.Appointment;
import com.erandevu.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {
    
    AppointmentMapper INSTANCE = Mappers.getMapper(AppointmentMapper.class);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "isDeleted", constant = "false")
    @Mapping(target = "endDateTime", ignore = true)
    @Mapping(target = "notes", source = "request.notes")
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "status", constant = "SCHEDULED")
    @Mapping(source = "doctor", target = "doctor")
    @Mapping(source = "patient", target = "patient")
    Appointment toAppointment(AppointmentRequest request, User doctor, User patient);
    
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "doctorName", source = "doctor", qualifiedByName = "mapDoctorName")
    @Mapping(target = "patientName", source = "patient", qualifiedByName = "mapPatientName")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "updatedBy", source = "updatedBy")
    AppointmentResponse toAppointmentResponse(Appointment appointment);
    
    @Named("mapDoctorName")
    default String mapDoctorName(User doctor) {
        if (doctor == null) return null;
        return "Dr. " + doctor.getFirstName() + " " + doctor.getLastName();
    }
    
    @Named("mapPatientName")
    default String mapPatientName(User patient) {
        if (patient == null) return null;
        return patient.getFirstName() + " " + patient.getLastName();
    }
    
    // Pagination methods
    List<AppointmentResponse> toAppointmentResponseList(List<Appointment> appointments);
    
    default Page<AppointmentResponse> toAppointmentResponsePage(Page<Appointment> appointmentPage) {
        return appointmentPage.map(this::toAppointmentResponse);
    }
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "endDateTime", ignore = true)
    @Mapping(target = "doctor", ignore = true)
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateAppointmentFromRequest(AppointmentRequest request, @MappingTarget Appointment appointment);
}
