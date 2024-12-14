package com.example.airbnbclone;

import com.example.airbnbclone.infrastructure.config.SecurityUtils;
import com.example.airbnbclone.user.application.UserService;
import com.example.airbnbclone.user.application.dto.ReadUserDTO;
import com.example.airbnbclone.user.domain.User;
import com.example.airbnbclone.user.mapper.UserMapper;
import com.example.airbnbclone.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAuthenticatedUserFromSecurityContext_Success() {
        User user = new User();
        user.setEmail("test@example.com");
        ReadUserDTO readUserDTO = new ReadUserDTO(
                UUID.randomUUID(),
                "FirstName",
                "LastName",
                "test@example.com",
                "image-url",
                Set.of("ROLE_USER")
        );

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(Map.of("email", "test@example.com"));
        SecurityContextHolder.setContext(securityContext);

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(() -> SecurityUtils.mapOauth2AttributesToUser(anyMap())).thenReturn(user);
            when(userRepository.findOneByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(userMapper.readUserDTOToUser(user)).thenReturn(readUserDTO);

            ReadUserDTO result = userService.getAuthenticatedUserFromSecurityContext();

            assertNotNull(result);
            assertEquals("test@example.com", result.email());
        }
    }

    @Test
    void testGetAuthenticatedUserFromSecurityContext_UserNotFound() {
        User user = new User();
        user.setEmail("test@example.com");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(Map.of("email", "test@example.com"));
        SecurityContextHolder.setContext(securityContext);

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(() -> SecurityUtils.mapOauth2AttributesToUser(anyMap())).thenReturn(user);
            when(userRepository.findOneByEmail(user.getEmail())).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class, userService::getAuthenticatedUserFromSecurityContext);
        }
    }

    @Test
    void testGetByEmail_Success() {
        User user = new User();
        user.setEmail("test@example.com");
        ReadUserDTO readUserDTO = new ReadUserDTO(
                UUID.randomUUID(),
                "FirstName",
                "LastName",
                "test@example.com",
                "image-url",
                Set.of("ROLE_USER")
        );

        when(userRepository.findOneByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userMapper.readUserDTOToUser(user)).thenReturn(readUserDTO);

        Optional<ReadUserDTO> result = userService.getByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().email());
    }

    @Test
    void testGetByEmail_NotFound() {
        when(userRepository.findOneByEmail("test@example.com")).thenReturn(Optional.empty());

        Optional<ReadUserDTO> result = userService.getByEmail("test@example.com");

        assertFalse(result.isPresent());
    }

    @Test
    void testSyncWithIdp_NewUser() {
        User user = new User();
        user.setEmail("test@example.com");

        Map<String, Object> attributes = Map.of("email", "test@example.com");
        when(oAuth2User.getAttributes()).thenReturn(attributes);

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(() -> SecurityUtils.mapOauth2AttributesToUser(attributes)).thenReturn(user);
            when(userRepository.findOneByEmail("test@example.com")).thenReturn(Optional.empty());

            userService.syncWithIdp(oAuth2User, false);

            verify(userRepository, times(1)).saveAndFlush(user);
        }
    }

    @Test
    void testSyncWithIdp_ExistingUser_UpdateRequired() {
        User existingUser = new User();
        existingUser.setEmail("test@example.com");
        existingUser.setLastModifiedDate(Instant.now().minusSeconds(3600));

        User newUser = new User();
        newUser.setEmail("test@example.com");
        Map<String, Object> attributes = Map.of("email", "test@example.com", "updated_at", Instant.now());
        when(oAuth2User.getAttributes()).thenReturn(attributes);

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(() -> SecurityUtils.mapOauth2AttributesToUser(attributes)).thenReturn(newUser);
            when(userRepository.findOneByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

            userService.syncWithIdp(oAuth2User, false);

            verify(userRepository, times(1)).saveAndFlush(existingUser);
        }
    }

    @Test
    void testSyncWithIdp_ExistingUser_NoUpdateRequired() {
        User existingUser = new User();
        existingUser.setEmail("test@example.com");
        existingUser.setLastModifiedDate(Instant.now());

        when(oAuth2User.getAttributes()).thenReturn(Map.of("email", "test@example.com", "updated_at", Instant.now().minusSeconds(3600)));

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(() -> SecurityUtils.mapOauth2AttributesToUser(anyMap())).thenReturn(existingUser);
            when(userRepository.findOneByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

            userService.syncWithIdp(oAuth2User, false);

            verify(userRepository, never()).saveAndFlush(any(User.class));
        }
    }

    @Test
    void testGetByPublicId_Success() {
        User user = new User();
        user.setPublicId(UUID.randomUUID());
        ReadUserDTO readUserDTO = new ReadUserDTO(
                user.getPublicId(),
                "FirstName",
                "LastName",
                "test@example.com",
                "image-url",
                Set.of("ROLE_USER")
        );

        when(userRepository.findOneByPublicId(user.getPublicId())).thenReturn(Optional.of(user));
        when(userMapper.readUserDTOToUser(user)).thenReturn(readUserDTO);

        Optional<ReadUserDTO> result = userService.getByPublicId(user.getPublicId());

        assertTrue(result.isPresent());
        assertEquals(user.getPublicId(), result.get().publicId());
    }

    @Test
    void testGetByPublicId_NotFound() {
        UUID publicId = UUID.randomUUID();

        when(userRepository.findOneByPublicId(publicId)).thenReturn(Optional.empty());

        Optional<ReadUserDTO> result = userService.getByPublicId(publicId);

        assertFalse(result.isPresent());
    }
}