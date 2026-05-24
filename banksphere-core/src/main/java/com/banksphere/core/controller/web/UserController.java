package com.banksphere.core.controller.web;

import com.banksphere.core.entity.User;
import com.banksphere.core.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador Web MVC que implementa el CRUD de Mantenimiento de Usuarios.
 * Restringido exclusivamente para usuarios con Rol ADMINISTRADOR.
 */
@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Muestra el listado de usuarios con filtros de búsqueda y paginación dinámica.
     *
     * @param searchTerm Texto de búsqueda para filtrar por nombre, email o DNI (opcional).
     * @param page       Número de página actual (por defecto 0).
     * @param size       Cantidad de registros por página (por defecto 6).
     */
    @GetMapping
    public String listUsers(
            @RequestParam(name = "search", required = false) String searchTerm,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "6") int size,
            Model model) {

        // Ordenamos los usuarios por fecha de registro descendente de forma predeterminada
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<User> userPage;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            userPage = userService.searchUsers(searchTerm, pageRequest);
            model.addAttribute("search", searchTerm);
        } else {
            userPage = userService.getAllUsers(pageRequest);
        }

        model.addAttribute("userPage", userPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalItems", userPage.getTotalElements());

        return "users/list"; // Retorna la vista HTML Thymeleaf
    }

    /**
     * Muestra el formulario para registrar un nuevo usuario en el sistema.
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", User.UserRole.values());
        model.addAttribute("statuses", User.UserStatus.values());
        model.addAttribute("isEdit", false);
        return "users/form";
    }

    /**
     * Procesa la creación de un nuevo usuario con validaciones del lado del servidor.
     */
    @PostMapping("/new")
    public String createUser(
            @Valid @ModelAttribute("user") User user,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", User.UserRole.values());
            model.addAttribute("statuses", User.UserStatus.values());
            model.addAttribute("isEdit", false);
            return "users/form"; // Recarga el formulario mostrando los errores de validación
        }

        try {
            userService.createUser(user);
            // Mensaje flash persistente tras redirección (Notificación Toast en el Frontend)
            redirectAttributes.addFlashAttribute("success", "Usuario '" + user.getEmail() + "' registrado con éxito.");
            return "redirect:/users";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("roles", User.UserRole.values());
            model.addAttribute("statuses", User.UserStatus.values());
            model.addAttribute("isEdit", false);
            return "users/form";
        }
    }

    /**
     * Muestra el formulario de edición cargando los datos del usuario especificado.
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        User user = userService.getUserById(id);

        model.addAttribute("user", user);
        model.addAttribute("roles", User.UserRole.values());
        model.addAttribute("statuses", User.UserStatus.values());
        model.addAttribute("isEdit", true);

        return "users/form";
    }

    /**
     * Procesa la actualización de datos de un usuario existente.
     */
    @PostMapping("/edit/{id}")
    public String updateUser(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("user") User user,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", User.UserRole.values());
            model.addAttribute("statuses", User.UserStatus.values());
            model.addAttribute("isEdit", true);
            return "users/form";
        }

        try {
            userService.updateUser(id, user);
            redirectAttributes.addFlashAttribute("success", "Datos del usuario actualizados correctamente.");
            return "redirect:/users";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error al actualizar el usuario: " + e.getMessage());
            model.addAttribute("roles", User.UserRole.values());
            model.addAttribute("statuses", User.UserStatus.values());
            model.addAttribute("isEdit", true);
            return "users/form";
        }
    }

    /**
     * Procesa la eliminación física de un usuario.
     */
    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "Usuario '" + user.getEmail() + "' eliminado del sistema.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo eliminar el usuario: " + e.getMessage());
        }
        return "redirect:/users";
    }

    /**
     * Funcionalidad Premium: Alterna rápidamente el estado del usuario entre ACTIVO y SUSPENDIDO.
     * Permite a los administradores congelar cuentas bancarias en un solo clic desde la tabla.
     */
    @GetMapping("/toggle-status/{id}")
    public String toggleUserStatus(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id);
            if (user.getStatus() == User.UserStatus.ACTIVE) {
                user.setStatus(User.UserStatus.SUSPENDED);
                redirectAttributes.addFlashAttribute("success", "Cuenta de " + user.getFirstName() + " SUSPENDIDA de inmediato.");
            } else {
                user.setStatus(User.UserStatus.ACTIVE);
                redirectAttributes.addFlashAttribute("success", "Cuenta de " + user.getFirstName() + " ACTIVADA correctamente.");
            }
            // Salvamos los cambios invocando al servicio de actualización
            userService.updateUser(id, user);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al modificar el estado: " + e.getMessage());
        }
        return "redirect:/users";
    }
}