import { createStore, applyMiddleware, compose } from 'redux';
import thunk from 'redux-thunk';
import auctioneer from './reducers';

const composeEnhancer = window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__ || compose;

const configureStore = () => {
	const middlewares = [ thunk ];

	return createStore(auctioneer, composeEnhancer(applyMiddleware(...middlewares)));
};

export default configureStore;