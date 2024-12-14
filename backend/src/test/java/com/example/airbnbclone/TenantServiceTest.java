package com.example.airbnbclone;

import com.example.airbnbclone.booking.application.BookingService;
import com.example.airbnbclone.listing.application.TenantService;
import com.example.airbnbclone.listing.application.dto.DisplayCardListingDTO;
import com.example.airbnbclone.listing.application.dto.DisplayListingDTO;
import com.example.airbnbclone.listing.application.dto.SearchDTO;
import com.example.airbnbclone.listing.application.dto.sub.ListingInfoDTO;
import com.example.airbnbclone.listing.application.dto.vo.BathsVO;
import com.example.airbnbclone.listing.application.dto.vo.BedroomsVO;
import com.example.airbnbclone.listing.application.dto.vo.BedsVO;
import com.example.airbnbclone.listing.application.dto.vo.GuestsVO;
import com.example.airbnbclone.listing.domain.BookingCategory;
import com.example.airbnbclone.listing.domain.Listing;
import com.example.airbnbclone.listing.mapper.ListingMapper;
import com.example.airbnbclone.listing.repository.ListingRepository;
import com.example.airbnbclone.sharedkernel.service.State;
import com.example.airbnbclone.user.application.UserService;
import com.example.airbnbclone.user.application.dto.ReadUserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TenantServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ListingMapper listingMapper;

    @Mock
    private UserService userService;

    @Mock
    private BookingService bookingService;

    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tenantService = new TenantService(listingRepository, listingMapper, userService, bookingService);
    }

    @Test
    void getAllByCategory_shouldReturnAllListingsWhenCategoryIsAll() {
        // Arrange
        Pageable pageable = Pageable.unpaged();
        Listing listing = mock(Listing.class);
        DisplayCardListingDTO dto = mock(DisplayCardListingDTO.class);

        Page<Listing> listingsPage = new PageImpl<>(List.of(listing));
        when(listingRepository.findAllWithCoverOnly(pageable)).thenReturn(listingsPage);
        when(listingMapper.listingToDisplayCardListingDTO(listing)).thenReturn(dto);

        // Act
        Page<DisplayCardListingDTO> result = tenantService.getAllByCategory(pageable, BookingCategory.ALL);

        // Assert
        assertThat(result.getContent()).contains(dto);
        verify(listingRepository, times(1)).findAllWithCoverOnly(pageable);
    }

    @Test
    void getOne_shouldReturnListingWithLandlordDetails() {
        // Arrange
        UUID publicId = UUID.randomUUID();
        Listing listing = mock(Listing.class);
        DisplayListingDTO displayListingDTO = mock(DisplayListingDTO.class);
        ReadUserDTO landlordDTO = mock(ReadUserDTO.class);

        when(listingRepository.findByPublicId(publicId)).thenReturn(Optional.of(listing));
        when(listingMapper.listingToDisplayListingDTO(listing)).thenReturn(displayListingDTO);
        when(userService.getByPublicId(any())).thenReturn(Optional.of(landlordDTO));

        // Act
        State<DisplayListingDTO, String> result = tenantService.getOne(publicId);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(displayListingDTO);
        verify(listingRepository, times(1)).findByPublicId(publicId);
        verify(userService, times(1)).getByPublicId(any());
    }

    @Test
    void getOne_shouldReturnErrorWhenListingNotFound() {
        // Arrange
        UUID publicId = UUID.randomUUID();
        when(listingRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

        // Act
        State<DisplayListingDTO, String> result = tenantService.getOne(publicId);

        // Assert
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("Listing doesn't exist");
        verify(listingRepository, times(1)).findByPublicId(publicId);
    }

    @Test
    void search_shouldReturnListingsNotBooked() {
        // Arrange
        Pageable pageable = Pageable.unpaged();
        SearchDTO searchDTO = mock(SearchDTO.class);
        Listing listing = mock(Listing.class);
        UUID publicId = UUID.randomUUID();

        // Mock the ListingInfoDTO
        ListingInfoDTO listingInfoDTO = mock(ListingInfoDTO.class);

        // Mock the baths() method to return a valid BathsVO object
        BathsVO bathsVO = mock(BathsVO.class);
        when(bathsVO.value()).thenReturn(1); // Set a value for baths
        when(listingInfoDTO.baths()).thenReturn(bathsVO); // Return the mocked BathsVO

        // Mock the bedrooms() method to return a valid BedroomsVO object
        BedroomsVO bedroomsVO = mock(BedroomsVO.class);
        when(bedroomsVO.value()).thenReturn(2); // Set a value for bedrooms
        when(listingInfoDTO.bedrooms()).thenReturn(bedroomsVO); // Return the mocked BedroomsVO

        // Mock the guests() method to return a valid GuestsVO object
        GuestsVO guestsVO = mock(GuestsVO.class);
        when(guestsVO.value()).thenReturn(4); // Set a value for guests
        when(listingInfoDTO.guests()).thenReturn(guestsVO); // Return the mocked GuestsVO

        // Mock the beds() method to return a valid BedsVO object
        BedsVO bedsVO = mock(BedsVO.class);
        when(bedsVO.value()).thenReturn(3); // Set a value for beds
        when(listingInfoDTO.beds()).thenReturn(bedsVO); // Return the mocked BedsVO

        // Mock SearchDTO.infos() to return the mocked ListingInfoDTO
        when(searchDTO.infos()).thenReturn(listingInfoDTO);

        // Mock other dependencies
        when(listing.getPublicId()).thenReturn(publicId);
        when(listingRepository.findAllByLocationAndBathroomsAndBedroomsAndGuestsAndBeds(any(), any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(listing)));
        when(bookingService.getBookingMatchByListingIdsAndBookedDate(anyList(), any()))
                .thenReturn(List.of());
        when(listingMapper.listingToDisplayCardListingDTO(listing)).thenReturn(mock(DisplayCardListingDTO.class));

        // Act
        Page<DisplayCardListingDTO> result = tenantService.search(pageable, searchDTO);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        verify(listingRepository, times(1)).findAllByLocationAndBathroomsAndBedroomsAndGuestsAndBeds(any(), any(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(bookingService, times(1)).getBookingMatchByListingIdsAndBookedDate(anyList(), any());
    }

}

