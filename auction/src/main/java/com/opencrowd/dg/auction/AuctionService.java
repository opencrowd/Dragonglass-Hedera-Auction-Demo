package com.opencrowd.dg.auction;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.ethereum.core.CallTransaction;
import org.ethereum.util.ByteUtil;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
import com.hedera.hashgraph.sdk.crypto.PublicKey;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.file.FileAppendTransaction;
import com.hedera.hashgraph.sdk.file.FileContentsQuery;
import com.hedera.hashgraph.sdk.file.FileCreateTransaction;
import com.hedera.hashgraph.sdk.file.FileId;
import com.hedera.hashgraph.sdk.file.FileInfo;
import com.hedera.hashgraph.sdk.file.FileInfoQuery;

/**
 * The service that powers the rest API endpoints.
 * 
 */
@Service
public class AuctionService {
  private String beneficiaryAccount;
	private PublicKey managerPublicKey;
	private String DEFAULT_CONTRACT_FILE;
	private String hederaNetwork;

	private AccountId aliceAccountId;
	private AccountId bobAccountId;
	private AccountId carolAccountId;
	private AccountId managerAccountId;
	
	private long start = System.currentTimeMillis();
	private int BID_INTERVAL_SEC = 2;
	private int BID_INCREMENT = 10;

	private long defaultContract;
	private FileId AUCTION_BIN_FILE_ID;
	private long BIDDING_TIME_SEC = 30;
	private ContractId DEFAULT_AUCTION_CONTRACT_ID = new ContractId(0, 0, defaultContract);
	private String AUCTION_FILE_NAME = "auctionTimer_sol_SimpleAuction.bin";

	private final static Logger log = LogManager.getLogger(AuctionService.class);
	
