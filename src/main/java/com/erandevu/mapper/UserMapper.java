package com.erandevu.mapper;

import com.erandevu.dto.request.RegisterRequest;
import com.erandevu.dto.response.UserResponse;
import com.erandevu.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {
    
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "doctorAppointments", ignore = true)
    @Mapping(target = "patientAppointments", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    User toUser(RegisterRequest request);
    
    @Mapping(target = "role", expression = "java(user.getRole().name())")
    UserResponse toUserResponse(User user);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    @Mapping(target = "doctorAppointments", ignore = true)
    @Mapping(target = "patientAppointments", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    void updateUserFromRequest(RegisterRequest request, @MappingTarget User user);
}
