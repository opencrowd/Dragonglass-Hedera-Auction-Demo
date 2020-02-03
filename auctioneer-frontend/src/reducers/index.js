import { combineReducers } from 'redux';
import bids, * as fromBids from './bids';
import transactions, * as fromTransactions from './transactions';
import users, * as fromUsers from './users';
import bid, * as fromBid from './bid';
const auctioneer = combineReducers({
	bids, transactions, users, bid
});

export default auctioneer;

export const getBids = (state) => fromBids.getBids(state.bids);
export const getTransactions = (state) => fromTransactions.getTransactions(state.transactions);
export const getUsers = (state) => fromUsers.getUsers(state.users);
export const getBid = (state) => fromBid.getBid(state.bid);
