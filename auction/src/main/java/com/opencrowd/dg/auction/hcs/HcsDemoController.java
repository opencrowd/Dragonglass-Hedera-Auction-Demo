package com.opencrowd.dg.auction.hcs;

import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.opencrowd.dg.auction.AuctionBase;
import com.opencrowd.dg.auction.Bid;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller(value = "hcs")
public class HcsDemoController extends AuctionBase {

  private final static Logger LOGGER = LoggerFactory.getLogger(HcsDemoController.class);
  private boolean started = false;
  private boolean ended = false;
  private long AUCTION_DURATION_SEC = 180;
  private long auctionEndTime = 0;
  private final AtomicLong counter = new AtomicLong();
  @Autowired
  private HcsAuctionService auctionService;
  private ConsensusTopicId lastAuction;
  private Bid highestBid;

  @Autowired
  public HcsDemoController(
      @Value("${hedera.account.Alice.ID}") String aliceAccount,
      @Value("${hedera.account.Bob.ID}") String bobAccount,
      @Value("${hedera.account.Carol.ID}") String carolAccount,
      @Value("${hedera.account.manager.ID}") String managerAccount,
      @Value("${hedera.topic.default:}") String defaultTopic
  ) throws Throwable {
    super(aliceAccount, bobAccount, carolAccount, managerAccount);
    if (!defaultTopic.isBlank()) {
      lastAuction = parseTopicID(defaultTopic);
      ended = true;
    }
    auctionEndTime = System.currentTimeMillis() + AUCTION_DURATION_SEC * 1000;
    highestBid = new Bid(-1, "", 0, "", "");
    history.clear();
  }

  @PostMapping("/hcs/auction/newAuction/{biddingTimeSec}")
  public ResponseEntity<ConsensusTopicId> newAuction(@PathVariable long biddingTimeSec)
      throws Exception {
    final ConsensusTopicId topic = auctionService.createTopic();
    if (topic == null || topic.topic == 0) {
      throw new Exception("(: Faild to create auction!");
    }
    initiaize(topic, biddingTimeSec);

    String msg = "Created new auction: topicId = " + topic;
    LOGGER.info(msg);
    return ResponseEntity.ok(topic);
  }

  private void initiaize(ConsensusTopicId topic,
      @PathVariable long biddingTimeSec) {
    lastAuction = topic;
    AUCTION_DURATION_SEC = biddingTimeSec;
    auctionEndTime = System.currentTimeMillis() + AUCTION_DURATION_SEC * 1000;
    started = false;
    ended = false;
    history.clear();
  }

  /**
   * Endpoint for resetting an existing auction instance by restoring the state to when the auction
   * instance was first deployed.
   *
   * @param contractId the contract ID in the form of 0.0.x
   * @return transaction record of this reset contract call
   * @throws Exception
   */
  @PostMapping("/hcs/auction/resetAuction/{topicId}")
  public ResponseEntity<String> resetAuction(@PathVariable String topicId) throws Exception {
    final Long topic = Long.valueOf(topicId.replaceFirst("0.0.", ""));
    if (lastAuction == null || lastAuction.topic != topic) {
      initiaize(new ConsensusTopicId(0, 0,
          topic), 60);

    }
    if (!isAuctionTimeExpired()) {
      throw new Exception("Auction not yet ended.");
    }
    if (!ended) {
      throw new Exception("auctionEnd has not been called.");
    }

    String msg = auctionService.sendMessage(parseTopicID(topicId),
        "{\"type\": \"resetAuction\", \"topicId\": \"" + topicId + "\"}");

    history.clear();
    started = false;
    auctionEndTime = System.currentTimeMillis() + AUCTION_DURATION_SEC * 1000;
    highestBid = new Bid(-1, "", 0, "", "");
    ended = false;
    LOGGER.info(msg);
    return ResponseEntity.ok(msg);
  }

  /**
   * Endpoint for starting an auction.
   *
   * @param contractId the contract ID in the form of 0.0.x
   * @return transaction record of this contract call
   * @throws Exception
   */
  @PostMapping("/hcs/auction/startTimer/{topicId}")
  public ResponseEntity<String> startTimer(@PathVariable(required = false) String topicId)
      throws Exception {
    auctionEndTime = System.currentTimeMillis() + AUCTION_DURATION_SEC * 1000;
    started = true;
    ended = false;
    String msg = auctionService.sendMessage(parseTopicID(topicId),
        "{\"type\": \"startTimer\", \"topicId\": \"" + topicId + "\"}");

    LOGGER.info(msg);
    return ResponseEntity.ok(msg);
  }

  /**
   * Endpoint for making a single bid contract call.
   *
   * @param bidder  the name of the bidder, e.g. Alice, Bob, or Carol
   * @param amount  the bidding amount in tiny bars
   * @param topicId the contract ID in the form of 0.0.x
   * @return the transaction record of the call
   * @throws Exception
   */
  @PostMapping("/hcs/auction/singleBid/{bidder}/{amount}/{topicId}")
  public ResponseEntity<String> singleBid(@PathVariable String bidder, @PathVariable long amount,
      @PathVariable(required = false) String topicId) throws Exception {
    String response = bidAuction(bidder, amount, topicId);
    LOGGER.info(response);
    return ResponseEntity.ok(response);
  }

