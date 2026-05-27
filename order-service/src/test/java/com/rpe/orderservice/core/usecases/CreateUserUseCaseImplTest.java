package com.rpe.orderservice.core.usecases;

import com.rpe.orderservice.core.domain.Role;
import com.rpe.orderservice.core.domain.User;
import com.rpe.orderservice.core.domain.exceptions.DomainException;
import com.rpe.orderservice.core.ports.outbound.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseImplTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CreateUserUseCaseImpl createUserUseCase;

    private User validUser;

    @BeforeEach
    void setUp() {
        validUser = new User();
        validUser.setLogin("admin");
        validUser.setPassword("123456");
    }

    @Test
    @DisplayName("Deve lançar exceção quando a senha tiver menos de 6 caracteres")
    void shouldThrowExceptionWhenPasswordIsTooShort() {
        validUser.setPassword("12345");

        DomainException exception = assertThrows(DomainException.class, () -> {
            createUserUseCase.execute(validUser);
        });

        assertEquals("A senha deve conter pelo menos 6 caracteres.", exception.getMessage());
        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o login já existir no banco")
    void shouldThrowExceptionWhenLoginAlreadyExists() {
        when(userRepositoryPort.existsByLogin("admin")).thenReturn(true);

        DomainException exception = assertThrows(DomainException.class, () -> {
            createUserUseCase.execute(validUser);
        });

        assertEquals("Este login já está em uso.", exception.getMessage());
        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Deve criar o usuário com sucesso, encriptar a senha e atribuir a Role")
    void shouldCreateUserSuccessfully() {
        when(userRepositoryPort.existsByLogin("admin")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("senha_criptografada_mock");

        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> {
            User userToSave = invocation.getArgument(0);
            userToSave.setId(java.util.UUID.randomUUID());
            return userToSave;
        });

        User savedUser = createUserUseCase.execute(validUser);

        assertNotNull(savedUser.getId());
        assertEquals(Role.ROLE_ORDER_CREATOR, savedUser.getRole());
        assertEquals("senha_criptografada_mock", savedUser.getPassword());
        verify(userRepositoryPort, times(1)).save(any(User.class));
    }
}