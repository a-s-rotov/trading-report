package com.trading.report.controller;

import com.trading.report.service.OperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.tinkoff.invest.openapi.models.Currency;
import ru.tinkoff.invest.openapi.models.operations.OperationsList;

@Controller
public class ReportController {
    @Autowired
    private OperationService service;

    @GetMapping("/report")
    public String greeting(Model model) {
        model.addAttribute("usd", service.getOperations().get(Currency.USD));
        model.addAttribute("eur", service.getOperations().get(Currency.EUR));
        model.addAttribute("rub", service.getOperations().get(Currency.RUB));
        return "report";
    }
}
