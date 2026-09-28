# Order Completion Business Logic - Specifications

## Overview
Implement business logic for completing client orders in the Toadstool trading platform. Orders are immutable records of trading intent that combine Instrument, Quantity, Order Type (Buy/Sell), and Account information.

## Design Principles
- **Immutability**: All database tables remain immutable. Orders are never updated after creation.
- **Audit Trail**: Store submission timestamp and calculated total price for historical accuracy.
- **Separation of Concerns**: Order submission vs. Fill execution are distinct and tracked separately.

## Entities Involved

### Order Entity
- `orderId` (UUID): Unique identifier
- `accountId` (UUID): Reference to trading account
- `instrumentId` (UUID): Reference to instrument being traded
- `side` (OrderSide enum): BUY or SELL
- `quantity` (BigDecimal): Number of units (precision: 5 decimal places)
- `idempotencyKey` (String): Duplicate detection
- `submittedAt` (OffsetDateTime): Timestamp of submission
- `totalPrice` (BigDecimal): Calculated at submission for audit trail (NOT persisted to DB)

### Fill Entity (Existing)
- `fillId` (UUID): Unique identifier
- `orderId` (UUID): Reference to associated order
- `price` (BigDecimal): Actual execution price
- `quantity` (BigDecimal): Quantity actually filled
- `status` (FillStatus): FILLED, FAILED, or PENDING
- `executedAt` (OffsetDateTime): Timestamp of execution

### Instrument Entity (Existing)
- `instrumentId` (UUID): Unique identifier
- `symbol` (String): Trading symbol
- `name` (String): Full name
- `assetClass` (AssetClass enum): EQUITY, FX, or CRYPTO
- `status` (InstrumentStatus): TRADABLE, HALTED, or INACTIVE

### Account Entity (Existing)
- `accountId` (UUID): Unique identifier
- `clientId` (UUID): Reference to account owner
- `status` (AccountStatus): ACTIVE, SUSPENDED, or CLOSED

## Fee Structure

### AssetClassFeeStructure Enum
Fees are differentiated by instrument type (AssetClass), not by order side.

| AssetClass | Percentage | Minimum Fee |
|------------|-----------|-------------|
| EQUITY    | 0.05%     | $1.00       |
| CRYPTO    | 0.10%     | $2.00       |
| FX        | 0.02%     | $0.50       |

**Calculation**: `fee = MAX(orderValue × percentage, minimumFee)`

Where `orderValue = instrumentPrice × quantity`

### Total Order Price Calculation
```
totalPrice = (instrumentPrice × quantity) + fee
```

**Note**: This is calculated at order submission time for audit purposes. The actual execution price may differ (captured in Fill).

## Pricing Strategy

### Temporary Implementation (For Testing)
- **InstrumentPricingService**: Generates random prices within realistic bounds
  - EQUITY: $10.00 - $500.00
  - CRYPTO: $100.00 - $50,000.00
  - FX: $0.50 - $2.00
- **IMPORTANT**: This is a placeholder. Replace with trading API in future.
- **Lines to remove/replace**: All random generation logic in `InstrumentPricingService`

### Future Implementation
- Call actual trading API to fetch current instrument prices
- Consider caching for performance

## Market Status Validation

### MarketStatusService
- **Production**: Validates actual market hours (9:30 AM - 4:00 PM EST)
- **Testing**: Returns `true` (markets always open for testing)
- **Rationale**: Allows testing past market close without timezone complexity

## Order Validation Rules

Orders must pass the following validations:

1. **Account Validation**
   - Account must exist
   - Account status must be ACTIVE

2. **Instrument Validation**
   - Instrument must exist
   - Instrument status must be TRADABLE

3. **Quantity Validation**
   - Quantity must be > 0
   - Quantity must have at most 5 decimal places

4. **Market Validation**
   - Market must be open (checked via MarketStatusService)

5. **Future Validation** (Not implemented yet)
   - Account balance must be sufficient for order value + fees
   - Position limits (if applicable)

## Database Schema

### No Schema Changes Required
- Order table remains unchanged (no `total_price` column)
- Fill table remains unchanged
- Instrument table remains immutable (no price column)

**Rationale**: 
- Prices are fetched live/generated at runtime
- `totalPrice` is calculated for the Order object in memory, used for calculations/display
- Immutability is preserved: no table updates after initial insert

## Data Precision

- All monetary values: 5 decimal places (BigDecimal)
- Uses ROUND_HALF_UP rounding mode
- Example: $1.23456 rounds to $1.23456 exactly

## Price Timeline

### Order Submission
- `Order.submittedAt`: Timestamp when order is created
- `Order.totalPrice`: Calculated from instrument price at submission time
- Instrument price is fetched via `InstrumentPricingService`

### Fill Execution
- `Fill.executedAt`: Timestamp when fill is executed
- `Fill.price`: Actual execution price (may differ from order submission price)
- Execution price is fetched via `InstrumentPricingService`

## Order Completion Logic

An order is considered:
- **In Progress**: No associated Fill exists, or Fill status is PENDING
- **Completed**: Associated Fill exists with status FILLED
- **Failed/Invalid**: Associated Fill exists with status FAILED or other non-FILLED status

**Query Logic**: Check if `SELECT * FROM fills WHERE order_id = ? AND status = 'FILLED'` returns results

## Services to Implement

1. **AssetClassFeeStructure** (Enum)
   - Maps AssetClass to fee percentage and minimum fee
   - Provides method to calculate fee for order value

2. **InstrumentPricingService** (Service)
   - Generates random prices for testing
   - Will be replaced with API call in future
   - Clear comments marking lines to remove/replace

3. **MarketStatusService** (Service)
   - Validates if markets are open
   - Spring profile-aware (test vs. production)

4. **OrderService** (Enhanced)
   - Add business logic to `createOrder()` method
   - Implement all validation rules
   - Calculate totalPrice
   - Return Order with totalPrice populated

## Exception Handling

Create custom exceptions for validation failures:
- `InvalidOrderException` (quantity <= 0, etc.)
- `AccountNotFoundException` / `InstrumentNotFoundException`
- `MarketClosedException`
- `InstrumentNotTradableException`

## Testing Strategy

Create comprehensive test suites:
- Unit tests for `AssetClassFeeStructure`
- Unit tests for `InstrumentPricingService`
- Unit tests for `MarketStatusService`
- Integration tests for `OrderService.createOrder()`
- Controller tests for order creation endpoint

## Future Considerations

1. **API Integration**: Replace `InstrumentPricingService` random generation with real API
2. **Account Balance Validation**: Add balance check before order creation
3. **Position Limits**: Add position limit validation per account/instrument
4. **Order Status Field**: Consider adding status field to Order if immutability can be maintained
5. **Price History**: Implement separate table for price history if needed
6. **Fill Partial Fills**: Handle scenarios where only partial quantity is filled

---

**Document Version**: 1.0  
**Last Updated**: 2026-09-28  
**Status**: Ready for Implementation
