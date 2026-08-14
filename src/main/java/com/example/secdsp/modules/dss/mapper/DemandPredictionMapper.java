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
    @Mapping(target = "seasonalityAdjustedDemand", ignore = true)
    @Mapping(target = "holidayAdjustmentFactor", ignore = true)
    @Mapping(target = "historicalFrom", ignore = true)
    @Mapping(target = "historicalTo", ignore = true)
    @Mapping(target = "historicalPeriodLabel", ignore = true)
    @Mapping(target = "forecastPeriodLabel", ignore = true)
    @Mapping(target = "forecastFrom", ignore = true)
    @Mapping(target = "forecastTo", ignore = true)
    @Mapping(target = "methodology", ignore = true)
    @Mapping(target = "trendFactor", ignore = true)
    @Mapping(target = "forecastSeries", ignore = true)
    @Mapping(target = "upcomingHolidays", ignore = true)
    @Mapping(target = "productContext", ignore = true)
    @Mapping(target = "priceChangeImpacts", ignore = true)
    @Mapping(target = "aiInsight", ignore = true)
    DemandPredictionResponse toResponse(DemandPrediction demandPrediction);
}
