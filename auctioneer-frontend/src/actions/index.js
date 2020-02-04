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

const contractId = '0.0.161470';
const consensusStartInEpoch='1579576576000';

//Fetch using DragonGlass RESTful API to fetch historical Calls made on the Contract
//Input parameters used for the call
//contractId : Auction contract ID on the network
//contractMethodName : function name called( bid )
//consensusStartInEpoch : send calls after the time
export const fetchEvents = (apiKey) => (dispatch, getState) => {
	return fetch(`https://api-testnet.dragonglass.me/hedera/api/contracts/${contractId}/calls?contractMethodName=1998aeef&consensusStartInEpoch=${consensusStartInEpoch}&status=SUCCESS&from=0&size=100&sortBy=desc`, {
	method: 'GET',
	headers: {
		'x-api-key': apiKey,
	}
	})
	.then(response => response.json())
	.then(json => {
		let events = [];
		if(json.data){
      json.data.forEach(function(item){
      let event = {};
      event["consensusTime"] = item.consensusTime;
      if(item.parsedEvents){
        event["account"] = item.parsedEvents[0].inputValues[0];
        event["amount"] = item.parsedEvents[0].inputValues[1];
        events.push(event);
        }
      });
		}
		dispatch({type: "HIGHESTBID",
    		payload: events && events[0] ? events[0].amount : 0});
		return (dispatch({type: types.FETCH_TRANSACTIONS_SUCCESS,
		payload: events}))
	},
	(error) => {
		dispatch({
			type: types.FETCH_TRANSACTIONS_FAILURE,
			message: error.message || 'Something went wrong.'
		});
	}
	)
}

export const placeBid = (amount) => (dispatch, getState) => { 
	console.log("IN PLACE BID")
	return fetch(`http://localhost:8081/bid/Alice/${amount}/${contractId}`, {
		method: 'POST',
		headers: {
			'Accept': "application/json",
			'Content-Type': "application/json",
		},
	})
	.then(response => response.json())
	.then(json => {
		console.log("BID PLACED: ", json.data);
	},
	)
}

export const fetchUsers = () => (dispatch, getState) => { 
	return fetch(`http://localhost:8081/bidders`)
	.then(response => response.json())
	.then(json => {
		return (dispatch({
		type: types.FETCH_USERS_SUCCESS,
		payload: json}))
	},
	(error) => {
		dispatch({
			type: types.FETCH_USERS_FAILURE,
			message: error.message || 'Something went wrong.'
		});
	}
	)
}

export const resetAuction = () => (dispatch, getState) => { 
	return fetch(`http://localhost:8081/startTimer/${contractId}`,
	{method: 'POST'},)
	.then(response => console.log(response));
}