  /**
   * Endpoint for making a single bid contract call and subsequently starting random bidding for
   * demo purposes.
   *
   * @param bidder       the name of the bidder, e.g. Alice, Bob, or Carol
   * @param amount       the bidding amount in tiny bars
   * @param contractAddr the contract ID in the form of 0.0.x
   * @return the transaction record of the call
   * @throws Exception
   */
  @PostMapping("/hcs/auction/bid/{bidder}/{amount}/{topicId}")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public String postBid(@PathVariable String bidder, @PathVariable long amount,
      @PathVariable String topicId) throws Exception {
    String response = bidAuction(bidder, amount, topicId);
    Thread.sleep(1000);
    initiateRandomBidding(topicId);
    LOGGER.info("postBid with random bidding done");
    return response;
  }


  @PostMapping("/hcs/auction/end/{topicId}")
  public ResponseEntity<String> endAuction(@PathVariable String topicId) throws Exception {
    if (!isAuctionTimeExpired()) {
      throw new Exception("Auction not yet ended.");
    }
    if (ended == true) {
      throw new Exception("auctionEnd has already been called.");
    }

    String result = auctionService.sendMessage(parseTopicID(topicId),
        "{\"type\": \"auctionEnd\", "
            + "\"topicId\": \"" + topicId
            + "\", \"highestBidder\": \"" + highestBid.getBidder()
            + "\", \"highestBidderAddress\": \"" + highestBid.getBidderAddr()
            + "\", \"highestBid\": " + highestBid.getAmount()
            + "}");
    ended = true;

    LOGGER.info(result);
    return ResponseEntity.ok(result);
  }

  /**
   * Make a single bid contract call.
   *
   * @param bidder  the name of the bidder, e.g. Alice, Bob, or Carol
   * @param amount  the bidding amount in tiny bars
   * @param topicId the contract ID in the form of 0.0.x
   * @return the transaction record of the call
   * @throws Exception
   */
  public String bidAuction(String bidder, long amount, String topicId) throws Exception {
    String bidderAddr = users.get(bidder);

    if (isAuctionTimeExpired()) {
      if (!ended) {
        endAuction(topicId);
      }
      throw new Exception("Auction already ended.");
    }

    if (topicId == null && lastAuction != null) {
      topicId = lastAuction.toString();
    }
    long id = counter.getAndIncrement();
    Bid bid = new Bid(id, bidder, amount, bidderAddr, topicId);

    if (amount <= highestBid.getAmount()) {
      throw new Exception("There already is a higher bid.");
    }

    history.put(getKey(bidder, id), bid);
    String rv = auctionService.sendMessage(bidderAddr, parseTopicID(topicId),
        String.format(
            "{\"type\": \"bid\", \"bidder\": \"%s\", \"bidderAddr\": \"%s\", \"amount\": %d}",
            bid.getBidder(), bid.getBidderAddr(), bid.getAmount()));

    if (rv.contains("Success")) {
      highestBid = bid;
    }

    LOGGER.info("path bid submitted: bid = " + bid);

    return rv;
  }


  /**
   * Random bidding by Alice, Bob, and Carol.
   *
   * @param topicId the contract ID in the form of 0.0.x
   * @return status of success
   * @throws Exception
   */
  public ResponseEntity<Map<String, String>> initiateRandomBidding(String topicId)
      throws Exception {
    final Context context = new Context(1000L);
    final Timer timer = new Timer();
    final TimerTask timerTask = new TimerTask() {
      //	    private int counter = 0;
      private int index = 0;

      @Override
      public void run() {
        try {
          if (index >= userList.length) {
            index = 0;
          }
          final String bidder = userList[index];
          long bid = context.getBid();
          context.setBid(++bid);
          final String response = bidAuction(bidder, bid, topicId);
          LOGGER.info(response);
          if (isAuctionTimeExpired()) {
            endAuction(topicId);
            cancel();
            timer.purge();
            LOGGER.info("Stopped auto bidding!");
            return;
          } else if (response.indexOf("Success") > -1) {
            index++;
          }
        } catch (Exception e) {
          cancel();
          timer.purge();
          LOGGER.error(e.getMessage(), e);
        }
      }
    };
    timer.schedule(timerTask, 2000, 3000);
    return ResponseEntity.ok(Map.of("status", "success"));
  }

  protected boolean isAuctionTimeExpired() {
    Boolean rv = false;
    if (ended) {
      rv = true;
    } else {
      rv = System.currentTimeMillis() >= auctionEndTime;
    }

    return rv;
  }

  /**
   * Endpoint for getting the account ID and name of the bidders for the demo.
   *
   * @return the account ID to bidder name map
   */
  @GetMapping("/hcs/auction/bidders")
  public ResponseEntity<Map<String, String>> getAllBidders() {
    return ResponseEntity.ok(accounts);
  }

  /**
   * Parse topic string in the form of "0.0.x".
   *
   * @param contractString topic in string form
   * @return converted topic ID
   */
  public static ConsensusTopicId parseTopicID(String contractString) {
    String[] parts = contractString.split("\\.");
    ConsensusTopicId contractId = new ConsensusTopicId(Long.parseLong(parts[0]),
        Long.parseLong(parts[1]),
        Long.parseLong(parts[2]));
    return contractId;
  }

}
