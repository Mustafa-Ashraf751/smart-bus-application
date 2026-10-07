package com.verysmartbus.dto.response;

import java.math.BigDecimal;

public record MapPointResponseDto(
        BigDecimal longitude,
        BigDecimal latitude
) {
}
