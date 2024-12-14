package com.example.airbnbclone.user.mapper;

import com.example.airbnbclone.user.application.dto.ReadUserDTO;
import com.example.airbnbclone.user.domain.Authority;
import com.example.airbnbclone.user.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    ReadUserDTO readUserDTOToUser(User user);

    default String mapAuthoritiesToString(Authority authority) {
        return authority.getName();
    }

}
