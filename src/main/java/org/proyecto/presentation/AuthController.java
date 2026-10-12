package org.proyecto.presentation;

import org.proyecto.business.AuthService;
import org.proyecto.business.PermisoService;
import org.proyecto.business.SessionManager;
import org.proyecto.entities.CuentaAcceso;

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final PermisoService permisoService;

    public AuthController(AuthService authService, PermisoService permisoService) {
        this.authService = authService;
        this.permisoService = permisoService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return authService.login(request.getUsuarioLogin(), request.getPassword())
                .map(this::buildLoginResponse)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Invalid credentials")));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Missing session token"));
        }

        SessionManager.getInstance().invalidateSession(token);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Object> buildLoginResponse(CuentaAcceso cuenta) {
        Set<String> permisos = permisoService.permisosDe(cuenta.getId());
        String token = SessionManager.getInstance().createSession(cuenta, permisos);
        return ResponseEntity.ok(LoginResponse.from(token, cuenta, permisos));
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }
}
