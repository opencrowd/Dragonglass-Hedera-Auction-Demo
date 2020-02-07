package com.opencrowd.dg.auction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hedera.hashgraph.sdk.TransactionRecord;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.contract.ContractFunctionResult;
import com.hedera.hashgraph.sdk.contract.ContractLogInfo;

/**
 * Provides auction users data.
 */
@Component
public abstract class AuctionBase {

	protected Map<String, Bid> history = new HashMap<>();
  protected Map<String, String> users = null;
  protected Map<String, String> accounts = null;
  protected String[] userList = new String[]{"Bob", "Carol", "Alice"};

  private final static Logger LOGGER = LoggerFactory.getLogger(AuctionBase.class);

  @Autowired
  public AuctionBase(
      String aliceAccount,
      String bobAccount,
      String carolAccount,
      String managerAccount
  ) throws Throwable {
    users = Map
        .of("Alice", aliceAccount, "Bob", bobAccount, "Carol", carolAccount);

    accounts = Map
        .of(aliceAccount, "Alice", bobAccount, "Bob", carolAccount, "Carol");
  }

  public static String account2Str(AccountId account) {
    return "0.0." + account.account;
  }

  /**
   * Getting a past bid info.
   */
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
  protected String getKey(String bidder, long id) {
    return bidder + "_" + id;
  }

  public class Context {

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
   * Convert transaction record to string
   * @param record transaction record to be converted
   * @return converted string
   */
	protected String toString(TransactionRecord record) {
		StringBuffer sb = new StringBuffer();
		String ln = "\n";
		sb
		.append("receipt status: " + record.receipt.status.name()).append(ln)
		.append("consensusTimestamp: " + record.consensusTimestamp).append(ln)
		.append("transactionID: " + record.transactionId).append(ln)
		.append("transactionFee: " + record.transactionFee).append(ln);
		
		ContractFunctionResult execResult = record.getContractExecuteResult();
		if(execResult != null) {
			sb.append("contractCallResult {\n\tgasUsed: " + execResult.gasUsed).append(ln);
			if(execResult.contractId.contract != 0)
				sb.append("\tcontractId: " + execResult.contractId).append(ln);
			if(execResult.errorMessage != null) {
				sb.append("\terrorMessage: " + execResult.errorMessage).append(ln);
				sb.append("\tcontractCallResult: " + CommonUtils.escapeBytes(execResult.asBytes())).append(ln);
			}

			List<ContractLogInfo> logs = execResult.logs;
			if(logs != null) {
				for(ContractLogInfo log : logs) {
					sb.append("\tlogInfo {\n");
					sb.append("\t\tcontractId: " + log.contractId).append(ln);
					sb.append("\t\tbloom: " + CommonUtils.escapeBytes(log.bloom)).append(ln);
					for(byte[] topic : log.topics) {
						sb.append("\t\ttopic: " + CommonUtils.escapeBytes(topic)).append(ln);
					}
					sb.append("\t\tdata: " + CommonUtils.escapeBytes(log.data)).append(ln);
					sb.append("\t}\n");
				}
			}
			sb.append("}\n");
		}
			
		String rv = sb.toString();
		return rv;
	}

	/**
	 * Make a single bid call.
	 *
	 * @param bidder       the name of the bidder, e.g. Alice, Bob, or Carol
	 * @param amount       the bidding amount in tiny bars
	 * @param contractAddrOrTopicId the contract or topic ID in the form of 0.0.x
	 * @return the transaction record of the call
	 * @throws Exception
	 */
	public abstract String bidAuction(String bidder,
	    long amount,
	    String contractAddrOrTopicId) throws Exception;
	
	/**
	 * Random bidding by Alice, Bob, and Carol.
	 *
	 * @param topicId the contract ID in the form of 0.0.x
	 * @return status of success
	 * @throws Exception
	 */
	public abstract ResponseEntity<Map<String, String>> initiateRandomBidding(String topicId)
	    throws Exception;

}
