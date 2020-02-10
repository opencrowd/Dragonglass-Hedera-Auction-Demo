package com.opencrowd.dg.auction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedera.hashgraph.sdk.TransactionRecord;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.contract.ContractId;
import java.util.HashMap;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Provides REST API to auction contract deployed on Hedera networks.
 */
@Controller
public class AuctionController {

  private String DEFAULT_CONTRACT;

  private final AtomicLong counter = new AtomicLong();
  private Map<String, Bid> history = new HashMap<>();
  private Map<String, String> users = null;
  private Map<String, String> accounts = null;
  private String[] userList = new String[]{"Bob", "Carol", "Alice"};

  private AuctionService auctionService;

  private final static Logger LOGGER = LoggerFactory.getLogger(AuctionController.class);

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  public AuctionController(AuctionService auctionService,
      @Value("${hedera.account.Alice.ID}") String aliceAccount,
      @Value("${hedera.account.Bob.ID}") String bobAccount,
      @Value("${hedera.account.Carol.ID}") String carolAccount,
      @Value("${hedera.account.manager.ID}") String managerAccount,
      @Value("${hedera.contract.default:}") String DEFAULT_CONTRACT
  ) throws Throwable {
    this.auctionService = auctionService;
    this.DEFAULT_CONTRACT = DEFAULT_CONTRACT;

    users = Map
        .of("Alice", aliceAccount, "Bob", bobAccount, "Carol", carolAccount);

    accounts = Map
        .of(aliceAccount, "Alice", bobAccount, "Bob", carolAccount, "Carol");
  }

  public static String account2Str(AccountId account) {
    return "0.0." + account.account;
  }

  /**
   * Endpoint for getting a past bid info.
   */
  @GetMapping("/getBid")
  @ResponseBody
  public Bid getBid(@RequestParam(name = "bidder", required = true) String bidder,
      @RequestParam(name = "id", required = true) long id) {
    Bid bid = history.get(getKey(bidder, id));
    return bid;
  }

  /**
   * Generate a key for bid history.
   *
   * @param bidder the name of the bidder
   * @param id     the id of the bid
   * @return generated key
   */
  private String getKey(String bidder, long id) {
    return bidder + "_" + id;
  }

  /**
   * Endpoint for making a auction end contract call.
   *
   * @param bidder       the name of the calling user
   * @param contractAddr the contract ID in the form of 0.0.x
   * @return the call transaction record
   * @throws Exception
   */
  @PostMapping("/endAuction/{bidder}/{contractAddr}")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public String postAuctionEnd(@PathVariable String bidder, @PathVariable String contractAddr)
      throws Exception {
    String bidderAddr = users.get(bidder);

    if (contractAddr == null) {
      contractAddr = DEFAULT_CONTRACT;
    }

    TransactionRecord record = auctionService.endAuction(bidderAddr, contractAddr);

    return objectMapper.writeValueAsString(record);
  }

