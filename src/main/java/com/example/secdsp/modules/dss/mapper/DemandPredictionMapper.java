package com.example.secdsp.modules.dss.mapper;

import com.example.secdsp.modules.dss.dto.response.DemandPredictionResponse;
import com.example.secdsp.modules.dss.entity.DemandPrediction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DemandPredictionMapper {

    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "predictedDemand", source = "predictedQuantity")
    @Mapping(target = "generatedAt", source = "createdAt")
    DemandPredictionResponse toResponse(DemandPrediction demandPrediction);
}
