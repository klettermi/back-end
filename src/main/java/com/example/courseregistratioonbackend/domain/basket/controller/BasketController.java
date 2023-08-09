package com.example.courseregistratioonbackend.domain.basket.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.courseregistratioonbackend.domain.basket.dto.CourseFromBasketResponseDto;
import com.example.courseregistratioonbackend.domain.basket.service.BasketService;
import com.example.courseregistratioonbackend.global.enums.SuccessCode;
import com.example.courseregistratioonbackend.global.responsedto.ApiResponse;
import com.example.courseregistratioonbackend.global.security.userdetails.UserDetailsImpl;
import com.example.courseregistratioonbackend.global.utils.ResponseUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "예비수강신청(장바구니) 관련 API", description = "예비수강신청(장바구니) 관련 API")
@Slf4j(topic = "BasketController")
@RequestMapping("/api/basket")
@RestController
@RequiredArgsConstructor
public class BasketController {

	private final BasketService basketService;

	@GetMapping()
	public ApiResponse<?> getCourseListFromBasket(@AuthenticationPrincipal UserDetailsImpl userDetails) {

		Long studentId = userDetails.getStudentUser().getId();

		List<CourseFromBasketResponseDto> courseListFromBasket = basketService.getCourseListFromBasket(studentId);

		return ResponseUtils.ok(courseListFromBasket);
	}

	@PostMapping("/{courseId}")
	public ApiResponse<?> addCourseToBasket(@PathVariable("courseId") Long courseId,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		Long studentId = userDetails.getStudentUser().getId();

		SuccessCode successCode = basketService.addCourseToBasket(courseId, studentId);

		return ResponseUtils.ok(successCode);
	}

	@DeleteMapping("/{basketId}")
	public ApiResponse<?> deleteCourseFromBasket(@PathVariable("basketId") Long basketId,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		Long studentId = userDetails.getStudentUser().getId();

		SuccessCode successCode = basketService.deleteCourseFromBasket(basketId, studentId);

		return ResponseUtils.ok(successCode);
	}

}
