package com.trading.report.configuration;

import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.SandboxOpenApi;
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApiFactory;

@Log
@Configuration
@Profile("sandbox")
@RequiredArgsConstructor
public class SandboxAppConfiguration {

  @Value("${project.token}")
  private String token;

  @Bean
  public OpenApi openApi() {
    OkHttpOpenApiFactory factory = new OkHttpOpenApiFactory(token, log);
    OpenApi api = factory.createOpenApiClient(Executors.newSingleThreadExecutor());
    ((SandboxOpenApi) api).getSandboxContext().performRegistration(null).join();
    return api;
  }
}
