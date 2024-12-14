package com.example.airbnbclone;

import com.example.airbnbclone.listing.application.LandlordService;
import com.example.airbnbclone.listing.application.PictureService;
import com.example.airbnbclone.listing.application.dto.CreatedListingDTO;
import com.example.airbnbclone.listing.application.dto.DisplayCardListingDTO;
import com.example.airbnbclone.listing.application.dto.ListingCreateBookingDTO;
import com.example.airbnbclone.listing.application.dto.SaveListingDTO;
import com.example.airbnbclone.listing.domain.Listing;
import com.example.airbnbclone.listing.mapper.ListingMapper;
import com.example.airbnbclone.listing.repository.ListingRepository;
import com.example.airbnbclone.sharedkernel.service.State;
import com.example.airbnbclone.user.application.Auth0Service;
import com.example.airbnbclone.user.application.UserService;
import com.example.airbnbclone.user.application.dto.ReadUserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LandlordServiceTest {

    private LandlordService landlordService;
    private ListingRepository listingRepository;
    private ListingMapper listingMapper;
    private UserService userService;
    private Auth0Service auth0Service;
    private PictureService pictureService;

    @BeforeEach
    void setUp() {
        listingRepository = mock(ListingRepository.class);
        listingMapper = mock(ListingMapper.class);
        userService = mock(UserService.class);
        auth0Service = mock(Auth0Service.class);
        pictureService = mock(PictureService.class);

        landlordService = new LandlordService(listingRepository, listingMapper, userService, auth0Service, pictureService);

    }

    @Test
    void create_ShouldSaveListingAndAssignLandlordRole() {
        // Arrange
        SaveListingDTO saveListingDTO = mock(SaveListingDTO.class);
        Listing listing = mock(Listing.class);
        Listing savedListing = mock(Listing.class);
        ReadUserDTO userConnected = mock(ReadUserDTO.class);
        CreatedListingDTO createdListingDTO = mock(CreatedListingDTO.class);

        when(listingMapper.saveListingDTOToListing(saveListingDTO)).thenReturn(listing);
        when(userService.getAuthenticatedUserFromSecurityContext()).thenReturn(userConnected);
        when(listingRepository.saveAndFlush(listing)).thenReturn(savedListing);
        when(listingMapper.listingToCreatedListingDTO(savedListing)).thenReturn(createdListingDTO);

        // Act
        CreatedListingDTO result = landlordService.create(saveListingDTO);

        // Assert
        verify(listing).setLandlordPublicId(userConnected.publicId());
        verify(listingRepository).saveAndFlush(listing);
        verify(pictureService).saveAll(saveListingDTO.getPictures(), savedListing);
        verify(auth0Service).addLandlordRoleToUser(userConnected);
        assertEquals(createdListingDTO, result);
    }

    @Test
    void getAllProperties_ShouldReturnDisplayCardListings() {
        // Arrange
        ReadUserDTO landlord = mock(ReadUserDTO.class);
        List<Listing> listings = List.of(mock(Listing.class));
        List<DisplayCardListingDTO> expectedDTOs = List.of(mock(DisplayCardListingDTO.class));

        when(landlord.publicId()).thenReturn(UUID.randomUUID());
        when(listingRepository.findAllByLandlordPublicIdFetchCoverPicture(landlord.publicId())).thenReturn(listings);
        when(listingMapper.listingToDisplayCardListingDTOs(listings)).thenReturn(expectedDTOs);

        // Act
        List<DisplayCardListingDTO> result = landlordService.getAllProperties(landlord);

        // Assert
        assertEquals(expectedDTOs, result);
    }

    @Test
    void delete_ShouldReturnSuccessState_WhenDeletedSuccessfully() {
        // Arrange
        UUID publicId = UUID.randomUUID();
        ReadUserDTO landlord = mock(ReadUserDTO.class);
        when(landlord.publicId()).thenReturn(UUID.randomUUID());
        when(listingRepository.deleteByPublicIdAndLandlordPublicId(publicId, landlord.publicId())).thenReturn(1L);

        // Act
        State<UUID, String> result = landlordService.delete(publicId, landlord);

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(publicId, result.getData());
    }

    @Test
    void delete_ShouldReturnUnauthorizedState_WhenDeleteFails() {
        // Arrange
        UUID publicId = UUID.randomUUID();
        ReadUserDTO landlord = mock(ReadUserDTO.class);
        when(landlord.publicId()).thenReturn(UUID.randomUUID());
        when(listingRepository.deleteByPublicIdAndLandlordPublicId(publicId, landlord.publicId())).thenReturn(0L);

        // Act
        State<UUID, String> result = landlordService.delete(publicId, landlord);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals("User not authorized to delete this listing", result.getError());
    }

    @Test
    void getByListingPublicId_ShouldReturnOptionalListingCreateBookingDTO() {
        // Arrange
        UUID publicId = UUID.randomUUID();
        Listing listing = mock(Listing.class);
        ListingCreateBookingDTO bookingDTO = mock(ListingCreateBookingDTO.class);

        when(listingRepository.findByPublicId(publicId)).thenReturn(Optional.of(listing));
        when(listingMapper.mapListingToListingCreateBookingDTO(listing)).thenReturn(bookingDTO);

        // Act
        Optional<ListingCreateBookingDTO> result = landlordService.getByListingPublicId(publicId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(bookingDTO, result.get());
    }

    @Test
    void getCardDisplayByListingPublicId_ShouldReturnDisplayCardListingDTOs() {
        // Arrange
        List<UUID> publicIds = List.of(UUID.randomUUID());
        Listing listing = mock(Listing.class);
        DisplayCardListingDTO dto = mock(DisplayCardListingDTO.class);

        when(listingRepository.findAllByPublicIdIn(publicIds)).thenReturn(List.of(listing));
        when(listingMapper.listingToDisplayCardListingDTO(listing)).thenReturn(dto);

        // Act
        List<DisplayCardListingDTO> result = landlordService.getCardDisplayByListingPublicId(publicIds);

        // Assert
        assertEquals(1, result.size());
        assertEquals(dto, result.get(0));
    }

    @Test
    void getByPublicIdAndLandlordPublicId_ShouldReturnOptionalDisplayCardListingDTO() {
        // Arrange
        UUID listingPublicId = UUID.randomUUID();
        UUID landlordPublicId = UUID.randomUUID();
        Listing listing = mock(Listing.class);
        DisplayCardListingDTO dto = mock(DisplayCardListingDTO.class);

        when(listingRepository.findOneByPublicIdAndLandlordPublicId(listingPublicId, landlordPublicId)).thenReturn(Optional.of(listing));
        when(listingMapper.listingToDisplayCardListingDTO(listing)).thenReturn(dto);

        // Act
        Optional<DisplayCardListingDTO> result = landlordService.getByPublicIdAndLandlordPublicId(listingPublicId, landlordPublicId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(dto, result.get());
    }
}
