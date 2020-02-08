package com.opencrowd.dragonglass.demo.auctioneer.lisener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@Profile("hcs")
public class HcsMessageListener {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(HcsMessageListener.class);

  @Autowired
  private ObjectMapper objectMapper;

  @SqsListener("${dragonglass.queue.topic:hashscan-test}")
  public void incomingBid(String message,
      @Header("SenderId") String senderId) {
    try {
      JsonNode root = objectMapper.readTree(message);
      String messageHex = root.get("message").toString().replace("\"", "");
      String messageString = new String(Hex.decodeHex(messageHex.toCharArray()),
          StandardCharsets.UTF_8);
      JsonNode messageRoot = objectMapper.readTree(messageString);
      String bidder = messageRoot.get("bidder").toString();
      Long amount = messageRoot.get("amount").asLong();
      LOGGER.info("Bidder : "+bidder+"   Amount: "+amount );
    } catch (JsonProcessingException | DecoderException e) {
      e.printStackTrace();
    }
  }

}
