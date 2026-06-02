package com.banksphere.core.controller.api;

import com.banksphere.common.dto.UserDTO;
import com.banksphere.core.entity.User;
import com.banksphere.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/openbanking/users")
@Tag(name = "Users API", description = "Endpoints de Open Banking para la gestión y registro de usuarios en la plataforma")
public class UserRestController {

    private final UserService userService;

    @Autowired
    public UserRestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Obtener listado de usuarios", description = "Recupera una lista paginada de todos los usuarios registrados en el sistema")
    public ResponseEntity<List<UserDTO>> getAllUsers(Pageable pageable) {
        Page<User> usersPage = userService.getAllUsers(pageable);
        List<UserDTO> dtos = usersPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuario por ID", description = "Recupera los datos del perfil de un usuario a través de su ID único")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado con éxito")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    public ResponseEntity<UserDTO> getUserById(@PathVariable("id") Long id) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(convertToDTO(user));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Registrar nuevo usuario", description = "Da de alta a un usuario nuevo en el sistema encriptando su password con BCrypt")
    @ApiResponse(responseCode = "201", description = "Usuario registrado con éxito")
    @ApiResponse(responseCode = "400", description = "Email o documento de identidad ya registrados")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserDTO userDTO, @RequestParam("password") String password) {
        try {
            User user = User.builder()
                    .firstName(userDTO.getFirstName())
                    .lastName(userDTO.getLastName())
                    .email(userDTO.getEmail())
                    .phone(userDTO.getPhone())
                    .nationalId(userDTO.getNationalId())
                    .password(password)
                    .role(User.UserRole.USER) // Registro inicial como USER
                    .status(User.UserStatus.PENDING_KYC) // Estado inicial por KYC
                    .build();

            User savedUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Helper method to convert Entity to DTO
    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .nationalId(user.getNationalId())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}