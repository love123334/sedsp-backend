package com.example.secdsp.modules.product.mapper;

import com.example.secdsp.modules.product.dto.response.PriceHistoryResponse;
import com.example.secdsp.modules.product.entity.PriceHistory;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PriceHistoryMapper {

    PriceHistoryResponse toResponse(PriceHistory entity);

    List<PriceHistoryResponse> toResponse(List<PriceHistory> entities);
}
