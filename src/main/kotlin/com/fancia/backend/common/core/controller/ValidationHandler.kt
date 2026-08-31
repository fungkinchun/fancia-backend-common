package com.fancia.backend.common.core.controller

import com.fancia.backend.common.config.ApplicationProperties
import com.fancia.backend.shared.common.core.exception.DomainException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import tools.jackson.core.JacksonException
import java.net.URI

@RestControllerAdvice
class ValidationHandler(
    private val applicationProperties: ApplicationProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, request: WebRequest): ProblemDetail {
        val errors = ex.bindingResult.allErrors.map { it.defaultMessage ?: "error" }
        val errorCode = "VALIDATION_ERROR"
        val problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed: ${errors.joinToString(", ")}",
        ).apply {
            applicationProperties.baseUrl?.let { type = URI.create(it) }
            title = "Validation Error"
            setProperty("errorCode", errorCode)
            instance = URI.create((request as ServletWebRequest).request.requestURI)
        }
        log.warn("Validation error: ${errors.joinToString(", ")}")
        return problem
    }

    @ExceptionHandler(DomainException::class)
    fun handleError(ex: DomainException, request: WebRequest): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.message,
        ).apply {
            applicationProperties.baseUrl?.let { type = URI.create(it) }
            title = ex.title
            setProperty("errorCode", ex.errorCode)
            instance = URI.create((request as ServletWebRequest).request.requestURI)
        }
        log.warn("Domain error: ${ex.errorCode} - ${ex.message}")
        return problem
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException, request: WebRequest): ProblemDetail {
        val cause = ex.mostSpecificCause
        val jackson = cause as? JacksonException
        val path = jackson
            ?.path
            ?.joinToString(".") { ref ->
                ref.propertyName ?: "[${ref.index}]"
            }
            ?.takeIf { it.isNotBlank() }
        val detail = path
            ?: jackson?.originalMessage?.take(500)
            ?: cause.message?.take(500)
            ?: "unknown"
        val problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            detail,
        ).apply {
            applicationProperties.baseUrl?.let { type = URI.create(it) }
            title = "Invalid Request Body"
            setProperty("errorCode", "INVALID_REQUEST_BODY")
            instance = URI.create((request as ServletWebRequest).request.requestURI)
        }
        log.warn("Invalid request body: {}", cause.message)
        return problem
    }
}