  /**
   * Endpoint for making a single bid contract call.
   *
   * @param bidder       the name of the bidder, e.g. Alice, Bob, or Carol
   * @param amount       the bidding amount in tiny bars
   * @param contractAddr the contract ID in the form of 0.0.x
   * @return the transaction record of the call
   * @throws Exception
   */
  @PostMapping("/singleBid/{bidder}/{amount}/{contractAddr}")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public String singleBid(@PathVariable String bidder, @PathVariable long amount,
      @PathVariable String contractAddr) throws Exception {
    String response = bidAuction(bidder, amount, contractAddr);
    return response;
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
  @PostMapping("/bid/{bidder}/{amount}/{contractAddr}")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public String postBid(@PathVariable String bidder, @PathVariable long amount,
      @PathVariable String contractAddr) throws Exception {
    String response = bidAuction(bidder, amount, contractAddr);
    Thread.sleep(1000);
    initiateRandomBidding(contractAddr);
    return response;
  }

  /**
   * Make a single bid contract call.
   *
   * @param bidder       the name of the bidder, e.g. Alice, Bob, or Carol
   * @param amount       the bidding amount in tiny bars
   * @param contractAddr the contract ID in the form of 0.0.x
   * @return the transaction record of the call
   * @throws Exception
   */
  public String bidAuction(String bidder,
      long amount,
      String contractAddr) throws Exception {
    String bidderAddr = users.get(bidder);

    if (contractAddr == null) {
      contractAddr = DEFAULT_CONTRACT;
    }

    long id = counter.getAndIncrement();
    Bid bid = new Bid(id, bidder, amount, bidderAddr, contractAddr);
    history.put(getKey(bidder, id), bid);
    TransactionRecord record = auctionService.bid(bid);
    LOGGER.info("path bid submitted: bid = " + bid);

    return objectMapper.writeValueAsString(record);
  }

  /**
   * Endpoint for creating a new auction contract instance.
   *
   * @param biddingTimeSec  the auction duration in seconds
   * @param beneficiaryAddr the beneficiary account ID in the form of 0.0.x
   * @return contract ID of the created instance
   * @throws Exception
   */
  @PostMapping("/newAuction/{biddingTimeSec}/{beneficiaryAddr}")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public String newAuction(@PathVariable long biddingTimeSec, @PathVariable String beneficiaryAddr)
      throws Exception {
    history.clear();
    ContractId auctionContract = auctionService.createAuction(beneficiaryAddr, biddingTimeSec);
    DEFAULT_CONTRACT = "0.0." + auctionContract.contract;
    LOGGER.info("set DEFAULT_CONTRACT = " + DEFAULT_CONTRACT);
    return auctionContract.toString();
  }

  /**
   * Endpoint for getting the account ID and name of the bidders for the demo.
   *
   * @return the account ID to bidder name map
   */
  @GetMapping("/bidders")
  public ResponseEntity<Map<String, String>> getAllBidders() {
    return ResponseEntity.ok(accounts);
  }

  /**
   * Endpoint for starting random bidding by Alice, Bob, and Carol.
   *
   * @param contractId the contract ID in the form of 0.0.x
   * @return status of success
   * @throws Exception
   */
  @PostMapping("/bid/{contractId}")
  public ResponseEntity<Map<String, String>> initiateRandomBidding(@PathVariable String contractId)
      throws Exception {
    final Context context = new Context(1000L);
    final Timer timer = new Timer();
    final TimerTask timerTask = new TimerTask() {
      private int counter = 0;
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
          final String response = bidAuction(bidder, bid, contractId);
          LOGGER.info(response);
          if (response.contains("auctionEnd has already been called")) {
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

  class Context {

    private long bid;

    public Context(long bid) {
      this.bid = bid;
    }

    public long getBid() {
      return bid;
    }

    public void setBid(long bid) {
      this.bid = bid;
    }
  }

  /**
   * Endpoint for resetting an existing auction instance by restoring the state to when the auction
   * instance was first deployed.
   *
   * @param contractId the contract ID in the form of 0.0.x
   * @return transaction record of this reset contract call
   * @throws Exception
   */
  @PostMapping("/resetAuction/{contractId}")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public String resetAuction(@PathVariable String contractId) throws Exception {
    history.clear();
    TransactionRecord record = auctionService.resetAuction(contractId);
    LOGGER.info("reset contract = " + contractId);
    return objectMapper.writeValueAsString(record);
  }

  /**
   * Endpoint for starting an auction.
   *
   * @param contractId the contract ID in the form of 0.0.x
   * @return transaction record of this contract call
   * @throws Exception
   */
  @PostMapping("/startTimer/{contractId}")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public String startTimer(@PathVariable String contractId) throws Exception {
    TransactionRecord record = auctionService.startTimer(contractId);
    LOGGER.info("restarted timer for contract = " + contractId);
    return objectMapper.writeValueAsString(record);
  }

}
