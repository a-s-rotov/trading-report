package com.trading.report.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.models.market.Instrument;
import ru.tinkoff.invest.openapi.models.market.InstrumentsList;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class DictionaryService {
  @Autowired
  private OpenApi api;

  private Map<String, Instrument> instruments;

  @PostConstruct
  private void init() {

    List<CompletableFuture<InstrumentsList>> futureList = new ArrayList<>();
    futureList.add(api.getMarketContext().getMarketStocks());
    futureList.add(api.getMarketContext().getMarketCurrencies());
    futureList.add(api.getMarketContext().getMarketBonds());
    futureList.add(api.getMarketContext().getMarketEtfs());

    instruments = futureList.stream()
            .flatMap(item -> item.join().instruments.stream())
            .collect(Collectors.toMap(i -> i.figi, i -> i));

  }

  public Instrument getInstrumentByFigi(String figi) {
    return instruments.get(figi);
  }
}
