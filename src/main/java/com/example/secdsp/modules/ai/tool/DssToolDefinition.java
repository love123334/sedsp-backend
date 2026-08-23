package com.example.secdsp.modules.ai.tool;

import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DssToolDefinition {

    private DssToolDefinition() {
    }

    public static FunctionDeclaration getDemandForecast() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put(
            "productId",
            Schema.builder()
                .type("INTEGER")
                .description("Product identifier to forecast demand for.")
                .build()
        );
        properties.put(
            "forecastDays",
            Schema.builder()
                .type("INTEGER")
                .description("Forecast horizon in days (e.g. 7, 14, 30). Default is 30.")
                .build()
        );

        return FunctionDeclaration.builder()
            .name("get_dss_demand_forecast")
            .description("""
                Get demand forecast predictions for a product using adaptive time series models
                (Moving Average, Holt Linear, Holt-Winters, and LightGBM ONNX).
                Returns daily forecasted demand, average daily demand, trend slope, seasonality, and explanation.
                """)
            .parameters(
                Schema.builder()
                    .type("OBJECT")
                    .properties(properties)
                    .required(List.of("productId"))
                    .build()
            )
            .build();
    }

    public static FunctionDeclaration getRestockRecommendations() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put(
            "planningDays",
            Schema.builder()
                .type("INTEGER")
                .description("Planning horizon in days (e.g. 7, 14, 30). Default is 14.")
                .build()
        );
        properties.put(
            "productId",
            Schema.builder()
                .type("INTEGER")
                .description("Optional product identifier. If omitted, returns ranked recommendations across all products.")
                .build()
        );

        return FunctionDeclaration.builder()
            .name("get_dss_restock_recommendations")
            .description("""
                Get inventory restock recommendations and reorder point (ROP) analysis.
                Products are ranked by a 5-factor Restock Score (Demand Forecast, Stock Risk, Sales Velocity, Profit Margin, Revenue Contribution).
                Use when seller asks what products to restock, reorder points, or inventory warnings.
                """)
            .parameters(
                Schema.builder()
                    .type("OBJECT")
                    .properties(properties)
                    .build()
            )
            .build();
    }

    public static FunctionDeclaration getWhatIfDiscountAnalysis() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put(
            "productId",
            Schema.builder()
                .type("INTEGER")
                .description("Product identifier to simulate discount or price change.")
                .build()
        );
        properties.put(
            "priceChangePercent",
            Schema.builder()
                .type("NUMBER")
                .description("Price change percentage (e.g. -10 for 10% discount, -15 for 15% discount, 5 for 5% increase).")
                .build()
        );
        properties.put(
            "simulationPeriod",
            Schema.builder()
                .type("INTEGER")
                .description("Simulation period in days (e.g. 14, 30). Default is 30.")
                .build()
        );

        return FunctionDeclaration.builder()
            .name("get_dss_what_if_discount")
            .description("""
                Perform What-if discount simulation using price elasticity (E = %ΔQ / %ΔP).
                Calculates expected sales quantity, projected revenue, COGS, gross profit, and margin comparison
                against keeping current price.
                Use when seller asks: "Nếu giảm giá X%?", "Nên giảm giá không?", "Giảm 10% thì lãi thế nào?".
                """)
            .parameters(
                Schema.builder()
                    .type("OBJECT")
                    .properties(properties)
                    .required(List.of("productId", "priceChangePercent"))
                    .build()
            )
            .build();
    }

    public static FunctionDeclaration getPriceRecommendation() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put(
            "productId",
            Schema.builder()
                .type("INTEGER")
                .description("Product identifier to get price recommendations.")
                .build()
        );
        properties.put(
            "lookbackDays",
            Schema.builder()
                .type("INTEGER")
                .description("Lookback period in days (e.g. 30). Default is 30.")
                .build()
        );

        return FunctionDeclaration.builder()
            .name("get_dss_price_recommendation")
            .description("""
                Get optimal price recommendations based on price elasticity and expected profit/revenue curve.
                Provides recommended price, estimated demand, and gross profit comparison.
                """)
            .parameters(
                Schema.builder()
                    .type("OBJECT")
                    .properties(properties)
                    .required(List.of("productId"))
                    .build()
            )
            .build();
    }

    public static FunctionDeclaration getBusinessHealth() {
        return FunctionDeclaration.builder()
            .name("get_dss_business_health")
            .description("""
                Evaluate the seller's overall business health score (0 - 100) across 5 pillars:
                Revenue Trend (25%), Order Trend (20%), Profit Trend (25%), Inventory Health (15%), Demand Trend (15%).
                Returns overall health score, status (Healthy, Moderate, At Risk), detected risks, and actionable recommendations.
                Use when seller asks: "Shop tôi đang ổn không?", "Tình hình kinh doanh thế nào?", "Đánh giá sức khỏe shop".
                """)
            .parameters(
                Schema.builder()
                    .type("OBJECT")
                    .properties(Map.of())
                    .build()
            )
            .build();
    }
}
