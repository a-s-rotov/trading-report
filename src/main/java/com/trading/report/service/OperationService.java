package com.trading.report.service;

import com.trading.report.dto.OperationEntity;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.models.Currency;
import ru.tinkoff.invest.openapi.models.operations.Operation;
import ru.tinkoff.invest.openapi.models.operations.OperationStatus;
import ru.tinkoff.invest.openapi.models.operations.OperationType;
import ru.tinkoff.invest.openapi.models.operations.OperationsList;
import ru.tinkoff.invest.openapi.models.portfolio.InstrumentType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static java.math.RoundingMode.HALF_UP;

@Service
public class OperationService {
  @Autowired
  private OpenApi api;

  @Autowired
  private FigiDictionaryService figiDictionaryService;

  @Value("${project.startDate}")
  private String startDate;

  public List<OperationEntity> getOperations(Currency currency) {
    OperationsList operations = api
            .getOperationsContext()
            .getOperations(OffsetDateTime.parse(startDate), OffsetDateTime.now(), "", "")
            .join();
    return processOperations(currency, operations);
  }

  private List<OperationEntity> processOperations(Currency currency, OperationsList operationsList) {
    Map<String, List<Operation>> figiOperations = new HashMap();
    operationsList.operations.stream()
            .filter(operation -> operation.currency.equals(currency))
            .filter(operation -> operation.status.equals(OperationStatus.Done))
            .filter(operation -> !InstrumentType.Currency.equals(operation.instrumentType))
            .filter(operation -> OperationType.Buy.equals(operation.operationType)
                    || OperationType.BuyCard.equals(operation.operationType)
                    || OperationType.Sell.equals(operation.operationType))
            .sorted(Comparator.comparing(operation -> operation.date))
            .forEach(operation -> {
              if (figiOperations.containsKey(operation.figi)) {
                figiOperations.get(operation.figi).add(operation);
              } else {
                List<Operation> list = new ArrayList<>();
                list.add(operation);
                figiOperations.put(operation.figi, list);
              }
            });


    List<OperationEntity> resultOperationList = new ArrayList<>();
    for (Map.Entry<String, List<Operation>> entry : figiOperations.entrySet()) {
      resultOperationList.addAll(convertOperationToEntity(entry.getValue()));
    }


    return resultOperationList;
  }

  private List<OperationEntity> convertOperationToEntity(List<Operation> operationList) {
    Queue<StorageItem> queue = new ArrayDeque<>();
    List<StorageItem> storageItems = new ArrayList<>();

    operationList.forEach(operation -> {
      if (operation.trades != null) {
        int countTrade = operation.trades.stream().mapToInt(trade -> trade.quantity).sum();
        operation.trades.stream()
                .forEach(trade -> {
                  for (int i = 0; i < trade.quantity; i++) {
                    BigDecimal commission = BigDecimal.ZERO;
                    if (operation.commission != null) {
                      commission = operation.commission.value.divide(BigDecimal.valueOf(countTrade), 5, HALF_UP);
                    }
                    OperationType operationType = operation.operationType == OperationType.BuyCard ? OperationType.Buy : operation.operationType;
                    storageItems.add(StorageItem.builder()
                            .date(trade.date)
                            .figi(operation.figi)
                            .operationType(operationType)
                            .price(trade.price)
                            .commission(commission)
                            .build());
                  }
                });
      }
    });

    List<OperationEntity> result = new ArrayList<>();

    storageItems.stream()
            .sorted(Comparator.comparing(item -> item.date))
            .forEach(storageItem -> {
              if (queue.peek() == null || queue.peek().operationType == storageItem.operationType) {
                queue.offer(storageItem);
              } else {
                StorageItem storageItemFromQueue = queue.poll();
                if (storageItemFromQueue != null) {
                  addOperationEntity(result, createOperationEntity(storageItemFromQueue, storageItem));
                }
              }
            });

    while (queue.peek() != null) {
      addOperationEntity(result, createOperationEntity(queue.poll(), null));
    }

    return result;
  }


  private void addOperationEntity(List<OperationEntity> result, OperationEntity operationEntity) {
    if (result.size() > 0) {
      OperationEntity savedOperationEntity = result.get(result.size() - 1);
      if (savedOperationEntity.isSimilar(operationEntity)) {
        mergeOperationEntity(savedOperationEntity, operationEntity);
      } else {
        fillSingleOperationEntity(operationEntity);
        result.add(operationEntity);
      }
    } else {
      fillSingleOperationEntity(operationEntity);
      result.add(operationEntity);
    }
  }

