package com.rovoq.electio.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)//Игнорировать все неизвестные параметры
@Data
public class CaptchaResponseDto {
    private boolean success;
    @JsonAlias("error-codes")//Преобразование для символа дефис
    private Set<String> errorsCodes;

}
