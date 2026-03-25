package com.erandevu.mapper;

import com.erandevu.dto.request.AppointmentRequest;
import com.erandevu.dto.response.AppointmentResponse;
import com.erandevu.entity.Appointment;
import com.erandevu.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {
    
    AppointmentMapper INSTANCE = Mappers.getMapper(AppointmentMapper.class);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "endDateTime", ignore = true)
    @Mapping(target = "notes", source = "request.notes")
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "status", constant = "SCHEDULED")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Appointment toAppointment(AppointmentRequest request, User doctor, User patient);
    
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "doctorName", source = "doctor", qualifiedByName = "mapDoctorName")
    @Mapping(target = "patientName", source = "patient", qualifiedByName = "mapPatientName")
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
}
