package com.neueda.leap.dto;

/**
 * Upcoming earnings report from the Finnhub earnings calendar.
 */
public class EarningsEventResponse {
    public String symbol;
    public String date;
    /** Finnhub reporting hour: "bmo" (before market open), "amc" (after market close), "dmh" (during market hours) or empty. */
    public String hour;
    public Integer quarter;
    public Integer year;
    public Double epsEstimate;
    public Double revenueEstimate;

    public EarningsEventResponse() {}

    public EarningsEventResponse(String symbol, String date, String hour, Integer quarter, Integer year,
                                 Double epsEstimate, Double revenueEstimate) {
        this.symbol = symbol;
        this.date = date;
        this.hour = hour;
        this.quarter = quarter;
        this.year = year;
        this.epsEstimate = epsEstimate;
        this.revenueEstimate = revenueEstimate;
    }

    public String getSymbol() { return symbol; }
    public String getDate() { return date; }
    public String getHour() { return hour; }
    public Integer getQuarter() { return quarter; }
    public Integer getYear() { return year; }
    public Double getEpsEstimate() { return epsEstimate; }
    public Double getRevenueEstimate() { return revenueEstimate; }
}
