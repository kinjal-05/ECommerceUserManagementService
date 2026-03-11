package com.userservice.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.HashMap;
import java.util.Map;

public class CustomMethodArgumentNotValidException extends RuntimeException
{
	public CustomMethodArgumentNotValidException(String message)
	{
		super(message);
	}
	public static ResponseEntity<Map<String,String>> handleValidationErrors(MethodArgumentNotValidException e)
	{
		Map<String,String> errors=new HashMap<>();

		e.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

		return ResponseEntity.badRequest().body(errors);
	}
}
