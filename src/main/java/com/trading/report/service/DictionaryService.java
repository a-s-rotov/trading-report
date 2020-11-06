package com.trading.report.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.models.market.Instrument;
import ru.tinkoff.invest.openapi.models.market.InstrumentsList;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DictionaryService {
    @Autowired
    private OpenApi api;

    private Map<String, Instrument> instruments;

    @PostConstruct
    private void init() {
        InstrumentsList instrumentsList = api.getMarketContext().getMarketStocks().join();
        instruments = instrumentsList.instruments.stream().collect(Collectors.toMap(i -> i.figi, i -> i));
    }

    public Instrument getInstrumentByFigi(String figi) {
        return instruments.get(figi);
    }
}
