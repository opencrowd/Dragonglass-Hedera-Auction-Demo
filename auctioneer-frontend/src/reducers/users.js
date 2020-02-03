import { FETCH_USERS_SUCCESS, FETCH_USERS_FAILURE } from '../types';

export default function users(state = [], action) {
	switch (action.type) {
		case FETCH_USERS_SUCCESS:
            // console.log("PAYLOAD: ", action.payload)
			return [ ...state, action.payload];
		case FETCH_USERS_FAILURE:
            console.log("USERS FETCH FAILURE")
			return [
                        {
                        "0.0.69102": "Bob",
                        "0.0.2695": "Carol",
                        "0.0.112224": "Alice",
                        "0.0.2697": "David"
                        }
                    ];
		default:
			return state;
	}
}

export const getUsers = (state) => state;