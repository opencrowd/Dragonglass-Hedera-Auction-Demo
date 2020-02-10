import * as api from '../apis';
import * as types from '../types';
// import * as constants from '../constants';

export const fetchBids = () => (dispatch, getState) =>
	api.fetchBids().then(
		(response) => {
			dispatch({
				type: types.FETCH_BIDS_SUCCESS,
				payload: response
			});
			return response;
		},
		(error) => {
			dispatch({
				type: types.FETCH_BIDS_FAILURE,
				message: error.message || 'Something went wrong.'
			});
		}
	);

export const fetchTransactions = (contractId, apiKey) => (dispatch, getState) =>
	api.fetchTransactions(contractId, apiKey).then(
		(response) => {
			dispatch({
				type: types.FETCH_TRANSACTIONS_SUCCESS,
				payload: response
			});
			console.log("RESPONSE IN ACTIONS: ", response)
			return response;
		},
		(error) => {
			dispatch({
				type: types.FETCH_TRANSACTIONS_FAILURE,
				message: error.message || 'Something went wrong.'
			});
		}
	);

const topicID = '0.0.168414';
const consensusStartInEpoch='1579576576000';

export const placeBid = (amount) => (dispatch, getState) => { 
	console.log("IN PLACE BID")
	return fetch(`http://localhost:8081/hcs/auction/bid/Alice/100/${topicID}`, {
		method: 'POST',
		headers: {
			'Accept': "application/json",
			'Content-Type': "application/json",
		},
	})
	.then(response => response.json())
	.then(json => {
		console.log("BID PLACED: ", json);
	},
	)
}

export const resetAuction = () => (dispatch, getState) => { 
	return fetch(`http://localhost:8081/hcs/auction/resetAuction/${topicID}`,
	{method: 'POST'},)
	.then(response => console.log(response));
}