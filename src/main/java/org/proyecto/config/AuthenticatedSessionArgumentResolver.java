package org.proyecto.config;

import jakarta.servlet.http.HttpServletRequest;
import org.proyecto.business.ForbiddenOperationException;
import org.proyecto.business.SesionActiva;
import org.proyecto.business.SessionManager;
import org.proyecto.entities.CuentaAcceso;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

public class AuthenticatedSessionArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedSession.class)
                && (parameter.getParameterType().equals(SesionActiva.class)
                || parameter.getParameterType().equals(CuentaAcceso.class));
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String authorizationHeader = request != null ? request.getHeader("Authorization") : null;

        String token = extractToken(authorizationHeader);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing session token");
        }

        SesionActiva sesion = SessionManager.getInstance().getSession(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session token"));
        AuthenticatedSession anotacion = parameter.getParameterAnnotation(AuthenticatedSession.class);
        if (sesion.debeCambiarPassword() && (anotacion == null || !anotacion.permitePasswordTemporal())) {
            throw new ForbiddenOperationException("Debe cambiar la contraseña temporal antes de continuar");
        }
        return parameter.getParameterType().equals(SesionActiva.class) ? sesion : sesion.cuenta();
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }
}
