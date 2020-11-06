package com.trading.report.service;

import com.trading.report.OperationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.models.Currency;
import ru.tinkoff.invest.openapi.models.market.Instrument;
import ru.tinkoff.invest.openapi.models.operations.Operation;
import ru.tinkoff.invest.openapi.models.operations.OperationStatus;
import ru.tinkoff.invest.openapi.models.operations.OperationType;
import ru.tinkoff.invest.openapi.models.operations.OperationsList;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OperationService {
    @Autowired
    private OpenApi api;

    @Autowired
    private DictionaryService dictionaryService;

    @Value("${project.startDate}")
    private String startDate;

    public Map<Currency, List<OperationEntity>> getOperations() {
        OperationsList operations = api
                .getOperationsContext()
                .getOperations(OffsetDateTime.parse(startDate), OffsetDateTime.now(), "", "")
                .join();
        return processOperations(operations);
    }

    private Map<Currency, List<OperationEntity>> processOperations(OperationsList operationsList) {
        Map<Currency, List<OperationEntity>> operationsMap = new HashMap<>();

        for (Currency currency : Currency.values()) {
            List<OperationEntity> entries = operationsList.operations.stream()
                    .filter(o -> o.currency.equals(currency))
                    .filter(o -> o.status.equals(OperationStatus.Done))
                    .filter(o -> !o.operationType.equals(OperationType.BrokerCommission))
                    .filter(o -> !o.operationType.equals(OperationType.PayIn))
                    .sorted(Comparator.comparing(o -> o.date))
                    .map(this::mapToEntity)
                    .collect(Collectors.toList());
            operationsMap.put(currency, entries);
        }

        for (List<OperationEntity> operationEntityList : operationsMap.values()) {
            List<OperationEntity> removeEntity = new ArrayList<>();

            ListIterator<OperationEntity> iterator = operationEntityList.listIterator();
            while (iterator.hasNext()) {
                OperationEntity currentEntity = iterator.next();

                if (currentEntity.getBuySum() != null
                        && currentEntity.getBuyQuantity() != null
                        && currentEntity.getResult() == null) {
                    OperationEntity found = getMatched(currentEntity, operationEntityList, iterator);
                    if (found != null) {
                        removeEntity.add(found);
                    }
                }
            }

            operationEntityList.removeAll(removeEntity);
            operationEntityList.stream().forEach(this::getInstrument);
        }


        return operationsMap;
    }

    private OperationEntity getMatched(
            OperationEntity entity,
            List<OperationEntity> operationEntityList,
            ListIterator<OperationEntity> iterator
    ) {
        OperationEntity found = operationEntityList.stream()
                .filter(e -> e.getSellSum() != null)
                .filter(e -> e.getSellQuantity() != null)
                .filter(e -> e.getResult() == null)
                .filter(e -> e.getInstrument().equalsIgnoreCase(entity.getInstrument()))
                .findFirst().orElse(null);

        if (found != null) {
            if ((entity.getBuyQuantity() + found.getSellQuantity()) == 0) {
                entity.setEndDate(found.getEndDate());
                entity.setSellQuantity(found.getSellQuantity());
                entity.setSellPrice(found.getSellPrice());
                entity.setSellCommission(found.getSellCommission());
                entity.setSellSum(found.getSellSum());

            } else if (entity.getBuyQuantity() > -found.getSellQuantity()) {
                OperationEntity newBuyEntity = (OperationEntity) entity.clone();
                newBuyEntity.setBuyQuantity(entity.getBuyQuantity() - found.getSellQuantity());
                newBuyEntity.setBuyCommission(
                        entity.getBuyCommission()
                                .divide(BigDecimal.valueOf(entity.getBuyQuantity()))
                                .multiply(BigDecimal.valueOf(newBuyEntity.getBuyQuantity()))
                );
                newBuyEntity.setBuySum(newBuyEntity.getBuyPrice()
                        .multiply(BigDecimal.valueOf(newBuyEntity.getBuyQuantity())));
                entity.setBuyQuantity(-found.getSellQuantity());
                entity.setBuyCommission(entity.getBuyCommission().subtract(newBuyEntity.getBuyCommission()));
                entity.setBuySum(entity.getBuySum().subtract(newBuyEntity.getBuySum()));
                entity.setEndDate(found.getEndDate());
                entity.setSellQuantity(found.getSellQuantity());
                entity.setSellPrice(found.getSellPrice());
                entity.setSellCommission(found.getSellCommission());
                entity.setSellSum(found.getSellSum());
                iterator.add(newBuyEntity);
                iterator.previous();
            } else if (entity.getBuyQuantity() < -found.getSellQuantity()) {
                OperationEntity newSellEntity = (OperationEntity) found.clone();
                newSellEntity.setSellQuantity(found.getSellQuantity() + entity.getBuyQuantity());
                newSellEntity.setSellCommission(
                        found.getSellCommission()
                                .divide(BigDecimal.valueOf(found.getSellQuantity()))
                                .multiply(BigDecimal.valueOf(newSellEntity.getSellQuantity()))
                );
                newSellEntity.setSellSum(newSellEntity.getSellPrice()
                        .multiply(BigDecimal.valueOf(-newSellEntity.getSellQuantity())));
                entity.setEndDate(found.getEndDate());
                entity.setSellQuantity(-entity.getBuyQuantity());
                entity.setSellPrice(found.getSellPrice());
                entity.setSellCommission(found.getSellCommission().subtract(newSellEntity.getSellCommission()));
                entity.setSellSum(found.getSellSum().subtract(newSellEntity.getSellSum()));
                iterator.add(newSellEntity);
                iterator.add(newSellEntity);
            }

            found.setResult(BigDecimal.ZERO);
            calculateResult(entity);
            return found;
        }

        return null;
    }

    private void calculateResult(OperationEntity entity) {
        entity.setResult(
                entity.getBuySum()
                        .add(entity.getBuyCommission())
                        .add(entity.getSellSum())
                        .add(entity.getSellCommission())
        );
        if (entity.getResult().intValue() > 0) {
            entity.setNetResult(entity.getResult().multiply(BigDecimal.valueOf(0.87)));
        } else {
            entity.setNetResult(entity.getResult());
        }

        entity.setEarningDuration(
                Period.between(entity.getStartDate().toLocalDate(),
                        entity.getEndDate().toLocalDate())
        );
    }

    private OperationEntity mapToEntity(Operation operation) {
        OperationEntity operationEntity = OperationEntity
                .builder()
                .currency(operation.currency)
                .instrument(operation.figi)
                .build();

        BigDecimal commission = operation.commission != null ? operation.commission.value : BigDecimal.ZERO;
        Integer quantity = operation.quantity;

        if (operation.quantity != null && operation.price != null
                && operation.payment.compareTo(operation.price.multiply(BigDecimal.valueOf(quantity))) != 0) {
            quantity = operation.payment.divide(operation.price, 2, RoundingMode.HALF_UP).intValue();
        }

        if (operation.operationType.equals(OperationType.Sell)) {
            operationEntity.setEndDate(operation.date);
            operationEntity.setSellPrice(operation.price);
            if (quantity != null) {
                operationEntity.setSellQuantity(-quantity);
            }
            operationEntity.setSellCommission(commission);
            operationEntity.setSellSum(operation.payment);
        } else {
            operationEntity.setStartDate(operation.date);
            operationEntity.setBuyPrice(operation.price);
            if (quantity != null) {
                operationEntity.setBuyQuantity(-quantity);
            }
            operationEntity.setBuyCommission(commission);
            operationEntity.setBuySum(operation.payment);
        }

        if (!operation.operationType.equals(OperationType.Buy) &&
                !operation.operationType.equals(OperationType.Sell)) {
            operationEntity.setInstrument(operation.operationType.toString());
            operationEntity.setResult(operation.payment);
            operationEntity.setNetResult(operation.payment);
        }

        return operationEntity;
    }

    private void getInstrument (OperationEntity operationEntity) {
        Instrument instrument = dictionaryService.getInstrumentByFigi(operationEntity.getInstrument());
        if (instrument != null) {
            operationEntity.setInstrument(instrument.name);
        }
    }


}
