export default function bid(state = 0, action) {
	switch (action.type) {
		case "HIGHESTBID":
			return action.payload;
		default:
			return state;
	}
}

export const getBid = (state) => state;