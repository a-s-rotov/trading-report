package com.trading.report.configuration;

import com.trading.report.subscriber.StreamingApiSubscriber;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.SandboxOpenApi;
import ru.tinkoff.invest.openapi.models.market.CandleInterval;
import ru.tinkoff.invest.openapi.models.streaming.StreamingRequest;
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApiFactory;

@Log
@Configuration
@RequiredArgsConstructor
public class AppConfiguration {

  @Value("${project.token}")
  private String token;

  @Bean
  public OpenApi init() {
    OkHttpOpenApiFactory factory = new OkHttpOpenApiFactory(token, log);
    OpenApi api = factory.createSandboxOpenApiClient(Executors.newSingleThreadExecutor());
    ((SandboxOpenApi) api).getSandboxContext().performRegistration(null).join();
    return api;
  }


}
