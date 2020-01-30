package com.opencrowd.dg.auction;

import java.time.Instant;
import java.util.Objects;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.ethereum.core.CallTransaction;
import org.ethereum.util.ByteUtil;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.proto.FileGetInfoQuery;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.HederaConstants;
import com.hedera.hashgraph.sdk.HederaNetworkException;
import com.hedera.hashgraph.sdk.HederaStatusException;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionRecord;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.contract.ContractCallQuery;
import com.hedera.hashgraph.sdk.contract.ContractCreateTransaction;
import com.hedera.hashgraph.sdk.contract.ContractExecuteTransaction;
import com.hedera.hashgraph.sdk.contract.ContractFunctionResult;
import com.hedera.hashgraph.sdk.contract.ContractId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.file.FileAppendTransaction;
import com.hedera.hashgraph.sdk.file.FileContentsQuery;
import com.hedera.hashgraph.sdk.file.FileCreateTransaction;
import com.hedera.hashgraph.sdk.file.FileId;
import com.hedera.hashgraph.sdk.file.FileInfo;
import com.hedera.hashgraph.sdk.file.FileInfoQuery;

import io.github.cdimascio.dotenv.Dotenv;

@Service
public class AuctionService {
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

	@Value("${hedera.contract.bin.file:}")
	private String DEFAULT_CONTRACT_FILE;

	@Value("${hedera.host:localhost}")
	private String hederaHost;

	private AccountId aliceAccountId;
	private AccountId bobAccountId;
	private AccountId carolAccountId;
	private AccountId daveAccountId;
	private AccountId mangerAccountId;
	private AccountId beneficiaryAccountId;

	protected static String testConfigFilePath = "config/auction.properties";
	private long start = System.currentTimeMillis();
	private int BID_INTERVAL_SEC = 2;
	private int BID_INCREMENT = 10;

	protected long defaultContract = 143766; // 143773; // 143766
	protected FileId AUCTION_BIN_FILE_ID = new FileId(0, 0, 143742);
	// protected long BIDDING_TIME_SEC = DAY_SEC * 30;
	protected long BIDDING_TIME_SEC = 30;
	protected ContractId DEFAULT_AUCTION_CONTRACT_ID = new ContractId(0, 0, defaultContract);
//  protected String AUCTION_FILE_NAME = "auctionR_sol_SimpleAuction.bin";
	protected String AUCTION_FILE_NAME = "auctionTimer_sol_SimpleAuction.bin";
	protected static AccountId nodeAccountId = new AccountId(0, 0, 3);
	protected boolean createNewContract = false;
	protected boolean createNewFile = false ;

