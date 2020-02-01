import { combineReducers } from 'redux';
import transactions, * as fromTransactions from './transactions';
import users, * as fromUsers from './users';
import bid, * as fromBid from './bid';
const auctioneer = combineReducers({
	transactions, users, bid
});

export default auctioneer;

export const getTransactions = (state) => fromTransactions.getTransactions(state.transactions);
export const getUsers = (state) => fromUsers.getUsers(state.users);
export const getBid = (state) => fromBid.getBid(state.bid);