  private static final String AUCTION_CONSTRUCTOR_ABI =
      "{\"inputs\":[{\"internalType\":\"uint256\",\"name\":\"_biddingTime\",\"type\":\"uint256\"},{\"internalType\":\"address payable\",\"name\":\"_beneficiary\",\"type\":\"address\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"}";
  private static int FILE_PART_SIZE = 4096; //4K bytes
  private Map<AccountId, Client> clients = new HashMap<>();

	@Autowired
	public AuctionService(
      @Value("${hedera.network}") String hederaNetwork,
      @Value("${hedera.account.Alice.ID}") String aliceAccount,
      @Value("${hedera.account.Alice.KEY}") String aliceKey,
      @Value("${hedera.account.Bob.ID}") String bobAccount,
      @Value("${hedera.account.Bob.KEY}") String bobKey,
      @Value("${hedera.account.Carol.ID}") String carolAccount,
      @Value("${hedera.account.Carol.KEY}") String carolKey,
      @Value("${hedera.account.manager.ID}") String managerAccount,
      @Value("${hedera.account.manager.KEY}") String managerKey,
      @Value("${hedera.account.manager.PUBLIC_KEY}") String managerPublicKey,
      @Value("${hedera.account.beneficiary.ID}") String beneficiaryAccount,
	    @Value("${hedera.contract.bin.file:}") String DEFAULT_CONTRACT_FILE,
	    @Value("${hedera.contract.default:}") String DEFAULT_CONTRACT) throws Throwable {
		
		this.hederaNetwork = hederaNetwork;
		this.managerPublicKey = PublicKey.fromString(managerPublicKey);
		this.DEFAULT_CONTRACT_FILE = DEFAULT_CONTRACT_FILE;
		aliceAccountId = parseAccountID(aliceAccount);
		bobAccountId = parseAccountID(bobAccount);
		carolAccountId = parseAccountID(carolAccount);
		managerAccountId = parseAccountID(managerAccount);
		this.beneficiaryAccount = beneficiaryAccount;
		parseAccountID(beneficiaryAccount);

		addClient(aliceAccountId, aliceKey);
		addClient(bobAccountId, bobKey);
		addClient(carolAccountId, carolKey);
		addClient(managerAccountId, managerKey);
		
		createAuctionContractFile();
	}

	/**
	 * Adds a client.
	 * 
	 * @param accoutId the account ID used by the client for payment 
	 * @param accountKey the correspoinding private key of the account
	 */
	private void addClient(AccountId accoutId, String accountKey) {
		Client client = null;
		if(hederaNetwork.equals("mainnet"))
			client = Client.forMainnet();
		else // testnet
			client = Client.forTestnet();

		Ed25519PrivateKey privateKey = Ed25519PrivateKey
		    .fromString(Objects.requireNonNull(accountKey));
		client.setOperator(accoutId, privateKey);
		clients.put(accoutId, client);
	}

	/**
	 * Gets the client by account ID.
	 */
	private Client getClient(AccountId accountId) {
		return clients.get(accountId);
	}
	
	/**
	 * Convert account ID from string form to object.
	 * 
	 */
	public static AccountId parseAccountID(String accountStr) {
		String[] parts = accountStr.split("\\.");
		AccountId id = new AccountId(Long.parseLong(parts[0]), Long.parseLong(parts[1]), Long.parseLong(parts[2]));
		return id;
	}

	/**
	 * Creates a file for the binary file of the auction contract on the Hedera network.
	 *  
	 * @throws Throwable
	 */
	public void createAuctionContractFile() throws Throwable {
		if (DEFAULT_CONTRACT_FILE == null || DEFAULT_CONTRACT_FILE.isBlank())
			uploadBinFile(managerAccountId);
		else
			AUCTION_BIN_FILE_ID = parseFileID(DEFAULT_CONTRACT_FILE);
	}

	/**
	 * Upload a binary file to Hedera network.
	 *  
	 * @param mangerAccount the account pays for the upload and owns the file
	 * @throws Throwable
	 */
	public void uploadBinFile(AccountId mangerAccount) throws Throwable {
		byte[] bytes = CommonUtils.readBinaryFileAsResource(AUCTION_FILE_NAME, getClass());
		log.info("bin file = " + AUCTION_FILE_NAME + ", size in bytes = " + bytes.length);

		FileId newFileId = uploadLargeFile(bytes);
		
		AUCTION_BIN_FILE_ID = newFileId;
		log.info(":) Auction bin file created successfully, file ID = " + newFileId);
	}
	
	/**
	 * Download the content of the given file from Hedera network.
	 *  
	 * @param fid file to get
	 * @return the hex of the downloaded content
	 * @throws HederaNetworkException
	 * @throws HederaStatusException
	 */
	public String getFileContentHex(FileId fid) throws HederaNetworkException, HederaStatusException {
    byte[] contents = getFileContent(fid);

    String hex = Hex.encodeHexString(contents);

    log.info("File content size = " + contents.length + ", hex=" + hex);
    return hex;
	}

	/**
	 * Create a auction contract instance on Hedera network.
	 * 
	 * @param beneficiaryStr beneficiary account ID in the form of 0.0.x. The winner's bid amount will be transfered from the bidder's account to this account at the end of the auction.
	 * @param biddingTime the duration of auction in seconds
	 * @return the contract ID of the created instance
	 * @throws HederaNetworkException
	 * @throws HederaStatusException
	 */
	public ContractId createAuction(String beneficiaryStr, long biddingTime) throws HederaNetworkException, HederaStatusException {
    if (beneficiaryStr == null || beneficiaryStr.isBlank()) {
    	beneficiaryStr = beneficiaryAccount;
    }
		Client client = getClient(managerAccountId);
    byte[] beneficiary = convert2SolidityAddress(beneficiaryStr);
    byte[] constructorParams = getEncodedConstructor(biddingTime, beneficiary);
		TransactionId contractTxId = new ContractCreateTransaction()
		    .setAutoRenewPeriod(HederaConstants.DEFAULT_AUTORENEW_DURATION).setGas(217000).setBytecodeFileId(AUCTION_BIN_FILE_ID)
		    .setContractMemo("OpenCrowd Dragonglass Auction Demo Contract")
		    .setConstructorParams(constructorParams )
		    // set an admin key so we can delete the contract later
		    .setAdminKey(managerPublicKey)
		    .setMaxTransactionFee(1500000000l)
		    .execute(client);

		TransactionReceipt contractReceipt = contractTxId.getReceipt(client);
		log.info(contractReceipt.toProto());
		ContractId newContractId = contractReceipt.getContractId();
		Assert.assertNotNull(newContractId);
		DEFAULT_AUCTION_CONTRACT_ID = newContractId;
		log.info(":) Auction contract created successfully, contract ID = " + newContractId);
		return newContractId;
	}

	/**
   * Make a single bid contract call.
   * 
   * @param bidder the account ID of the bidder
   * @param contractId the contract ID in the form of 0.0.x
   * @param bidAmount the bidding amount in tiny bars
   * @return the transaction record of the call
	 * @throws HederaNetworkException
	 * @throws HederaStatusException
	 */
	public TransactionRecord bid(AccountId bidder, ContractId contractId, long bidAmount) throws HederaNetworkException, HederaStatusException {
		Client client = getClient(bidder);
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

	/**
	 * Makes a bid contract call.
	 * 
	 * @param bid the Bid object used for the call
   * @return the transaction record of the call
	 * @throws Exception
	 */
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
	 * Parse contract string in the form of "0.0.x".
	 * 
	 * @param contractString contract in string form
	 * @return converted contract ID
	 */
	public static ContractId parseContractID(String contractString) {
		String[] parts = contractString.split("\\.");
		ContractId contractId = new ContractId(Long.parseLong(parts[0]), Long.parseLong(parts[1]),
		    Long.parseLong(parts[2]));
		return contractId;
	}

	/**
	 * Parse file string in the form of "0.0.x".
	 * 
	 * @param fileString the file ID in String form
	 * @return converted contract ID
	 */
	public static FileId parseFileID(String fileString) {
		String[] parts = fileString.split("\\.");
		FileId id = new FileId(Long.parseLong(parts[0]), Long.parseLong(parts[1]), Long.parseLong(parts[2]));
		return id;
	}
	
	/**
	 * Signals the start of the auction.
	 * 
	 * @param contractId the contract instance to start
	 * @return transaction record of the call
	 * @throws HederaNetworkException
	 * @throws HederaStatusException
	 */
	public TransactionRecord startTimer(String contractId) throws HederaNetworkException, HederaStatusException {
		Client client = getClient(managerAccountId);
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

	/**
	 * Reset the state of the auction.
	 * 
	 * @param contractId the contract instance to reset
	 * @return transaction record of the call
	 * @throws HederaNetworkException
	 * @throws HederaStatusException
	 */
	public TransactionRecord resetAuction(String contractId) throws HederaNetworkException, HederaStatusException {
		Client client = getClient(managerAccountId);
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

	/**
	 * Make a contract call to end an auction.
	 * 
   * @param bidderAddr the account ID of the form 0.0.x of the calling user
   * @param contractAddr the contract ID in the form of 0.0.x
   * @return the call transaction record
	 * @throws Exception
	 */
	public TransactionRecord endAuction(String bidderAddr, String contractAddr) throws Exception {
		AccountId managerId = parseAccountID(bidderAddr);
		ContractId contractId = parseContractID(contractAddr);
		
		return endAuction(managerId, contractId);
	}

	
	/**
	 * Make a contract call to end an auction.
	 * 
   * @param managerId the account ID of the calling user
   * @param contractId the contract ID
   * @return the call transaction record
	 * @throws Exception
	 */
	public TransactionRecord endAuction(AccountId managerId, ContractId contractId) throws Exception {
		Client client = getClient(managerAccountId);
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

	/**
	 * Get the encoded constructor parameters.
	 * 
	 * @param biddingTime auction duration
	 * @param beneficiary the solidity address of the beneficiary account
	 * @return the encoded constructor parameters
	 */
	public static byte[] getEncodedConstructor(long biddingTime, byte[] beneficiary) {
    String funcJson = AUCTION_CONSTRUCTOR_ABI.replaceAll("'", "\"");
    CallTransaction.Function func = CallTransaction.Function.fromJsonInterface(funcJson);
    byte[] encodedFunc = func.encodeArguments(biddingTime, beneficiary);

    return encodedFunc;
  }

	/**
	 * Convert an Hedera account ID to solidity address.
	 * 
	 * @param accountStr Hedera account ID in string form
	 * @return converted solidity address
	 */
	public byte[] convert2SolidityAddress(String accountStr) {
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

	/**
	 * Copy array data.
	 * 
	 * @param startInToArray start position
	 * @param toArray destination array
	 * @param fromArray source array
	 */
  public static void copyArray(int startInToArray, byte[] toArray, byte[] fromArray) {
    if (fromArray == null || toArray == null) {
      return;
    }
    for (int i = 0; i < fromArray.length; i++) {
      toArray[i + startInToArray] = fromArray[i];
    }
  }

  /**
   * Uploads a large file to Hedera network, which may require splitting the file and make multiple file append calls.
   * 
   * @param bytes file content
   * @return the file ID of the uploaded file
   * @throws Throwable
   */
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

    byte[] content = getFileContent(fid);
    Assert.assertArrayEquals(bytes, content);
    return fid;
  }

  /**
   * Get the file content from Hedera network.
   * 
   * @param fid file to get
   * @return the content bytes
   * @throws HederaNetworkException
   * @throws HederaStatusException
   */
	public byte[] getFileContent(FileId fid) throws HederaNetworkException, HederaStatusException {
		Client client = getClient(managerAccountId);
    byte[] contents = new FileContentsQuery()
        .setFileId(fid)
        .execute(client);
		return contents;
	}

	/**
	 * Get file info from Hedera network.
	 * 
	 * @param fid file to get
	 * @return FileInfo object
	 * @throws HederaNetworkException
	 * @throws HederaStatusException
	 */
	public FileInfo getFileInfo(FileId fid) throws HederaNetworkException, HederaStatusException {
		Client client = getClient(managerAccountId);
    FileInfo contents = new FileInfoQuery()
        .setFileId(fid)
        .execute(client);
		return contents;
	}

	/**
	 * Appends data to an existing file in the Hedera network.
	 * 
	 * @param fid file to append
	 * @param bytes data to append
	 * @throws HederaNetworkException
	 * @throws HederaStatusException
	 */
	public void appendFile(FileId fid, byte[] bytes) throws HederaNetworkException, HederaStatusException {
		Client client = getClient(managerAccountId);
		TransactionId fileTxId = new FileAppendTransaction()
				.setFileId(fid)
		    .setContents(bytes)
		    .setMaxTransactionFee(200000000l)
		    .execute(client);

		TransactionReceipt fileReceipt = fileTxId.getReceipt(client);
		Status status = fileReceipt.status;
		log.info("append file receipt: " + fileReceipt);
		Assert.assertEquals(Status.Success, status);
	}

	/**
	 * Create a new file on the Hedera network.
	 * 
	 * @param bytes the content of the file
	 * @return created file ID
	 * @throws HederaNetworkException
	 * @throws HederaStatusException
	 */
	public FileId createFile(byte[] bytes) throws HederaNetworkException, HederaStatusException {
		Client client = getClient(managerAccountId);
		TransactionId fileTxId = new FileCreateTransaction()
		    .setExpirationTime(Instant.now().plus(HederaConstants.DEFAULT_AUTORENEW_DURATION))
		    // Use the same key as the operator to "own" this file
		    .addKey(managerPublicKey)
		    .setContents(bytes).setMaxTransactionFee(300000000l)
		    .execute(client);

		TransactionReceipt fileReceipt = fileTxId.getReceipt(client);
		FileId newFileId = fileReceipt.getFileId();
		log.info("contract bytecode file: " + newFileId);
		Assert.assertNotNull(newFileId);
		
		byte[] downloadBytes = getFileContent(newFileId);
		Assert.assertArrayEquals(bytes, downloadBytes);
		return newFileId;
	}

}
