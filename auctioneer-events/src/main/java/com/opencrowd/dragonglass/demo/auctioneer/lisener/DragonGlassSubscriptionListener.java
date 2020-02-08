package com.opencrowd.dragonglass.demo.auctioneer.lisener;

import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("event")
public class DragonGlassSubscriptionListener {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(DragonGlassSubscriptionListener.class);

  @Autowired
  private SimpMessagingTemplate brokerMessagingTemplate;

  @SqsListener("${dragonglass.queue.bid:hashscan-test}")
  public void incomingBid(String message,
      @Header("SenderId") String senderId) {
    LOGGER.info("Bid : " + message);
    brokerMessagingTemplate.convertAndSend("/queue/bid", message);
  }

  @SqsListener("${dragonglass.queue.auctionEnd:hashscan-test}")
  public void auctionEnded(String message,
      @Header("SenderId") String senderId) {
    LOGGER.info("High : " + message);
    brokerMessagingTemplate.convertAndSend("/queue/auctionEnd", message);
  }

}
