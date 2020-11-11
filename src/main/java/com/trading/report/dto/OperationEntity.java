package com.trading.report.dto;

import lombok.Data;
import ru.tinkoff.invest.openapi.models.Currency;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
public class OperationEntity implements Cloneable {
  private Currency currency;
  private ZonedDateTime buyDate;
  private String instrument;
  private BigDecimal buyPrice;
  private Integer buyQuantity;
  private BigDecimal buyCommission;
  private BigDecimal buySum;
  private ZonedDateTime sellDate;
  private BigDecimal sellPrice;
  private Integer sellQuantity;
  private BigDecimal sellCommission;
  private BigDecimal sellSum;
  private BigDecimal result;
  private BigDecimal netResult;
  private Long earningDuration;
  private boolean sell;
  private Type type;

  public boolean isSimilar(OperationEntity operationEntity) {
    return (operationEntity.buyPrice == null || operationEntity.buyPrice.equals(buyPrice)) &&
            (operationEntity.sellPrice == null || operationEntity.sellPrice.equals(sellPrice)) &&
            (operationEntity.buyDate == null || operationEntity.buyDate.equals(buyDate)) &&
            (operationEntity.sellDate == null || operationEntity.sellDate.equals(sellDate));
  }

  public enum Type {
    SHORT, LONG;
  }
}
