package com.trading.report.subscriber;

import java.util.concurrent.Executor;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.example.unicast.AsyncSubscriber;
import ru.tinkoff.invest.openapi.models.streaming.StreamingEvent;


public class StreamingApiSubscriber extends AsyncSubscriber<StreamingEvent> {


  private final Logger logger;

  public StreamingApiSubscriber(@NotNull final Logger logger, @NotNull final Executor executor) {
    super(executor);
    this.logger = logger;
  }

  @Override
  protected boolean whenNext(final StreamingEvent event) {
    logger.info("New candle event received from Streaming API\n" + event);

    return true;
  }

}