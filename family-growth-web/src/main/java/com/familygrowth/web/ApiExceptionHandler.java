package com.familygrowth.web;
import com.familygrowth.application.FamilyGrowthService.NotFoundException; import jakarta.validation.ConstraintViolationException; import org.springframework.http.*; import org.springframework.web.bind.MethodArgumentNotValidException; import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiExceptionHandler {
 @ExceptionHandler(NotFoundException.class) ResponseEntity<ApiResponse<Void>> notFound(){return ResponseEntity.status(404).body(new ApiResponse<>(null,new ApiResponse.ApiError("RESOURCE_NOT_FOUND","Resource not found"),null));}
 @ExceptionHandler({MethodArgumentNotValidException.class,ConstraintViolationException.class,IllegalArgumentException.class}) ResponseEntity<ApiResponse<Void>> invalid(Exception e){return ResponseEntity.badRequest().body(new ApiResponse<>(null,new ApiResponse.ApiError("VALIDATION_FAILED","Request validation failed"),null));}
}
