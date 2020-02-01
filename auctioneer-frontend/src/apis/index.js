export const fetchTransactions = (contractID, apiKey) => { 
	return fetch(`https://api-testnet.dragonglass.me/hedera/api/transactions?contractID=${contractID}&transactionTypes=CONTRACT_CALL`, {
	method: 'GET',
	headers: {
		'x-api-key': apiKey,
	}
	}).then(response => response.json())
	.then(json => {
		console.log("DATA IN APIS: ", json.data);
		return json.data;
	})
}
