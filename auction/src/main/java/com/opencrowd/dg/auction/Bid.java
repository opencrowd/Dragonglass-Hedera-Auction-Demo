package com.opencrowd.dg.auction;

public class Bid {
	private final long id;
	private final String bidder;
	private final long amount;
	private final String bidderAddr;
	private final String contractAddr;

	public Bid(long id, String bidder, long amount, String bidderAddr, String contractAddr) {
		super();
		this.id = id;
		this.bidder = bidder;
		this.amount = amount;
		this.bidderAddr = bidderAddr;
		this.contractAddr = contractAddr;
	}
	
	public long getId() {
		return id;
	}
	public String getBidder() {
		return bidder;
	}
	public long getAmount() {
		return amount;
	}
	public String getBidderAddr() {
		return bidderAddr;
	}
	public String getContractAddr() {
		return contractAddr;
	}

	@Override
	public String toString() {
		return "Bid [id=" + id + ", bidder=" + bidder + ", amount=" + amount + ", bidderAddr=" + bidderAddr
		    + ", contractAddr=" + contractAddr + "]";
	}
	
}