  private void fillSingleOperationEntity(OperationEntity operationEntity) {
    operationEntity.setBuyQuantity(1);
    operationEntity.setBuySum(operationEntity.getBuyPrice());
    if (operationEntity.isSell()) {
      operationEntity.setSellQuantity(1);
      operationEntity.setSellSum(operationEntity.getSellPrice());
      operationEntity.setResult(calcResult(operationEntity));

      if (operationEntity.getBuyDate().isAfter(operationEntity.getSellDate())) {
        operationEntity.setType(OperationEntity.Type.SHORT);
      } else {
        operationEntity.setType(OperationEntity.Type.LONG);
      }
      operationEntity.setEarningDuration(Math.abs(ChronoUnit.HOURS.between(operationEntity.getBuyDate(), operationEntity.getSellDate())));
    }
  }

  private void mergeOperationEntity(OperationEntity savedOperationEntity, OperationEntity currentOperationEntity) {
    savedOperationEntity.setBuyQuantity(savedOperationEntity.getBuyQuantity() + 1);
    savedOperationEntity.setBuySum(savedOperationEntity.getBuyPrice().multiply(BigDecimal.valueOf(savedOperationEntity.getBuyQuantity())));

    BigDecimal buyCommission = savedOperationEntity.getBuyCommission();
    if (buyCommission != null && !BigDecimal.ZERO.equals(buyCommission) && currentOperationEntity.getBuyCommission() != null) {
      savedOperationEntity.setBuyCommission(buyCommission.add(currentOperationEntity.getBuyCommission()));
    } else {
      savedOperationEntity.setBuyCommission(BigDecimal.ZERO);
    }

    if (currentOperationEntity.isSell()) {
      BigDecimal sellCommission = savedOperationEntity.getSellCommission();
      if (sellCommission != null && !BigDecimal.ZERO.equals(sellCommission)) {
        savedOperationEntity.setSellCommission(sellCommission.add(currentOperationEntity.getSellCommission()));
      } else {
        savedOperationEntity.setSellCommission(BigDecimal.ZERO);
      }
      savedOperationEntity.setSellQuantity(savedOperationEntity.getSellQuantity() + 1);
      savedOperationEntity.setSellSum(savedOperationEntity.getSellPrice().multiply(BigDecimal.valueOf(savedOperationEntity.getSellQuantity())));
      savedOperationEntity.setResult(calcResult(savedOperationEntity));

    }
  }

  private BigDecimal calcResult(OperationEntity operationEntity) {
    BigDecimal buyCommission = BigDecimal.ZERO;
    if (operationEntity.getBuyCommission() != null) {
      buyCommission = operationEntity.getBuyCommission();
    }

    BigDecimal sellCommission = BigDecimal.ZERO;
    if (operationEntity.getSellCommission() != null) {
      sellCommission = operationEntity.getSellCommission();
    }

    return operationEntity.getSellSum()
            .subtract(operationEntity.getBuySum())
            .add(buyCommission)
            .add(sellCommission);


  }

  private OperationEntity createOperationEntity(StorageItem storageItemFirst, StorageItem storageItemSecond) {
    OperationEntity operationEntity;
    if (OperationType.Sell.equals(storageItemFirst.getOperationType())) {
      operationEntity = createDummyOperationEntity(storageItemFirst, storageItemSecond);
    } else {
      operationEntity = createDummyOperationEntity(storageItemSecond, storageItemFirst);
    }
    if (storageItemSecond == null) {
      operationEntity.setSell(false);
    } else {
      operationEntity.setSell(true);
    }

    return operationEntity;
  }

  private OperationEntity createDummyOperationEntity(StorageItem first, StorageItem second) {

    OperationEntity operationEntity = new OperationEntity();
    if (first != null) {
      operationEntity.setSellCommission(first.getCommission());
      operationEntity.setSellPrice(first.getPrice());
      operationEntity.setSellDate(first.getDate().atZoneSameInstant(ZoneId.systemDefault()));
      operationEntity.setInstrument(figiDictionaryService.getInstrumentByFigi(first.getFigi()).name);
    }

    if (second != null) {
      operationEntity.setBuyCommission(second.getCommission());
      operationEntity.setBuyPrice(second.getPrice());
      operationEntity.setBuyDate(second.getDate().atZoneSameInstant(ZoneId.systemDefault()));
      operationEntity.setInstrument(figiDictionaryService.getInstrumentByFigi(second.getFigi()).name);
    }

    return operationEntity;
  }

//  private boolean compareOperationType(OperationType first, OperationType second) {
//    if (((first == OperationType.Buy || first == OperationType.BuyCard) && second == OperationType.Sell) ||
//            (first == OperationType.Sell && (second == OperationType.Buy || second == OperationType.BuyCard))) {
//      return false;
//    }
//    return true;
//
////    if ((first == OperationType.Buy && second == OperationType.BuyCard)
////      || first == OperationType.BuyCard && second == OperationType.Buy) {
////      return true;
////    }
////    return false;
//  }

  @Builder
  @Data
  private static class StorageItem {

    private BigDecimal commission;
    private BigDecimal price;
    private OffsetDateTime date;
    private OperationType operationType;
    private String figi;
  }


}
