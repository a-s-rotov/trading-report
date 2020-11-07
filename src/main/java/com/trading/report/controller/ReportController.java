package com.trading.report.controller;

import com.trading.report.service.OperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.tinkoff.invest.openapi.models.Currency;

@Controller
public class ReportController {
  @Autowired
  private OperationService operationService;

  @GetMapping("/report")
  public String getReport(@RequestParam(value = "currency", required = false) String currency, Model model) {
    Currency currentCurrency = currency == null ? Currency.USD : Currency.valueOf(currency.toUpperCase());
    model.addAttribute("report", operationService.getOperations().get(currentCurrency));
    model.addAttribute("currency", currentCurrency.toString());
    return "report";
  }
}
