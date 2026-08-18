-- Remove persistence owned only by the retired DSS modules.
-- Reporting views V37/V38 do not depend on any object below.

DROP TABLE IF EXISTS demand_predictions;

DROP TABLE IF EXISTS dss_results;
DROP TABLE IF EXISTS dss_scenario_items;
DROP TABLE IF EXISTS dss_scenarios;
DROP TABLE IF EXISTS dss_recommendations;

DROP TYPE IF EXISTS recommendation_type;
