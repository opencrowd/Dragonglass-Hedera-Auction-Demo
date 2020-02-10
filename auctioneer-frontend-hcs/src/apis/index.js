// import axios from 'axios';

// const extract = ({ data }) => data;

export const fetchBids = () => {
	// return axios.get(`/api/dapps/${id}`).then(extract);
	return Promise.resolve(bids);
};


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


const bids = [
	{
		account: '568721',
		amount: '27.00',
		time: '22:18:45'
	},
	{
		account: '287364',
		amount: '26.59',
		time: '21:45:22'
	},
	{
		account: '234071',
		amount: '26.00',
		time: '21:45:22'
	},
	{
		account: '132134',
		amount: '25.75',
		time: '21:45:22'
	},
	{
		account: '230873',
		amount: '25.50',
		time: '21:45:22'
	},
	{
		account: '123412',
		amount: '25.00',
		time: '21:45:22'
	},
	{
		account: '456456',
		amount: '24.00',
		time: '21:45:22'
	},
	{
		account: '916723',
		amount: '22.01',
		time: '21:45:22'
	},
	{
		account: '123098',
		amount: '21.00',
		time: '21:45:22'
	},
	{
		account: '237123',
		amount: '20.00',
		time: '21:45:22'
	}
];