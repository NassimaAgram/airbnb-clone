package com.example.airbnbclone;

import com.example.airbnbclone.booking.application.BookingService;
import com.example.airbnbclone.booking.application.dto.BookedDateDTO;
import com.example.airbnbclone.booking.application.dto.NewBookingDTO;
import com.example.airbnbclone.booking.domain.Booking;
import com.example.airbnbclone.booking.mapper.BookingMapper;
import com.example.airbnbclone.booking.repository.BookingRepository;
import com.example.airbnbclone.listing.application.LandlordService;
import com.example.airbnbclone.listing.application.dto.ListingCreateBookingDTO;
import com.example.airbnbclone.listing.application.dto.vo.PriceVO;
import com.example.airbnbclone.sharedkernel.service.State;
import com.example.airbnbclone.user.application.UserService;
import com.example.airbnbclone.user.application.dto.ReadUserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private UserService userService;

    @Mock
    private LandlordService landlordService;

    @InjectMocks
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateBooking_Success() {
        // Arrange
        OffsetDateTime startDate = OffsetDateTime.now();
        OffsetDateTime endDate = startDate.plusDays(2);

        NewBookingDTO newBookingDTO = new NewBookingDTO(startDate, endDate, UUID.randomUUID());
        Booking booking = new Booking();
        ReadUserDTO user = new ReadUserDTO(
                UUID.randomUUID(),
                "John",
                "Doe",
                "tenant@example.com",
                null,
                Set.of("ROLE_USER")
        );
        PriceVO priceVO = new PriceVO(100);
        ListingCreateBookingDTO listing = new ListingCreateBookingDTO(UUID.randomUUID(), priceVO);

        // Mock responses
        when(bookingMapper.newBookingToBooking(newBookingDTO)).thenReturn(booking);
        when(landlordService.getByListingPublicId(newBookingDTO.listingPublicId())).thenReturn(Optional.of(listing));
        when(bookingRepository.bookingExistsAtInterval(any(), any(), any())).thenReturn(false);
        when(userService.getAuthenticatedUserFromSecurityContext()).thenReturn(user);

        // Act
        State<Void, String> result = bookingService.create(newBookingDTO);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Booking creation should be successful");
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }


    @Test
    void testCreateBooking_ListingNotFound() {
        NewBookingDTO newBookingDTO = new NewBookingDTO(
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(2),
                UUID.randomUUID()
        );

        when(landlordService.getByListingPublicId(newBookingDTO.listingPublicId())).thenReturn(Optional.empty());

        State<Void, String> result = bookingService.create(newBookingDTO);

        assertFalse(result.isSuccess());
        assertEquals("Landlord public id not found", result.getError());
    }

    @Test
    void testCheckAvailability() {
        UUID listingId = UUID.randomUUID();
        Booking booking = new Booking();
        BookedDateDTO bookedDateDTO = new BookedDateDTO(
                OffsetDateTime.now(),
                OffsetDateTime.now().plusDays(2)
        );

        when(bookingRepository.findAllByFkListing(listingId)).thenReturn(List.of(booking));
        when(bookingMapper.bookingToCheckAvailability(booking)).thenReturn(bookedDateDTO);

        List<BookedDateDTO> availability = bookingService.checkAvailability(listingId);

        assertNotNull(availability);
        assertEquals(1, availability.size());
        assertEquals(bookedDateDTO, availability.get(0));
    }

    @Test
    void testCancelBooking_SuccessByTenant() {
        UUID bookingId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        ReadUserDTO user = new ReadUserDTO(
                UUID.randomUUID(),
                "John",
                "Doe",
                "tenant@example.com",
                null,
                Set.of("ROLE_USER")
        );

        when(userService.getAuthenticatedUserFromSecurityContext()).thenReturn(user);
        when(bookingRepository.deleteBookingByFkTenantAndPublicId(user.publicId(), bookingId)).thenReturn(1);

        State<UUID, String> result = bookingService.cancel(bookingId, listingId, false);

        assertTrue(result.isSuccess());
        assertEquals(bookingId, result.getValue());
    }

    @Test
    void testCancelBooking_NotFound() {
        UUID bookingId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        ReadUserDTO user = new ReadUserDTO(
                UUID.randomUUID(),
                "John",
                "Doe",
                "tenant@example.com",
                null,
                Set.of("ROLE_USER")
        );

        when(userService.getAuthenticatedUserFromSecurityContext()).thenReturn(user);
        when(bookingRepository.deleteBookingByFkTenantAndPublicId(user.publicId(), bookingId)).thenReturn(0);

        State<UUID, String> result = bookingService.cancel(bookingId, listingId, false);

        assertFalse(result.isSuccess());
        assertEquals("Booking not found", result.getError());
    }
}