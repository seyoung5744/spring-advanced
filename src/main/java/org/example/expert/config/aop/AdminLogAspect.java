package org.example.expert.config.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AdminLogAspect {

    private static final String USER_ID_KEY = "userId";
    private static final String NO_REQUEST_BODY = "[no request body]";
    private static final String NO_RESPONSE_BODY = "[no response body]";

    private final ObjectMapper objectMapper;

    @Around("@within(org.example.expert.config.aop.AdminTrace)")
    public Object adminLogTrace(ProceedingJoinPoint pjp) throws Throwable {

        HttpServletRequest request = getHttpServletRequest();

        // 1. 요청 메타데이터
        Long userId = (Long) request.getAttribute(USER_ID_KEY);
        LocalDateTime requestTime = LocalDateTime.now();
        String httpMethod = request.getMethod();
        String url = request.getRequestURI();

        // 2. 요청 본문 (메서드 인자에서 추출)
        Object[] args = pjp.getArgs();
        Parameter[] parameters = getMethodParameters(pjp);

        String requestBody = getRequestBody(args, parameters);

        log.info("\n=== [ADMIN API 요청] ===\n" +
                "사용자 ID: {}\n" +
                "요청 시각: {}\n" +
                "요청 URL : {} {}\n" +
                "요청 본문: {}", userId, requestTime, httpMethod, url, requestBody);

        // 3. target 실행
        Object result;
        try {
            result = pjp.proceed();
        } catch (Exception e) {
            log.error("\n=== [ADMIN API ERROR] ===\n{}", e.getMessage());
            throw e;
        }

        // 4. 응답 본문
        log.info("\n=== [ADMIN API 응답] ===\n응답 본문: {}", formatResponseBody(result));

        return result;
    }

    private HttpServletRequest getHttpServletRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    private Parameter[] getMethodParameters(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        return method.getParameters();
    }

    private String getRequestBody(Object[] args, Parameter[] parameters) throws JsonProcessingException {
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            Parameter parameter = parameters[i];

            if (arg == null) continue;

            // RequestParam, PathVariable 같은 단순 값 매핑 제외
            if (parameter.isAnnotationPresent(RequestParam.class)
                    || parameter.isAnnotationPresent(PathVariable.class)
                    || parameter.isAnnotationPresent(RequestHeader.class)) {
                continue;
            }

            // request body 후보
            return objectMapper.writeValueAsString(arg);
        }
        return NO_REQUEST_BODY;
    }

    private String formatResponseBody(Object result) throws JsonProcessingException {
        return result == null ? NO_RESPONSE_BODY : objectMapper.writeValueAsString(result);
    }
}
