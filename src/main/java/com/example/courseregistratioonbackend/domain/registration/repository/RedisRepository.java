package com.example.courseregistratioonbackend.domain.registration.repository;

import com.example.courseregistratioonbackend.domain.course.entity.Course;
import com.example.courseregistratioonbackend.domain.course.repository.CourseRepository;
import com.example.courseregistratioonbackend.domain.registration.entity.Registration;
import com.example.courseregistratioonbackend.global.exception.RequiredFieldException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.example.courseregistratioonbackend.global.enums.ErrorCode.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRepository {
    private final RedisTemplate<String, Long> redisTemplateCache;
    private final CourseRepository courseRepository;
    private final RegistrationRepository registrationRepository;
    private final RedisScript<Boolean> decrementLeftSeatRedisScript = new DefaultRedisScript<>(
            DECREMENT_LEFT_SEAT_SCRIPT, Boolean.class);
    // Lua Script
    private static final String DECREMENT_LEFT_SEAT_SCRIPT =
            "local leftSeats = tonumber(redis.call('get', KEYS[1])) " +
                    "if leftSeats - ARGV[1] >= 0 then " +
                    "  redis.call('decrby', KEYS[1], ARGV[1]) " +
                    "  return true " +
                    "else " +
                    "  return false " +
                    "end";

    // redis 서버확인
    public boolean isRedisDown() {
        try {
            redisTemplateCache.execute((RedisCallback<Object>) connection -> connection.info());
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    // 강의를 Redis에 저장
    public void saveCourseToRedis(Course course){
        String key = "c" + course.getId();
        if(redisTemplateCache.hasKey(key)){
            throw new RequiredFieldException(EXISTED_CACHE_EXCEPTION);
        }

        redisTemplateCache.opsForValue().set(key, course.getLimitation()-course.getCurrent());
    }

    // key로 redis에 캐시가 있는지 조회하고 Boolean 반환
    public Boolean hasLeftSeatsInRedis(Long courseId){
        String key = "c" + courseId;
        return redisTemplateCache.hasKey(key);
    }

    // 수강 신청 가능한지 확인
    public Boolean checkLeftSeatInRedis(Long courseId){
        String key = "c" + courseId;
        if(redisTemplateCache.opsForValue().get(key) <= 0){
            return false;
        }else {
            return true;
        }
    }

    // count만큼 남은좌석수 차감
    public Boolean decrementLeftSeatInRedis(Long courseId) {
        String key = "c" + courseId;
        // Lua Script 실행
        return redisTemplateCache.execute(decrementLeftSeatRedisScript, Collections.singletonList(key), 1);
    }

    // 수강 신청 취소 시 남은 인원 +1
    public void incrementLeftSeatInRedis(Long courseId){
        String key = "c" + courseId;
        if(hasLeftSeatsInRedis(courseId)){
            redisTemplateCache.opsForValue().increment(key);
        }
    }

    // Redis랑 DB 정합성 검사
    public void refreshLeftSeats(){
        log.error("redis 서버가 닫혔습니다.");
        List<Registration> registrations = registrationRepository.findAll();
        Set<Long> courseIdList = null;
        registrations.forEach(registration -> {
            courseIdList.add(registration.getCourse().getId());
        });

        courseIdList.forEach(courseId -> {
            Course course = courseRepository.findById(courseId).orElseThrow(
                    () -> new RequiredFieldException(COURSE_NOT_FOUND)
            );
            Long accurateReserveSeats = registrationRepository.countByCourseId(courseId);
            if (accurateReserveSeats == null) {
                throw new RequiredFieldException(REGISTRATION_NOT_FOUND);
            }
            Long accurateLeftSeats = course.getLimitation() - accurateReserveSeats;
            String key = "c" + courseId;
            course.setCurrent(accurateLeftSeats);
            redisTemplateCache.opsForValue().set(key, accurateLeftSeats);
        });
    }
}
