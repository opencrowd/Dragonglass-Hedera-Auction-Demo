package com.opencrowd.dg.auction;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
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

import com.hedera.hashgraph.sdk.TransactionRecord;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.contract.ContractId;


@Controller
public class AuctionController {

  @Value("${hedera.account.Alice:0.0.2}")
  private String aliceAccount;

  @Value("${hedera.account.Bob:0.0.2}")
  private String bobAccount;

  @Value("${hedera.account.Carol:0.0.2}")
  private String carolAccount;

  @Value("${hedera.account.Dave:0.0.2}")
  private String daveAccount;

  @Value("${hedera.account.manager:0.0.2}")
  private String managerAccount;

  @Value("${hedera.account.beneficiary:0.0.2}")
  private String beneficiaryAccount;

  @Value("${hedera.contract.default:}")
  private String DEFAULT_CONTRACT;

  private final AtomicLong counter = new AtomicLong();
  private Map<String, Bid> history = new HashMap<>();

  private Map<String, String> users = null;
  private Map<String, String> accounts = null;
  private String[] userList = new String[]{"Bob", "Carol", "Alice"};

  private AuctionService auctionService;

  private final static Logger LOGGER = Logger.getLogger(AuctionController.class);

  @Autowired
//  public AuctionController(AuctionService auctionService) {
  public AuctionController(AuctionService auctionService,
      @Value("${umbrella.property.file:umbrellaTest.properties}") String testConfigFilePath,
      @Value("${hedera.host:localhost}") String hederaHost,
      @Value("${hedera.account.Alice:0.0.2}") String aliceAccount,
      @Value("${hedera.account.Bob:0.0.2}") String bobAccount,
      @Value("${hedera.account.Carol:0.0.2}") String carolAccount,
      @Value("${hedera.account.Dave:0.0.2}") String daveAccount,
      @Value("${hedera.account.manager:0.0.2}") String managerAccount,
      @Value("${hedera.account.beneficiary:0.0.2}") String beneficiaryAccount,
      @Value("${hedera.contract.default:}") String DEFAULT_CONTRACT
  ) throws Throwable {
    this.auctionService = auctionService;
    this.aliceAccount = aliceAccount;
    this.bobAccount = bobAccount;
    this.carolAccount = carolAccount;
    this.daveAccount = daveAccount;
    this.managerAccount = managerAccount;
    this.beneficiaryAccount = beneficiaryAccount;
    this.DEFAULT_CONTRACT = DEFAULT_CONTRACT;

    users = Map
        .of("Alice", aliceAccount, "Bob", bobAccount, "Carol", carolAccount, "Dave", daveAccount);

    accounts = Map
        .of(aliceAccount, "Alice", bobAccount, "Bob", carolAccount, "Carol", daveAccount, "Dave");
  }

  public static String account2Str(AccountId account) {
    return "0.0." + account.account;
  }

  @GetMapping("/getBid")
  @ResponseBody
  public Bid getBid(@RequestParam(name = "bidder", required = true) String bidder,
      @RequestParam(name = "id", required = true) long id) {
//		long seqId = counter.getAndIncrement();
//		Bid b1 = new Bid(seqId , Bidder.alice.name(), 100 + seqId, aliceAccount, DEFAULT_CONTRACT);
//		history.put(getKey(bidder, seqId), b1);
    Bid bid = history.get(getKey(bidder, id));
    return bid;
  }

  private String getKey(String bidder, long id) {
    return bidder + "_" + id;
  }

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

    return record.toString();
  }

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

  private String bidAuction(String bidder,
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

    return record.toString();
  }

  @PostMapping("/newAuction/{biddingTimeSec}/{beneficiaryAddr}")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public String newAuction(@PathVariable long biddingTimeSec, @PathVariable String beneficiaryAddr)
      throws Exception {
    if (beneficiaryAddr == null) {
      beneficiaryAddr = beneficiaryAccount;
    }

    ContractId auctionContract = auctionService.createAuction(beneficiaryAddr, biddingTimeSec);
    DEFAULT_CONTRACT = "0.0." + auctionContract.contract;
    LOGGER.info("set DEFAULT_CONTRACT = " + DEFAULT_CONTRACT);
    return auctionContract.toString();
  }

  @GetMapping("/bidders")
  public ResponseEntity<Map<String, String>> getAllBidders() {
    return ResponseEntity.ok(accounts);
  }

  @PostMapping("/bid/{contractId}")
  public ResponseEntity<Map<String, String>> initiateRandomBidding(@PathVariable String contractId)
      throws Exception {
    final Context context = new Context(1000L);
    final Timer timer = new Timer();
    final TimerTask timerTask = new TimerTask() {
//      private int counter = 0;
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
          } else if (response.indexOf("SUCCESS") > -1) {
            index++;
          }
        } catch (Exception e) {
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

  @PostMapping("/resetAuction/{contractId}")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public String resetAuction(@PathVariable String contractId) throws Exception {
    TransactionRecord record = auctionService.resetAuction(managerAccount, contractId);
    LOGGER.info("reset contract = " + contractId);
    return record.toString();
  }

  @PostMapping("/startTimer/{contractId}")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public String startTimer(@PathVariable String contractId) throws Exception {
    TransactionRecord record = auctionService.startTimer(managerAccount, contractId);
    LOGGER.info("restarted timer for contract = " + contractId);
    return record.toString();
  }
}
