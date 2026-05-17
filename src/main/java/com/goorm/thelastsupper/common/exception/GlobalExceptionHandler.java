package com.goorm.thelastsupper.common.exception;

import java.util.ArrayList;
import java.util.List;

import com.goorm.thelastsupper.account.exception.AccountException;
import com.goorm.thelastsupper.common.dto.ErrorResponse;
import com.goorm.thelastsupper.reservation.common.exception.ReservationException;
import com.goorm.thelastsupper.reservation.common.exception.SlotAlreadyExistsException;
import com.goorm.thelastsupper.restaurant.exception.ApiResponse;
import com.goorm.thelastsupper.common.security.exception.AuthException;
import com.goorm.thelastsupper.restaurant.exception.RestaurantException;
import com.goorm.thelastsupper.waiting.exception.WaitingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // Spring valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validationExceptionsHandler(MethodArgumentNotValidException ex) {
        // 첫 번째 에러만 꺼내서 CustomException으로 감쌈
        FieldError fieldError = ex.getBindingResult().getFieldError();

        String message = fieldError != null ? fieldError.getDefaultMessage() : "검증 오류입니다.";
        if (fieldError != null) {
            log.info("입력 오류 필드 - {}, 입력값 : {}", fieldError.getField(), fieldError.getRejectedValue());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ErrorCode.INVALID_INPUT_PARAMETER.name(), message));
    }

    @ExceptionHandler(AccountException.class)
    public ResponseEntity<ErrorResponse> AccountExceptionHandler(AccountException ex) {
        ErrorResponse response = new ErrorResponse(ex.getErrorCode().name(), ex.getErrorCode().getMessage());
        return new ResponseEntity<>(response, ex.getErrorCode().getHttpStatus());
    }

    @ExceptionHandler(RestaurantException.class)
    public ResponseEntity<ErrorResponse> RestaurantExceptionHandler(RestaurantException ex) {
        ErrorResponse response = new ErrorResponse(ex.getErrorCode().name(), ex.getErrorCode().getMessage());
        return new ResponseEntity<>(response, ex.getErrorCode().getHttpStatus());
    }

    @ExceptionHandler(WaitingException.class)
    public ResponseEntity<ErrorResponse> waitingExceptionHandler(WaitingException ex) {
        ErrorResponse response = new ErrorResponse(ex.getErrorCode().name(), ex.getErrorCode().getMessage());
        return new ResponseEntity<>(response, ex.getErrorCode().getHttpStatus());
    }

	/**
	 * 1. ReservationException Handler
	 * Reservation 정의 비즈니스 로직 오류가 발생했을 때(ReservationException) 처리합니다.
	 * HTTP 상태 코드: ErrorCode에 정의된 값
	 * 에러 코드: ErrorCode에 정의된 값
	 * 에러 요약 메시지: ErrorCode에 정의된 message
	 * 에러 상세 메시지 목록: HTTP 상태, 로그 레벨 등 추가 정보
	 *
	 * @param e ReservationException 객체
	 * @return 오류 응답(ResponseEntity<ApiResponse<?>>)
	 */
	@ExceptionHandler(ReservationException.class)
	protected ResponseEntity<ApiResponse<?>> handlReservationException(ReservationException e) {
		log.error("[ReservationException] 발생", e);
		String summaryMessage = e.getMessage();

		List<String> detailList = new ArrayList<>();
		detailList.add(String.valueOf(e.getErrorCode().getMessage()));
		detailList.add(String.valueOf(e.getErrorCode()));
		return ResponseEntity.status(e.getErrorCode().getHttpStatus()).body(ApiResponse.error(
			summaryMessage,
			detailList,
			e.getErrorCode().getHttpStatus()
		));
	}

	@ExceptionHandler(SlotAlreadyExistsException.class)
	public ResponseEntity<ApiResponse<?>> handleSlotAlreadyExistsException(SlotAlreadyExistsException e) {
		// 예외 메시지와 에러 코드 처리
		String message = e.getMessage();  // 예외에서 메시지를 추출

		// ApiResponse 객체를 생성하고, 메시지 및 코드 전달
		ApiResponse<?> response = ApiResponse.error(
			message,
			List.of(message),
			HttpStatus.BAD_REQUEST
		);

		// 적절한 HTTP 상태 코드와 함께 응답 반환
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
	/**
	 * 2. BindException Handler
	 * 폼 데이터(또는 쿼리 파라미터) 바인딩 과정에서 유효성 검증에 실패한 경우 발생하는 예외를 처리합니다.
	 * HTTP 상태 코드: 400 (BAD_REQUEST)
	 * 에러 코드: C-002 (INVALID_PARAMETER)
	 * 에러 요약 메시지: ErrorCode.INVALID_PARAMETER.getMessage()
	 * 에러 상세 메시지 목록: BindException 내부의 FieldError 정보를 기반으로 생성
	 *
	 * @param e BindException 객체
	 * @return 오류 응답(ResponseEntity<ApiResponse<?>>)
	 */
	@ExceptionHandler(BindException.class)
	protected ResponseEntity<ApiResponse<?>> handleBindException(BindException e) {
		log.error("[handleBindException] 발생", e);
		String summaryMessage = ErrorCode.INVALID_INPUT_PARAMETER.getMessage();
		List<String> detailList = new ArrayList<>();
		for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
			StringBuilder errorMsg = new StringBuilder();
			errorMsg.append("필드 [").append(fieldError.getField()).append("]: ");
			String[] codes = fieldError.getCodes();
			boolean isMissing = false;
			boolean isTypeMismatch = false;
			for (String code : codes) {
				if (code != null) {
					if (code.contains("NotNull") || code.contains("NotBlank") || code.contains("NotEmpty")) {
						isMissing = true;
					}
					if (code.contains("typeMismatch")) {
						isTypeMismatch = true;
					}
				}
			}
			if (isMissing) {
				errorMsg.append("필수 입력값이 누락되었습니다. ");
			}
			if (isTypeMismatch) {
				errorMsg.append("형식이 올바르지 않습니다. ");
			}
			errorMsg.append(fieldError.getDefaultMessage());
			detailList.add(errorMsg.toString());
		}
		ApiResponse<?> errorResponse = ApiResponse.error(
			summaryMessage,
			detailList,
			ErrorCode.INVALID_INPUT_PARAMETER.getHttpStatus()
		);
		return ResponseEntity.status(ErrorCode.INVALID_INPUT_PARAMETER.getHttpStatus()).body(errorResponse);
	}

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> MethodNotSupportedHandler(HttpRequestMethodNotSupportedException ex) {
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        ErrorResponse response = new ErrorResponse(errorCode.name(), errorCode.getMessage());
        log.info("잘못된 HTTP 메서드 - {}", ex.getMethod());
        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        ErrorCode errorCode = ErrorCode.INVALID_SERVER_ERROR;
        ErrorResponse response = new ErrorResponse(errorCode.name(), errorCode.getMessage());
        log.error("유틸리티 클래스 인스턴스화 오류", ex);
        return new ResponseEntity<>(response, errorCode.getHttpStatus());
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> AuthExceptionHandler(AuthException ex) {
        ErrorResponse response = new ErrorResponse(ex.getErrorCode().name(), ex.getErrorCode().getMessage());
        return new ResponseEntity<>(response, ex.getErrorCode().getHttpStatus());
    }
}
