package com.opencrowd.dg.auction;

public class AuctionEnd {
	private final long id;
	private final String bidder;
	private final String bidderAddr;
	private final String contractAddr;

	public AuctionEnd(long id, String bidder, String bidderAddr, String contractAddr) {
		super();
		this.id = id;
		this.bidder = bidder;
		this.bidderAddr = bidderAddr;
		this.contractAddr = contractAddr;
	}
	
	@Override
	public String toString() {
		return "AuctionEnd [id=" + id + ", bidder=" + bidder + ", bidderAddr=" + bidderAddr + ", contractAddr="
		    + contractAddr + "]";
	}

	public long getId() {
		return id;
	}
	public String getBidder() {
		return bidder;
	}
	public String getBidderAddr() {
		return bidderAddr;
	}
	public String getContractAddr() {
		return contractAddr;
	}
	
}
