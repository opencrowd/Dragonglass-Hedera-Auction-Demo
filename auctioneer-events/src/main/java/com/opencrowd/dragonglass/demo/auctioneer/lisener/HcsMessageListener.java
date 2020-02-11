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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("hcs")
public class HcsMessageListener {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(HcsMessageListener.class);

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private SimpMessagingTemplate brokerMessagingTemplate;

  private final String bidResponse = "{\"bidder\": \"%s\", \"account\": \"%s\", \"amount\": %d, \"consensusTime\": \"%s\"}";
  private final String auctionEndResponse = "{\"winner\": \"%s\", \"account\": \"%s\", \"amount\": %d}";


  @SqsListener("${dragonglass.queue.topic:hashscan-test}")
  public void incomingBid(String message,
      @Header("SenderId") String senderId) {
    try {
      JsonNode root = objectMapper.readTree(message);
      String messageHex = root.get("message").toString().replace("\"", "");
      String messageString = new String(Hex.decodeHex(messageHex.toCharArray()),
          StandardCharsets.UTF_8);
      JsonNode messageRoot = objectMapper.readTree(messageString);
      String consensusTime = root.get("consensusTime").textValue();
      if (!messageRoot.has("type")) {
        LOGGER.info("Incoming Message: " + messageString);
      } else if (messageRoot.get("type").asText().equals("bid")) {
        String bidder = messageRoot.get("bidder").asText();
        String bidderAddr = messageRoot.get("bidderAddr").asText();
        Long amount = messageRoot.get("amount").asLong();
        String data = String.format(bidResponse, bidder, bidderAddr, amount, consensusTime);
        LOGGER.info("Bid: :" + data);
        brokerMessagingTemplate.convertAndSend("/queue/bid", data);
      } else if (messageRoot.get("type").asText().equals("auctionEnd")) {
        String winner = messageRoot.get("highestBidder").asText();
        String bidderAddr = messageRoot.get("highestBidderAddress").asText();
        Long amount = messageRoot.get("highestBid").asLong();
        String data = String.format(auctionEndResponse, winner, bidderAddr, amount);
        LOGGER.info("Winner: :" + data);
        brokerMessagingTemplate.convertAndSend("/queue/auctionEnd", data);
      }
    } catch (JsonProcessingException | DecoderException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

}
