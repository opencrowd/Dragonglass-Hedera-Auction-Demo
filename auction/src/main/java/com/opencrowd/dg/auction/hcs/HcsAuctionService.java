package com.opencrowd.dg.auction.hcs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.HederaStatusException;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.consensus.ConsensusMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicCreateTransaction;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicUpdateTransaction;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.tomcat.util.buf.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HcsAuctionService {

  private final static Logger LOGGER = LoggerFactory.getLogger(HcsAuctionService.class);
  private final String hederaNetwork;
  private final Map<AccountId, Client> clients;
  private AccountId aliceAccountId;
  private AccountId bobAccountId;
  private AccountId carolAccountId;
  private final AccountId managerAccountId;

  @Autowired
  private ObjectMapper objectMapper;

  public HcsAuctionService(
      @Value("${hedera.network}") String hederaNetwork,
      @Value("${hedera.account.Alice.ID}") String aliceAccount,
      @Value("${hedera.account.Alice.KEY}") String aliceKey,
      @Value("${hedera.account.Bob.ID}") String bobAccount,
      @Value("${hedera.account.Bob.KEY}") String bobKey,
      @Value("${hedera.account.Carol.ID}") String carolAccount,
      @Value("${hedera.account.Carol.KEY}") String carolKey,
      @Value("${hedera.account.manager.ID}") String managerAccount,
      @Value("${hedera.account.manager.KEY}") String managerKey) {
    this.hederaNetwork = hederaNetwork;
    clients = new HashMap<>();
    aliceAccountId = parseAccountID(aliceAccount);
    bobAccountId = parseAccountID(bobAccount);
    carolAccountId = parseAccountID(carolAccount);
    managerAccountId = parseAccountID(managerAccount);
    addClient(aliceAccountId, aliceKey);
    addClient(bobAccountId, bobKey);
    addClient(carolAccountId, carolKey);
    addClient(managerAccountId, managerKey);
  }

  public static AccountId parseAccountID(String accountStr) {
    String[] parts = accountStr.split("\\.");
    AccountId id = new AccountId(Long.parseLong(parts[0]), Long.parseLong(parts[1]),
        Long.parseLong(parts[2]));
    return id;
  }

  /**
   * Gets the client by account ID.
   */
  private Client getClient(AccountId accountId) {
    return clients.get(accountId);
  }

  public ConsensusTopicId createTopic() throws HederaStatusException {
    Client client = getClient(managerAccountId);
    final TransactionId transactionId = new ConsensusTopicCreateTransaction()
        .execute(client);
    //Grab the newly generated topic ID
    final ConsensusTopicId topicId = transactionId.getReceipt(client).getConsensusTopicId();
    LOGGER.info("Your topic ID is: " + topicId);
    return topicId;
  }

  public void updateTopic() throws HederaStatusException {
    Client client = getClient(managerAccountId);
    final TransactionId transactionId = new ConsensusTopicUpdateTransaction()
        .setTopicMemo("calling update")
        .execute(client);
    //Grab the newly generated topic ID
    final ConsensusTopicId topicId = transactionId.getReceipt(client).getConsensusTopicId();
    LOGGER.info("Your updated topic ID is: " + topicId);
  }

  public String sendMessage(ConsensusTopicId topicID, String message)
      throws HederaStatusException, JsonProcessingException {
    return sendMessage(managerAccountId.toString(), topicID, message);
  }

  public String sendMessage(String bidder, ConsensusTopicId topicID, String message)
      throws HederaStatusException, JsonProcessingException {
    Client client = getClient(parseAccountID(bidder));
    final TransactionId transactionId = new ConsensusMessageSubmitTransaction()
        .setTopicId(topicID)
        .setMessage(message)
        .execute(client);
    TransactionReceipt receipt = transactionId.getReceipt(client);
    final long topicSeqNumber = receipt.getConsensusTopicSequenceNumber();
    final byte[] topicRunningHash = receipt.getConsensusTopicRunningHash();
    String rv = String
        .format(
            "{\"status\": \"Success\", \"message\": \"Submitted Message on %s with topicSeqNumber %d and topicRunningHash %s\"}",
            topicID,
            topicSeqNumber, HexUtils.toHexString(topicRunningHash));
    rv += "\nmessage content ==> " + message;
    LOGGER.info(rv);
    return rv;
  }


  /**
   * Adds a client.
   *
   * @param accoutId   the account ID used by the client for payment
   * @param accountKey the correspoinding private key of the account
   */
  private void addClient(AccountId accoutId, String accountKey) {
    Client client = null;
    if (hederaNetwork.equals("mainnet")) {
      client = Client.forMainnet();
    } else // testnet
    {
      client = Client.forTestnet();
    }

    Ed25519PrivateKey privateKey = Ed25519PrivateKey
        .fromString(Objects.requireNonNull(accountKey));
    client.setOperator(accoutId, privateKey);
    clients.put(accoutId, client);
  }


}
