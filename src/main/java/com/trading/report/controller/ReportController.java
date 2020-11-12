package com.trading.report.controller;

import com.trading.report.service.OperationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.tinkoff.invest.openapi.models.Currency;

@Controller
@RequiredArgsConstructor
public class ReportController {

  private final OperationService operationService;

  @GetMapping("/table-report")
  public String getTableReport(@RequestParam(value = "currency", required = false) String currency, Model model) {
    Currency currentCurrency = currency == null ? Currency.USD : Currency.valueOf(currency.toUpperCase());
    model.addAttribute("report", operationService.getOperations(currentCurrency));
    model.addAttribute("currency", currentCurrency.toString());
    return "table-report";
  }

  @GetMapping("/overall-report")
  public String getOverallReport(Model model) {

    return "overall-report";
  }
}