	protected final static Logger log = LogManager.getLogger(AuctionService.class);
	private static final AccountId OPERATOR_ID = AccountId
	    .fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));
	private static final Ed25519PrivateKey OPERATOR_KEY = Ed25519PrivateKey
	    .fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));
	private Client client = null;
	
  protected static final String AUCTION_START_TIMER = "{\"constant\":false,\"inputs\":[],\"name\":\"startTimer\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}";
  protected static final String AUCTION_RESRET_ABI = "{\"constant\":false,\"inputs\":[],\"name\":\"auctionReset\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}";
  protected static final String AUCTION_CONSTRUCTOR_ABI =
      "{\"inputs\":[{\"internalType\":\"uint256\",\"name\":\"_biddingTime\",\"type\":\"uint256\"},{\"internalType\":\"address payable\",\"name\":\"_beneficiary\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}";
  protected static final String BID_ABI =
      "{\"constant\":false,\"inputs\":[],\"name\":\"bid\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"}";
  protected static final String AUCTION_END_ABI =
      "{\"constant\":false,\"inputs\":[],\"name\":\"auctionEnd\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}";
  protected static int FILE_PART_SIZE = 4096; //4K bytes

	@Autowired
	public AuctionService(@Value("${umbrella.property.file:umbrellaTest.properties}") String testConfigFilePath,
	    @Value("${hedera.host:localhost}") String hederaHost, @Value("${hedera.account.Alice:0.0.2}") String aliceAccount,
	    @Value("${hedera.account.Bob:0.0.2}") String bobAccount,
	    @Value("${hedera.account.Carol:0.0.2}") String carolAccount,
	    @Value("${hedera.account.Dave:0.0.2}") String daveAccount,
	    @Value("${hedera.account.manager:0.0.2}") String managerAccount,
	    @Value("${hedera.account.beneficiary:0.0.2}") String beneficiaryAccount,
	    @Value("${hedera.contract.bin.file:}") String DEFAULT_CONTRACT_FILE,
	    @Value("${hedera.contract.default:}") String DEFAULT_CONTRACT,
	    @Value("${hedera.account.key.path:}") String acccountKeyPath) throws Throwable {
		this.aliceAccount = aliceAccount;
		this.bobAccount = bobAccount;
		this.carolAccount = carolAccount;
		this.daveAccount = daveAccount;
		this.managerAccount = managerAccount;
		this.beneficiaryAccount = beneficiaryAccount;
		this.DEFAULT_CONTRACT_FILE = DEFAULT_CONTRACT_FILE;
		this.DEFAULT_CONTRACT = DEFAULT_CONTRACT;
		aliceAccountId = parseAccountID(aliceAccount);
		bobAccountId = parseAccountID(bobAccount);
		carolAccountId = parseAccountID(carolAccount);
		daveAccountId = parseAccountID(daveAccount);
		mangerAccountId = parseAccountID(managerAccount);
		beneficiaryAccountId = parseAccountID(beneficiaryAccount);
		demoTestnet();
	}

	public static AccountId parseAccountID(String accountStr) {
		String[] parts = accountStr.split("\\.");
		AccountId id = new AccountId(Long.parseLong(parts[0]), Long.parseLong(parts[1]), Long.parseLong(parts[2]));
		return id;
	}

	public void demoTestnet() throws Throwable {
		// `Client.forMainnet()` is provided for connecting to Hedera mainnet
		client = Client.forTestnet();

		// Defaults the operator account ID and key such that all generated transactions
		// will be paid for
		// by this account and be signed by this key
		client.setOperator(OPERATOR_ID, OPERATOR_KEY);

//    aliceAccountId = AccountId.newBuilder().setAccountNum(112224).build();
//    bobAccountId = AccountId.newBuilder().setAccountNum(69102).build();

//		AccountId[] accounts = {mangerAccountId, aliceAccountId, bobAccountId, carolAccountId, daveAccountId, beneficiaryAccountId};
//		Map<String, Long> bals = getBalances(accounts);
//		log.info("account balances: " + bals);
		if (DEFAULT_CONTRACT_FILE == null || DEFAULT_CONTRACT_FILE.isBlank())
			uploadBinFile(mangerAccountId);
		else
			AUCTION_BIN_FILE_ID = parseFileID(DEFAULT_CONTRACT_FILE);
	}

	private void uploadBinFile(AccountId mangerAccount) throws Throwable {
		byte[] bytes = CommonUtils.readBinaryFileAsResource(AUCTION_FILE_NAME, getClass());
		log.info("bin file = " + AUCTION_FILE_NAME + ", size in bytes = " + bytes.length);

		FileId newFileId = uploadLargeFile(bytes);
		
		AUCTION_BIN_FILE_ID = newFileId;
		log.info(":) Auction bin file created successfully, file ID = " + newFileId);
	}
	
	public String getFileContentHex(FileId fid) throws HederaNetworkException, HederaStatusException {
    byte[] contents = getFileContent(fid);

    String hex = Hex.encodeHexString(contents);

    log.info("File content size = " + contents.length + ", hex=" + hex);
    return hex;
	}

	public ContractId createAuction(String beneficiaryStr, long biddingTime) throws HederaNetworkException, HederaStatusException {
    byte[] beneficiary = convert2SolidityAddress(beneficiaryStr);
    byte[] constructorParams = getEncodedConstructor(biddingTime, beneficiary);
		TransactionId contractTxId = new ContractCreateTransaction()
		    .setAutoRenewPeriod(HederaConstants.DEFAULT_AUTORENEW_DURATION).setGas(217000).setBytecodeFileId(AUCTION_BIN_FILE_ID)
		    .setContractMemo("OpenCrowd Dragonglass Auction Demo Contract")
		    .setConstructorParams(constructorParams )
		    // set an admin key so we can delete the contract later
		    .setAdminKey(OPERATOR_KEY.publicKey).setMaxTransactionFee(1500000000l).execute(client);

		TransactionReceipt contractReceipt = contractTxId.getReceipt(client);
		log.info(contractReceipt.toProto());
		ContractId newContractId = contractReceipt.getContractId();
		Assert.assertNotNull(newContractId);
		DEFAULT_AUCTION_CONTRACT_ID = newContractId;
		log.info(":) Auction contract created successfully, contract ID = " + newContractId);
		return newContractId;
	}

	public void autoBid(AccountId bidder, AccountId manager) throws Exception {
		for (int i = 0; i < BIDDING_TIME_SEC; i += BID_INTERVAL_SEC) {
			long bidAmount = (i + 1) * BID_INCREMENT;
			TransactionRecord setRecord = bid(bidder, DEFAULT_AUCTION_CONTRACT_ID, bidAmount);
			log.info("bid iteration " + (i + 1) + " completed successfully==>");
		}

		// wait to end auction
		long elapse = System.currentTimeMillis() - start;
		long bidmillis = BIDDING_TIME_SEC * 1000;
		if (elapse <= bidmillis) {
			long sleep = bidmillis - elapse + 1000;
			log.info("sleep " + sleep + " millisec zzz ... ");
			Thread.sleep(sleep);
		}
		TransactionRecord setRecord = endAuction(manager, DEFAULT_AUCTION_CONTRACT_ID);
		log.info("end auction call completed successfully :), record = " + setRecord);
	}

	private TransactionRecord bid(AccountId bidder, ContractId contractId, long bidAmount) throws HederaNetworkException, HederaStatusException {
    TransactionId transactionId = new ContractExecuteTransaction()
        .setGas(30000)
        .setContractId(contractId)
        .setFunction("bid")
        .setPayableAmount(bidAmount)
        .execute(client);

    TransactionRecord record = transactionId.getRecord(client);
    log.info("bid call record = " + record);
    
    ContractFunctionResult contractCallResult = record.getContractExecuteResult();
    
    if (contractCallResult.errorMessage != null) {
        log.warn("error calling contract: " + contractCallResult.errorMessage);
    }

    return record;
	}

	public static void main(String args[]) throws Throwable {
	}

	public TransactionRecord bid(Bid bid) throws Exception {
		AccountId bidder = parseAccountID(bid.getBidderAddr());
		TransactionRecord rv = bid(bidder, parseContractID(bid.getContractAddr()), bid.getAmount());
		return rv;
	}

	public AccountId getAliceAccount() {
		return aliceAccountId;
	}

	public void setAliceAccount(AccountId aliceAccount) {
		this.aliceAccountId = aliceAccount;
	}

	public AccountId getBobAccount() {
		return bobAccountId;
	}

	public void setBobAccount(AccountId bobAccount) {
		this.bobAccountId = bobAccount;
	}

	/**
	 * Parse contract string in the form of "0.0.1001".
	 * 
	 * @param contract String
	 * @return
	 */
	public static ContractId parseContractID(String contractString) {
		String[] parts = contractString.split("\\.");
		ContractId contractId = new ContractId(Long.parseLong(parts[0]), Long.parseLong(parts[1]),
		    Long.parseLong(parts[2]));
		return contractId;
	}

	public static FileId parseFileID(String fileString) {
		String[] parts = fileString.split("\\.");
		FileId id = new FileId(Long.parseLong(parts[0]), Long.parseLong(parts[1]), Long.parseLong(parts[2]));
		return id;
	}
	
	public void localCall(String functionName) throws HederaNetworkException, HederaStatusException {
    ContractFunctionResult contractCallResult = new ContractCallQuery()
        .setGas(30000)
        .setContractId(DEFAULT_AUCTION_CONTRACT_ID)
        .setFunction(functionName)
        .execute(client);

    if (contractCallResult.errorMessage != null) {
        log.warn("error calling contract: " + contractCallResult.errorMessage);
        return;
    }

    String message = contractCallResult.getString(0);
    log.info("contract message: " + message);
		
	}

	public TransactionRecord startTimer(String managerAccount2, String contractId) throws HederaNetworkException, HederaStatusException {
    TransactionId transactionId = new ContractExecuteTransaction()
        .setGas(30000)
        .setContractId(parseContractID(contractId))
        .setFunction("startTimer")
        .execute(client);

    TransactionRecord record = transactionId.getRecord(client);
    log.info("startTimer call record = " + record);
    
    ContractFunctionResult contractCallResult = record.getContractExecuteResult();
    
    if (contractCallResult.errorMessage != null) {
        log.warn("error calling contract: " + contractCallResult.errorMessage);
    }

    return record;
	}

	public TransactionRecord resetAuction(String managerAccount2, String contractId) throws HederaNetworkException, HederaStatusException {
    TransactionId transactionId = new ContractExecuteTransaction()
        .setGas(30000)
        .setContractId(parseContractID(contractId))
        .setFunction("auctionReset")
        .execute(client);

    TransactionRecord record = transactionId.getRecord(client);
    log.info("auctionReset call record = " + record);
    
    ContractFunctionResult contractCallResult = record.getContractExecuteResult();
    
    if (contractCallResult.errorMessage != null) {
        log.warn("error calling contract: " + contractCallResult.errorMessage);
    }

    return record;
	}

	public TransactionRecord endAuction(String bidderAddr, String contractAddr) throws Exception {
		AccountId managerId = parseAccountID(bidderAddr);
		ContractId contractId = parseContractID(contractAddr);
		
		return endAuction(managerId, contractId);
	}

	
	public TransactionRecord endAuction(AccountId managerId, ContractId contractId) throws Exception {
    TransactionId transactionId = new ContractExecuteTransaction()
        .setGas(30000)
        .setContractId(contractId)
        .setFunction("auctionEnd")
        .execute(client);

    TransactionRecord record = transactionId.getRecord(client);
    log.info("auctionEnd call record = " + record);
    
    ContractFunctionResult contractCallResult = record.getContractExecuteResult();
    
    if (contractCallResult.errorMessage != null) {
        log.warn("error calling contract: " + contractCallResult.errorMessage);
    }

    return record;
	}

  protected static byte[] getEncodedConstructor(long biddingTime, byte[] beneficiary) {
    String funcJson = AUCTION_CONSTRUCTOR_ABI.replaceAll("'", "\"");
    CallTransaction.Function func = CallTransaction.Function.fromJsonInterface(funcJson);
    byte[] encodedFunc = func.encodeArguments(biddingTime, beneficiary);

    return encodedFunc;
  }

  protected byte[] convert2SolidityAddress(String accountStr) {
  	AccountId crAccount = parseAccountID(accountStr);
    byte[] solidityByteArray = new byte[20];
    byte[] indicatorBytes = ByteUtil.intToBytes(0);
    copyArray(0, solidityByteArray, indicatorBytes);
    byte[] realmNumBytes = ByteUtil.longToBytes(crAccount.realm);
    copyArray(4, solidityByteArray, realmNumBytes);
    byte[] accountNumBytes = ByteUtil.longToBytes(crAccount.account);
    copyArray(12, solidityByteArray, accountNumBytes);

    return solidityByteArray;
  }

  public static void copyArray(int startInToArray, byte[] toArray, byte[] fromArray) {
    if (fromArray == null || toArray == null) {
      return;
    }
    for (int i = 0; i < fromArray.length; i++) {
      toArray[i + startInToArray] = fromArray[i];
    }
  }

  public FileId uploadLargeFile(byte[] bytes) throws Throwable {
    int numParts = bytes.length / FILE_PART_SIZE;
    int remainder = bytes.length % FILE_PART_SIZE;
    log.info("@@@ file size=" + bytes.length + "; FILE_PART_SIZE=" + FILE_PART_SIZE + "; numParts="
        + numParts + "; remainder=" + remainder);

    byte[] firstPartBytes = null;
    if (bytes.length <= FILE_PART_SIZE) {
      firstPartBytes = bytes;
      remainder = 0;
    } else {
      firstPartBytes = CommonUtils.copyBytes(0, FILE_PART_SIZE, bytes);
    }

    // create file with first part
    FileId fid = createFile(firstPartBytes);
    log.info("@@@ created file with first part: fileID = " + fid);

    // append the rest of the parts
    int i = 1;
    for (; i < numParts; i++) {
      byte[] partBytes = CommonUtils.copyBytes(i * FILE_PART_SIZE, FILE_PART_SIZE, bytes);
      appendFile(fid, partBytes);
      log.info("@@@ append file count = " + i);
    }

    if (remainder > 0) {
      byte[] partBytes = CommonUtils.copyBytes(numParts * FILE_PART_SIZE, remainder, bytes);
      appendFile(fid, partBytes);
      log.info("@@@ append file count = " + i);
    }

    FileInfo fi = getFileInfo(fid);
    log.info("file info: file size = " + fi.size + ", acl = " + fi.keys);

    // get file content and save to disk
    byte[] content = getFileContent(fid);
    Assert.assertArrayEquals(bytes, content);
    return fid;
  }

	public byte[] getFileContent(FileId fid) throws HederaNetworkException, HederaStatusException {
    byte[] contents = new FileContentsQuery()
        .setFileId(fid)
        .execute(client);
		return contents;
	}

	public FileInfo getFileInfo(FileId fid) throws HederaNetworkException, HederaStatusException {
    FileInfo contents = new FileInfoQuery()
        .setFileId(fid)
        .execute(client);
		return contents;
	}

	public void appendFile(FileId fid, byte[] partBytes) throws HederaNetworkException, HederaStatusException {
		TransactionId fileTxId = new FileAppendTransaction()
				.setFileId(fid)
		    .setContents(partBytes)
		    .setMaxTransactionFee(200000000l)
		    .execute(client);

		TransactionReceipt fileReceipt = fileTxId.getReceipt(client);
		Status status = fileReceipt.status;
		log.info("append file receipt: " + fileReceipt);
		Assert.assertEquals(Status.Success, status);
	}

	public FileId createFile(byte[] bytes) throws HederaNetworkException, HederaStatusException {
		TransactionId fileTxId = new FileCreateTransaction()
		    .setExpirationTime(Instant.now().plus(HederaConstants.DEFAULT_AUTORENEW_DURATION))
		    // Use the same key as the operator to "own" this file
		    .addKey(OPERATOR_KEY.publicKey)
		    .setContents(bytes).setMaxTransactionFee(300000000l)
		    .execute(client);

		TransactionReceipt fileReceipt = fileTxId.getReceipt(client);
		FileId newFileId = fileReceipt.getFileId();
		log.info("contract bytecode file: " + newFileId);
		Assert.assertNotNull(newFileId);

//		FileId newFileId = new FileId(0, 0, 156384);
		
		byte[] downloadBytes = getFileContent(newFileId);
		Assert.assertArrayEquals(bytes, downloadBytes);
		return newFileId;
	}

}
