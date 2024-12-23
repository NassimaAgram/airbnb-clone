package com.example.airbnbclone.listing.application.dto;

import com.example.airbnbclone.booking.application.dto.BookedDateDTO;
import com.example.airbnbclone.listing.application.dto.sub.ListingInfoDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record SearchDTO(@Valid BookedDateDTO dates,
                        @Valid ListingInfoDTO infos,
                        @NotEmpty String location) {
}
