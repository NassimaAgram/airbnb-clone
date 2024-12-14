package com.example.airbnbclone;

import com.example.airbnbclone.listing.application.PictureService;
import com.example.airbnbclone.listing.application.dto.sub.PictureDTO;
import com.example.airbnbclone.listing.domain.Listing;
import com.example.airbnbclone.listing.domain.ListingPicture;
import com.example.airbnbclone.listing.mapper.ListingPictureMapper;
import com.example.airbnbclone.listing.repository.ListingPictureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PictureServiceTest {

    @Mock
    private ListingPictureRepository listingPictureRepository;

    @Mock
    private ListingPictureMapper listingPictureMapper;

    @InjectMocks
    private PictureService pictureService;

    private Listing listing;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listing = new Listing();
        // Use a valid UUID string
        listing.setPublicId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    }

    @Test
    void testSaveAll() {
        // Arrange: Prepare test data
        byte[] fileContent = new byte[]{1, 2, 3}; // Sample byte array for file
        PictureDTO pictureDTO1 = new PictureDTO(fileContent, "image/jpeg", true);
        PictureDTO pictureDTO2 = new PictureDTO(fileContent, "image/jpeg", false);

        List<PictureDTO> pictures = List.of(pictureDTO1, pictureDTO2);

        // Create distinct ListingPicture objects by setting their properties
        ListingPicture listingPicture1 = new ListingPicture();
        listingPicture1.setFileContentType("image/jpeg");
        listingPicture1.setCover(true);

        ListingPicture listingPicture2 = new ListingPicture();
        listingPicture2.setFileContentType("image/jpeg");
        listingPicture2.setCover(false);

        // Mock the behavior of listingPictureMapper
        when(listingPictureMapper.pictureDTOsToListingPictures(any())).thenReturn(Set.of(listingPicture1, listingPicture2));
        when(listingPictureMapper.listingPictureToPictureDTO(any())).thenReturn(List.of(pictureDTO1, pictureDTO2));

        // Act: Call the method to be tested
        List<PictureDTO> result = pictureService.saveAll(pictures, listing);

        // Assert: Verify that the method behaves as expected
        assertEquals(2, result.size());  // Checking the size of the result
        assertEquals(pictureDTO1, result.get(0));  // Verifying the first element
        assertEquals(pictureDTO2, result.get(1));  // Verifying the second element

        // Verify the interaction with the mocked dependencies
        Mockito.verify(listingPictureRepository).saveAll(any());  // Ensure saveAll was called on repository
        Mockito.verify(listingPictureMapper).pictureDTOsToListingPictures(any());  // Ensure mapping was done correctly
        Mockito.verify(listingPictureMapper).listingPictureToPictureDTO(any());  // Ensure DTO conversion was done
    }
}
