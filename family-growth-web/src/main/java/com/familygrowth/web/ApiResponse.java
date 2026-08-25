package com.familygrowth.web;
public record ApiResponse<T>(T data, ApiError error, String traceId) { public static <T> ApiResponse<T> ok(T data){return new ApiResponse<>(data,null,null);} public record ApiError(String code,String message){} }
