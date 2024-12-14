package com.example.airbnbclone.booking.mapper;

import com.example.airbnbclone.booking.application.dto.BookedDateDTO;
import com.example.airbnbclone.booking.application.dto.NewBookingDTO;
import com.example.airbnbclone.booking.domain.Booking;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    Booking newBookingToBooking(NewBookingDTO newBookingDTO);

    BookedDateDTO bookingToCheckAvailability(Booking booking);
}
