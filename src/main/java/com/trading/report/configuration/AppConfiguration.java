package com.trading.report.configuration;

import com.trading.report.service.DictionaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.SandboxOpenApi;
import ru.tinkoff.invest.openapi.models.operations.OperationsList;
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApiFactory;

import java.time.OffsetDateTime;
import java.util.concurrent.Executors;

@Log
@Configuration
@RequiredArgsConstructor
public class AppConfiguration {

  @Value("${project.token}")
  private String token;

  @Bean
  public OpenApi init() {
    OkHttpOpenApiFactory factory = new OkHttpOpenApiFactory(token, log);
    OpenApi api = factory.createOpenApiClient(Executors.newSingleThreadExecutor());
    //((SandboxOpenApi) api).getSandboxContext().performRegistration(null).join();
    return api;
  }
}
