package com.trading.report;

import lombok.Builder;
import lombok.Data;
import ru.tinkoff.invest.openapi.models.Currency;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.Period;

@Data
@Builder
public class OperationEntity implements Cloneable {
    private Currency currency;
    private OffsetDateTime startDate;
    private String instrument;
    private BigDecimal buyPrice;
    private Integer buyQuantity;
    private BigDecimal buyCommission;
    private BigDecimal buySum;
    private OffsetDateTime endDate;
    private BigDecimal sellPrice;
    private Integer sellQuantity;
    private BigDecimal sellCommission;
    private BigDecimal sellSum;
    private BigDecimal result;
    private BigDecimal netResult;
    private Period earningDuration;

    @Override
    public Object clone () {
        try {
            return (OperationEntity)super.clone();
        } catch (CloneNotSupportedException e) {
            return OperationEntity
                    .builder()
                    .currency(this.currency)
                    .startDate(this.startDate)
                    .instrument(this.instrument)
                    .buyPrice(this.buyPrice)
                    .buyQuantity(this.buyQuantity)
                    .buyCommission(this.buyCommission)
                    .buySum(this.buySum)
                    .endDate(this.endDate)
                    .sellPrice(this.sellPrice)
                    .sellQuantity(this.sellQuantity)
                    .sellCommission(this.sellCommission)
                    .sellSum(this.sellSum)
                    .result(this.result)
                    .netResult(this.netResult)
                    .earningDuration(this.earningDuration)
                    .build();
        }
    }
}
